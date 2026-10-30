"""Local course chat integration and Chrome checks, with disposable fixtures only."""
from pathlib import Path
import importlib.util, os, sys, json, re, secrets, uuid, base64, io, wave, struct, math, subprocess
from concurrent.futures import ThreadPoolExecutor
R=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('recording_checks',R/'scripts/verify-recordings.py')
v=importlib.util.module_from_spec(spec);spec.loader.exec_module(v)
a=v.a
a.BASE=os.environ.get('UPSELF_TEST_API_URL','http://127.0.0.1:8081')
if a.BASE not in ('http://127.0.0.1:8080','http://127.0.0.1:8081'):raise RuntimeError('Local verification only')
work=R/'.local/chat-verification';work.mkdir(exist_ok=True)
prefix='chat_check_'+secrets.token_hex(4)
users=[];courses=[];tokens=[];keys=set();checks=[]
def call(label,path,method='GET',body=None,token=None,code=200):
 r=a.request(path,method,body,token)[1]
 assert r.get('code')==code,(label,r.get('code'),r.get('msg'))
 checks.append(label);print('PASS',label,flush=True);return r
def upload(label,gid,token,name,raw,alt='',code=200,management=False):
 r=v.multipart(label,('/chat' if management else '/client/chat')+f'/groups/{gid}/files',token,{'altText':alt},raw,name,code)
 if code==200:
  key=a.sql(f"SELECT storage_key FROM chat_file WHERE id={int(r['data']['id'])}")
  keys.add(key)
 checks.append(label);return r.get('data')
def audio():
 b=io.BytesIO()
 with wave.open(b,'wb') as w:
  w.setnchannels(1);w.setsampwidth(2);w.setframerate(8000)
  w.writeframes(b''.join(struct.pack('<h',int(2000*math.sin(2*math.pi*440*i/8000))) for i in range(8000)))
 return b.getvalue()
