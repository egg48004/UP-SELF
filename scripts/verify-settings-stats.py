"""Local FR-55/27 integration checks. Removes only its disposable fixtures."""
from pathlib import Path
import importlib.util,json,secrets,re,uuid,subprocess,os,sys,concurrent.futures
R=Path(__file__).resolve().parents[1];W=R/'.local/settings-stats-verification';W.mkdir(parents=True,exist_ok=True)
spec=importlib.util.spec_from_file_location('local',R/'scripts/verify-local.py');a=importlib.util.module_from_spec(spec);spec.loader.exec_module(a)
users=[];courses=[];tokens=[];checks=[];original=None;admin=None;prefix='stats_'+secrets.token_hex(4)+'_'
def call(label,path,method='GET',body=None,token=None,code=200):
 b=a.request(path,method,body,token)[1];assert b.get('code')==code,(label,b.get('code'),b.get('msg'));assert not re.search(r'[\u4e00-\u9fff]',b.get('msg',''));checks.append(label);print('PASS',label,flush=True);return b
def data(label,path,method='GET',body=None,token=None):return call(label,path,method,body,token)['data']
def check(label,condition):assert condition,label;checks.append(label);print('PASS',label,flush=True)
def main():
 global original,admin
 a.clear_our_rate_limits();admin=call('bootstrap administrator login','/empLogin','POST',{'username':'admin01','password':'1234'})['token'];tokens.append((admin,False))
 original=data('anonymous default read','/accessibility/defaults');check('public response is non-personal',set(original)=={'size','contrast','motion','version'})
 actors={}
 for key,role in [('student',10),('otherstudent',10),('empty',10),('mentor',20),('admin',30)]:
  password=secrets.token_urlsafe(18);name=prefix+key
  uid=data('create '+key,'/system/user','POST',{'userName':name,'nickName':'Stats '+key,'roleId':role,'password':password},admin);users.append(uid)
  token=call('login '+key,'/client/login' if role==10 else '/empLogin','POST',{'username':name,'password':password})['token'];tokens.append((token,role==10));actors[key]={'id':uid,'username':name,'password':password,'token':token}
 config=(R/'config/application-local.yml').read_text(encoding='utf-8-sig');pwd=re.search(r'^\s{4}maintenance:\s*(.*?)\s*$',config,re.M).group(1).strip('"\'')
 internal=call('internal login','/empLogin','POST',{'username':'maintenance','password':pwd})['token'];tokens.append((internal,False))
 st=actors['student']['token'];ot=actors['otherstudent']['token'];mt=actors['mentor']['token'];at=actors['admin']['token'];sid=actors['student']['id'];oid=actors['otherstudent']['id']
 call('anonymous update denied','/accessibility/defaults','PUT',original,code=401)
 for label,path,token in [('student','/client/accessibility/defaults',st),('mentor','/accessibility/defaults',mt),('maintenance','/accessibility/defaults',internal)]:call(label+' default update denied',path,'PUT',original,token,403)
 for key,value in [('size','tiny'),('contrast','low'),('motion',None),('version',-1)]:call('invalid setting '+key,'/accessibility/defaults','PUT',{**original,key:value},at,400)
 call('missing defaults rejected','/accessibility/defaults','PUT',{},at,400)
 changed=data('administrator saves defaults','/accessibility/defaults','PUT',{**original,'size':'large','contrast':'highContrast','motion':True},at)
 check('configuration version increments',changed['version']==original['version']+1)
 check('defaults persist in database',a.sql('SELECT text_size,contrast,reduce_motion FROM accessibility_defaults WHERE id=1;')=='large\thighContrast\t1')
 call('stale edit rejected','/accessibility/defaults','PUT',original,at,409)
 def update(_):return a.request('/accessibility/defaults','PUT',{**changed,'size':'standard'},at)[1]['code']
 with concurrent.futures.ThreadPoolExecutor(2) as pool:results=list(pool.map(update,range(2)))
 check('one concurrent update succeeds',sorted(results)==[200,409])
 fresh=data('fresh public read','/accessibility/defaults');data('restore values after API setting checks','/accessibility/defaults','PUT',{**original,'version':fresh['version']},at)
 for label,token,path in [('mentor',mt,'/learning'),('admin',at,'/learning'),('maintenance',internal,'/learning')]:
  for tail in ['/statistics','/statistics/courses']:call(label+' stats denied',path+tail,token=token,code=403)
 call('anonymous stats denied','/client/learning/statistics',code=401)
 empty=data('empty student statistics','/client/learning/statistics',token=actors['empty']['token']);check('all empty statistics are zero',all(v==0 for v in empty.values()))
 check('empty course page',call('empty course list','/client/learning/statistics/courses',token=actors['empty']['token'])['total']==0)
 def course(name,enroll,status='PUBLISHED',count=0):
  c=data('create '+name,'/learning/courses','POST',{'title':prefix+name,'description':'Disposable statistics verification.'},at);courses.append(c);p=f'/learning/courses/{c}'
  call('assign '+name,p+'/mentor','PUT',{'mentorId':actors['mentor']['id']},at)
  for u in enroll:call('enroll '+name,p+'/students','POST',{'studentId':u},at)
  chapters=[]
  for i in range(count):chapters.append(data('chapter '+name,p+'/chapters','POST',{'title':'Step '+str(i+1),'body':'One small step.','status':'PUBLISHED','sortOrder':i,'languageCode':'en'},mt))
  if status!='DRAFT':call('publish '+name,p+'/status','PUT',{'status':status},at)
  return c,chapters
 c1,ch=course('Main course',[sid,oid],count=3);c2,ch2=course('Second course',[sid],count=1)
 foreign,fc=course('Other student only',[oid],count=2);draft,dc=course('Unpublished course',[sid],status='DRAFT',count=1)
 archived,ac=course('Archived course',[sid],count=1);removed,rc=course('Former course',[sid],count=1)
 for cid in ch[:2]:call('complete own chapter',f'/client/learning/chapters/{cid}/completion','PUT',{'completed':True},st)
 for cid in ch+fc:call('complete other student chapter',f'/client/learning/chapters/{cid}/completion','PUT',{'completed':True},ot)
 for cid in ac+rc:call('complete formerly accessible chapter',f'/client/learning/chapters/{cid}/completion','PUT',{'completed':True},st)
 call('archive fixture course',f'/learning/courses/{archived}/status','PUT',{'status':'ARCHIVED'},at)
 call('remove fixture enrollment',f'/learning/courses/{removed}/students/{sid}','DELETE',token=at)
 assignments=[]
 for i in range(7):
  b={'courseId':c1,'title':'Practice '+str(i),'instructions':'Share one idea.','maxScore':10,'status':'DRAFT' if i==5 else 'PUBLISHED','version':0,'languageCode':'en'}
  aid=data('create practice '+str(i),'/assignments','POST',b,mt);assignments.append(aid)
  if i==6:call('archive practice',f'/assignments/{aid}','PUT',{**b,'status':'ARCHIVED'},mt);continue
  if i not in [1,2,3,4]:continue
  p=f'/client/assignments/{aid}';d=data('student saves draft',p+'/submission','PUT',{'version':0,'answerText':'Private statistics fixture answer.'},st)
  if i==1:continue
  w=data('student submits work',p+'/submit','POST',{'version':d['version'],'assignmentVersion':0,'answerText':'Private statistics fixture answer.','requestKey':str(uuid.uuid4())},st)
  if i in [3,4]:data('mentor reviews work',f'/assignments/{aid}/submissions/{w["id"]}/review','POST',{'version':w['version'],'decision':'GRADED' if i==3 else 'RETURNED','score':8 if i==3 else None,'feedback':'Private fixture feedback.'},mt)
 def stats():return data('read own learning totals','/client/learning/statistics',token=st)
 expected={'courseCount':2,'totalChapters':4,'completedChapters':2,'percent':50,'assignmentCount':5,'notSubmittedCount':2,'submittedCount':1,'gradedCount':1,'returnedCount':1}
 check('weighted progress and exact assignment states',stats()==expected)
 check('another student ID cannot change scope',data('attempt scope injection',f'/client/learning/statistics?studentId={oid}',token=st)==expected)
 check('no private answers in statistics','Private' not in json.dumps(stats()))
 page=call('current course page','/client/learning/statistics/courses?current=1&pageSize=1',token=st);check('pagination counts all accessible courses',page['total']==2 and len(page['rows'])==1)
 ids={row['courseId'] for row in call('all current rows','/client/learning/statistics/courses?current=1&pageSize=100',token=st)['rows']};check('draft archived removed and foreign courses excluded',ids=={c1,c2})
 body={'title':'Step 1','body':'One small step.','status':'DRAFT','sortOrder':0,'languageCode':'en'}
 call('unpublish completed chapter',f'/learning/chapters/{ch[0]}','PUT',body,mt);s=stats();check('unpublished progress excluded',s['totalChapters']==3 and s['completedChapters']==1 and s['percent']==33)
 call('republish chapter',f'/learning/chapters/{ch[0]}','PUT',{**body,'status':'PUBLISHED'},mt)
 call('temporarily archive main course',f'/learning/courses/{c1}/status','PUT',{'status':'ARCHIVED'},at);s=stats();check('archived course and its assignments vanish',s['courseCount']==1 and s['assignmentCount']==0)
 call('republish main course',f'/learning/courses/{c1}/status','PUT',{'status':'PUBLISHED'},at)
 call('revoke current enrollment',f'/learning/courses/{c1}/students/{sid}','DELETE',token=at);s=stats();check('revoked enrollment updates totals',s['courseCount']==1 and s['totalChapters']==1 and s['completedChapters']==0)
 call('restore enrollment',f'/learning/courses/{c1}/students','POST',{'studentId':sid},at)
 check('existing completion data is reused',stats()==expected)
 for i in range(9):course('Extra '+str(i),[sid])
 s=stats();check('empty courses do not distort percentage',s['courseCount']==11 and s['percent']==50)
 page=call('second statistics page','/client/learning/statistics/courses?current=2&pageSize=10',token=st);check('page two preserves total and rows',page['total']==11 and len(page['rows'])==1 and page['rows'][0]['courseId']==c1)
 fixture=W/'fixtures.json';fixture.write_text(json.dumps({'actors':{k:{x:y for x,y in v.items() if x!='token'} for k,v in actors.items()},'courseId':c1,'chapterId':ch[2],'prefix':prefix},indent=2),encoding='utf-8')
 if '--browser' in sys.argv:
  tools=json.loads((R/'.local/development-tools.json').read_text(encoding='utf-8-sig'));node=Path(tools['nodeHome'])/'node.exe';env=os.environ.copy();env['UPSELF_SETTINGS_FIXTURE']=str(fixture);env['UPSELF_SCREENSHOTS']=str(W)
  with (W/'browser.log').open('w',encoding='utf-8') as log:p=subprocess.run([str(node),str(R/'frontend/node_modules/@playwright/test/cli.js'),'test','settings-stats.spec.ts'],cwd=R/'frontend',env=env,stdout=log,stderr=subprocess.STDOUT)
  assert p.returncode==0,'Inspect .local/settings-stats-verification/browser.log'
  check('Chrome three-role defaults and statistics workflow',True)
 (W/'result.json').write_text(json.dumps({'apiChecks':len(checks),'browser':'passed' if '--browser' in sys.argv else 'not run'},indent=2),encoding='utf-8');print('API checks:',len(checks),flush=True)
