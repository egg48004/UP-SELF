"""Optional local assignment API regression; run with backend running. Uses disposable accounts/courses only, and removes its own fixtures. Never changes existing credentials. Same local Windows database tools as verify-local.py."""
from pathlib import Path
import importlib.util,json,secrets,re,urllib.request,urllib.error,uuid,concurrent.futures
ROOT=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('auth',ROOT/'scripts/verify-local.py');auth=importlib.util.module_from_spec(spec);spec.loader.exec_module(auth)
PREFIX='assignment_check_'+secrets.token_hex(4);USERS=[];COURSES=[];TOKENS=[];CHECKS=[]
def call(label,path,method='GET',body=None,token=None,code=200):
 r=auth.request(path,method,body,token)[1];assert r['code']==code,(label,r.get('code'),r.get('msg'));CHECKS.append(label);print('PASS',label,flush=True);return r
def upload(a,v,token,name='answer.txt',content=b'My answer'):
 boundary='----'+uuid.uuid4().hex
 b=('--'+boundary+'\r\nContent-Disposition: form-data; name="version"\r\n\r\n'+str(v)+'\r\n--'+boundary+'\r\nContent-Disposition: form-data; name="file"; filename="'+name+'"\r\nContent-Type: application/octet-stream\r\n\r\n').encode()+content+('\r\n--'+boundary+'--\r\n').encode()
 return raw('/client/assignments/%d/files'%a,token,b,'multipart/form-data; boundary='+boundary)
def raw(path,token=None,body=None,mime=None):
 h={'Authorization':'Bearer '+token} if token else {}
 if mime:h['Content-Type']=mime
 req=urllib.request.Request(auth.BASE+path,data=body,headers=h)
 try:r=urllib.request.urlopen(req,timeout=30)
 except urllib.error.HTTPError as e:r=e
 with r:return r.status,r.headers,r.read()
def uploaded(label,r,code=200):
 d=json.loads(r[2]);assert d['code']==code,(label,d);CHECKS.append(label);print('PASS',label,flush=True);return d.get('data')
