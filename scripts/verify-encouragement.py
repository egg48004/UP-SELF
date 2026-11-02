"""Local FR-59 API/Chrome verification. Creates and removes only its own fixtures."""
from pathlib import Path
import importlib.util,json,os,re,secrets,subprocess,sys,uuid
from concurrent.futures import ThreadPoolExecutor
R=Path(__file__).resolve().parents[1];W=R/'.local/encouragement-verification';W.mkdir(exist_ok=True)
spec=importlib.util.spec_from_file_location('local_checks',R/'scripts/verify-local.py');a=importlib.util.module_from_spec(spec);spec.loader.exec_module(a)
a.BASE=os.environ.get('UPSELF_TEST_API_URL','http://127.0.0.1:8080');assert a.BASE in ['http://127.0.0.1:8080','http://127.0.0.1:8081']
prefix='enc_'+secrets.token_hex(4)+'_';users=[];courses=[];tokens=[];checks=[];actors={};fixture=W/'fixture.json'
def check(label,value):
 assert value,label
 checks.append(label);print('PASS',label,flush=True)
def call(label,path,method='GET',body=None,token=None,code=200):
 b=a.request(path,method,body,token)[1];assert b.get('code')==code,(label,b.get('code'),b.get('msg'))
 assert not re.search(r'[\u4e00-\u9fff]',b.get('msg',''));checks.append(label);print('PASS',label,flush=True);return b
def data(label,path,method='GET',body=None,token=None,code=200):return call(label,path,method,body,token,code).get('data')
def send(label,token,ids,text='Keep learning. One small step at a time.',key=None,code=200,**extra):
 return data(label,'/encouragement/messages','POST',{'body':text,'studentIds':ids,'clientKey':key or str(uuid.uuid4()),**extra},token,code)
def clear_limit(uid):
 for key in a.redis('KEYS',f'upself:rate_limit:{uid}-com.upself.web.controller.mentorship.EncouragementController-*') or []:a.redis('DEL',key)