try:main()
finally:
 if original is not None and admin:
  current=a.request('/accessibility/defaults')[1]['data'];r=a.request('/accessibility/defaults','PUT',{**original,'version':current['version']},admin)[1];assert r['code']==200,'Could not restore platform defaults.'
 for token,student in tokens:
  try:a.request('/client/logout' if student else '/logout','POST',token=token)
  except Exception:pass
 if users:
  ids=','.join(map(str,users));a.sql(f'DELETE FROM follow_up_reminder WHERE mentor_id IN ({ids}) OR student_id IN ({ids});')
  if courses:
   cs=','.join(map(str,courses));sub=f'SELECT id FROM assignment_submission WHERE student_id IN ({ids})'
   a.sql(f'DELETE FROM assignment_file WHERE submission_id IN ({sub});DELETE FROM assignment_attempt WHERE submission_id IN ({sub});DELETE FROM assignment_submission WHERE student_id IN ({ids});DELETE FROM assignment WHERE course_id IN ({cs});DELETE FROM learning_chapter_progress WHERE student_id IN ({ids});DELETE FROM learning_chapter WHERE course_id IN ({cs});DELETE FROM learning_enrollment WHERE course_id IN ({cs});DELETE FROM learning_course WHERE id IN ({cs});')
  a.sql(f'DELETE FROM sys_audit_log WHERE actor_id IN ({ids});DELETE FROM sys_user WHERE id IN ({ids});')
 fixture=W/'fixtures.json'
 if fixture.exists():fixture.unlink()