def main():
 config=(R/'config/demo-accounts.yml').read_text(encoding='utf-8-sig')
 password=re.search(r'^  password:\s*(.*?)\s*$',config,re.M).group(1).strip('"\'')
 a.clear_our_rate_limits()
 admin=call('admin login','/empLogin','POST',{'username':'admin01','password':password})['token'];tokens.append((admin,False))
 actors={}
 for name,role in [('mentor',20),('othermentor',20),('alice',10),('bob',10),('nonmember',10),('outsider',10)]:
  pwd=secrets.token_urlsafe(18);username=prefix+name
  uid=call('create '+name,'/system/user','POST',{'userName':username,'nickName':'DEMO '+name,'roleId':role,'password':pwd},admin)['data'];users.append(uid)
  token=call('login '+name,'/client/login' if role==10 else '/empLogin','POST',{'username':username,'password':pwd})['token'];tokens.append((token,role==10));actors[name]={'id':uid,'token':token,'username':username,'password':pwd}
 mt=actors['mentor']['token'];st=actors['alice']['token'];bt=actors['bob']['token'];ot=actors['othermentor']['token']
 cid=call('create course','/learning/courses','POST',{'title':prefix,'description':'Disposable course chat verification'},admin)['data'];courses.append(cid);cp=f'/learning/courses/{cid}'
 call('assign course mentor',cp+'/mentor','PUT',{'mentorId':actors['mentor']['id']},admin)
 for name in ['alice','bob','nonmember']:call('enroll '+name,cp+'/students','POST',{'studentId':actors[name]['id']},admin)
 call('publish course',cp+'/status','PUT',{'status':'PUBLISHED'},admin)
 gid=call('mentor creates group',f'/chat/courses/{cid}/groups','POST',{'title':'Assignment study group'},mt)['data'];gp=f'/chat/groups/{gid}';sp=f'/client/chat/groups/{gid}'
 call('student cannot create group',f'/client/chat/courses/{cid}/groups','POST',{'title':'Not allowed'},st,403)
 call('admin cannot read groups',f'/chat/courses/{cid}/groups',token=admin,code=403)
 call('other mentor cannot manage group',gp,token=ot,code=403)
 call('nonmember cannot read history',sp+'/messages',token=st,code=403)
 for name in ['alice','bob']:call('add member '+name,gp+'/members','POST',{'studentId':actors[name]['id']},mt)
 call('cannot add student outside course',gp+'/members','POST',{'studentId':actors['outsider']['id']},mt,400)
 call('student cannot add members',sp+'/members','POST',{'studentId':actors['nonmember']['id']},st,403)
 call('student sees own group',f'/client/chat/courses/{cid}/groups',token=st)
 assert call('group pagination total',f'/chat/courses/{cid}/groups?current=1&pageSize=1',token=mt)['total']==1
 assert call('course peer sees no groups',f'/client/chat/courses/{cid}/groups',token=actors['nonmember']['token'])['total']==0
 call('course peer cannot guess group ID',sp,token=actors['nonmember']['token'],code=403)
 call('anonymous denied',sp+'/messages',code=401)
 key=str(uuid.uuid4());payload={'body':'Hello, let us work together 👋','fileIds':[],'clientKey':key}
 message=call('student sends Unicode text',sp+'/messages','POST',payload,st)['data']
 assert call('mentor receives text',gp+'/messages?afterId=0',token=mt)['data']['rows'][0]['body']==payload['body']
 assert call('send retry is idempotent',sp+'/messages','POST',payload,st)['data']['id']==message['id']
 def retry(_):return a.request(sp+'/messages','POST',payload,st)[1]
 with ThreadPoolExecutor(max_workers=4) as pool:
  results=list(pool.map(retry,range(4)))
 assert all(r.get('code')==200 and r['data']['id']==message['id'] for r in results);checks.append('concurrent send retry deduplicated')
 call('reused key cannot change text',sp+'/messages','POST',{**payload,'body':'Changed'},st,409)
 call('empty message rejected',sp+'/messages','POST',{'body':'  ','clientKey':str(uuid.uuid4())},st,400)
 call('long message rejected',sp+'/messages','POST',{'body':'x'*2001,'clientKey':str(uuid.uuid4())},st,400)
 raw=audio();png=base64.b64decode('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jRZkAAAAASUVORK5CYII=')
 (work/'voice.wav').write_bytes(raw);(work/'diagram.png').write_bytes(png);(work/'study-notes.txt').write_text('One step at a time.',encoding='utf-8')
 upload('image description required',gid,st,'diagram.png',png,code=400)
 upload('HTML executable rejected',gid,st,'unsafe.html',b'<script>alert(1)</script>',code=400)
 upload('disguised audio rejected',gid,st,'fake.webm',b'not sound',code=400)
 upload('oversize audio rejected',gid,st,'large.wav',raw+b'0'*(5*1024*1024),code=400)
 image=upload('upload private image',gid,st,'diagram.png',png,'A learning step diagram')
 file=upload('upload private document',gid,st,'notes.txt',b'Ask one question.')
 voice=upload('upload private audio',gid,st,'voice.wav',raw)
 assert 'storageKey' not in json.dumps([image,file,voice])
 v.content('own pending image accessible',f'/client/chat/files/{image["id"]}/content',st,png)
 v.content('peer cannot read unsent image',f'/client/chat/files/{image["id"]}/content',bt,code=403)
 v.content('mentor cannot read unsent recording',f'/chat/files/{voice["id"]}/content',mt,code=403)
 call('peer cannot attach another draft',f'/client/chat/groups/{gid}/messages','POST',{'clientKey':str(uuid.uuid4()),'fileIds':[image['id']]},bt,403)
 call('remove own pending document',f'/client/chat/files/{file["id"]}','DELETE',token=st)
 v.content('deleted pending document unavailable',f'/client/chat/files/{file["id"]}/content',st,code=404)
 file=upload('replace private document',gid,st,'notes.txt',b'Ask one question.')
 call('send files and voice',sp+'/messages','POST',{'body':'Here is my idea.','fileIds':[image['id'],file['id'],voice['id']],'clientKey':str(uuid.uuid4())},st)
 v.content('member receives image',f'/client/chat/files/{image["id"]}/content',bt,png)
 v.content('mentor hears sent audio',f'/chat/files/{voice["id"]}/content',mt,raw)
 v.content('admin cannot download chat file',f'/chat/files/{file["id"]}/content',admin,code=403)
 call('sent file cannot be deleted as draft',f'/client/chat/files/{file["id"]}','DELETE',token=st,code=403)
 mentor_files=[upload('mentor uploads image',gid,mt,'mentor-diagram.png',png,'A mentor learning diagram',management=True),upload('mentor uploads document',gid,mt,'mentor-notes.txt',b'Work together.',management=True),upload('mentor uploads audio',gid,mt,'mentor-voice.wav',raw,management=True)]
 call('mentor sends text and attachments',gp+'/messages','POST',{'body':'Let us help each other.','fileIds':[f['id'] for f in mentor_files],'clientKey':str(uuid.uuid4())},mt)
 v.content('student hears mentor recording',f'/client/chat/files/{mentor_files[2]["id"]}/content',st,raw)
 for i in range(34):
  r=a.request(sp+'/messages','POST',{'body':f'History {i}','clientKey':str(uuid.uuid4())},bt)[1];assert r.get('code')==200
 batch=call('load bounded recent history',sp+'/messages',token=st)['data'];assert len(batch['rows'])==30 and batch['hasMore']
 earlier=call('load earlier history',sp+'/messages?beforeId='+str(batch['rows'][0]['id']),token=st)['data'];assert len(earlier['rows'])==7
 assert [m['id'] for m in batch['rows']]==sorted(m['id'] for m in batch['rows'])
 assert call('incremental read excludes previous messages',sp+'/messages?afterId='+str(batch['rows'][-1]['id']),token=st)['data']['rows']==[]
 call('invalid mixed cursors rejected',sp+'/messages?afterId=0&beforeId=3',token=st,code=400)
 call('close discussion',gp,'PUT',{'title':'Assignment study group','status':'CLOSED','version':0},mt)
 call('closed group history retained',sp+'/messages',token=st)
 call('closed group rejects sends',sp+'/messages','POST',{'body':'No','clientKey':str(uuid.uuid4())},st,409)
 upload('closed group rejects uploads',gid,st,'notes.txt',b'No',code=409)
 call('stale group edit rejected',gp,'PUT',{'title':'New','status':'ACTIVE','version':0},mt,409)
 call('reopen discussion',gp,'PUT',{'title':'Assignment study group','status':'ACTIVE','version':1},mt)
 call('remove bob',gp+'/members/'+str(actors['bob']['id']),'DELETE',token=mt)
 call('removed member loses history',f'/client/chat/groups/{gid}/messages',token=bt,code=403)
 v.content('removed member loses file',f'/client/chat/files/{voice["id"]}/content',bt,code=403)
 call('restore bob',gp+'/members','POST',{'studentId':actors['bob']['id']},mt)
 call('remove course enrollment',cp+'/students/'+str(actors['bob']['id']),'DELETE',token=admin)
 call('former course member loses group',f'/client/chat/groups/{gid}',token=bt,code=403)
 call('restore course enrollment',cp+'/students','POST',{'studentId':actors['bob']['id']},admin)
 call('reassign course mentor',cp+'/mentor','PUT',{'mentorId':actors['othermentor']['id']},admin)
 call('old mentor loses history',gp+'/messages',token=mt,code=403)
 call('new mentor can read retained history',gp+'/messages',token=ot)
 call('restore mentor',cp+'/mentor','PUT',{'mentorId':actors['mentor']['id']},admin)
 assert a.sql("SELECT COUNT(*) FROM flyway_schema_history WHERE version='12' AND success=1")=='1'
 print('Chat API checks:',len(set(checks)|set(v.checks)),flush=True)
 if '--browser' in sys.argv:
  fixture=work/'fixture.json';fixture.write_text(json.dumps({'actors':actors,'courseId':cid,'adminPassword':password}),encoding='utf-8')
  env=os.environ.copy();env['UPSELF_CHAT_FIXTURE']=str(fixture);env['UPSELF_SCREENSHOTS']=str(work)
  node=Path(json.loads((R/'.local/development-tools.json').read_text(encoding='utf-8-sig'))['nodeHome'])
  env['PATH']=str(node)+os.pathsep+env.get('PATH','')
  try:
   result=subprocess.run([str(node/'node.exe'),str(R/'frontend/node_modules/@playwright/test/cli.js'),'test','tests/e2e/chat.spec.ts'],cwd=R/'frontend',env=env)
   if result.returncode:raise RuntimeError('Chat Chrome verification failed')
  finally:fixture.unlink(missing_ok=True)
