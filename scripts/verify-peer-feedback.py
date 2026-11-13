"""Disposable local FR-48 acceptance; audit records are retained."""
from pathlib import Path
import argparse, concurrent.futures, json, os, re, secrets, subprocess, sys, urllib.error, urllib.request

R = Path(__file__).resolve().parents[1]
W = R / '.local/peer-verification'
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
prefix = 'peer_'  + secrets.token_hex(4) + '_'
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


course_id=None
assignment_ids=[]
def submit(a,actor,text):
    token=actors[actor]['token'];p='/client/assignments/'+str(a)
    detail=call('read assignment',p,token=token)['data']
    own=call('own submission',p+'/submission',token=token)['data']
    import uuid
    return call('submit '+actor,p+'/submit','POST',{'version':own['version'],'assignmentVersion':detail['version'],'answerText':text,'requestKey':str(uuid.uuid4())},token)['data']
def main():
    global course_id
    check('V20 applied',scalar("SELECT COUNT(*) FROM flyway_schema_history WHERE version='20' AND success=1")==1)
    password=re.search(r'username: admin01\s+password: "([^"]+)"',(R/'config/demo-accounts.yml').read_text(encoding='utf-8-sig')).group(1)
    admin=call('admin login','/empLogin','POST',{'username':'admin01','password':password})['token']
    actors['admin']={'username':'admin01','password':password,'token':admin}
    for name,role in [('mentor',20),('othermentor',20),('student',10),('second',10),('outsider',10)]:
        pwd=secrets.token_urlsafe(18)
        uid=call('create '+name,'/system/user','POST',{'userName':prefix+name,'nickName':name,'roleId':role,'password':pwd},admin)['data'];users.append(uid)
        token=call('login '+name,'/client/login' if role==10 else '/empLogin','POST',{'username':prefix+name,'password':pwd})['token']
        actors[name]={'id':uid,'username':prefix+name,'password':pwd,'token':token}
    m=actors['mentor']['token'];st=actors['student']['token'];st2=actors['second']['token'];other=actors['othermentor']['token']
    course_id=call('create course','/learning/courses','POST',{'title':prefix,'description':'Peer feedback acceptance'},admin)['data'];cp='/learning/courses/'+str(course_id)
    call('assign mentor',cp+'/mentor','PUT',{'mentorId':actors['mentor']['id']},admin)
    for actor in ['student','second']:call('enroll '+actor,cp+'/students','POST',{'studentId':actors[actor]['id']},admin)
    call('publish course',cp+'/status','PUT',{'status':'PUBLISHED'},admin)
    body={'courseId':course_id,'title':'Peer feedback practice','instructions':'Share one business idea. Give a kind, useful suggestion.','status':'PUBLISHED','maxScore':100,'version':0,'languageCode':'en','peerFeedback':True}
    aid=call('new disclosed assignment','/assignments','POST',body,m)['data'];assignment_ids.append(aid)
    sp='/client/assignments/'+str(aid);p=sp+'/peer-feedback';mp='/assignments/'+str(aid)+'/peer-feedback'
    call('anonymous denied',p,code=401)
    call('student cannot manage',p+'/manage',token=st,code=403)
    call('admin cannot read peer content',mp+'/manage',token=admin,code=403)
    call('other mentor cannot read',mp+'/manage',token=other,code=403)
    call('outsider cannot view',p,token=actors['outsider']['token'],code=403)
    call('student must submit first',p+'/allocate','POST',token=st,code=409)
    first=submit(aid,'student','Student one business idea')
    call('cannot review self',p+'/allocate','POST',token=st,code=409)
    second=submit(aid,'second','Student two business idea')
    # Add an authenticated attachment to the frozen second attempt for file-grant checks.
    # Upload through the ordinary API on a fresh assignment below; no fixture SQL fabricates submissions.
    with concurrent.futures.ThreadPoolExecutor(2) as pool:
        out=list(pool.map(lambda _:request(p+'/allocate','POST',token=st),range(2)))
    check('concurrent allocation returns same row',all(x['code']==200 for x in out) and out[0]['data']['outgoing']['id']==out[1]['data']['outgoing']['id'])
    own=out[0]['data']['outgoing'];fid=own['id']
    check('frozen answer correct',own['answerText']=='Student two business idea')
    check('reviewer and recipient IDs absent',own.get('reviewerId') is None and own.get('submissionId') is None and own.get('studentName') is None)
    call('empty feedback rejected',p,'POST',{'version':0,'strength':' ','suggestion':''},st,400)
    call('oversized feedback rejected',p,'POST',{'version':0,'strength':'x'*501,'suggestion':''},st,400)
    call('content filter reused',p,'POST',{'version':0,'strength':'kill yourself','suggestion':''},st,422)
    call('send feedback',p,'POST',{'version':0,'strength':'Your idea is clear.','suggestion':'Try an example.'},st)
    call('duplicate send rejected',p,'POST',{'version':0,'strength':'duplicate','suggestion':''},st,409)
    received=call('recipient sees feedback',p,token=st2)['data']['received'];check('one anonymous feedback',len(received)==1 and received[0]['strength']=='Your idea is clear.' and received[0].get('reviewerId') is None)
    managed=call('mentor sees mapping',mp+'/manage?current=1&pageSize=1',token=m)
    check('mentor page and names',managed['total']==1 and managed['rows'][0]['reviewerName']==actors['student']['username'] and managed['rows'][0]['studentName']==actors['second']['username'])
    call('student cannot hide',p+'/'+str(fid)+'/hide','PUT',{'version':1,'reason':'test'},st,403)
    call('foreign mentor cannot hide',mp+'/'+str(fid)+'/hide','PUT',{'version':1,'reason':'test'},other,403)
    # Returning/revising the recipient does not expose their private new draft.
    call('mentor returns submission','/assignments/'+str(aid)+'/submissions/'+str(second['id'])+'/review','POST',{'version':second['version'],'decision':'RETURNED','feedback':'Add an example','score':None},m)
    latest=call('recipient reads returned work',sp+'/submission',token=st2)['data']
    call('save private revision',sp+'/submission','PUT',{'version':latest['version'],'answerText':'PRIVATE REVISION'},st2)
    snapshot=call('reviewer still sees old snapshot',p,token=st)['data']['outgoing'];check('draft stays private',snapshot['answerText']=='Student two business idea')
    submit(aid,'second','Student two revised business idea')
    check('earlier submission marked',call('recipient history',p,token=st2)['data']['received'][0]['earlierSubmission'])
    body.update(version=0,peerFeedback=False);call('close feedback','/assignments/'+str(aid),'PUT',body,m)
    check('sharing disclosure retained while closed',call('read closed disclosure',sp,token=st)['data']['peerFeedbackConfigured'] is True)
    call('closed cannot allocate',p+'/allocate','POST',token=st2,code=409)
    check('closed retains sent feedback',len(call('read closed feedback',p,token=st2)['data']['received'])==1)
    body.update(version=1,peerFeedback=True);call('reopen disclosed assignment','/assignments/'+str(aid),'PUT',body,m)
    call('stale hide rejected',mp+'/'+str(fid)+'/hide','PUT',{'version':0,'reason':'Not useful'},m,409)
    call('mentor hides feedback',mp+'/'+str(fid)+'/hide','PUT',{'version':1,'reason':'Please use specific feedback.'},m)
    check('hidden removed for recipient',not call('recipient refresh',p,token=st2)['data']['received'])
    hidden=call('sender sees hidden status',p,token=st)['data']['outgoing'];check('hidden text not leaked',hidden['hidden'] and hidden['strength'] is None and hidden['hideReason'] is None)
    # Legacy submissions cannot be silently disclosed.
    body.update(title='Legacy private assignment',peerFeedback=False,version=0)
    legacy=call('create private assignment','/assignments','POST',body,m)['data'];assignment_ids.append(legacy);submit(legacy,'student','Private legacy work')
    body.update(peerFeedback=True)
    call('legacy sharing blocked','/assignments/'+str(legacy),'PUT',body,m,409)
    # Browser gets a fresh enabled assignment; it exercises the entire UI submission/review flow.
    body.update(title='Browser peer practice',peerFeedback=True)
    browser_id=call('create browser assignment','/assignments','POST',body,m)['data'];assignment_ids.append(browser_id)
    fixture={'prefix':prefix,'actors':actors,'assignmentId':browser_id,'courseId':course_id}
    (W/'fixture.json').write_text(json.dumps(fixture),encoding='utf-8')
    if args.browser:
        env=os.environ.copy();env['UPSELF_PEER_FIXTURE']=str(W/'fixture.json')
        tools=json.loads((R/'.local/development-tools.json').read_text(encoding='utf-8-sig'))
        result=subprocess.run([str(Path(tools['nodeHome'])/'node.exe'),str(R/'frontend/node_modules/@playwright/test/cli.js'),'test','tests/e2e/peer-feedback.spec.ts'],cwd=R/'frontend',env=env)
        check('Chrome peer workflow',result.returncode==0)
        file_rows=sql('SELECT f.id FROM assignment_file f JOIN assignment_submission s ON s.id=f.submission_id WHERE s.assignment_id='+str(browser_id))
        check('browser uploaded one real attachment',len(file_rows)==1)
        file_id=int(file_rows[0][0])
        def file_code(token,client=True):
            req=urllib.request.Request(BASE+('/client' if client else '')+'/assignments/files/'+str(file_id)+'/content',headers={'Authorization':'Bearer '+token})
            try: response=urllib.request.urlopen(req,timeout=20)
            except urllib.error.HTTPError as e: response=e
            raw=response.read()
            return json.loads(raw)['code'] if raw.startswith(b'{') else 200
        check('allocated reviewer downloads exact attempt',file_code(st)==200)
        check('outsider cannot download peer file',file_code(actors['outsider']['token'])==403)
        check('admin cannot download peer file',file_code(admin,False)==403)
        check('other mentor cannot download peer file',file_code(other,False)==403)
    # Course reassignment and unenrollment revoke old grants on every request.
    call('remove recipient',cp+'/students/'+str(actors['second']['id']),'DELETE',token=admin)
    check('former classmate snapshot revoked',call('reviewer after unenrollment',p,token=st)['data']['outgoing']['answerText'] is None)
    call('removed student denied',p,token=st2,code=403)
    call('reassign course',cp+'/mentor','PUT',{'mentorId':actors['othermentor']['id']},admin)
    call('previous mentor revoked',mp+'/manage',token=m,code=403)
    call('new mentor can see history',mp+'/manage',token=other)
    print('Peer feedback checks:',len(checks),flush=True)
    (W/'result.json').write_text(json.dumps({'checks':checks,'count':len(checks)}),encoding='utf-8')
