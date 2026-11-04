"""Local FR-26 verification. Only disposable badge_ fixtures are created/removed.
Run with the built application on localhost. Optional --pymysql-path selects an
already prepared test-only client when Windows blocks mysql.exe. No app dependency.
"""
from pathlib import Path
import argparse,concurrent.futures,json,os,re,secrets,subprocess,sys,urllib.request,urllib.error,uuid
R=Path(__file__).resolve().parents[1];W=R/'.local/badge-verification';W.mkdir(exist_ok=True)
p=argparse.ArgumentParser();p.add_argument('--browser',action='store_true');p.add_argument('--pymysql-path');p.add_argument('--keep-fixture',action='store_true');p.add_argument('--cleanup-fixture',action='store_true');args=p.parse_args()
C=json.loads((R/'migration/local-snapshot/local-credentials.json').read_text(encoding='utf-8-sig'))
if args.pymysql_path:sys.path.insert(0,args.pymysql_path);import pymysql
BASE='http://127.0.0.1:8080';prefix='badge_'+secrets.token_hex(4)+'_';checks=[];users=[];courses=[];triggers=[];actors={};audit_floor=None
def check(label,value):
 assert value,label
 checks.append(label);print('PASS',label,flush=True)
def call(label,path,method='GET',body=None,token=None,code=200):
 req=urllib.request.Request(BASE+path,data=json.dumps(body).encode() if body is not None else None,method=method,headers={'Content-Type':'application/json',**({'Authorization':'Bearer '+token} if token else {})})
 try:r=urllib.request.urlopen(req,timeout=25)
 except urllib.error.HTTPError as e:r=e
 with r:b=json.load(r)
 assert b.get('code')==code,(label,b.get('code'),b.get('msg'))
 check(label,True);return b
def sql(statement):
 if args.pymysql_path:
  conn=pymysql.connect(host='127.0.0.1',port=3306,user='root',password=C['mysql_root_password'],database='up_self_dev',charset='utf8mb4',autocommit=True)
  try:
   with conn.cursor() as q:q.execute(statement);return list(q.fetchall())
  finally:conn.close()
 saved=R/'.local/service-tools.json'
 home=Path(json.loads(saved.read_text(encoding='utf-8-sig'))['mysqlHome']) if saved.exists() else Path(json.loads((R/'.local/development-tools.json').read_text(encoding='utf-8-sig'))['serviceHome'])/'mysql'
 cfg=W/'root-client.cnf';cfg.write_text('[client]\nuser=root\npassword="'+C['mysql_root_password'].replace('\\','\\\\').replace('"','\\"')+'"\nhost=127.0.0.1\nport=3306\nprotocol=tcp\n',encoding='utf-8')
 run=subprocess.run([str(home/'bin/mysql.exe'),'--defaults-extra-file='+str(cfg),'--batch','--skip-column-names','--default-character-set=utf8mb4','up_self_dev','-e',statement],capture_output=True,text=True,encoding='utf-8')
 if run.returncode:raise RuntimeError('Local SQL failed; no credentials were logged')
 return [tuple(line.split('\t')) for line in run.stdout.splitlines()]