def cleanup():
 for token,student in tokens:
  try:a.request('/client/logout' if student else '/logout','POST',token=token)
  except Exception:pass
 for cid in courses:
  if a.sql(f"SELECT COUNT(*) FROM learning_course WHERE id={cid} AND title='{prefix}'")!='1':continue
  keys.update(a.sql(f'SELECT storage_key FROM chat_file WHERE group_id IN(SELECT id FROM chat_group WHERE course_id={cid})').splitlines())
  a.sql(f'DELETE FROM chat_file WHERE group_id IN(SELECT id FROM chat_group WHERE course_id={cid}); DELETE FROM chat_message WHERE group_id IN(SELECT id FROM chat_group WHERE course_id={cid}); DELETE FROM chat_member WHERE group_id IN(SELECT id FROM chat_group WHERE course_id={cid}); DELETE FROM chat_group WHERE course_id={cid}; DELETE FROM learning_enrollment WHERE course_id={cid}; DELETE FROM learning_course WHERE id={cid};')
 for uid in users:
  if a.sql(f"SELECT COUNT(*) FROM sys_user WHERE id={uid} AND user_name LIKE '{prefix}%'")=='1':a.sql(f'DELETE FROM sys_user WHERE id={uid}')
 for key in keys:
  if re.fullmatch(r'[a-f0-9]{32}\.(pdf|docx|png|jpg|txt|webm|m4a|ogg|wav)',key):
   folder='recordings' if key.rsplit('.',1)[1] in ('webm','m4a','ogg','wav') else 'chat'
   base=(R/'.local/uploads'/folder).resolve();target=(base/key).resolve()
   if target.parent==base:target.unlink(missing_ok=True)
if __name__=='__main__':
 try:main()
 finally:cleanup()