def cleanup():
    # Delete only disposable rows owned by the exact fixture course and accounts; preserve all audits.
    if course_id:
        assert scalar("SELECT COUNT(*) FROM learning_course WHERE id="+str(course_id)+" AND title='"+prefix+"'")==1, 'Unexpected course owner; no cleanup'
        ids=','.join(map(str,assignment_ids)) or '0'
        keys=sql('SELECT f.storage_key FROM assignment_file f JOIN assignment_submission s ON s.id=f.submission_id WHERE s.assignment_id IN ('+ids+')')
        sql('DELETE FROM assignment_peer_feedback WHERE assignment_id IN ('+ids+')')
        for table in ['assignment_file','assignment_attempt']:
            sql('DELETE FROM '+table+' WHERE submission_id IN (SELECT id FROM assignment_submission WHERE assignment_id IN ('+ids+'))')
        sql('DELETE FROM assignment_submission WHERE assignment_id IN ('+ids+')')
        sql('DELETE FROM assignment WHERE course_id='+str(course_id))
        sql('DELETE FROM learning_enrollment WHERE course_id='+str(course_id))
        sql('DELETE FROM learning_course WHERE id='+str(course_id))
        for (key,) in keys:
            if re.fullmatch(r'[a-f0-9]{32}\.txt',key):
                root=(R/'.local/uploads/assignments').resolve(); path=(root/key).resolve()
                assert path.parent==root
                path.unlink(missing_ok=True)
    for name,actor in actors.items():
        try: request('/client/logout' if name in ['student','second','outsider'] else '/logout','POST',{},actor['token'])
        except Exception: pass
    for uid in users:
        sql('DELETE FROM student_badge WHERE student_id='+str(uid))
        sql('DELETE FROM sys_user WHERE id='+str(uid)+" AND user_name LIKE '"+prefix+"%'")
try:
    main()
finally:
    cleanup()
