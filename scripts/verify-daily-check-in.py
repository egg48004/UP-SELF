"""Verify global questionnaire links on local development services.
Creates only qtest_ accounts/links and removes only those fixtures. Audit records
are retained. Use --browser for the prepared Chrome acceptance workflow.
"""
from pathlib import Path
import argparse, concurrent.futures, json, os, re, secrets, subprocess, sys, urllib.error, urllib.request

R = Path(__file__).resolve().parents[1]
W = R / '.local/checkin-verification'
W.mkdir(parents=True, exist_ok=True)
p = argparse.ArgumentParser()
p.add_argument('--pymysql-path', help='Optional existing test-only client directory')
p.add_argument('--browser', action='store_true')
args = p.parse_args()
C = json.loads((R / 'migration/local-snapshot/local-credentials.json').read_text(encoding='utf-8-sig'))
if args.pymysql_path:
    sys.path.insert(0, args.pymysql_path)
    import pymysql
BASE = 'http://127.0.0.1:8080'
prefix = 'daily_'  + secrets.token_hex(4) + '_'
checks, users, actors = [], [], {}
last_headers = {}

def check(label, condition):
    assert condition, label
    checks.append(label)
    print('PASS', label, flush=True)

def request(path, method='GET', body=None, token=None):
    global last_headers
    headers = {'Content-Type': 'application/json'}
    if token:
        headers['Authorization'] = 'Bearer ' + token
    req = urllib.request.Request(BASE + path, data=json.dumps(body).encode() if body is not None else None, method=method, headers=headers)
    try:
        response = urllib.request.urlopen(req, timeout=25)
    except urllib.error.HTTPError as error:
        response = error
    with response:
        last_headers = dict(response.headers)
        return json.load(response)

def call(label, path, method='GET', body=None, token=None, code=200):
    result = request(path, method, body, token)
    assert result.get('code') == code, (label, result.get('code'), result.get('msg'))
    check(label, True)
    return result

def sql(statement):
    if args.pymysql_path:
        conn = pymysql.connect(host='127.0.0.1', port=3306, user='root', password=C['mysql_root_password'], database='up_self_dev', charset='utf8mb4', autocommit=True)
        try:
            with conn.cursor() as cursor:
                cursor.execute(statement)
                return list(cursor.fetchall())
        finally:
            conn.close()
    saved = R / '.local/service-tools.json'
    if saved.exists():
        home = Path(json.loads(saved.read_text(encoding='utf-8-sig'))['mysqlHome'])
    else:
        home = Path(json.loads((R / '.local/development-tools.json').read_text(encoding='utf-8-sig'))['serviceHome']) / 'mysql'
    cfg = W / 'root-client.cnf'
    cfg.write_text('[client]\nuser=root\npassword="' + C['mysql_root_password'].replace('\\', '\\\\').replace('"', '\\"') + '"\nhost=127.0.0.1\nport=3306\nprotocol=tcp\n', encoding='utf-8')
    result = subprocess.run([str(home / 'bin/mysql.exe'), '--defaults-extra-file=' + str(cfg), '--batch', '--skip-column-names', '--default-character-set=utf8mb4', 'up_self_dev', '-e', statement], capture_output=True, text=True, encoding='utf-8')
    if result.returncode:
        raise RuntimeError('Local SQL check failed; no credentials were printed')
    return [tuple(row.split('\t')) for row in result.stdout.splitlines()]

def scalar(statement):
    return int(sql(statement)[0][0])


def main():
    import datetime
    password=re.search(r'username: admin01\s+password: "([^"]+)"',(R/'config/demo-accounts.yml').read_text(encoding='utf-8-sig')).group(1)
    admin=call('admin login','/empLogin','POST',{'username':'admin01','password':password})['token']
    actors['admin']={'username':'admin01','password':password,'token':admin}
    for name,role in [('student',10),('second',10),('mentor',20)]:
        pwd=secrets.token_urlsafe(18);uid=call('create '+name,'/system/user','POST',{'userName':prefix+name,'nickName':name,'roleId':role,'password':pwd},admin)['data'];users.append(uid)
        token=call('login '+name,'/client/login' if role==10 else '/empLogin','POST',{'username':prefix+name,'password':pwd})['token'];actors[name]={'id':uid,'username':prefix+name,'password':pwd,'token':token}
    p='/client/learning/check-in';st=actors['student']['token'];uid=actors['student']['id']
    call('anonymous denied',p,code=401)
    call('mentor channel denied',p,token=actors['mentor']['token'],code=401)
    call('admin channel denied',p,token=admin,code=401)
    v=call('initial state',p,token=st)['data'];assert not v['checkedIn'] and v['streak']==0 and not v['history']
    today=datetime.datetime.now(datetime.timezone(datetime.timedelta(hours=8))).date();assert v['today']==str(today)
    for offset in [1,2,4,40]:sql("INSERT INTO student_daily_check_in(student_id,check_date) VALUES (%d,'%s')"%(uid,today-datetime.timedelta(days=offset)))
    assert call('yesterday streak retained',p,token=st)['data']['streak']==2
    with concurrent.futures.ThreadPoolExecutor(4) as pool:responses=list(pool.map(lambda _:request(p,'POST',{},st),range(4)))
    check('concurrent idempotent check-in',all(x['code']==200 and x['data']['streak']==3 for x in responses))
    assert scalar("SELECT COUNT(*) FROM student_daily_check_in WHERE student_id=%d AND check_date='%s'"%(uid,today))==1
    other=call('other student is private',p,token=actors['second']['token'])['data'];assert not other['history']
    state=call('30 day history',p,token=st)['data'];assert len(state['history'])==4
    (W/'fixture.json').write_text(json.dumps({'actors':actors,'today':str(today)}),encoding='utf-8')
    if args.browser:
        env=os.environ.copy();env['UPSELF_DAILY_FIXTURE']=str(W/'fixture.json');tools=json.loads((R/'.local/development-tools.json').read_text(encoding='utf-8-sig'))
        result=subprocess.run([str(Path(tools['nodeHome'])/'node.exe'),str(R/'frontend/node_modules/@playwright/test/cli.js'),'test','tests/e2e/daily-check-in.spec.ts'],cwd=R/'frontend',env=env);check('Chrome check-in workflow',result.returncode==0)
    (W/'result.json').write_text(json.dumps({'checks':checks,'passed':True}),encoding='utf-8')
def cleanup():
    for uid in users:
        sql('DELETE FROM student_daily_check_in WHERE student_id='+str(uid))
        sql('DELETE FROM sys_user WHERE id='+str(uid)+" AND user_name LIKE '"+prefix+"%'")
try:main()
finally:cleanup()
