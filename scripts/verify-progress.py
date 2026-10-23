"""Real API checks for V6. Creates and removes only its own disposable fixtures."""
from pathlib import Path
import importlib.util,re,secrets,concurrent.futures,json
ROOT=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('a',ROOT/'scripts/verify-local.py');a=importlib.util.module_from_spec(spec);spec.loader.exec_module(a)
checks=[];users=[];courses=[];tokens=[];prefix='p6_check_'+secrets.token_hex(4)
def call(label,path,method='GET',body=None,token=None,code=200):
 r=a.request(path,method,body,token)[1];assert r.get('code')==code,(label,r)
 checks.append(label);print('PASS',label,flush=True);return r
try:
 a.clear_our_rate_limits();config=(ROOT/'config/application-local.yml').read_text(encoding='utf-8-sig')
 pwd=lambda key:re.search(r'^\s{4}'+key+r':\s*(.*?)\s*$',config,re.M).group(1).strip('"\'')
 admin=call('admin-login','/empLogin','POST',{'username':'admin01','password':pwd('admin01')})['token']
 internal=call('maintenance-login','/empLogin','POST',{'username':'maintenance','password':pwd('maintenance')})['token']
 actors={}
 for name,role in [('mentor',20),('othermentor',20),('student',10),('otherstudent',10)]:
  password=secrets.token_urlsafe(18)
  uid=call('create-'+name,'/system/user','POST',{'userName':prefix+name,'nickName':name,'password':password,'roleId':role},admin)['data'];users.append(uid)
  token=call('login-'+name,'/client/login' if role==10 else '/empLogin','POST',{'username':prefix+name,'password':password})['token'];actors[name]=(uid,token);tokens.append(token)
 mid,m=actors['mentor'];mid2,m2=actors['othermentor'];sid,s=actors['student'];sid2,s2=actors['otherstudent']
 body={'title':prefix,'description':'Disposable progress fixture','difficulty':'INTERMEDIATE','estimatedMinutes':45}
 for patch in [{'difficulty':'INVALID'},{'estimatedMinutes':0},{'estimatedMinutes':10081},{'difficulty':None}]:call('invalid-course-metadata','/learning/courses','POST',dict(body,**patch),admin,400)
 cid=call('create-course-metadata','/learning/courses','POST',body,admin)['data'];courses.append(cid);cp='/learning/courses/'+str(cid)
 c=call('metadata-roundtrip',cp,token=admin)['data'];assert c['difficulty']=='INTERMEDIATE' and c['estimatedMinutes']==45
 call('assign-course',cp+'/mentor','PUT',{'mentorId':mid},admin);call('enroll',cp+'/students','POST',{'studentId':sid},admin)
 call('publish',cp+'/status','PUT',{'status':'PUBLISHED'},admin)
 chapters=[]
 for i in range(4):chapters.append(call('create-chapter',cp+'/chapters','POST',{'title':'Chapter '+str(i),'body':'Test','sortOrder':i,'status':'DRAFT' if i==3 else 'PUBLISHED'},m)['data'])
 own='/client/learning';summary=own+f'/courses/{cid}/progress';ch=own+f'/chapters/{chapters[0]}'
 call('anonymous-denied',summary,code=401)
 for t in [admin,internal,m,m2]:
  call('nonstudent-cannot-read-owner',f'/learning/courses/{cid}/progress',token=t,code=403)
  call('nonstudent-cannot-write',f'/learning/chapters/{chapters[0]}/completion','PUT',{'completed':True},t,403)
 for t in [admin,internal,s,s2]:call('nonmentor-overview-denied','/client/learning/students/overview' if t in [s,s2] else '/learning/students/overview',token=t,code=403)
 call('unenrolled-student-denied',summary,token=s2,code=403)
 r=call('new-progress-no-invented-history',summary,token=s)['data'];assert (r['total'],r['completed'],r['percent'],r['lastChapterId'])==(3,0,0,None)
 call('draft-visit-denied',own+f'/chapters/{chapters[3]}/visit','POST',token=s,code=403)
 call('missing-boolean-rejected',ch+'/completion','PUT',{},s,400)
 call('visit-recorded',ch+'/visit','POST',token=s)
 r=call('visit-does-not-complete',summary,token=s)['data'];assert r['completed']==0 and r['lastChapterId']==chapters[0]
 call('explicit-complete',ch+'/completion','PUT',{'completed':True},s)
 first=call('completion-date-recorded',summary,token=s)['data'];assert first['completed']==1 and first['percent']==33
 original=first['chapters'][0]['completedAt']
 with concurrent.futures.ThreadPoolExecutor(2) as pool:
  rr=list(pool.map(lambda _:a.request(ch+'/completion','PUT',{'completed':True},s)[1],range(2)))
 assert all(x['code']==200 for x in rr);checks.append('concurrent-idempotent-completion')
 assert call('completion-idempotent-time',summary,token=s)['data']['chapters'][0]['completedAt']==original
 h=call('history-owner-only',own+'/history?studentId='+str(sid2)+'&current=1&pageSize=1',token=s);assert h['total']==1 and h['rows'][0]['chapterId']==chapters[0]
 call('visit-second-chapter',own+f'/chapters/{chapters[1]}/visit','POST',token=s)
 resume=call('resume-across-courses',own+'/resume',token=s)['data'];assert resume['chapterId']==chapters[1] and resume['courseId']==cid
 call('other-student-history-empty',own+'/history',token=s2);assert a.request(own+'/history',token=s2)[1]['total']==0
 overview=call('mentor-current-course-counts',f'/learning/students/overview?courseId={cid}&current=1&pageSize=1',token=m)
 assert overview['total']==1 and overview['rows'][0]['completedChapters']==1 and overview['rows'][0]['totalChapters']==3
 assert 'privateNote' not in json.dumps(overview) and 'answerText' not in json.dumps(overview)
 assert call('wrong-mentor-filter-cannot-bypass',f'/learning/students/overview?courseId={cid}',token=m2)['total']==0
 assert call('search-with-pagination','/learning/students/overview?keyword=not-a-match',token=m)['total']==0
 call('oversize-search-rejected','/learning/students/overview?keyword='+'a'*101,token=m,code=400)
 call('undo-completion',ch+'/completion','PUT',{'completed':False},s)
 assert call('undo-removes-history',own+'/history',token=s)['total']==0
 call('complete-again',ch+'/completion','PUT',{'completed':True},s)
 chapterBody={'title':'Chapter 0','body':'Test','status':'DRAFT','sortOrder':0}
 call('unpublish-chapter',f'/learning/chapters/{chapters[0]}','PUT',chapterBody,m)
 r=call('published-denominator-and-count',summary,token=s)['data'];assert (r['total'],r['completed'])==(2,0)
 assert call('unpublished-history-hidden',own+'/history',token=s)['total']==0
 call('republish-chapter',f'/learning/chapters/{chapters[0]}','PUT',dict(chapterBody,status='PUBLISHED'),m)
 assert call('republish-preserves-real-completion',summary,token=s)['data']['completed']==1
 call('transfer-course-mentor',cp+'/mentor','PUT',{'mentorId':mid2},admin)
 assert call('old-mentor-access-revoked',f'/learning/students/overview?courseId={cid}',token=m)['total']==0
 assert call('new-mentor-sees-existing-progress',f'/learning/students/overview?courseId={cid}',token=m2)['total']==1
 call('remove-enrollment',cp+'/students/'+str(sid),'DELETE',token=admin)
 call('removed-student-write-denied',ch+'/visit','POST',token=s,code=403)
 assert call('removed-history-hidden',own+'/history',token=s)['total']==0
 assert call('removed-resume-hidden',own+'/resume',token=s).get('data') == {}
 call('reenroll-preserves-record',cp+'/students','POST',{'studentId':sid},admin)
 assert call('reenrolled-progress-restored',summary,token=s)['data']['completed']==1
 call('archive-course',cp+'/status','PUT',{'status':'ARCHIVED'},admin)
 call('archived-progress-denied',summary,token=s,code=403)
 assert call('archived-overview-hidden',f'/learning/students/overview?courseId={cid}',token=m2)['total']==0
 print('TOTAL',len(checks),'checks passed')
finally:
 for cid in courses:
  a.sql(f'DELETE FROM learning_chapter_progress WHERE chapter_id IN(SELECT id FROM learning_chapter WHERE course_id={cid});DELETE FROM learning_chapter WHERE course_id={cid};DELETE FROM learning_enrollment WHERE course_id={cid};DELETE FROM learning_course WHERE id={cid};')
 for uid in users:a.sql(f'DELETE FROM sys_user WHERE id={uid};')
 for token in tokens:
  try:a.redis('DEL',a.key_for(token))
  except Exception:pass