def main():
 a.clear_our_rate_limits();demo=(R/'config/demo-accounts.yml').read_text(encoding='utf-8-sig');pwd=re.search(r'username: admin01\s+password: "([^"]+)"',demo).group(1)
 admin=call('administrator login','/empLogin','POST',{'username':'admin01','password':pwd})['token'];tokens.append((admin,False))
 config=(R/'config/application-local.yml').read_text(encoding='utf-8-sig');internalpwd=re.search(r'^\s{4}maintenance:\s*(.*?)\s*$',config,re.M).group(1).strip('"\'')
 internal=call('internal maintenance login','/empLogin','POST',{'username':'maintenance','password':internalpwd})['token'];tokens.append((internal,False))
 for name,role in [('mentor',20),('othermentor',20),('alice',10),('bob',10),('charlie',10),('outsider',10),('draftonly',10)]:
  username=prefix+name;password=secrets.token_urlsafe(18)
  uid=data('create '+name,'/system/user','POST',{'userName':username,'nickName':'DEMO '+name,'roleId':role,'password':password},admin);users.append(uid)
  token=call('login '+name,'/client/login' if role==10 else '/empLogin','POST',{'username':username,'password':password})['token'];tokens.append((token,role==10));actors[name]={'id':uid,'username':username,'password':password,'token':token}
 mt=actors['mentor']['token'];ot=actors['othermentor']['token'];st=actors['alice']['token'];bt=actors['bob']['token'];sid=actors['alice']['id'];bid=actors['bob']['id'];mid=actors['mentor']['id']
 pageusers=[]
 for i in range(9):
  uid=data('create paging student '+str(i),'/system/user','POST',{'userName':prefix+'page'+str(i),'nickName':f'DEMO page{i:02d}','roleId':10,'password':secrets.token_urlsafe(18)},admin);users.append(uid);pageusers.append(uid)
 def course(name,mentor,ids,published=True):
  cid=data('create '+name,'/learning/courses','POST',{'title':prefix+name,'description':'Disposable encouragement verification.'},admin);courses.append(cid);p=f'/learning/courses/{cid}'
  call('assign '+name,p+'/mentor','PUT',{'mentorId':mentor},admin)
  for uid in ids:call('enroll '+name+' '+str(uid),p+'/students','POST',{'studentId':uid},admin)
  if published:call('publish '+name,p+'/status','PUT',{'status':'PUBLISHED'},admin)
  return cid
 cid=course('main',mid,[sid,bid,actors['charlie']['id']]+pageusers);second=course('second',mid,[sid]);draft=course('draft',mid,[actors['draftonly']['id']],False);other=course('other',actors['othermentor']['id'],[sid,actors['outsider']['id']]);cp=f'/learning/courses/{cid}'
 for path in ['/encouragement/students','/encouragement/sent','/client/encouragement/inbox']:call('anonymous denied '+path,path,code=401)
 for name,token in [('administrator',admin),('maintenance',internal)]:
  for root in ['/encouragement','/client/encouragement']:
   expected=401 if root.startswith('/client/') else 403
   for path in ['/students','/sent','/inbox']:call(name+' cannot read '+root+path,root+path,token=token,code=expected)
   call(name+' cannot send '+root,root+'/messages','POST',{'body':'No','studentIds':[sid],'clientKey':str(uuid.uuid4())},token,expected)
 call('student cannot choose recipients','/client/encouragement/students',token=st,code=403)
 send('student token cannot use management channel',st,[sid],code=401)
 call('student cannot send through student channel','/client/encouragement/messages','POST',{'body':'No','studentIds':[sid],'clientKey':str(uuid.uuid4())},st,403)
 call('student cannot read sent history','/client/encouragement/sent',token=st,code=403)
 call('mentor cannot read student inbox','/encouragement/inbox',token=mt,code=403)
 page=call('mentor recipient pagination','/encouragement/students?current=1&pageSize=10',token=mt)
 check('recipient count deduplicates shared courses',page['total']==12 and len(page['rows'])==10 and len({p['id'] for p in page['rows']})==10)
 check('candidate fields contain no account secrets',all(set(p)=={'id','name','username'} for p in page['rows']))
 check('second recipient page works',len(call('candidate page two','/encouragement/students?current=2&pageSize=10',token=mt)['rows'])==2)
 search=call('search only current students','/encouragement/students?keyword='+actors['alice']['username'],token=mt);check('search returns a unique student',search['total']==1 and search['rows'][0]['id']==sid)
 data('prefill assigned student','/encouragement/students/'+str(sid),token=mt)
 data('prefill foreign student denied','/encouragement/students/'+str(actors['outsider']['id']),token=mt,code=403)
 call('long candidate search rejected','/encouragement/students?keyword='+'x'*101,token=mt,code=400)
 check('student inbox starts empty',call('empty inbox','/client/encouragement/inbox',token=actors['draftonly']['token'])['total']==0)
 single=send('single student delivery',mt,[sid],'You made a good start. 🌱',mentorId=actors['othermentor']['id'])
 check('sender receipt has no private routing fields',set(single)=={'id','body','recipientCount','sentAt'} and single['recipientCount']==1)
 key=str(uuid.uuid4());bulk=send('deduplicated bulk delivery',mt,[bid,sid,bid],'Keep going. You can ask for help.',key)
 check('bulk stores one message and two deliveries',bulk['recipientCount']==2 and a.sql(f'SELECT COUNT(*) FROM encouragement_recipient WHERE message_id={bulk["id"]}')=='2')
 duplicate=send('same request key returns original delivery',mt,[sid,bid],' Keep going. You can ask for help. ',key)
 check('idempotency keeps original message ID',duplicate['id']==bulk['id'])
 send('key cannot change text',mt,[sid,bid],'Changed text.',key,409);send('key cannot change recipients',mt,[sid],'Keep going. You can ask for help.',key,409)
 received=data('read own received message','/client/encouragement/inbox/'+str(bulk['id']),token=st)
 check('student projection hides all other recipients',set(received)=={'id','body','mentorName','sentAt'} and received['mentorName']=='DEMO mentor')
 data('unaddressed student cannot guess a message','/client/encouragement/inbox/'+str(single['id']),token=bt,code=404)
 check('forged inbox studentId cannot change owner',call('forged inbox owner ignored','/client/encouragement/inbox?studentId='+str(sid),token=bt)['total']==1)
 details=data('sender sees own recipient list','/encouragement/sent/'+str(bulk['id']),token=mt)
 check('sender recipient list contains exactly addressed students',{r['id'] for r in details['recipients']}=={sid,bid})
 data('another responsible mentor cannot inherit sent message','/encouragement/sent/'+str(bulk['id']),token=ot,code=404)
 send('other mentor sends independently',ot,[sid],'Another mentor can support your learning.')
 clear_limit(mid)
 for label,ids,text in [('empty recipients',[],'Hello'),('null student',[None],'Hello'),('invalid student',[0],'Hello'),('too many students',[sid]*101,'Hello'),('blank body',[sid],'   '),('long body',[sid],'x'*2001)]:send(label+' rejected',mt,ids,text,code=400)
 before=call('history before filter','/encouragement/sent',token=mt)['total']
 send('reused chat filter rejects hurtful text',mt,[sid,bid],'YOU ARE STUPID!',code=422)
 check('filter rejects entire batch without storing',call('history after filter','/encouragement/sent',token=mt)['total']==before)
 send('unrelated student rejected',mt,[actors['outsider']['id']],code=403)
 send('draft-only relationship rejected',mt,[actors['draftonly']['id']],code=403)
 send('mixed permitted and foreign recipients rejected atomically',mt,[sid,actors['outsider']['id']],code=403)
 check('invalid mixed batch stores no partial message',call('history after invalid batch','/encouragement/sent',token=mt)['total']==before)
 concurrent_key=str(uuid.uuid4());payload={'body':'Concurrent retry remains one message.','studentIds':[sid,bid],'clientKey':concurrent_key}
 with ThreadPoolExecutor(4) as pool:responses=list(pool.map(lambda _:a.request('/encouragement/messages','POST',payload,mt)[1],range(4)))
 check('concurrent retries deliver one copy per student',all(r.get('code')==200 for r in responses) and len({r['data']['id'] for r in responses})==1 and a.sql(f"SELECT COUNT(*) FROM encouragement_message WHERE mentor_id={mid} AND client_key='{concurrent_key}'")=='1')
 clear_limit(mid)
 for iteration in range(5):
  conflict_key=str(uuid.uuid4())
  with ThreadPoolExecutor(2) as pool:responses=list(pool.map(lambda uid:a.request('/encouragement/messages','POST',{'body':'One sender and one request key.','studentIds':[uid],'clientKey':conflict_key},mt)[1],[sid,bid]))
  assert sorted(r.get('code') for r in responses)==[200,409],('Concurrent outcomes',[(r.get('code'),r.get('msg')) for r in responses])
 check('five conflicting concurrent payload races each have one winner',True)
 call('remove one shared enrollment',cp+'/students/'+str(sid),'DELETE',token=admin)
 send('another qualifying course preserves send access',mt,[sid],'Your other course still connects us.')
 call('remove final qualifying enrollment',f'/learning/courses/{second}/students/{sid}','DELETE',token=admin)
 send('removed relationship cannot receive new sends',mt,[sid],code=403)
 check('delivered messages remain in student history',call('student keeps received messages','/client/encouragement/inbox',token=st)['total']>=4)
 data('author retains their own sent record','/encouragement/sent/'+str(single['id']),token=mt)
 check('retry after relationship removal does not resend',send('old receipt can be recovered',mt,[sid,bid],'Keep going. You can ask for help.',key)['id']==bulk['id'])
 for c in [cid,second]:call('restore enrollment '+str(c),f'/learning/courses/{c}/students','POST',{'studentId':sid},admin)
 call('disable recipient','/system/user','PUT',{'id':sid,'nickName':'DEMO alice','roleId':10,'status':'1'},admin)
 send('disabled recipient rejected',mt,[sid],code=403)
 call('disabled student session revoked','/client/encouragement/inbox',token=st,code=401)
 call('restore recipient','/system/user','PUT',{'id':sid,'nickName':'DEMO alice','roleId':10,'status':'0'},admin)
 st=call('student signs in after account restoration','/client/login','POST',{'username':actors['alice']['username'],'password':actors['alice']['password']})['token'];tokens.append((st,True));actors['alice']['token']=st
 for c in [cid,second]:call('reassign '+str(c),f'/learning/courses/{c}/mentor','PUT',{'mentorId':actors['othermentor']['id']},admin)
 send('former mentor cannot send',mt,[sid],code=403)
 check('replacement mentor only sees their own history',call('replacement history remains author-scoped','/encouragement/sent',token=ot)['total']==1)
 for c in [cid,second]:call('restore mentor '+str(c),f'/learning/courses/{c}/mentor','PUT',{'mentorId':mid},admin)
 clear_limit(mid)
 # Deterministic history pages without consuming the user send rate cap.
 statements=[]
 for i in range(11):statements.append(f"INSERT INTO encouragement_message(mentor_id,body,client_key,recipient_count) VALUES({mid},'History message {i:02d}','{uuid.uuid4()}',1);INSERT INTO encouragement_recipient(message_id,student_id) VALUES(LAST_INSERT_ID(),{sid});")
 a.sql(''.join(statements));page=call('history page two uses table total','/encouragement/sent?current=2&pageSize=10',token=mt)
 check('history page total and size are correct',page['total']>10 and len(page['rows'])>0 and len(page['rows'])<=10)
 check('inbox pagination uses current owner',call('inbox page two','/client/encouragement/inbox?current=2&pageSize=10',token=st)['total']>10)
 gid=data('create peer-hide check group',f'/chat/courses/{cid}/groups','POST',{'title':'Encouragement peer-hide check'},mt)
 for uid in [sid,bid]:call('add group member '+str(uid),f'/chat/groups/{gid}/members','POST',{'studentId':uid},mt)
 call('student hides peer messages',f'/client/chat/groups/{gid}/hidden-peers/{bid}','PUT',token=st)
 support=send('peer hide cannot suppress teacher encouragement',mt,[sid],'Your mentor messages remain visible.')
 data('encouragement remains readable with hidden classmate','/client/encouragement/inbox/'+str(support['id']),token=st)
 check('Flyway V17 applied once',a.sql("SELECT COUNT(*) FROM flyway_schema_history WHERE version='17' AND success=1")=='1')
 if '--browser' in sys.argv:
  clear_limit(mid);fixture.write_text(json.dumps({'actors':actors,'courseId':cid,'admin':{'username':'admin01','password':pwd}}),encoding='utf-8')
  env=os.environ.copy();env['UPSELF_ENCOURAGEMENT_FIXTURE']=str(fixture);env['UPSELF_SCREENSHOTS']=str(W)
  node=Path(json.loads((R/'.local/development-tools.json').read_text(encoding='utf-8-sig'))['nodeHome']);env['PATH']=str(node)+os.pathsep+env.get('PATH','')
  result=subprocess.run([str(node/'node.exe'),str(R/'frontend/node_modules/@playwright/test/cli.js'),'test','tests/e2e/encouragement.spec.ts'],cwd=R/'frontend',env=env)
  check('Chrome three-role encouragement workflow',result.returncode==0)
 clear_limit(mid);limited=False
 for i in range(21):
  b=a.request('/encouragement/messages','POST',{'body':'Rate check '+str(i),'studentIds':[sid],'clientKey':str(uuid.uuid4())},mt)[1]
  if b.get('code')==429:limited=True;break
  assert b.get('code')==200
 check('per-mentor send rate limit enforced',limited);send('another mentor remains able to send',ot,[sid],'Other mentors are not rate limited by a shared IP.')
 summary={'checks':len(checks),'browser':'passed' if '--browser' in sys.argv else 'not requested','schema':'V17'};(W/'result.json').write_text(json.dumps(summary,indent=2),encoding='utf-8');print(json.dumps(summary),flush=True)
