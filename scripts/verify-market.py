"""Disposable local FR-47 acceptance. Does not change existing courses or credentials."""
from pathlib import Path
import argparse, concurrent.futures, json, os, re, secrets, subprocess, sys, urllib.error, urllib.request

R = Path(__file__).resolve().parents[1]
W = R / '.local/market-verification'
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
prefix = 'market_'  + secrets.token_hex(4) + '_'
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


courses=[]
ids=[]
def main():
    check('V22 migration',scalar("SELECT COUNT(*) FROM flyway_schema_history WHERE version='22' AND success=1")==1)
    password=re.search(r'username: admin01\s+password: "([^"]+)"',(R/'config/demo-accounts.yml').read_text(encoding='utf-8-sig')).group(1)
    admin=call('administrator login','/empLogin','POST',{'username':'admin01','password':password})['token'];actors['admin']={'username':'admin01','password':password,'token':admin}
    for name,role in [('mentor',20),('othermentor',20),('student',10),('second',10),('outsider',10)]:
        pwd=secrets.token_urlsafe(18);uid=call('create '+name,'/system/user','POST',{'userName':prefix+name,'nickName':name,'roleId':role,'password':pwd},admin)['data'];users.append(uid)
        token=call('login '+name,'/client/login' if role==10 else '/empLogin','POST',{'username':prefix+name,'password':pwd})['token'];actors[name]={'id':uid,'username':prefix+name,'password':pwd,'token':token}
    m=actors['mentor']['token'];st=actors['student']['token'];second=actors['second']['token'];other=actors['othermentor']['token']
    c=call('create course','/learning/courses','POST',{'title':prefix,'description':'Market scenario acceptance'},admin)['data'];courses.append(c);cp='/learning/courses/'+str(c)
    call('assign mentor',cp+'/mentor','PUT',{'mentorId':actors['mentor']['id']},admin)
    for name in ['student','second']:call('enroll '+name,cp+'/students','POST',{'studentId':actors[name]['id']},admin)
    call('publish course',cp+'/status','PUT',{'status':'PUBLISHED'},admin)
    content={'courseId':c,'title':'Late supplier delivery','objective':'Consider the customer and delivery options.','situation':'Your customer needs the items on Friday. Your supplier can deliver on Saturday.','question':'What would you do first?','reflectionPrompt':'What would you try differently?','version':0,'options':[{'label':'Contact the customer','outcome':'The customer can consider another delivery date.','explanation':'Early communication gives the customer time to decide.'},{'label':'Wait without telling the customer','outcome':'The customer may not receive the items in time.','explanation':'Waiting leaves less time to find another solution.'}]}
    call('anonymous denied','/client/market-tasks',code=401)
    call('student cannot author','/client/market-tasks','POST',content,st,403)
    call('admin cannot use mentor wildcard','/market-tasks','POST',content,admin,403)
    call('other mentor cannot author here','/market-tasks','POST',content,other,403)
    for invalid in [{**content,'options':content['options'][:1]},{**content,'title':' '},{**content,'options':content['options']*2}]:call('invalid content rejected','/market-tasks','POST',invalid,m,400)
    duplicate={**content,'options':[content['options'][0],content['options'][0]]}
    call('duplicate labels rejected','/market-tasks','POST',duplicate,m,400)
    task=call('mentor creates draft','/market-tasks','POST',content,m)['data'];ids.append(task);p='/market-tasks/'+str(task);sp='/client'+p
    check('draft default',call('mentor detail',p,token=m)['data']['status']=='DRAFT')
    call('draft unavailable to student',sp,token=st,code=404)
    check('draft absent in student list',call('student list','/client/market-tasks?courseId='+str(c),token=st)['total']==0)
    content['objective']='Practise clear communication.';call('edit unused task',p,'PUT',content,m)
    call('stale edit rejected',p,'PUT',content,m,409)
    call('publish task',p+'/status','PUT',{'status':'PUBLISHED','version':1},m)
    detail=call('student reads published task',sp,token=st)['data'];options=detail['options']
    check('outcomes withheld before choice',all(o.get('outcome') is None and o.get('explanation') is None for o in options))
    check('private responses no-store','no-store' in last_headers.get('Cache-Control',''))
    call('unrelated student denied',sp,token=actors['outsider']['token'],code=403)
    call('other mentor denied',p,token=other,code=403)
    call('admin cannot see participants',p+'/participants',token=admin,code=403)
    call('student cannot see participants',sp+'/participants',token=st,code=403)
    call('student cannot request other history',sp+'/attempts?studentId='+str(actors['second']['id']),token=st,code=403)
    import uuid
    request_body={'version':2,'optionId':options[0]['id'],'requestKey':str(uuid.uuid4()),'studentId':actors['second']['id']}
    call('foreign option denied',sp+'/attempts','POST',{**request_body,'optionId':999999999},st,400)
    with concurrent.futures.ThreadPoolExecutor(3) as pool:responses=list(pool.map(lambda _:request(sp+'/attempts','POST',request_body,st),range(3)))
    check('concurrent duplicate attempts idempotent',all(x['code']==200 for x in responses) and len({x['data']['id'] for x in responses})==1)
    first=responses[0]['data'];first_id=first['id']
    check('selected outcome returned',first['outcome']==content['options'][0]['outcome'] and first['firstAttempt'])
    check('owner from session',scalar('SELECT student_id FROM market_attempt WHERE id='+str(first_id))==actors['student']['id'])
    check('student response omits identity',first.get('studentId') is None and first.get('requestKey') is None)
    locked=call('mentor sees content lock',p,token=m)['data'];check('locked flag',locked['locked'])
    content['version']=2;call('cannot overwrite participated content',p,'PUT',content,m,409)
    call('cannot hide participated history as draft',p+'/status','PUT',{'version':2,'status':'DRAFT'},m,409)
    call('other student cannot reflect',sp+'/attempts/'+str(first_id)+'/reflection','PUT',{'version':0,'reflection':'Foreign edit'},second,404)
    call('reflection filter',sp+'/attempts/'+str(first_id)+'/reflection','PUT',{'version':0,'reflection':'kill yourself'},st,422)
    call('save reflection',sp+'/attempts/'+str(first_id)+'/reflection','PUT',{'version':0,'reflection':'I would contact the customer early.'},st)
    call('stale reflection denied',sp+'/attempts/'+str(first_id)+'/reflection','PUT',{'version':0,'reflection':'overwrite'},st,409)
    request_body.update(optionId=options[1]['id'],requestKey=str(uuid.uuid4()))
    later=call('retry creates separate attempt',sp+'/attempts','POST',request_body,st)['data']
    check('retry is not first',not later['firstAttempt'] and later['id']!=first_id)
    history=call('own paginated history',sp+'/attempts?current=1&pageSize=1',token=st)
    check('history totals and page',history['total']==2 and len(history['rows'])==1)
    rows=call('mentor participant list',p+'/participants?current=1&pageSize=1',token=m)
    check('participant total includes not started',rows['total']==2)
    first_row=rows['rows'][0];check('first choice preserved in mentor overview',first_row['firstChoice']==content['options'][0]['label'] and first_row['attemptCount']==2)
    own=call('mentor reads student history',p+'/attempts?studentId='+str(actors['student']['id']),token=m)['rows']
    check('mentor sees reflection and first attempt',any(x['firstAttempt'] and x['reflection']=='I would contact the customer early.' for x in own))
    call('close task',p+'/status','PUT',{'version':2,'status':'CLOSED'},m)
    call('new attempt blocked when closed',sp+'/attempts','POST',{**request_body,'version':3,'requestKey':str(uuid.uuid4())},st,409)
    check('closed history readable',call('closed history',sp+'/attempts',token=st)['total']==2)
    call('closed reflection read-only',sp+'/attempts/'+str(first_id)+'/reflection','PUT',{'version':1,'reflection':'later'},st,409)
    copy=call('copy participated task','/market-tasks/'+str(task)+'/copy','POST',token=m)['data'];ids.append(copy)
    copied=call('copied task detail','/market-tasks/'+str(copy),token=m)['data'];check('copy is independent draft',copied['status']=='DRAFT' and not copied['locked'] and copied['version']==0 and copied['options'][0]['id']!=options[0]['id'])
    call('cannot pick another task option',sp+'/attempts','POST',{'version':3,'optionId':copied['options'][0]['id'],'requestKey':str(uuid.uuid4())},st,409)
    call('reopen task',p+'/status','PUT',{'version':3,'status':'PUBLISHED'},m)
    call('cross-task option rejected',sp+'/attempts','POST',{'version':4,'optionId':copied['options'][0]['id'],'requestKey':str(uuid.uuid4())},st,400)
    fixture={'prefix':prefix,'actors':actors,'courseId':c}
    (W/'fixture.json').write_text(json.dumps(fixture),encoding='utf-8')
    if args.browser:
        env=os.environ.copy();env['UPSELF_MARKET_FIXTURE']=str(W/'fixture.json');tools=json.loads((R/'.local/development-tools.json').read_text(encoding='utf-8-sig'))
        result=subprocess.run([str(Path(tools['nodeHome'])/'node.exe'),str(R/'frontend/node_modules/@playwright/test/cli.js'),'test','tests/e2e/market.spec.ts'],cwd=R/'frontend',env=env);check('Chrome market workflow',result.returncode==0)
    call('unenroll student',cp+'/students/'+str(actors['student']['id']),'DELETE',token=admin)
    call('membership revoked',sp+'/attempts',token=st,code=403)
    call('reassign course',cp+'/mentor','PUT',{'mentorId':actors['othermentor']['id']},admin)
    call('former mentor denied',p+'/participants',token=m,code=403)
    check('current mentor keeps prior records',call('new mentor history',p+'/attempts?studentId='+str(actors['student']['id']),token=other)['total']==2)
    call('archive course',cp+'/status','PUT',{'status':'ARCHIVED'},admin)
    call('archived course read-only',p+'/copy','POST',token=other,code=400)
    (W/'result.json').write_text(json.dumps({'passed':True,'count':len(checks),'checks':checks}),encoding='utf-8');print('Market checks:',len(checks))
def cleanup():
    # Scope every delete to exact disposable course/account IDs; retain audit records.
    for cid in courses:
        assert scalar("SELECT COUNT(*) FROM learning_course WHERE id=%d AND title='%s'"%(cid,prefix))==1
        tasks='SELECT id FROM market_task WHERE course_id='+str(cid)
        sql('DELETE FROM market_attempt WHERE task_id IN ('+tasks+')')
        sql('DELETE FROM market_option WHERE task_id IN ('+tasks+')')
        sql('DELETE FROM market_task WHERE course_id='+str(cid))
        sql('DELETE FROM learning_enrollment WHERE course_id='+str(cid))
        sql('DELETE FROM learning_course WHERE id='+str(cid))
    for name,actor in actors.items():
        try:request('/client/logout' if name in ['student','second','outsider'] else '/logout','POST',{},actor['token'])
        except Exception:pass
    for uid in users:sql("DELETE FROM sys_user WHERE id=%d AND user_name LIKE '%s%%'"%(uid,prefix))
try:main()
finally:cleanup()
