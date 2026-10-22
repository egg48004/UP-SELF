"""Real local mood API regression. Backend must be running. Only disposable fixtures
created by this invocation are removed; existing records and passwords are never changed.
Uses the same local Windows DB helper as verify-local.py. Logs no credentials or notes.
"""
from pathlib import Path
import importlib.util,json,secrets,re,concurrent.futures
from datetime import datetime,timedelta,timezone
ROOT=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('auth',ROOT/'scripts/verify-local.py')
auth=importlib.util.module_from_spec(spec);spec.loader.exec_module(auth)
PREFIX='mood_check_'+secrets.token_hex(4)
USERS=[];COURSES=[];TOKENS=[];CHECKS=[]
def call(label,path,method='GET',body=None,token=None,code=200):
    result=auth.request(path,method,body,token)[1]
    assert result.get('code')==code,(label,result.get('code'),result.get('msg'))
    assert not any('\u4e00'<=c<='\u9fff' for c in result.get('msg',''))
    CHECKS.append(label);print('PASS',label,flush=True)
    return result
def public(result):
    text=json.dumps(result)
    assert 'privateNote' not in text and 'MOOD_PRIVATE_SENTINEL' not in text
    assert 'password' not in text and 'noticeVersion' not in text
def main():
    auth.clear_our_rate_limits()
    config=(ROOT/'config/application-local.yml').read_text(encoding='utf-8')
    password=lambda key: re.search(r'^\s{4}'+key+r':\s*(.*?)\s*$',config,re.M).group(1).strip('"\'')
    admin=call('admin-login','/empLogin','POST',{'username':'admin01','password':password('admin01')})['token'];TOKENS.append((admin,False))
    internal=call('internal-login','/empLogin','POST',{'username':'maintenance','password':password('maintenance')})['token'];TOKENS.append((internal,False))
    actors={}
    for name,role in [('mentor',20),('othermentor',20),('student',10),('otherstudent',10)]:
        username=PREFIX+name;pwd=secrets.token_urlsafe(20)
        uid=call('create-'+name,'/system/user','POST',{'userName':username,'nickName':name,'roleId':role,'password':pwd},admin)['data'];USERS.append(uid)
        token=call('login-'+name,'/client/login' if role==10 else '/empLogin','POST',{'username':username,'password':pwd})['token'];actors[name]=(uid,token);TOKENS.append((token,role==10))
    mid,m=actors['mentor'];mid2,m2=actors['othermentor'];sid,s=actors['student'];sid2,s2=actors['otherstudent']
    own='/client/moods';rp='/moods/students/'+str(sid)
    call('anonymous-owner-denied',own+'/current',code=401)
    for token in [admin,internal]:
        for endpoint in ['/moods/current','/moods/students',rp, rp+'/history',rp+'/trend']:
            call('admin-and-maintenance-cannot-read-mood',endpoint,token=token,code=403)
    current=call('first-use-metadata',own+'/current',token=s)['data']
    assert current['acknowledged'] is False and current.get('entry') is None and current['viewers']==[]
    now=datetime.now(timezone(timedelta(hours=8))).date();week=now-timedelta(days=now.weekday())
    assert current['weekStart']==week.isoformat() and current['weekEnd']==(week+timedelta(days=6)).isoformat() and current['timeZone']=='Asia/Kuala_Lumpur'
    body={'weekStart':week.isoformat(),'moodLevel':3,'privateNote':'MOOD_PRIVATE_SENTINEL \U0001f331'}
    call('cannot-record-before-notice',own+'/entries','POST',body,s,403)
    for endpoint in ['/history','/trend']:call('cannot-bypass-first-use',own+endpoint,token=s,code=403)
    call('false-acknowledgment-rejected',own+'/acknowledgment','POST',{'accepted':False,'noticeVersion':current['noticeVersion']},s,400)
    call('stale-notice-rejected',own+'/acknowledgment','POST',{'accepted':True,'noticeVersion':'old'},s,400)
    ack={'accepted':True,'noticeVersion':current['noticeVersion']}
    call('acknowledge-current-notice',own+'/acknowledgment','POST',ack,s)
    accepted=auth.sql('SELECT accepted_at FROM mood_notice_ack WHERE student_id=%d;'%sid)
    call('acknowledgment-idempotent',own+'/acknowledgment','POST',ack,s)
    assert accepted==auth.sql('SELECT accepted_at FROM mood_notice_ack WHERE student_id=%d;'%sid)
    for value in [0,6,None]:call('invalid-level-rejected',own+'/entries','POST',dict(body,moodLevel=value),s,400)
    call('oversize-private-note-rejected',own+'/entries','POST',dict(body,privateNote='x'*2001),s,400)
    for delta in [-7,7]:call('cannot-backfill-or-postdate',own+'/entries','POST',dict(body,weekStart=(week+timedelta(days=delta)).isoformat()),s,409)
    with concurrent.futures.ThreadPoolExecutor(2) as pool:
        results=list(pool.map(lambda _:auth.request(own+'/entries','POST',body,s)[1],range(2)))
    assert sorted(r['code'] for r in results)==[200,409]
    assert auth.sql('SELECT COUNT(*) FROM mood_entry WHERE student_id=%d;'%sid)=='1'
    CHECKS.append('concurrent-once-per-week');print('PASS concurrent-once-per-week')
    mine=call('own-record-survives-refresh',own+'/current',token=s)['data'];assert mine['entry']['privateNote']==body['privateNote']
    call('saved-week-cannot-be-overwritten',own+'/entries','POST',dict(body,moodLevel=5),s,409)
    for endpoint in [rp, rp+'/history',rp+'/trend']:call('unrelated-mentor-denied',endpoint,token=m,code=403)
    # A mood record is global to the student; two current courses must not duplicate them.
    for suffix in ['one','two']:
        cid=call('create-current-course','/learning/courses','POST',{'title':PREFIX+suffix,'description':'Disposable mood permission fixture'},admin)['data'];COURSES.append(cid)
        course='/learning/courses/'+str(cid)
        call('assign-current-mentor',course+'/mentor','PUT',{'mentorId':mid},admin)
        for student in [sid,sid2]:call('enroll-student',course+'/students','POST',{'studentId':student},admin)
        call('publish-current-course',course+'/status','PUT',{'status':'PUBLISHED'},admin)
    viewers=call('current-viewers-listed',own+'/current',token=s)['data']['viewers'];assert len(viewers)==1 and viewers[0]['id']==mid
    students=call('mentor-students-deduplicated','/moods/students?current=1&pageSize=1',token=m);public(students);assert students['total']==2 and len(students['rows'])==1
    call('scoped-student-identity',rp,token=m)
    call('wrong-mentor-cannot-read',rp+'/history',token=m2,code=403)
    for endpoint in ['/current','/history','/trend']:call('mentor-cannot-read-private-owner-api','/moods'+endpoint,token=m,code=403)
    call('student-cannot-list-peers',own+'/students',token=s,code=403)
    call('mentor-cannot-write-mood','/moods/entries','POST',body,m,403)
    others=call('query-param-cannot-switch-owner',own+'/history?studentId='+str(sid),token=s2,code=403)
    call('other-student-acknowledges',own+'/acknowledgment','POST',ack,s2)
    assert call('other-student-still-has-no-record',own+'/history?studentId='+str(sid),token=s2)['total']==0
    for n in range(1,15):
        if n in [2,5]:continue
        day=(week-timedelta(weeks=n)).isoformat()
        auth.sql("INSERT INTO mood_entry(student_id,week_start,mood_level,private_note) VALUES(%d,'%s',%d,'MOOD_PRIVATE_SENTINEL historical');"%(sid,day,(n%5)+1))
    owner=call('owner-history-paged',own+'/history?current=2&pageSize=10',token=s);assert owner['total']==13 and len(owner['rows'])==3 and all('privateNote' in r for r in owner['rows'])
    ment=call('mentor-history-paged-without-notes',rp+'/history?current=2&pageSize=10&includePrivateNotes=true',token=m);public(ment);assert ment['total']==13 and len(ment['rows'])==3
    trend=call('truthful-weekly-trend',own+'/trend?weeks=12',token=s)['data'];assert len(trend['points'])==12
    missing={(week-timedelta(weeks=n)).isoformat() for n in [2,5]}
    assert all(point['moodLevel'] is None for point in trend['points'] if point['weekStart'] in missing)
    assert sum(point['moodLevel'] is None for point in trend['points'])==2
    public(call('mentor-trend-without-notes',rp+'/trend?weeks=12',token=m))
    for count in [0,3,53]:call('bounded-trend-range',own+'/trend?weeks='+str(count),token=s,code=400)
    assert len(call('short-trend-range',own+'/trend?weeks=4',token=s)['data']['points'])==4
    # Reconfirmation is enforced for a future notice change, including mentor projections.
    auth.sql("UPDATE mood_notice_ack SET notice_version='test-old' WHERE student_id=%d;"%sid)
    assert call('changed-notice-requires-confirmation',own+'/current',token=s)['data']['acknowledged'] is False
    call('changed-notice-blocks-owner-history',own+'/history',token=s,code=403)
    assert call('unconfirmed-history-not-shared',rp+'/history',token=m)['total']==0
    call('reconfirm-preserves-history',own+'/acknowledgment','POST',ack,s)
    assert call('history-preserved',own+'/history',token=s)['total']==13
    c1='/learning/courses/'+str(COURSES[0]);c2='/learning/courses/'+str(COURSES[1])
    call('remove-first-membership',c1+'/students/'+str(sid),'DELETE',token=admin)
    call('second-current-course-retains-access',rp+'/history',token=m)
    call('remove-last-membership',c2+'/students/'+str(sid),'DELETE',token=admin)
    for endpoint in [rp,rp+'/history',rp+'/trend']:call('former-mentor-loses-access',endpoint,token=m,code=403)
    call('reenroll-student',c1+'/students','POST',{'studentId':sid},admin)
    call('reassign-mentor',c1+'/mentor','PUT',{'mentorId':mid2},admin)
    call('previous-mentor-after-reassignment-denied',rp+'/history',token=m,code=403)
    public(call('new-current-mentor-sees-public-history',rp+'/history',token=m2))
    newview=call('viewer-list-updates-after-reassignment',own+'/current',token=s)['data']['viewers'];assert len(newview)==1 and newview[0]['id']==mid2
    call('archive-course',c1+'/status','PUT',{'status':'ARCHIVED'},admin)
    call('archived-course-does-not-grant-mood-access',rp+'/trend',token=m2,code=403)
    assert call('student-retains-own-history',own+'/history',token=s)['total']==13
    call('restore-course',c1+'/status','PUT',{'status':'PUBLISHED'},admin)
    public(call('restored-current-mentor-public-only',rp+'/trend',token=m2))
    print('Mood API checks:',len(CHECKS))
def cleanup():
    for token,student in TOKENS:
        try:auth.request('/client/logout' if student else '/logout','POST',token=token)
        except Exception:pass
    for cid in COURSES:
        if auth.sql("SELECT COUNT(*) FROM learning_course WHERE id=%d AND title LIKE '%s%%';"%(cid,PREFIX))=='1':
            auth.sql('DELETE FROM learning_enrollment WHERE course_id=%d;DELETE FROM learning_course WHERE id=%d;'%(cid,cid))
    for uid in USERS:
        if auth.sql("SELECT COUNT(*) FROM sys_user WHERE id=%d AND user_name LIKE '%s%%';"%(uid,PREFIX))=='1':
            auth.sql('DELETE FROM mood_entry WHERE student_id=%d;DELETE FROM mood_notice_ack WHERE student_id=%d;DELETE FROM sys_user WHERE id=%d;'%(uid,uid,uid))
if __name__=='__main__':
    try:main()
    finally:cleanup()
