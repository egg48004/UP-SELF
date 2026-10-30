"""Local-only chat safety checks. All actors, courses and uploads are disposable."""
from pathlib import Path
import base64, importlib.util, json, os, re, secrets, subprocess, sys, time, uuid
from concurrent.futures import ThreadPoolExecutor

R=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('chat_checks',R/'scripts/verify-chat.py')
v=importlib.util.module_from_spec(spec);spec.loader.exec_module(v)
a=v.a;a.BASE=os.environ.get('UPSELF_TEST_API_URL','http://127.0.0.1:8080')
if a.BASE not in ('http://127.0.0.1:8080','http://127.0.0.1:8081'):raise RuntimeError('Local checks only')
W=R/'.local/chat-safety-verification';W.mkdir(exist_ok=True)
actors={};checks=[];performance={};fixture=W/'fixture.json'

def check(label,condition):
 assert condition,label
 checks.append(label);print('PASS',label,flush=True)
def call(label,path,method='GET',body=None,token=None,code=200):
 result=v.call(label,path,method,body,token,code);checks.append(label);return result
def send(label,gid,token,text,files=None,management=False,code=200,**extra):
 return call(label,('/chat' if management else '/client/chat')+f'/groups/{gid}/messages','POST',{'body':text,'fileIds':files or [],'clientKey':str(uuid.uuid4()),**extra},token,code).get('data')
def bodyrows(gid,token,management=False,query=''):
 return a.request(('/chat' if management else '/client/chat')+f'/groups/{gid}/messages'+query,token=token)[1]['data']
def upload(label,gid,token,name,raw,alt='',code=200,management=False):
 return v.upload(label,gid,token,name,raw,alt,code,management)