def scalar(s):return int(sql(s)[0][0])
def badge_list(who):return call(who+' reads own badge catalog','/client/learning/badges',token=actors[who]['token'])['data']
def count(who,code=None):return scalar("SELECT COUNT(*) FROM student_badge WHERE student_id="+str(actors[who]['id'])+(" AND badge_code='"+code+"'" if code else ''))
def main():
 global audit_floor
 audit_floor=scalar('SELECT COALESCE(MAX(id),0) FROM sys_audit_log')
 # Verify only source students, before disposable records exist.
 expected={}
 for uid,code in sql("SELECT DISTINCT p.student_id,'FIRST_STEP' FROM learning_chapter_progress p JOIN sys_user u ON u.id=p.student_id AND u.role_id=10 WHERE p.completed_at IS NOT NULL UNION SELECT DISTINCT s.student_id,'FIRST_SUBMISSION' FROM assignment_submission s JOIN assignment_attempt a ON a.submission_id=s.id JOIN sys_user u ON u.id=s.student_id AND u.role_id=10 UNION SELECT DISTINCT g.student_id,'GOAL_REACHED' FROM personal_goal g JOIN sys_user u ON u.id=g.student_id AND u.role_id=10 WHERE g.status='COMPLETED' AND g.completed_at IS NOT NULL"):
  expected[(int(uid),str(code))]=True
 saved={(int(uid),str(code)) for uid,code in sql('SELECT student_id,badge_code FROM student_badge')}
 check('V18 credits all verifiable existing evidence',set(expected).issubset(saved))
 check('V18 grants badge permission only to the public student role',sql('SELECT role_id FROM sys_role_menu WHERE menu_id=1600') in [[(10,)],[('10',)]])
 check('V18 completed successfully',scalar("SELECT COUNT(*) FROM flyway_schema_history WHERE version='18' AND success=1")==1)
 password=re.search(r'username: admin01\s+password: "([^"]+)"',(R/'config/demo-accounts.yml').read_text(encoding='utf-8-sig')).group(1)
 admin=call('admin login','/empLogin','POST',{'username':'admin01','password':password})['token']
 for name,role in [('mentor',20),('alice',10),('bob',10),('race',10),('rollback',10)]:
  pwd=secrets.token_urlsafe(18)
  uid=call('create '+name,'/system/user','POST',{'userName':prefix+name,'nickName':'Badge test '+name,'roleId':role,'password':pwd},admin)['data'];users.append(uid)
  token=call('login '+name,'/client/login' if role==10 else '/empLogin','POST',{'username':prefix+name,'password':pwd})['token']
  actors[name]={'id':uid,'username':prefix+name,'password':pwd,'token':token}
 st=actors['alice']['token'];mt=actors['mentor']['token'];rt=actors['rollback']['token']
 for who in ['alice','bob','race','rollback']:
  rows=badge_list(who);check(who+' starts with three unearned private badges',len(rows)==3 and all(r['awardedAt'] is None and set(r)=={'code','awardedAt'} for r in rows))
 call('anonymous badge read denied','/client/learning/badges',code=401)
 for label,tok in [('mentor',mt),('administrator',admin)]:call(label+' management token cannot enter student badge channel','/client/learning/badges',token=tok,code=401)
 call('student ID query cannot change ownership','/client/learning/badges?studentId='+str(actors['bob']['id']),token=st)
 check('query spoof cannot award or read another student',count('alice')==0 and count('bob')==0)
 cid=call('create course','/learning/courses','POST',{'title':prefix+'course','description':'Disposable badge verification.'},admin)['data'];courses.append(cid);cp='/learning/courses/'+str(cid)
 call('assign mentor',cp+'/mentor','PUT',{'mentorId':actors['mentor']['id']},admin)
 for who in ['alice','race','rollback']:call('enroll '+who,cp+'/students','POST',{'studentId':actors[who]['id']},admin)
 ch=call('mentor creates chapter',cp+'/chapters','POST',{'title':'First chapter','body':'One small step.','sortOrder':1,'status':'PUBLISHED','languageCode':'en'},mt)['data']
 call('publish course',cp+'/status','PUT',{'status':'PUBLISHED'},admin)
 completion='/client/learning/chapters/'+str(ch)+'/completion'
 call('unjoined chapter completion denied',completion,'PUT',{'completed':True},actors['bob']['token'],403)
 check('denied action has no badge',count('bob')==0)
 call('visit only','/client/learning/chapters/'+str(ch)+'/visit','POST',{},st)
 call('unmark before completion',completion,'PUT',{'completed':False},st)
 check('visit and unmark do not award',count('alice')==0)
 first=call('mark chapter complete',completion,'PUT',{'completed':True},st)
 check('first successful completion returns its new badge',first['earnedBadgeCode']=='FIRST_STEP')
 repeated=call('repeat chapter completion',completion,'PUT',{'completed':True},st)
 check('repeat is successful without a second award or toast',repeated['earnedBadgeCode'] is None and count('alice','FIRST_STEP')==1)
 call('undo completion',completion,'PUT',{'completed':False},st)
 check('undo keeps earlier milestone',count('alice','FIRST_STEP')==1)
 goal=call('create personal goal','/client/goals','POST',{'title':'Read one chapter','details':''},st)['data']
 check('creating a goal is not completing it',count('alice','GOAL_REACHED')==0)
 gp='/client/goals/'+str(goal)
 result=call('complete goal',gp+'/completion','PUT',{'completed':True,'version':0},st)
 check('goal returns its earned badge',result['earnedBadgeCode']=='GOAL_REACHED')
 call('stale goal completion rejected',gp+'/completion','PUT',{'completed':True,'version':0},st,409)
 call('reopen goal',gp+'/completion','PUT',{'completed':False,'version':1},st)
 call('delete reopened goal',gp+'?version=2','DELETE',token=st)
 check('goal reopening and deletion keep award',count('alice','GOAL_REACHED')==1)
 gids=[]
 for i in range(2):gids.append(call('create race goal '+str(i),'/client/goals','POST',{'title':'Concurrent milestone '+str(i),'details':''},actors['race']['token'])['data'])
 def complete_goal(g):return call('concurrent distinct goal completion','/client/goals/'+str(g)+'/completion','PUT',{'completed':True,'version':0},actors['race']['token'])
 with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:results=list(pool.map(complete_goal,gids))
 check('concurrent awards commit both goals with one new badge',sum(x['earnedBadgeCode']=='GOAL_REACHED' for x in results)==1 and count('race','GOAL_REACHED')==1)
 a=call('mentor publishes assignment','/assignments','POST',{'courseId':cid,'title':'First formal work','instructions':'Add an answer.','status':'PUBLISHED','maxScore':100,'languageCode':'en','dueAt':None},mt)['data']
 ap='/client/assignments/'+str(a)
 mine=call('get work draft',ap+'/submission',token=st)['data']
 draft=call('save draft',ap+'/submission','PUT',{'version':mine['version'],'answerText':'My first answer'},st)['data']
 check('assignment draft does not award',count('alice','FIRST_SUBMISSION')==0)
 detail=call('get current assignment',ap,token=st)['data'];key=str(uuid.uuid4())
 body={'version':draft['version'],'assignmentVersion':detail['version'],'answerText':'My first answer','requestKey':key}
 sent=call('formally submit assignment',ap+'/submit','POST',body,st)['data']
 check('formal submit returns milestone',sent['earnedBadgeCode']=='FIRST_SUBMISSION')
 again=call('retry formal request key',ap+'/submit','POST',body,st)['data']
 check('retry keeps one attempt and one badge',count('alice','FIRST_SUBMISSION')==1 and len(again['attempts'])==1 and not again.get('earnedBadgeCode'))
 reviewed=call('mentor returns assignment','/assignments/'+str(a)+'/submissions/'+str(sent['id'])+'/review','POST',{'version':sent['version'],'decision':'RETURNED','score':None,'feedback':'Please add one example.'},mt)['data']
 check('mentor return does not remove milestone',count('alice','FIRST_SUBMISSION')==1)
 body.update(version=reviewed['version'],requestKey=str(uuid.uuid4()),answerText='Revised answer with an example.')
 second=call('submit revised assignment',ap+'/submit','POST',body,st)['data']
 check('resubmission has no repeated badge',second.get('earnedBadgeCode') is None and count('alice')==3)
 # Force only this disposable account's award insert to fail and check all three transaction boundaries.
 rbgoal=call('create rollback goal','/client/goals','POST',{'title':'Rollback check','details':''},rt)['data']
 rdraft=call('rollback account draft',ap+'/submission',token=rt)['data']
 rollback_id=actors['rollback']['id'];trigger=prefix+'fail_award';assert re.fullmatch(r'badge_[a-f0-9]+_fail_award',trigger)
 sql("CREATE TRIGGER `"+trigger+"` BEFORE INSERT ON student_badge FOR EACH ROW BEGIN IF NEW.student_id="+str(rollback_id)+" THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Disposable badge failure'; END IF; END");triggers.append(trigger)
 try:
  call('chapter award failure rolls back completion',completion,'PUT',{'completed':True},rt,500)
  check('failed chapter award leaves no completed row',scalar('SELECT COUNT(*) FROM learning_chapter_progress WHERE student_id='+str(rollback_id)+' AND completed_at IS NOT NULL')==0)
  call('goal award failure rolls back completion','/client/goals/'+str(rbgoal)+'/completion','PUT',{'completed':True,'version':0},rt,500)
  check('failed goal award preserves original version',scalar('SELECT COUNT(*) FROM personal_goal WHERE id='+str(rbgoal)+" AND status='ACTIVE' AND version=0")==1)
  call('submission award failure rolls back attempt',ap+'/submit','POST',{'version':rdraft['version'],'assignmentVersion':detail['version'],'answerText':'Rollback work','requestKey':str(uuid.uuid4())},rt,500)
  check('failed submission inserts no attempt',scalar('SELECT COUNT(*) FROM assignment_attempt a JOIN assignment_submission s ON s.id=a.submission_id WHERE s.student_id='+str(rollback_id))==0)
  check('failed actions award nothing',count('rollback')==0)
 finally:sql('DROP TRIGGER `'+trigger+'`');triggers.remove(trigger)
 call('same goal succeeds after database recovers','/client/goals/'+str(rbgoal)+'/completion','PUT',{'completed':True,'version':0},rt)
 all_badges=badge_list('alice');check('student sees all three persisted milestones',len(all_badges)==3 and all(b['awardedAt'] for b in all_badges))
 bob=badge_list('bob');check('other student remains private and unearned',all(b['awardedAt'] is None for b in bob))
 call('remove old enrollment',cp+'/students/'+str(actors['alice']['id']),'DELETE',token=admin)
 check('loss of enrollment does not erase milestones',all(b['awardedAt'] for b in badge_list('alice')))
 call('restore disposable enrollment',cp+'/students','POST',{'studentId':actors['alice']['id']},admin)
 call('enroll fresh browser student',cp+'/students','POST',{'studentId':actors['bob']['id']},admin)
 fixture={'admin':{'username':'admin01','password':password},'actors':actors,'courseId':cid,'chapterId':ch,'assignmentId':a,'prefix':prefix,'auditFloor':audit_floor}
 (W/'fixture.json').write_text(json.dumps(fixture,indent=2),encoding='utf-8')
 return fixture