def cleanup():
 fixture.unlink(missing_ok=True)
 for token,student in tokens:
  try:a.request('/client/logout' if student else '/logout','POST',token=token)
  except Exception:pass
 for uid in users:clear_limit(uid)
 owned=','.join(str(uid) for uid in users)
 if owned:a.sql(f'DELETE r FROM encouragement_recipient r JOIN encouragement_message m ON m.id=r.message_id WHERE m.mentor_id IN({owned});DELETE FROM encouragement_message WHERE mentor_id IN({owned});')
 for cid in courses:
  if a.sql(f"SELECT COUNT(*) FROM learning_course WHERE id={cid} AND title LIKE '{prefix}%'")!='1':continue
  a.sql(f'DELETE FROM chat_message WHERE group_id IN(SELECT id FROM chat_group WHERE course_id={cid});DELETE FROM chat_member WHERE group_id IN(SELECT id FROM chat_group WHERE course_id={cid});DELETE FROM chat_group WHERE course_id={cid};DELETE FROM learning_enrollment WHERE course_id={cid};DELETE FROM learning_course WHERE id={cid};')
 for uid in users:a.sql(f"DELETE FROM sys_user WHERE id={uid} AND user_name LIKE '{prefix}%'")
if __name__=='__main__':
 try:main()
 finally:cleanup()