def main():
 a.clear_our_rate_limits()
 config=(R/'config/demo-accounts.yml').read_text(encoding='utf-8-sig')
 password=re.search(r'^  password:\s*(.*?)\s*$',config,re.M).group(1).strip('"\'')
 admin=call('administrator signs in','/empLogin','POST',{'username':'admin01','password':password})['token'];v.tokens.append((admin,False))
 local=(R/'config/application-local.yml').read_text(encoding='utf-8-sig')
 internal_password=re.search(r'^\s{4}maintenance:\s*(.*?)\s*$',local,re.M).group(1).strip('"\'')
 internal=call('internal maintenance signs in','/empLogin','POST',{'username':'maintenance','password':internal_password})['token'];v.tokens.append((internal,False))
 for name,role in [('mentor',20),('othermentor',20),('alice',10),('bob',10),('charlie',10),('outsider',10)]:
  pwd=secrets.token_urlsafe(18);username=v.prefix+name
  uid=call('create '+name,'/system/user','POST',{'userName':username,'nickName':'DEMO '+name,'roleId':role,'password':pwd},admin)['data'];v.users.append(uid)
  token=call('sign in '+name,'/client/login' if role==10 else '/empLogin','POST',{'username':username,'password':pwd})['token'];v.tokens.append((token,role==10));actors[name]={'id':uid,'token':token,'username':username,'password':pwd}
 mt=actors['mentor']['token'];st=actors['alice']['token'];bt=actors['bob']['token'];ct=actors['charlie']['token'];ot=actors['othermentor']['token'];bob=actors['bob']['id'];alice=actors['alice']['id']
 cid=call('create disposable course','/learning/courses','POST',{'title':v.prefix,'description':'Disposable safety verification'},admin)['data'];v.courses.append(cid);cp=f'/learning/courses/{cid}'
 call('assign mentor',cp+'/mentor','PUT',{'mentorId':actors['mentor']['id']},admin)
 for name in ['alice','bob','charlie']:call('enroll '+name,cp+'/students','POST',{'studentId':actors[name]['id']},admin)
 call('publish course',cp+'/status','PUT',{'status':'PUBLISHED'},admin)
 groups=[]
 for title in ['Safety study group','Other study group','Long history group']:
  gid=call('create '+title,f'/chat/courses/{cid}/groups','POST',{'title':title},mt)['data'];groups.append(gid)
  for name in ['alice','bob','charlie']:call('add '+name+' to '+title,f'/chat/groups/{gid}/members','POST',{'studentId':actors[name]['id']},mt)
 gid,other,history=groups;sp=f'/client/chat/groups/{gid}';gp=f'/chat/groups/{gid}';hide=sp+f'/hidden-peers/{bob}'
 png=base64.b64decode('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jRZkAAAAASUVORK5CYII=');raw=v.audio()
 (W/'diagram.png').write_bytes(png);(W/'voice.wav').write_bytes(raw);(W/'notes.txt').write_text('One learning step.',encoding='utf-8')
 call('anonymous sync denied',sp+'/sync',code=401)
 call('outsider sync denied',sp+'/sync',token=actors['outsider']['token'],code=403)
 call('other mentor sync denied',gp+'/sync',token=ot,code=403)
 call('administrator sync denied',gp+'/sync',token=admin,code=403)
 call('maintenance wildcard cannot read chat',gp+'/sync',token=internal,code=403)
 call('maintenance wildcard cannot hide peers',gp+f'/hidden-peers/{bob}','PUT',token=internal,code=403)
 for token,label in [(mt,'mentor'),(admin,'administrator'),(ot,'other mentor')]:
  call(label+' cannot hide peers',gp+f'/hidden-peers/{bob}','PUT',token=token,code=403)
 call('student cannot hide self',sp+f'/hidden-peers/{alice}','PUT',token=st,code=400)
 call('student cannot hide mentor',sp+f'/hidden-peers/{actors["mentor"]["id"]}','PUT',token=st,code=400)
 call('student cannot hide outsider',sp+f'/hidden-peers/{actors["outsider"]["id"]}','PUT',token=st,code=400)
 for i,text in enumerate(['YOU ARE STUPID!','you...are\tstupid','ｙｏｕ ａｒｅ ｓｔｕｐｉｄ','kau bodoh','I will kill you']):send('student filter '+str(i),gid,st,text,code=422)
 send('mentor follows the same filter',gid,mt,'You are an idiot',management=True,code=422)
 own=send('learning and help-seeking language allowed',gid,st,'Class assignment business. I feel sad. Please help me understand.')
 peer=send('client cannot forge teacher role',gid,bt,'A peer explanation.',senderRole='mentor')
 check('sender role comes from login context',peer['senderRole']=='student')
 teacher=send('mentor teaching and meeting links allowed',gid,mt,'Meeting: https://meet.example.com/class. One step at a time.',management=True)
 send('third student participates',gid,ct,'I can help with the next step.')
 send('other group remains independent',other,bt,'This message belongs to another group.')
 upload('hurtful file name rejected',gid,bt,'you-are-stupid.txt',b'Notes',code=422)
 upload('hurtful image description rejected',gid,bt,'diagram.png',png,'you are stupid',code=422)
 image=upload('peer image upload',gid,bt,'diagram.png',png,'A learning step diagram')
 voice=upload('peer voice upload',gid,bt,'voice.wav',raw)
 doc=upload('peer file upload',gid,bt,'notes.txt',b'One learning step.')
 media=send('peer shares image file and voice',gid,bt,'My files and voice.',[image['id'],voice['id'],doc['id']])
 pending=upload('keep draft on blocked message',gid,st,'notes.txt',b'Unsent notes')
 send('filtered send keeps uploaded draft',gid,st,'You are stupid',[pending['id']],code=422)
 check('rejected send leaves draft unattached',a.sql(f'SELECT message_id IS NULL FROM chat_file WHERE id={pending["id"]}')=='1')
 send('corrected message sends original draft',gid,st,'Please explain the instructions.',[pending['id']])
 # Explicitly model pre-V16 and historical teacher records; never infer their role from today's account.
 a.sql(f"INSERT INTO chat_message(group_id,sender_id,body,client_key) VALUES({gid},{bob},'Legacy message stays visible','{uuid.uuid4()}'); INSERT INTO chat_message(group_id,sender_id,sender_role,body,client_key) VALUES({gid},{bob},'mentor','Historical teacher message stays visible','{uuid.uuid4()}');")
 call('hide peer',hide,'PUT',token=st);call('hide is idempotent',hide,'PUT',token=st)
 hidden=bodyrows(gid,st);hidden_ids={m['id'] for m in hidden['rows']}
 check('peer text and media absent from backend page',peer['id'] not in hidden_ids and media['id'] not in hidden_ids)
 check('own and mentor messages remain visible',own['id'] in hidden_ids and teacher['id'] in hidden_ids)
 check('legacy and historical teaching preserved',{'Legacy message stays visible','Historical teacher message stays visible'}.issubset({m['body'] for m in hidden['rows']}))
 check('peer still sees the viewer and own message',own['id'] in {m['id'] for m in bodyrows(gid,bt)['rows']} and peer['id'] in {m['id'] for m in bodyrows(gid,bt)['rows']})
 check('third student and mentor see hidden peer normally',peer['id'] in {m['id'] for m in bodyrows(gid,ct)['rows']} and peer['id'] in {m['id'] for m in bodyrows(gid,mt,True)['rows']})
 check('hide does not affect other group',len(bodyrows(other,st)['rows'])==1)
 snapshot=call('combined sync returns own visibility',sp+'/sync',token=st)['data'];check('own hidden list returned',snapshot['hiddenStudentIds']==[bob])
 check('other student cannot see viewer hide settings',call('peer visibility remains private',sp+'/sync',token=bt)['data']['hiddenStudentIds']==[])
 for f,data in [(image,png),(voice,raw),(doc,b'One learning step.')]:
  v.v.content('hidden peer attachment denied',f'/client/chat/files/{f["id"]}/content',st,code=403)
  v.v.content('mentor still accesses peer attachment',f'/chat/files/{f["id"]}/content',mt,data)
  v.v.content('file owner retains own attachment',f'/client/chat/files/{f["id"]}/content',bt,data)
 call('show peer restores messages',hide,'DELETE',token=st);check('restored peer history',peer['id'] in {m['id'] for m in bodyrows(gid,st)['rows']})
 v.v.content('show peer restores file access',f'/client/chat/files/{voice["id"]}/content',st,raw)
 # The cap and cursor are tested with deterministic fixture rows, avoiding artificial send spam.
 values=','.join(f"({history},{bob},'student','History {i:03d}','{uuid.uuid4()}')" for i in range(260))
 a.sql('INSERT INTO chat_message(group_id,sender_id,sender_role,body,client_key) VALUES'+values)
 batch=bodyrows(history,st,query='?afterId=0');check('incremental batch is bounded',len(batch['rows'])==30 and batch['hasMore'])
 collected=[];cursor=0
 while True:
  b=bodyrows(history,st,query=f'?afterId={cursor}');collected.extend(m['id'] for m in b['rows']);cursor=b['nextCursor']
  if not b['hasMore']:break
 check('cursor drains all messages once and in order',len(collected)==260 and collected==sorted(set(collected)))
 call('hide long peer history',f'/client/chat/groups/{history}/hidden-peers/{bob}','PUT',token=st)
 hidden_tail=bodyrows(history,st,query='?afterId=0');check('hidden-only history advances to committed high water',hidden_tail['rows']==[] and hidden_tail['nextCursor']==collected[-1] and not hidden_tail['hasMore'])
 later=send('message after hidden tail remains reachable',history,ct,'Visible after hidden history.')
 check('no visible message skipped after hidden tail',bodyrows(history,st,query=f'?afterId={hidden_tail["nextCursor"]}')['rows'][0]['id']==later['id'])
 call('restore long peer history',f'/client/chat/groups/{history}/hidden-peers/{bob}','DELETE',token=st)
 # Current teaching information remains visible even if a formerly hidden student becomes the course mentor.
 call('hide before role change',hide,'PUT',token=st)
 a.sql(f'UPDATE sys_user SET role_id=20 WHERE id={bob}')
 try:
  call('assign former peer as current mentor',cp+'/mentor','PUT',{'mentorId':bob},admin)
  check('current mentor old messages cannot be hidden',peer['id'] in {m['id'] for m in bodyrows(gid,st)['rows']})
  call('current mentor target cannot be hidden',hide,'PUT',token=st,code=400)
 finally:
  call('restore original course mentor',cp+'/mentor','PUT',{'mentorId':actors['mentor']['id']},admin);a.sql(f'UPDATE sys_user SET role_id=10 WHERE id={bob}')
 call('remove hidden peer',gp+f'/members/{bob}','DELETE',token=mt)
 member=call('removed hidden peer can still be restored in Members',sp+'/members',token=st)['data']
 check('removed hidden peer remains in own settings only',any(m['studentId']==bob and m['hidden'] and not m['active'] for m in member))
 call('show removed peer is allowed',hide,'DELETE',token=st);call('restore peer membership',gp+'/members','POST',{'studentId':bob},mt)
 call('close group',gp,'PUT',{'title':'Safety study group','status':'CLOSED','version':0},mt)
 call('closed group still allows own visibility setting',hide,'PUT',token=st)
 call('closed group sync readable',sp+'/sync',token=st)
 send('closed group still rejects sends',gid,st,'Hello',code=409)
 call('restore closed group visibility',hide,'DELETE',token=st)
 call('reopen group',gp,'PUT',{'title':'Safety study group','status':'ACTIVE','version':1},mt)
 call('remove student membership',gp+f'/members/{alice}','DELETE',token=mt)
 call('removed member cannot change hide settings',hide,'PUT',token=st,code=403);call('removed member cannot sync',sp+'/sync',token=st,code=403)
 call('restore student membership',gp+'/members','POST',{'studentId':alice},mt)
 call('legacy messages endpoint remains compatible',sp+'/messages',token=st)
 call('invalid sync cursor rejected',sp+'/sync?afterId=-1',token=st,code=400)
 check('Flyway V16 applied once',a.sql("SELECT COUNT(*) FROM flyway_schema_history WHERE version='16' AND success=1")=='1')
 # Thirty browser-equivalent polling clients, using the existing test memberships.
 def polling_client(index):
  token=[st,bt,ct][index%3];latencies=[]
  for cycle in range(3):
   start=time.perf_counter();status,result=a.request(sp+'/sync?afterId=0',token=token);elapsed=time.perf_counter()-start
   assert status==200 and result.get('code')==200;latencies.append(elapsed)
   if cycle<2:time.sleep(max(0,5-elapsed))
  return latencies
 def other_work():
  timings=[]
  for i in range(6):
   path=[f'/client/learning/courses/{cid}','/client/learning/statistics',f'/client/assignments?courseId={cid}'][i%3]
   start=time.perf_counter();r=a.request(path,token=st)[1];assert r.get('code')==200;timings.append(time.perf_counter()-start);time.sleep(1)
   if i==0:upload('upload during concurrent polling',gid,st,'concurrent-notes.txt',b'A'*(1024*1024))
  return timings
 with ThreadPoolExecutor(max_workers=31) as pool:
  other_future=pool.submit(other_work);futures=[pool.submit(polling_client,i) for i in range(30)]
  samples=sorted(x for future in futures for x in future.result());other_samples=other_future.result()
 performance.update({'clients':30,'cycles':3,'successfulPolls':len(samples),'pollMedianMs':round(samples[len(samples)//2]*1000,1),'pollP95Ms':round(samples[int(len(samples)*.95)-1]*1000,1),'pollMaxMs':round(max(samples)*1000,1),'otherPageMaxMs':round(max(other_samples)*1000,1),'scope':'Local Windows concurrency smoke check; not a VPS capacity benchmark.'})
 check('30 polling clients and learning pages succeed together',len(samples)==90 and len(other_samples)==6)
 print('PERFORMANCE',json.dumps(performance),flush=True)
 if '--browser' in sys.argv:
  fixture.write_text(json.dumps({'actors':actors,'courseId':cid,'groupId':gid,'historyGroupId':history,'adminPassword':password}),encoding='utf-8')
  env=os.environ.copy();env['UPSELF_CHAT_SAFETY_FIXTURE']=str(fixture);env['UPSELF_SCREENSHOTS']=str(W)
  node=Path(json.loads((R/'.local/development-tools.json').read_text(encoding='utf-8-sig'))['nodeHome']);env['PATH']=str(node)+os.pathsep+env.get('PATH','')
  process=subprocess.run([str(node/'node.exe'),str(R/'frontend/node_modules/@playwright/test/cli.js'),'test','tests/e2e/chat-safety.spec.ts'],cwd=R/'frontend',env=env)
  check('Chrome three-role safety workflow',process.returncode==0)
 # Rate limits are scoped to a user, so classmates sharing an IP are unaffected.
 blocked=False
 for i in range(65):
  response=a.request(sp+'/messages','POST',{'body':f'Rate check {i}','fileIds':[],'clientKey':str(uuid.uuid4())},st)[1]
  if response.get('code')==429:blocked=True;break
  assert response.get('code')==200
 check('per-user send rate limit enforced',blocked)
 send('another student sharing localhost remains able to send',gid,ct,'Classmates can still send.')
 result={'apiAndBrowserChecks':len(checks)+len(v.v.checks),'performance':performance,'schema':'V16','browser':'passed' if '--browser' in sys.argv else 'not requested'}
 (W/'result.json').write_text(json.dumps(result,indent=2),encoding='utf-8');print(json.dumps(result,indent=2),flush=True)

def cleanup():
 fixture.unlink(missing_ok=True)
 # Restore only this run's role-change fixture if a check was interrupted.
 if 'bob' in actors:a.sql(f"UPDATE sys_user SET role_id=10 WHERE id={actors['bob']['id']} AND user_name='{actors['bob']['username']}'")
 for uid in v.users:
  for key in a.redis('KEYS',f'upself:rate_limit:{uid}-com.upself.web.controller.chat.ChatController-*') or []:a.redis('DEL',key)
 v.cleanup()

if __name__=='__main__':
 try:main()
 finally:cleanup()