def cleanup():
 for t in list(triggers):sql('DROP TRIGGER IF EXISTS `'+t+'`')
 for name,actor in actors.items():
  if not actor.get('token'):continue
  path='/logout' if name=='mentor' else '/client/logout'
  try:
   req=urllib.request.Request(BASE+path,data=b'{}',method='POST',headers={'Content-Type':'application/json','Authorization':'Bearer '+actor['token']})
   with urllib.request.urlopen(req,timeout=8):pass
  except Exception:pass
 ids=','.join(str(x) for x in users)
 if not ids:return
 # Delete only records owned by the verified disposable user IDs and courses.
 for table,col in [('encouragement_recipient','student_id'),('student_badge','student_id'),('personal_goal','student_id'),('learning_chapter_progress','student_id')]:sql('DELETE FROM '+table+' WHERE '+col+' IN ('+ids+')')
 submissions=[int(r[0]) for r in sql('SELECT id FROM assignment_submission WHERE student_id IN ('+ids+')')]
 if submissions:
  ss=','.join(map(str,submissions));sql('DELETE FROM assignment_file WHERE submission_id IN ('+ss+')');sql('DELETE FROM assignment_attempt WHERE submission_id IN ('+ss+')');sql('DELETE FROM assignment_submission WHERE id IN ('+ss+')')
 if courses:
  cs=','.join(map(str,courses));sql('DELETE FROM assignment WHERE course_id IN ('+cs+')');sql('DELETE FROM learning_enrollment WHERE course_id IN ('+cs+')');sql('DELETE FROM learning_chapter WHERE course_id IN ('+cs+')');sql('DELETE FROM learning_course WHERE id IN ('+cs+')')
 sql('DELETE FROM sys_audit_log WHERE id>'+str(audit_floor)+' AND actor_id IN ('+ids+')');sql('DELETE FROM sys_audit_log WHERE id>'+str(audit_floor)+' AND target IN ('+','.join("'"+str(x)+"'" for x in users+courses)+') AND action IN (\'USER_CREATE\',\'COURSE_CREATE\',\'COURSE_ASSIGN\',\'COURSE_ENROLL\',\'COURSE_STATUS\')')
 sql('DELETE FROM sys_user WHERE id IN ('+ids+") AND user_name LIKE '"+prefix+"%'")