def main():
 auth.clear_our_rate_limits()
 c=ROOT/'config/application-local.yml';s=c.read_text(encoding='utf-8')
 pwd=lambda k: re.search(r'^\s{4}'+k+r':\s*(.*?)\s*$',s,re.M).group(1).strip('"\'')
 admin=call('admin-login','/empLogin','POST',{'username':'admin01','password':pwd('admin01')})['token'];TOKENS.append((admin,False))
 maintenance=call('internal-login','/empLogin','POST',{'username':'maintenance','password':pwd('maintenance')})['token'];TOKENS.append((maintenance,False))
 actors={}
 for name,role in [('mentor',20),('othermentor',20),('student',10),('otherstudent',10)]:
  user=PREFIX+name;password=secrets.token_urlsafe(20)
  uid=call('create-'+name,'/system/user','POST',{'userName':user,'nickName':name,'roleId':role,'password':password},admin)['data'];USERS.append(uid)
  token=call('login-'+name,'/client/login' if role==10 else '/empLogin','POST',{'username':user,'password':password})['token'];actors[name]=(uid,token);TOKENS.append((token,role==10))
 mid,m=actors['mentor'];mid2,m2=actors['othermentor'];sid,st=actors['student'];sid2,st2=actors['otherstudent']
 c=call('create-test-course','/learning/courses','POST',{'title':PREFIX,'description':'Disposable assignment checks'},admin)['data'];COURSES.append(c)
 cp='/learning/courses/'+str(c)
 call('assign-mentor',cp+'/mentor','PUT',{'mentorId':mid},admin)
 for u in [sid,sid2]:call('enroll-student',cp+'/students','POST',{'studentId':u},admin)
 call('publish-course',cp+'/status','PUT',{'status':'PUBLISHED'},admin)
 body={'courseId':c,'title':'Reflection','instructions':'Share one idea. 🌱','status':'DRAFT','maxScore':10,'version':0,'languageCode':'en','dueAt':'2020-01-01T12:00:00+08:00'}
 a=call('mentor-create','/assignments','POST',body,m)['data'];ap='/assignments/'+str(a);sp='/client'+ap
 call('anonymous-denied','/assignments',code=401)
 for token in [admin,maintenance]:call('platform-no-private-assignment-access',ap,token=token,code=403)
 call('other-mentor-denied',ap,token=m2,code=403);call('draft-hidden',sp,token=st,code=403)
 body['status']='PUBLISHED';call('publish-assignment',ap,'PUT',body,m)
 detail=call('student-read',sp,token=st)['data'];av=detail['version'];assert detail['instructions'].endswith('🌱')
 call('student-cannot-create','/client/assignments','POST',body,st,403)
 v=call('empty-own-work',sp+'/submission',token=st)['data'];assert v['id'] is None
 uploaded('reject-fake-pdf',upload(a,0,st,'fake.pdf',b'<script/>'),400)
 draft=uploaded('upload-private-text',upload(a,0,st));fid=draft['files'][0]['id'];v=draft['version']
 assert 'storageKey' not in draft['files'][0]
 for token in [m,st2,admin,maintenance]:uploaded('private-draft-file-denied',raw(('/client' if token==st2 else '')+'/assignments/files/%d/content'%fid,token),403)
 downloaded=raw('/client/assignments/files/%d/content'%fid,st);assert downloaded[2]==b'My answer' and 'attachment' in downloaded[1]['Content-Disposition'];CHECKS.append('download-exact-authenticated-bytes')
 call('stale-draft-denied',sp+'/submission','PUT',{'version':0,'answerText':'stale'},st,409)
 d=call('save-unicode-answer',sp+'/submission','PUT',{'version':v,'answerText':'My idea 🌱'},st)['data'];v=d['version']
 request={'version':v,'assignmentVersion':av,'answerText':'My idea 🌱','requestKey':str(uuid.uuid4())}
 with concurrent.futures.ThreadPoolExecutor(2) as pool:
  responses=list(pool.map(lambda _:auth.request(sp+'/submit','POST',request,st)[1],range(2)))
 assert all(r['code']==200 for r in responses) and all(r['data']['attemptNo']==1 for r in responses),[(r.get('code'),r.get('msg')) for r in responses];CHECKS.append('concurrent-duplicate-submit-idempotent')
 w=responses[0]['data'];subid=w['id'];assert w['attempts'][0]['late'] is True and len(w['attempts'][0]['files'])==1
 call('submitted-answer-locked',sp+'/submission','PUT',{'version':w['version'],'answerText':'overwrite'},st,409)
 uploaded('submitted-file-cannot-remove',raw('/client/assignments/files/%d/content'%fid,None),401)
 r=call('mentor-real-submission-page',ap+'/submissions?current=1&pageSize=1',token=m);assert r['total']==1 and len(r['rows'])==1 and r['rows'][0].get('answerText') is None
 rp=ap+'/submissions/'+str(subid)
 for token in [m2,st2,admin,maintenance]:call('private-review-denied',('/client' if token==st2 else '')+rp,token=token,code=403)
 assert raw('/assignments/files/%d/content'%fid,m)[2]==b'My answer';CHECKS.append('mentor-downloads-submitted-file')
 rv={'version':w['version'],'decision':'GRADED','score':11,'feedback':'Good effort'}
 call('score-out-of-range',rp+'/review','POST',rv,m,400)
 rv.update(decision='RETURNED',score=None,feedback='Please add a concrete example.');w=call('return-with-feedback',rp+'/review','POST',rv,m)['data']
 d=call('student-reads-return',sp+'/submission',token=st)['data'];assert d['attempts'][0]['feedback']==rv['feedback']
 d=call('private-revision',sp+'/submission','PUT',{'version':d['version'],'answerText':'My private new draft'},st)['data']
 r=call('mentor-sees-prior-snapshot-only',rp,token=m)['data'];assert r['answerText']=='My idea 🌱' and not r['files']
 call('stale-assignment-edit',ap,'PUT',body,m,409)
 body.update(version=av,maxScore=20);call('edit-next-attempt-max',ap,'PUT',body,m);av+=1
 request.update(version=d['version'],assignmentVersion=av,answerText='A revised example',requestKey=str(uuid.uuid4()));w=call('resubmit-new-attempt',sp+'/submit','POST',request,st)['data']
 assert len(w['attempts'])==2 and w['attempts'][1]['answerText']=='My idea 🌱' and w['attempts'][1]['maxScore']==10 and w['attempts'][0]['maxScore']==20
 rv.update(version=w['version'],decision='GRADED',score=18,feedback='Clear example. Well done.');w=call('mentor-grades',rp+'/review','POST',rv,m)['data']
 call('stale-review-denied',rp+'/review','POST',rv,m,409)
 final=call('student-feedback-and-history',sp+'/submission',token=st)['data'];assert final['status']=='GRADED' and final['attempts'][0]['score']==18 and final['attempts'][1]['feedback']=='Please add a concrete example.'
 assert call('student-list-does-not-expose-pending-count',sp.rsplit('/',1)[0],token=st)['rows'][0].get('pendingCount') is None
 call('unenroll',cp+'/students/'+str(sid),'DELETE',token=admin)
 call('unenrolled-feedback-denied',sp+'/submission',token=st,code=403);uploaded('unenrolled-download-denied',raw('/client/assignments/files/%d/content'%fid,st),403)
 call('reassign',cp+'/mentor','PUT',{'mentorId':mid2},admin);call('previous-mentor-denied',rp,token=m,code=403);call('new-mentor-can-review-history',rp,token=m2)
 call('archive-course',cp+'/status','PUT',{'status':'ARCHIVED'},admin)
 body['version']=av;call('archived-course-readonly',ap,'PUT',body,m2,400)
 print('Assignment API checks:',len(CHECKS))
def cleanup():
 for token,student in TOKENS:
  try:auth.request('/client/logout' if student else '/logout','POST',token=token)
  except Exception:pass
 for cid in COURSES:
  if auth.sql("SELECT COUNT(*) FROM learning_course WHERE id=%d AND title='%s';"%(cid,PREFIX))!='1':continue
  ids='SELECT id FROM assignment_submission WHERE assignment_id IN(SELECT id FROM assignment WHERE course_id=%d)'%cid
  keys=auth.sql('SELECT storage_key FROM assignment_file WHERE submission_id IN('+ids+')')
  auth.sql('DELETE FROM assignment_file WHERE submission_id IN('+ids+');DELETE FROM assignment_attempt WHERE submission_id IN('+ids+');DELETE FROM assignment_submission WHERE assignment_id IN(SELECT id FROM assignment WHERE course_id=%d);DELETE FROM assignment WHERE course_id=%d;DELETE FROM learning_enrollment WHERE course_id=%d;DELETE FROM learning_course WHERE id=%d;'%(cid,cid,cid,cid))
  base=(ROOT/'.local/uploads/assignments').resolve()
  for k in keys.splitlines():
   f=(base/k).resolve()
   if base in f.parents and f.is_file():f.unlink()
 for uid in USERS:auth.sql("DELETE FROM sys_user WHERE id=%d AND user_name LIKE '%s%%';"%(uid,PREFIX))
if __name__=='__main__':
 try:main()
 finally:cleanup()
