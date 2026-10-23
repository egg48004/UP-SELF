"""Local, explicitly labelled synthetic demo fixtures. Never inserted by Flyway.
Run once against the local development server. Saves credentials locally without logging them.
Does not reset existing passwords or update existing courses. Rerun exits if the manifest exists.
"""
from pathlib import Path
import importlib.util,json,re,secrets,urllib.request,uuid
from datetime import date,timedelta
ROOT=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('local',ROOT/'scripts/verify-local.py');a=importlib.util.module_from_spec(spec);spec.loader.exec_module(a)
manifest=ROOT/'.local/learning-demo.json'
if manifest.exists():
 existing=json.loads(manifest.read_text(encoding='utf-8'))
 if not existing.get('completed'):raise RuntimeError('A previous demo seed was interrupted. Inspect its manifest; existing data was not changed.')
 print('Demo manifest already exists; no existing data changed.');raise SystemExit(0)
config=(ROOT/'config/application-local.yml').read_text(encoding='utf-8-sig')
password=re.search(r'^\s{4}admin01:\s*(.*?)\s*$',config,re.M).group(1).strip('"\'')
def api(path,method='GET',body=None,token=None):
 r=a.request(path,method,body,token)[1]
 assert r.get('code')==200,(path,r.get('code'),r.get('msg'))
 return r.get('data')
a.clear_our_rate_limits()
admin=a.request('/empLogin','POST',{'username':'admin01','password':password})[1]['token']
demo={'label':'Synthetic demonstration data - not real students','users':[],'courses':[],'assignments':[]}
pwd='Demo-'+secrets.token_urlsafe(14)+'!'
def save():
 safe=dict(demo,users=[{k:v for k,v in u.items() if k!='token'} for u in demo['users']])
 manifest.write_text(json.dumps(safe,indent=2),encoding='utf-8')
# Refuse name collisions rather than altering previously existing users.
assert a.sql("SELECT COUNT(*) FROM sys_user WHERE user_name LIKE 'demo_p6_%'")=='0','Demo names already exist; inspect before seeding.'
for kind,n,role in [('mentor',2,20),('student',6,10)]:
 for i in range(1,n+1):
  name=f'demo_p6_{kind}{i:02}'
  uid=api('/system/user','POST',{'userName':name,'nickName':f'DEMO {kind.title()} {i:02}','password':pwd,'roleId':role},admin)
  token=a.request('/client/login' if role==10 else '/empLogin','POST',{'username':name,'password':pwd})[1]['token']
  demo['users'].append({'id':uid,'username':name,'role':kind,'password':pwd})
  save()
  # Tokens only in memory.
  demo['users'][-1]['token']=token
mentors=[x for x in demo['users'] if x['role']=='mentor'];students=[x for x in demo['users'] if x['role']=='student']
def upload_demo(chapter_id, token):
 boundary='upself-demo-'+uuid.uuid4().hex
 def upload(filename,kind,title,**extra):
  parts=[]
  fields=dict(kind=kind,title=title,status='PUBLISHED',languageCode='en',**extra)
  for key,value in fields.items():parts.append(('--'+boundary+'\r\nContent-Disposition: form-data; name="'+key+'"\r\n\r\n'+str(value)+'\r\n').encode())
  content=(ROOT/'frontend/tests/fixtures'/filename).read_bytes()
  parts.append(('--'+boundary+'\r\nContent-Disposition: form-data; name="file"; filename="'+filename+'"\r\nContent-Type: application/octet-stream\r\n\r\n').encode()+content+b'\r\n')
  parts.append(('--'+boundary+'--\r\n').encode())
  req=urllib.request.Request(a.BASE+f'/learning/chapters/{chapter_id}/resources',data=b''.join(parts),headers={'Authorization':'Bearer '+token,'Content-Type':'multipart/form-data; boundary='+boundary},method='POST')
  with urllib.request.urlopen(req) as response:r=json.load(response)
  assert r['code']==200,r.get('msg')
  return r['data']
 caption=upload('lesson.vtt','CAPTION','DEMO sample captions')
 upload('lesson.mp4','VIDEO','DEMO short playback sample',captionResourceId=caption,transcript='Demonstration video used to test playback and captions.')
 upload('lesson.png','IMAGE','DEMO sample illustration',altText='Demonstration illustration used to test image viewing.')