if __name__=='__main__':
 if args.cleanup_fixture:
  fixture=json.loads((W/'fixture.json').read_text(encoding='utf-8'))
  prefix=fixture['prefix'];audit_floor=int(fixture['auditFloor']);assert re.fullmatch(r'badge_[a-f0-9]+_',prefix)
  for actor in fixture['actors'].values():
   assert re.fullmatch(prefix+r'[a-z]+',actor['username'])
   assert scalar("SELECT COUNT(*) FROM sys_user WHERE id="+str(int(actor['id']))+" AND user_name='"+actor['username']+"'")==1
   users.append(int(actor['id']))
  cid=int(fixture['courseId']);assert scalar("SELECT COUNT(*) FROM learning_course WHERE id="+str(cid)+" AND title LIKE '"+prefix+"%'")==1
  actors.update(fixture['actors']);courses.append(cid);cleanup();(W/'fixture.json').unlink(missing_ok=True);print('Disposable badge fixtures cleaned.');sys.exit()
 succeeded=False
 try:
  fixture=main()
  if args.browser:
   tools=json.loads((R/'.local/development-tools.json').read_text(encoding='utf-8-sig'))
   node=Path(tools['nodeHome'])/'node.exe';env=os.environ.copy();env['UPSELF_BADGE_FIXTURE']=str(W/'fixture.json');env['UPSELF_SCREENSHOTS']=str(W)
   result=subprocess.run([str(node),str(R/'frontend/node_modules/@playwright/test/cli.js'),'test','tests/e2e/badges.spec.ts'],cwd=R/'frontend',env=env)
   check('Chrome three-role badge workflow',result.returncode==0)
  succeeded=True
  (W/'api-result.json').write_text(json.dumps({'passed':True,'checks':checks},indent=2),encoding='utf-8')
  print('API PASS:',len(checks),'checks.',flush=True)
 finally:
  if not (succeeded and args.keep_fixture):
   cleanup()
   fixture_file=W/'fixture.json'
   if fixture_file.exists() and json.loads(fixture_file.read_text(encoding='utf-8')).get('prefix')==prefix:fixture_file.unlink()
