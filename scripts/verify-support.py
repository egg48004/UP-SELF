"""Local FR-34/58/54 API and Chrome checks; only this run's fixtures are removed."""
from pathlib import Path
import importlib.util,json,secrets,sys,os,subprocess,datetime,csv,io,urllib.request,concurrent.futures,re
R=Path(__file__).resolve().parents[1];W=R/'.local/support-verification';W.mkdir(parents=True,exist_ok=True)
spec=importlib.util.spec_from_file_location('a',R/'scripts/verify-local.py');a=importlib.util.module_from_spec(spec);spec.loader.exec_module(a)
prefix='support_'+secrets.token_hex(4)+'_';users=[];courses=[];groups=[];tokens=[];file_keys=[];checks=[]
def call(label,path,method='GET',body=None,token=None,code=200):
 r=a.request(path,method,body,token)[1];assert r.get('code')==code,(label,r.get('code'),r.get('msg'));assert not re.search(r'[\u4e00-\u9fff]',r.get('msg',''));checks.append(label);print('PASS',label,flush=True);return r
def data(label,path,method='GET',body=None,token=None):return call(label,path,method,body,token)['data']
def sql(s):return a.sql(s)
def main():
 a.clear_our_rate_limits();admin=call('administrator login','/empLogin','POST',{'username':'admin01','password':'1234'})['token'];tokens.append((admin,False))
 config=(R/'config/application-local.yml').read_text(encoding='utf-8-sig');pwd=re.search(r'^\s{4}maintenance:\s*(.*?)\s*$',config,re.M).group(1).strip('"\'')
 internal=call('internal login','/empLogin','POST',{'username':'maintenance','password':pwd})['token'];tokens.append((internal,False))
 actors={}
 for name,role in [('student',10),('otherstudent',10),('mentor',20),('othermentor',20)]:
  password=secrets.token_urlsafe(18);username=prefix+name
  uid=data('create '+name,'/system/user','POST',{'userName':username,'nickName':'DEMO '+name,'roleId':role,'password':password},admin);users.append(uid)
  t=call('login '+name,'/client/login' if role==10 else '/empLogin','POST',{'username':username,'password':password})['token'];tokens.append((t,role==10));actors[name]={'id':uid,'username':username,'password':password,'token':t}
 st=actors['student']['token'];ot=actors['otherstudent']['token'];mt=actors['mentor']['token'];other=actors['othermentor']['token'];sid=actors['student']['id'];oid=actors['otherstudent']['id']
 cid=data('create course','/learning/courses','POST',{'title':prefix+'course','description':'Disposable support verification.'},admin);courses.append(cid);cp=f'/learning/courses/{cid}'
 call('assign mentor',cp+'/mentor','PUT',{'mentorId':actors['mentor']['id']},admin)
 for uid in [sid,oid]:call('enroll student',cp+'/students','POST',{'studentId':uid},admin)
 chapters=[]
 for i in range(2):chapters.append(data('create chapter '+str(i),cp+'/chapters','POST',{'title':'Practice '+str(i),'body':'Take one small step.','status':'PUBLISHED','sortOrder':i,'languageCode':'en'},mt))
 call('publish course',cp+'/status','PUT',{'status':'PUBLISHED'},admin)
 draft=data('create draft course','/learning/courses','POST',{'title':prefix+'draft','description':'Draft fixture.'},admin);courses.append(draft)
 due=(datetime.datetime.now(datetime.timezone.utc)-datetime.timedelta(days=4)).isoformat()
 aid=data('publish overdue assignment','/assignments','POST',{'courseId':cid,'title':'Practice assignment','instructions':'Share one idea.','maxScore':10,'status':'PUBLISHED','languageCode':'en','dueAt':due},mt)
 g=data('create group',f'/chat/courses/{cid}/groups','POST',{'title':'Peer support group'},mt);groups.append(g)
 for uid in [sid,oid]:call('add member',f'/chat/groups/{g}/members','POST',{'studentId':uid},mt)
 msg=data('mentor message',f'/chat/groups/{g}/messages','POST',{'body':'API report evidence','fileIds':[],'clientKey':secrets.token_hex(16)},mt)
 ui=data('UI report message',f'/chat/groups/{g}/messages','POST',{'body':'UI message to report','fileIds':[],'clientKey':secrets.token_hex(16)},mt)
 student_msg=data('student message',f'/client/chat/groups/{g}/messages','POST',{'body':'Student message for mentor reporting','fileIds':[],'clientKey':secrets.token_hex(16)},st)
 # A real private upload supplies metadata without exposing its content to moderation.
 boundary='support-'+secrets.token_hex(10);payload=(f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="study.txt"\r\nContent-Type: text/plain\r\n\r\nPRIVATE-FILE-CONTENTS\r\n--{boundary}--\r\n').encode()
 req=urllib.request.Request(a.BASE+f'/chat/groups/{g}/files',data=payload,headers={'Authorization':'Bearer '+mt,'Content-Type':'multipart/form-data; boundary='+boundary},method='POST')
 with urllib.request.urlopen(req) as response:upload=json.load(response)
 assert upload['code']==200;fid=upload['data']['id'];file_keys.extend(sql(f'SELECT storage_key FROM chat_file WHERE id={fid}').splitlines())
 fm=data('message with attachment',f'/chat/groups/{g}/messages','POST',{'body':'Attachment evidence','fileIds':[fid],'clientKey':secrets.token_hex(16)},mt)
 report={'messageId':msg['id'],'reason':'SPAM','details':'API details\nSecond line','accusedId':oid,'messageBody':'FORGED'}
 rid=data('student submits report','/client/reports','POST',report,st)
 call('duplicate pending report rejected','/client/reports','POST',report,st,409)
 with concurrent.futures.ThreadPoolExecutor(2) as pool:
  codes=list(pool.map(lambda _:a.request('/client/reports','POST',{'messageId':fm['id'],'reason':'OTHER'},st)[1]['code'],range(2)))
 assert sorted(codes)==[200,409];checks.append('concurrent duplicate report is prevented')
 data('independent reporter can report same message','/client/reports','POST',report,ot)
 own=call('reporter history is private','/client/reports/mine?reporterId='+str(oid),token=st)['rows'];assert len(own)==2 and all('reporterName' not in r and 'messageBody' not in r and 'resolution' not in r for r in own)
 detail=data('administrator sees scoped evidence',f'/moderation/reports/{rid}',token=admin);assert detail['messageBody']=='API report evidence' and detail['accusedId']==actors['mentor']['id']
 attachment=sql(f'SELECT id FROM user_report WHERE reporter_id={sid} AND message_id={fm["id"]}')
 ev=data('attachment metadata only',f'/moderation/reports/{attachment}',token=admin);assert 'study.txt' in ev['attachmentSummary'] and 'PRIVATE-FILE-CONTENTS' not in str(ev) and file_keys[0] not in str(ev)
 call('administrator has no general chat bypass',f'/chat/groups/{g}',token=admin,code=403)
 for label,t in [('mentor',mt),('other mentor',other),('internal wildcard',internal)]:call(label+' cannot review reports',f'/moderation/reports/{rid}',token=t,code=403)
 call('student cannot review reports',f'/client/moderation/reports/{rid}',token=st,code=403)
 call('cannot report own message','/client/reports','POST',{'messageId':student_msg['id'],'reason':'OTHER'},st,400)
 call('invalid reason rejected','/client/reports','POST',{'messageId':ui['id'],'reason':'INVALID'},st,400)
 call('processing note required',f'/moderation/reports/{rid}','PUT',{'status':'RESOLVED','resolution':' ','version':0},admin,400)
 call('resolve report',f'/moderation/reports/{rid}','PUT',{'status':'RESOLVED','resolution':'API checked. No account action required.','version':0},admin)
 call('stale review denied',f'/moderation/reports/{rid}','PUT',{'status':'DISMISSED','resolution':'Changed','version':0},admin,409)
 assert any(r['id']==rid and r['status']=='RESOLVED' for r in call('reporter sees outcome only','/client/reports/mine',token=st)['rows'])
 assert sql('SELECT status FROM sys_user WHERE id='+str(actors['mentor']['id']))=='0'
 call('remove reporter from group',f'/chat/groups/{g}/members/{sid}','DELETE',token=mt)
 call('former member can still read own report status','/client/reports/mine',token=st)
 call('former member cannot report inaccessible messages','/client/reports','POST',{'messageId':ui['id'],'reason':'OTHER'},st,403)
 call('restore member',f'/chat/groups/{g}/members','POST',{'studentId':sid},mt)
 for i in range(10):
  m=data('pagination message '+str(i),f'/chat/groups/{g}/messages','POST',{'body':'Pagination fixture '+str(i),'fileIds':[],'clientKey':secrets.token_hex(16)},mt)
  data('pagination report '+str(i),'/client/reports','POST',{'messageId':m['id'],'reason':'OTHER'},st)
 assert len(call('report history paginates','/client/reports/mine?current=2&pageSize=10',token=st)['rows'])==2
 for label,t,base in [('student',st,'/client'),('administrator',admin,''),('internal wildcard',internal,'')]:
  call(label+' cannot view followups',base+'/follow-ups',token=t,code=403)
 for label,t,base in [('student',st,'/client'),('mentor',mt,''),('internal wildcard',internal,'')]:call(label+' cannot export',base+'/exports/users',token=t,code=403)
 for path in ['/client/reports/mine','/follow-ups','/exports/users']:call('anonymous denied '+path,path,code=401)
 # Rule fixtures use Malaysia-local database time; no existing learner data is modified.
 for actor in ['student','otherstudent']:
  t=actors[actor]['token'];cur=data('read check-in metadata','/client/moods/current',token=t);call('acknowledge mood notice','/client/moods/acknowledgment','POST',{'noticeVersion':cur['noticeVersion'],'accepted':True},t)
 week=datetime.date.fromisoformat(cur['weekStart']);prev=(week-datetime.timedelta(weeks=1)).isoformat();older=(week-datetime.timedelta(weeks=2)).isoformat()
 sql(f"INSERT INTO mood_entry(student_id,week_start,mood_level,private_note,note_shared) VALUES({sid},'{prev}',1,'PRIVATE-JOURNAL',1),({sid},'{older}',2,'PRIVATE-JOURNAL',1),({oid},'{prev}',1,'PRIVATE-JOURNAL',1)")
 sql(f'UPDATE learning_enrollment SET create_time=DATE_SUB(NOW(),INTERVAL 15 DAY) WHERE course_id={cid};')
 sql(f'INSERT INTO learning_chapter_progress(student_id,chapter_id,last_visited_at,completed_at) VALUES({sid},{chapters[0]},DATE_SUB(NOW(),INTERVAL 15 DAY),NULL),({oid},{chapters[0]},DATE_SUB(NOW(),INTERVAL 15 DAY),NOW()),({oid},{chapters[1]},DATE_SUB(NOW(),INTERVAL 15 DAY),NOW());')
 sub=data('other student submits work',f'/client/assignments/{aid}/submit','POST',{'version':0,'assignmentVersion':0,'answerText':'A small idea.','requestKey':secrets.token_hex(16)},ot)
 call('returned work is not never-submitted',f'/assignments/{aid}/submissions/{sub["id"]}/review','POST',{'version':sub['version'],'decision':'RETURNED','score':None,'feedback':'Add one example.'},mt)
 def summary(t=mt):return data('refresh follow-up summary','/follow-ups/summary',token=t)
 assert summary()['total']==3
 reminders=call('three correct reminders','/follow-ups',token=mt)['rows'];assert {r['kind'] for r in reminders}=={'MOOD','ACTIVITY','ASSIGNMENT'} and all(r['studentId']==sid for r in reminders) and 'PRIVATE-JOURNAL' not in str(reminders)
 assert summary(other)['total']==0
 sql(f"INSERT INTO mood_entry(student_id,week_start,mood_level,private_note,note_shared) VALUES({sid},'{week.isoformat()}',5,'PRIVATE-JOURNAL',1)");assert summary()['total']==2
 sql(f"DELETE FROM mood_entry WHERE student_id={sid} AND week_start='{week.isoformat()}'");assert summary()['total']==3
 reminders=call('reload reminder versions after a new episode','/follow-ups',token=mt)['rows'];mood=next(r for r in reminders if r['kind']=='MOOD')
 call('foreign mentor cannot mark reminder',f'/follow-ups/{mood["id"]}/review','PUT',{'version':mood['version']},other,404)
 call('mark reminder reviewed',f'/follow-ups/{mood["id"]}/review','PUT',{'version':mood['version']},mt)
 assert summary()['total']==2 and summary()['total']==2
 assert len(call('reviewed condition stays reviewed','/follow-ups?status=REVIEWED',token=mt)['rows'])==1
 sql(f"UPDATE mood_entry SET mood_level=3 WHERE student_id={sid} AND week_start='{prev}'")
 assert summary()['total']==2;assert any(r['kind']=='MOOD' for r in call('good mood ends reminder','/follow-ups?status=ENDED',token=mt)['rows'])
 sql(f"DELETE FROM mood_entry WHERE student_id={sid} AND week_start='{older}';UPDATE mood_entry SET mood_level=1 WHERE student_id={sid};")
 assert summary()['total']==2
 sql(f"INSERT INTO mood_entry(student_id,week_start,mood_level,private_note,note_shared) VALUES({sid},'{older}',2,'PRIVATE-JOURNAL',1)")
 assert summary()['total']==3;assert sql(f"SELECT COUNT(*) FROM follow_up_reminder WHERE mentor_id={actors['mentor']['id']} AND student_id={sid} AND kind='MOOD'")=='1'
 call('recent visit',f'/client/learning/chapters/{chapters[0]}/visit','POST',token=st);assert summary()['total']==2
 sql(f'UPDATE learning_chapter_progress SET last_visited_at=DATE_SUB(NOW(),INTERVAL 15 DAY) WHERE student_id={sid};UPDATE learning_enrollment SET create_time=DATE_SUB(NOW(),INTERVAL 13 DAY) WHERE course_id={cid} AND student_id={sid};');assert summary()['total']==2
 sql(f'UPDATE learning_enrollment SET create_time=DATE_SUB(NOW(),INTERVAL 15 DAY) WHERE course_id={cid} AND student_id={sid};');assert summary()['total']==3
 for expression in ['DATE_SUB(NOW(),INTERVAL 2 DAY)','NULL']:
  sql(f'UPDATE assignment SET due_at={expression} WHERE id={aid}');assert summary()['total']==2
 sql(f"UPDATE assignment SET due_at=DATE_SUB(NOW(),INTERVAL 4 DAY),status='DRAFT' WHERE id={aid}");assert summary()['total']==2
 sql(f"UPDATE assignment SET status='PUBLISHED' WHERE id={aid}");assert summary()['total']==3
 call('archive clears current reminders',cp+'/status','PUT',{'status':'ARCHIVED'},admin);assert summary()['total']==0
 call('restore published course',cp+'/status','PUT',{'status':'PUBLISHED'},admin);assert summary()['total']==3
 call('reassign course',cp+'/mentor','PUT',{'mentorId':actors['othermentor']['id']},admin);assert summary()['total']==0 and summary(other)['total']==3
 call('former mentor cannot mark old reminder',f'/follow-ups/{mood["id"]}/review','PUT',{'version':mood['version']},mt,404)
 call('restore mentor',cp+'/mentor','PUT',{'mentorId':actors['mentor']['id']},admin);assert summary()['total']==3;summary(other)
 sql(f"UPDATE sys_user SET status='1' WHERE id={sid}");assert summary()['total']==0
 sql(f"UPDATE sys_user SET status='0' WHERE id={sid}");assert summary()['total']==3
 def export(kind,query=''):
  req=urllib.request.Request(a.BASE+'/exports/'+kind+query,headers={'Authorization':'Bearer '+admin})
  with urllib.request.urlopen(req) as response:body=response.read();assert response.headers['Content-Type'].startswith('text/csv');assert 'attachment' in response.headers['Content-Disposition']
  assert body.startswith(b'\xef\xbb\xbf');text=body.decode('utf-8-sig');assert all(x not in text for x in ['PRIVATE-JOURNAL','PRIVATE-FILE-CONTENTS','password','totp_secret','private_note','estimatedProfit'])
  checks.append('CSV '+kind);return list(csv.DictReader(io.StringIO(text)))
 sql(f"UPDATE sys_user SET nick_name='=SUM(1,2)' WHERE id={oid}")
 rows=export('users','?keyword='+prefix);assert len(rows)==4;assert next(r for r in rows if r['Username']==actors['otherstudent']['username'])['Display name']=="'=SUM(1,2)"
 assert len(export('users','?keyword='+prefix+'&role=mentor'))==2
 assert len(export('users','?keyword=maintenance'))==0
 assert len(export('courses','?keyword='+prefix+'&status=DRAFT'))==1
 lr=export('learning','?courseId='+str(cid));assert len(lr)==2;assert next(r for r in lr if r['Student username']==actors['otherstudent']['username'])['Completion percent']=='100.00'
 sql(f"UPDATE sys_user SET nick_name='DEMO otherstudent' WHERE id={oid}")
 # Preserve only synthetic business data during browser execution.
 fixture=W/'fixtures.json';fixture.write_text(json.dumps({'actors':{k:{kk:vv for kk,vv in v.items() if kk!='token'} for k,v in actors.items()},'admin':{'username':'admin01','password':'1234'},'courseId':cid,'groupId':g,'prefix':prefix,'assignmentId':aid},indent=2),encoding='utf-8')
 if '--browser' in sys.argv:
  tools=json.loads((R/'.local/development-tools.json').read_text(encoding='utf-8-sig'));node=Path(tools['nodeHome'])/'node.exe';env=os.environ.copy();env['UPSELF_SUPPORT_FIXTURE']=str(fixture);env['UPSELF_SCREENSHOTS']=str(W)
  with (W/'browser.log').open('w',encoding='utf-8') as log:p=subprocess.run([str(node),str(R/'frontend/node_modules/@playwright/test/cli.js'),'test','support.spec.ts'],cwd=R/'frontend',env=env,stdout=log,stderr=subprocess.STDOUT)
  assert p.returncode==0,'Chrome verification failed; inspect .local/support-verification/browser.log'
  print('PASS Chrome reporting, review, reminder and CSV workflows',flush=True)
 (W/'result.json').write_text(json.dumps({'apiChecks':len(checks),'browser':'passed' if '--browser' in sys.argv else 'not run'},indent=2),encoding='utf-8');print('API checks:',len(checks),flush=True)
try:main()
finally:
 if users:
  ids=','.join(map(str,users));sql(f'DELETE FROM user_report WHERE reporter_id IN ({ids}) OR accused_id IN ({ids});DELETE FROM follow_up_reminder WHERE mentor_id IN ({ids}) OR student_id IN ({ids});DELETE FROM mood_entry WHERE student_id IN ({ids});DELETE FROM mood_notice_ack WHERE student_id IN ({ids});')
  if groups:
   gs=','.join(map(str,groups));sql(f'DELETE FROM chat_file WHERE group_id IN ({gs});DELETE FROM chat_message WHERE group_id IN ({gs});DELETE FROM chat_member WHERE group_id IN ({gs});DELETE FROM chat_group WHERE id IN ({gs});')
  if courses:
   cs=','.join(map(str,courses));sub=f'SELECT id FROM assignment_submission WHERE student_id IN ({ids})'
   sql(f'DELETE FROM assignment_file WHERE submission_id IN ({sub});DELETE FROM assignment_attempt WHERE submission_id IN ({sub});DELETE FROM assignment_submission WHERE student_id IN ({ids});DELETE FROM assignment WHERE course_id IN ({cs});DELETE FROM learning_chapter_progress WHERE student_id IN ({ids});DELETE FROM learning_chapter WHERE course_id IN ({cs});DELETE FROM learning_enrollment WHERE course_id IN ({cs});DELETE FROM learning_course WHERE id IN ({cs});')
  sql(f'DELETE FROM sys_audit_log WHERE actor_id IN ({ids});DELETE FROM sys_user WHERE id IN ({ids});')
 for key in file_keys:
  base=(R/'.local/uploads/chat').resolve();p=(base/key).resolve();assert p.parent==base
  if p.exists():p.unlink()
 for token,student in tokens:a.request('/client/logout' if student else '/logout','POST',token=token)
 fixture=W/'fixtures.json'
 if fixture.exists():fixture.unlink()