for idx,title in enumerate(['DEMO - Small steps in business','DEMO - Planning a simple budget','DEMO - Sharing your idea']):
 m=mentors[0 if idx<2 else 1]
 cid=api('/learning/courses','POST',{'title':title,'description':'Synthetic demo course. Try one short chapter at a time. No real student data.','difficulty':['BEGINNER','INTERMEDIATE','BEGINNER'][idx],'estimatedMinutes':[30,45,20][idx],'languageCode':'en'},admin)
 course={'id':cid,'title':title,'mentorId':m['id'],'chapters':[]};demo['courses'].append(course);save()
 cp=f'/learning/courses/{cid}'
 api(cp+'/mentor','PUT',{'mentorId':m['id']},admin)
 members=students if idx==0 else students[:3] if idx==1 else students[3:]
 for s in members:api(cp+'/students','POST',{'studentId':s['id']},admin)
 for i in range(6):
  chapter=api(cp+'/chapters','POST',{'title':f'Step {i+1}: '+['Notice a need','Choose one idea','List what you need','Plan a small step','Try your plan','Reflect and share'][i],'body':'This is a short demonstration lesson.\nChoose one idea. Take a break whenever you need.\nMark this chapter complete when you feel ready.','sortOrder':i,'status':'PUBLISHED','languageCode':'en'},m['token'])
  course['chapters'].append(chapter);save()
 upload_demo(course['chapters'][0],m['token'])
 api(cp+'/posts','POST',{'title':'DEMO - Welcome to this course','body':'Try the short lessons at your own pace. This is demonstration content.','type':'ANNOUNCEMENT','status':'PUBLISHED'},m['token'])
 api(cp+'/status','PUT',{'status':'PUBLISHED'},admin)
 for j in range(2):
  aid=api('/assignments','POST',{'courseId':cid,'title':f'DEMO task {idx+1}.{j+1}','instructions':'Write one small idea in a sentence. This is a demo task.','maxScore':10,'status':'PUBLISHED','languageCode':'en'},m['token'])
  demo['assignments'].append(aid);save()
  for si,s in enumerate(members[:3]):
   if si==2 and j==1:continue
   sub=api(f'/client/assignments/{aid}/submission',token=s['token'])
   work=api(f'/client/assignments/{aid}/submit','POST',{'version':sub['version'],'assignmentVersion':api(f'/client/assignments/{aid}',token=s['token'])['version'],'answerText':'DEMO answer: I can start with one small step.','requestKey':str(uuid.uuid4())},s['token'])
   if si<2:
    api(f'/assignments/{aid}/submissions/{work["id"]}/review','POST',{'version':work['version'],'decision':'GRADED' if si==0 else 'RETURNED','score':8 if si==0 else None,'feedback':'DEMO feedback: Clear first step.' if si==0 else 'DEMO feedback: Add one example and try again.'},m['token'])
 for si,s in enumerate(members):
  count=6 if si==0 and idx<2 else (si+idx)%5
  for ch in course['chapters'][:count]:
   api(f'/client/learning/chapters/{ch}/visit','POST',token=s['token'])
   api(f'/client/learning/chapters/{ch}/completion','PUT',{'completed':True},s['token'])
for i,s in enumerate(students):
 if i==5:continue # Explicit empty-state student.
 cur=api('/client/moods/current',token=s['token'])
 api('/client/moods/acknowledgment','POST',{'accepted':True,'noticeVersion':cur['noticeVersion']},s['token'])
 api('/client/moods/entries','POST',{'weekStart':cur['weekStart'],'moodLevel':(i%5)+1,'privateNote':'DEMO private note - student only.'},s['token'])
 # Historical synthetic points are only on newly created DEMO accounts, never real users.
 week=date.fromisoformat(cur['weekStart'])
 for w in [1,2,4]:
  a.sql("INSERT INTO mood_entry(student_id,week_start,mood_level,private_note) VALUES(%d,'%s',%d,'DEMO synthetic history - not a real record');"%(s['id'],(week-timedelta(weeks=w)).isoformat(),((i+w)%5)+1))
# Do not persist session tokens.
demo['completed']=True
for u in demo['users']:u.pop('token',None)
save()
print('Demo data created:',len(demo['users']),'accounts;',len(demo['courses']),'courses; 18 chapters;',len(demo['assignments']),'assignments; 20 synthetic mood records.')
print('Accounts and initial password: .local/learning-demo.json (not printed).')
