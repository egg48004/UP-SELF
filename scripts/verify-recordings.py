"""Local recording integration checks. No transcription or external audio service.
Creates disposable accounts/courses and removes only their rows and private audio files.
"""
from pathlib import Path
import io, wave, math, struct, json, re, os, sys, secrets, uuid
import importlib.util, urllib.request, urllib.error, subprocess
ROOT=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('auth',ROOT/'scripts/verify-local.py')
a=importlib.util.module_from_spec(spec);spec.loader.exec_module(a)
a.BASE=os.environ.get('UPSELF_TEST_API_URL','http://127.0.0.1:8080')
if a.BASE not in ('http://127.0.0.1:8080','http://127.0.0.1:8081'): raise RuntimeError('Local tests only')
prefix='recording_check_'+secrets.token_hex(4)
users=[];courses=[];tokens=[];checks=[]
work=ROOT/'.local/recordings-verification';work.mkdir(parents=True,exist_ok=True)
def call(label,path,method='GET',body=None,token=None,code=200):
    result=a.request(path,method,body,token)[1]
    assert result.get('code')==code,(label,result.get('code'),result.get('msg'))
    checks.append(label);print('PASS',label,flush=True);return result
def multipart(label,path,token,fields,content,name='voice.wav',code=200):
    boundary='UPSELF'+secrets.token_hex(12);parts=[]
    for key,value in fields.items(): parts.append((f'--{boundary}\r\nContent-Disposition: form-data; name="{key}"\r\n\r\n{value}\r\n').encode())
    parts.extend([(f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="{name}"\r\nContent-Type: audio/wav\r\n\r\n').encode(),content,f'\r\n--{boundary}--\r\n'.encode()])
    request=urllib.request.Request(a.BASE+path,data=b''.join(parts),headers={'Authorization':'Bearer '+token,'Content-Type':'multipart/form-data; boundary='+boundary})
    try: response=urllib.request.urlopen(request,timeout=30)
    except urllib.error.HTTPError as e: response=e
    with response: result=json.load(response)
    assert result.get('code')==code,(label,result.get('code'),result.get('msg'))
    checks.append(label);print('PASS',label,flush=True);return result
def content(label,path,token=None,expected=None,code=200):
    request=urllib.request.Request(a.BASE+path,headers={'Authorization':'Bearer '+token} if token else {})
    try: response=urllib.request.urlopen(request,timeout=20)
    except urllib.error.HTTPError as e: response=e
    with response:
        result=response.read()
        if response.headers.get('Content-Type','').startswith('application/json'):
            assert json.loads(result).get('code')==code,(label,json.loads(result).get('code'))
        else:
            assert code==200 and result==expected,label
            assert response.headers.get('Cache-Control')=='private, no-store'
            assert response.headers.get('X-Content-Type-Options')=='nosniff'
    checks.append(label);print('PASS',label,flush=True)
def main():
    config=(ROOT/'config/application-local.yml').read_text(encoding='utf-8-sig')
    password=lambda name: re.search(r'^\s{4}'+name+r':\s*(.*?)\s*$',config,re.M).group(1).strip('"\'')
    a.clear_our_rate_limits()
    admin=call('admin login','/empLogin','POST',{'username':'admin01','password':password('admin01')})['token'];tokens.append((admin,False))
    actors={}
    for name,role in [('mentor',20),('othermentor',20),('student',10),('peer',10),('fresh',10)]:
        username=prefix+name;pwd=secrets.token_urlsafe(18)
        uid=call('create '+name,'/system/user','POST',{'userName':username,'nickName':'DEMO audio '+name,'roleId':role,'password':pwd},admin)['data'];users.append(uid)
        token=call('login '+name,'/client/login' if role==10 else '/empLogin','POST',{'username':username,'password':pwd})['token'];tokens.append((token,role==10));actors[name]={'id':uid,'token':token,'username':username,'password':pwd}
    mentor=actors['mentor'];student=actors['student'];peer=actors['peer'];other=actors['othermentor']
    mt,st=mentor['token'],student['token']
    cid=call('create course','/learning/courses','POST',{'title':prefix,'description':'Disposable recording checks'},admin)['data'];courses.append(cid)
    cp='/learning/courses/'+str(cid)
    call('assign mentor',cp+'/mentor','PUT',{'mentorId':mentor['id']},admin)
    for name in ['student','peer','fresh']: call('enroll student',cp+'/students','POST',{'studentId':actors[name]['id']},admin)
    call('publish course',cp+'/status','PUT',{'status':'PUBLISHED'},admin)
    aid=call('mentor creates assignment','/assignments','POST',{'courseId':cid,'title':'A recorded learning reflection','instructions':'Share one small learning step. You may type or record.','status':'PUBLISHED','maxScore':10,'languageCode':'en'},mt)['data']
    audio=io.BytesIO()
    with wave.open(audio,'wb') as wav:
        wav.setnchannels(1);wav.setsampwidth(2);wav.setframerate(8000)
        wav.writeframes(b''.join(struct.pack('<h',int(3000*math.sin(2*math.pi*440*i/8000))) for i in range(8000)))
    raw=audio.getvalue();(work/'sample.wav').write_bytes(raw)
    ap='/client/assignments/'+str(aid)
    multipart('reject disguised audio',ap+'/files',st,{'version':0},b'<html>not sound</html>','fake.webm',400)
    result=multipart('upload draft recording',ap+'/files',st,{'version':0},raw)['data'];fid=result['files'][0]['id']
    assert result['files'][0]['mimeType']=='audio/wav' and 'storageKey' not in json.dumps(result)
    file='/client/assignments/files/'+str(fid)+'/content'
    content('student plays own draft',file,st,raw)
    content('anonymous cannot play',file,code=401)
    content('peer cannot play',file,peer['token'],code=403)
    content('mentor cannot hear unsubmitted draft','/assignments/files/'+str(fid)+'/content',mt,code=403)
    content('admin cannot hear student work','/assignments/files/'+str(fid)+'/content',admin,code=403)
    removed=call('student deletes uploaded draft recording',ap+'/files/'+str(fid)+'?version='+str(result['version']),'DELETE',token=st)['data']
    content('removed recording unavailable',file,st,code=404)
    result=multipart('upload replacement',ap+'/files',st,{'version':removed['version']},raw)['data'];fid=result['files'][0]['id']
    multipart('stale upload rejected',ap+'/files',st,{'version':0},raw,code=409)
    workdata=call('submit audio-only assignment',ap+'/submit','POST',{'version':result['version'],'assignmentVersion':0,'requestKey':str(uuid.uuid4()),'answerText':''},st)['data']
    content('assigned mentor plays submitted audio','/assignments/files/'+str(fid)+'/content',mt,raw)
    content('unrelated mentor denied','/assignments/files/'+str(fid)+'/content',other['token'],code=403)
    call('mentor grades audio assignment',f'/assignments/{aid}/submissions/{workdata["id"]}/review','POST',{'version':workdata['version'],'decision':'GRADED','score':8,'feedback':'One clear step. Thank you.'},mt)

    current=call('updated diary sharing notice','/client/moods/current',token=st)['data']
    old='Earlier test diary '+prefix
    # Deliberately simulate an older notice and legacy flag: neither can hide saved diaries from an eligible mentor now.
    import datetime
    week=(datetime.date.fromisoformat(current['weekStart'])-datetime.timedelta(days=7)).isoformat()
    a.sql(f"INSERT INTO mood_notice_ack(student_id,notice_version) VALUES({student['id']},'old'); INSERT INTO mood_entry(student_id,week_start,mood_level,private_note,note_shared) VALUES({student['id']},'{week}',3,'{old}',0);")
    history=f'/moods/students/{student["id"]}/history'
    assert call('legacy diary visible before notice reconfirmation',history,token=mt)['rows'][0]['diaryNote']==old
    fields={'weekStart':current['weekStart'],'moodLevel':4,'privateNote':'Audio diary'}
    multipart('cannot save before notice','/client/moods/entries/recording',st,fields,raw,code=403)
    call('student confirms audio disclosure','/client/moods/acknowledgment','POST',{'noticeVersion':current['noticeVersion'],'accepted':True},st)
    multipart('invalid diary recording rejected','/client/moods/entries/recording',st,fields,b'not audio','bad.wav',400)
    diary=multipart('save diary with recording','/client/moods/entries/recording',st,fields,raw)['data']['entry'];rid=diary['recordingId']
    assert diary['recordingMime']=='audio/wav' and 'recordingKey' not in diary
    content('student replays own diary',f'/client/moods/recordings/{rid}/content',st,raw)
    content('assigned mentor plays diary',f'/moods/recordings/{rid}/content',mt,raw)
    content('peer cannot hear diary',f'/client/moods/recordings/{rid}/content',peer['token'],code=403)
    content('unrelated mentor cannot hear diary',f'/moods/recordings/{rid}/content',other['token'],code=403)
    content('admin cannot hear diary',f'/moods/recordings/{rid}/content',admin,code=403)
    multipart('saved diary cannot be overwritten','/client/moods/entries/recording',st,fields,raw,code=409)
    h=call('history includes recording metadata',history,token=mt);assert h['total']==2 and h['rows'][0]['recordingId']==rid and 'recordingKey' not in json.dumps(h)
    call('remove diary relationship',cp+'/students/'+str(student['id']),'DELETE',token=admin)
    content('former relationship loses diary audio',f'/moods/recordings/{rid}/content',mt,code=403)
    call('restore enrollment',cp+'/students','POST',{'studentId':student['id']},admin)
    call('reassign mentor',cp+'/mentor','PUT',{'mentorId':other['id']},admin)
    content('former mentor loses submitted audio','/assignments/files/'+str(fid)+'/content',mt,code=403)
    content('new assigned mentor hears submitted work','/assignments/files/'+str(fid)+'/content',other['token'],raw)
    content('new assigned mentor hears diary',f'/moods/recordings/{rid}/content',other['token'],raw)
    assert old in json.dumps(call('new mentor reads all earlier diaries',history,token=other['token']))
    call('restore original mentor',cp+'/mentor','PUT',{'mentorId':mentor['id']},admin)
    assert a.sql("SELECT COUNT(*) FROM flyway_schema_history WHERE version='11' AND success=1")=='1'
    print('Recording API checks:',len(checks),flush=True)
    if '--browser' in sys.argv:
        fixture=work/'fixture.json';fixture.write_text(json.dumps({'actors':actors,'assignmentId':aid,'courseId':cid,'adminPassword':password('admin01'),'legacyDiary':old}),encoding='utf-8')
        env=os.environ.copy();env['UPSELF_RECORDING_FIXTURE']=str(fixture);env['UPSELF_SCREENSHOTS']=str(work)
        runtime=Path(json.loads((ROOT/'.local/development-tools.json').read_text(encoding='utf-8-sig'))['nodeHome']);env['PATH']=str(runtime)+os.pathsep+env.get('PATH','')
        try:
            result=subprocess.run([str(runtime/'node.exe'),str(ROOT/'frontend/node_modules/@playwright/test/cli.js'),'test','tests/e2e/recordings.spec.ts'],cwd=ROOT/'frontend',env=env)
            if result.returncode: raise RuntimeError('Recording Chrome checks failed')
        finally: fixture.unlink(missing_ok=True)
def cleanup():
    for token,student in tokens:
        try:a.request('/client/logout' if student else '/logout','POST',token=token)
        except Exception:pass
    keys=[]
    for cid in courses:
        if a.sql(f"SELECT COUNT(*) FROM learning_course WHERE id={cid} AND title='{prefix}'")!='1':continue
        keys.extend(a.sql(f'SELECT f.storage_key FROM assignment_file f JOIN assignment_submission s ON s.id=f.submission_id JOIN assignment a ON a.id=s.assignment_id WHERE a.course_id={cid}').splitlines())
        subs=f'SELECT id FROM assignment_submission WHERE assignment_id IN(SELECT id FROM assignment WHERE course_id={cid})'
        a.sql(f'DELETE FROM assignment_file WHERE submission_id IN({subs}); DELETE FROM assignment_attempt WHERE submission_id IN({subs}); DELETE FROM assignment_submission WHERE assignment_id IN(SELECT id FROM assignment WHERE course_id={cid}); DELETE FROM assignment WHERE course_id={cid}; DELETE FROM learning_enrollment WHERE course_id={cid}; DELETE FROM learning_course WHERE id={cid};')
    for uid in users:
        if a.sql(f"SELECT COUNT(*) FROM sys_user WHERE id={uid} AND user_name LIKE '{prefix}%'")!='1':continue
        keys.extend(a.sql(f'SELECT recording_key FROM mood_entry WHERE student_id={uid} AND recording_key IS NOT NULL').splitlines())
        a.sql(f'DELETE FROM mood_entry WHERE student_id={uid}; DELETE FROM mood_notice_ack WHERE student_id={uid}; DELETE FROM sys_user WHERE id={uid};')
    base=(ROOT/'.local/uploads/recordings').resolve()
    for key in keys:
        if re.fullmatch(r'[a-f0-9]{32}\.(webm|m4a|wav|ogg)',key):
            target=(base/key).resolve()
            if target.parent==base:target.unlink(missing_ok=True)
if __name__=='__main__':
    try:main()
    finally:cleanup()
