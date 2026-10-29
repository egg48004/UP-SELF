"""Local FR-60/44/46 API and Chrome checks. Removes only this run's fixtures."""
from pathlib import Path
import concurrent.futures
import importlib.util
import json
import os
import re
import secrets
import subprocess
import sys

ROOT=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('local',ROOT/'scripts/verify-local.py')
a=importlib.util.module_from_spec(spec);spec.loader.exec_module(a)
a.BASE=os.environ.get('UPSELF_TEST_API_URL','http://127.0.0.1:8080')
assert a.BASE in ['http://127.0.0.1:8080','http://127.0.0.1:8081']
W=ROOT/'.local/mvp-verification';W.mkdir(parents=True,exist_ok=True)
prefix='fr3_'+secrets.token_hex(4)+'_'
users=[];courses=[];tokens=[];checks=[]
def call(label,path,method='GET',body=None,token=None,code=200):
    r=a.request(path,method,body,token)[1]
    assert r.get('code')==code,(label,r.get('code'),r.get('msg'))
    assert not re.search(r'[\u4e00-\u9fff]',r.get('msg','')), 'Expected English feedback'
    checks.append(label);print('PASS',label,flush=True);return r
def data(label,path,method='GET',body=None,token=None):
    return call(label,path,method,body,token)['data']
def main():
    a.clear_our_rate_limits()
    # The current demo password is an owner-maintained local inventory. Bootstrap is initial-only.
    demo=(ROOT/'config/demo-accounts.yml').read_text(encoding='utf-8')
    pwd=re.search(r'username: admin01\s+password: "([^"]+)"',demo).group(1)
    admin=call('administrator login','/empLogin','POST',{'username':'admin01','password':pwd})['token'];tokens.append((admin,False))
    config=(ROOT/'config/application-local.yml').read_text(encoding='utf-8-sig')
    maintenance_pwd=re.search(r'^\s{4}maintenance:\s*(.*?)\s*$',config,re.M).group(1).strip('"\'')
    internal=call('internal login','/empLogin','POST',{'username':'maintenance','password':maintenance_pwd})['token'];tokens.append((internal,False))
    actors={}
    for name,role in [('student',10),('otherstudent',10),('mentor',20),('othermentor',20)]:
        password=secrets.token_urlsafe(18);username=prefix+name
        uid=data('create '+name,'/system/user','POST',{'userName':username,'nickName':'DEMO '+name,'password':password,'roleId':role},admin);users.append(uid)
        token=call('login '+name,'/client/login' if role==10 else '/empLogin','POST',{'username':username,'password':password})['token'];tokens.append((token,role==10))
        actors[name]={'id':uid,'username':username,'password':password,'token':token}
    sid=actors['student']['id'];st=actors['student']['token'];other=actors['otherstudent']['token'];mt=actors['mentor']['token'];ot=actors['othermentor']['token']
    cid=data('create test course','/learning/courses','POST',{'title':prefix+'course','description':'Disposable FR verification.'},admin);courses.append(cid)
    cp='/learning/courses/'+str(cid)
    call('assign mentor',cp+'/mentor','PUT',{'mentorId':actors['mentor']['id']},admin)
    for k in ['student','otherstudent']:call('enroll '+k,cp+'/students','POST',{'studentId':actors[k]['id']},admin)
    call('publish course',cp+'/status','PUT',{'status':'PUBLISHED'},admin)
    nb='/mentorship/students/'+str(sid)+'/notes';bb='/client/business'
    for path in [nb,bb+'/ideas',bb+'/cost-plan']:call('anonymous denied '+path,path,code=401)
    for label,token in [('administrator',admin),('internal wildcard',internal)]:
        call(label+' cannot read notes',nb,token=token,code=403)
        call(label+' cannot write notes',nb,'POST',{'content':'Forbidden'},token,403)
        call(label+' cannot read business','/business/ideas',token=token,code=403)
        call(label+' cannot calculate','/business/cost-plan/calculate','POST',{'materialsCost':'1','otherCosts':'1','quantity':'1','pricePerItem':'1'},token,403)
    call('student cannot read mentor notes','/client'+nb,token=st,code=403)
    call('mentor cannot read student ideas','/business/ideas',token=mt,code=403)
    call('unrelated mentor denied',nb,token=ot,code=403)
    data('student metadata scoped',nb+'/student',token=mt)
    nid=data('create private note',nb,'POST',{'content':'Private API follow-up','mentorId':actors['othermentor']['id']},mt)
    rows=call('note persisted',nb,token=mt)['rows'];assert rows[0]['id']==nid and 'mentorId' not in rows[0] and 'studentId' not in rows[0]
    call('blank note rejected',nb,'POST',{'content':'  '},mt,400)
    call('oversized note rejected',nb,'POST',{'content':'x'*2001},mt,400)
    with concurrent.futures.ThreadPoolExecutor(2) as pool:
        results=list(pool.map(lambda text:a.request(nb+'/'+str(nid),'PUT',{'content':text,'version':0},mt)[1]['code'],['API follow-up A','API follow-up B']))
    assert sorted(results)==[200,409];checks.append('concurrent note edit preserves one winner')
    # Both mentors can be responsible for a student while notes remain author-private.
    second=data('create second course','/learning/courses','POST',{'title':prefix+'second course','description':'Disposable relationship.'},admin);courses.append(second)
    sp='/learning/courses/'+str(second)
    call('assign other mentor',sp+'/mentor','PUT',{'mentorId':actors['othermentor']['id']},admin)
    call('enroll shared student',sp+'/students','POST',{'studentId':sid},admin);call('publish second course',sp+'/status','PUT',{'status':'PUBLISHED'},admin)
    assert call('other responsible mentor sees no private notes',nb,token=ot)['total']==0
    call('other mentor cannot edit private note',nb+'/'+str(nid),'PUT',{'content':'No','version':1},ot,404)
    data('other mentor writes own note',nb,'POST',{'content':'Other mentor private note'},ot)
    assert call('notes remain author-scoped',nb,token=mt)['total']==1
    call('reassign first course',cp+'/mentor','PUT',{'mentorId':actors['othermentor']['id']},admin)
    call('former mentor immediately loses notes',nb,token=mt,code=403)
    assert call('new mentor cannot inherit predecessor notes',nb,token=ot)['total']==1
    assert a.sql('SELECT COUNT(*) FROM mentor_note WHERE id='+str(nid))=='1'
    call('restore first mentor',cp+'/mentor','PUT',{'mentorId':actors['mentor']['id']},admin)
    call('unenroll student revokes notes',cp+'/students/'+str(sid),'DELETE',token=admin)
    call('unenrolled access denied',nb,token=mt,code=403)
    call('restore enrollment',cp+'/students','POST',{'studentId':sid},admin)
    iid=data('create incomplete idea',bb+'/ideas','POST',{'title':'API first idea','studentId':actors['otherstudent']['id']},st)
    idea=data('read own idea',bb+'/ideas/'+str(iid),token=st);assert idea['offer']=='' and 'studentId' not in idea
    assert call('forged list owner ignored',bb+'/ideas?studentId='+str(sid),token=other)['total']==0
    call('foreign idea read denied',bb+'/ideas/'+str(iid),token=other,code=404)
    call('foreign idea edit denied',bb+'/ideas/'+str(iid),'PUT',{'title':'No','version':0},other,404)
    call('blank idea rejected',bb+'/ideas','POST',{'title':' '},st,400)
    call('long idea answer rejected',bb+'/ideas','POST',{'title':'Title','offer':'x'*501},st,400)
    call('save guided answers',bb+'/ideas/'+str(iid),'PUT',{'title':'API first idea','offer':'Handmade bookmarks','audience':'Classmates','resources':'Paper','nextStep':'Make one sample','version':0},st)
    with concurrent.futures.ThreadPoolExecutor(2) as pool:
        results=list(pool.map(lambda x:a.request(bb+'/ideas/'+str(iid),'PUT',{'title':x,'version':1},st)[1]['code'],['API idea A','API idea B']))
    assert sorted(results)==[200,409];checks.append('concurrent idea edit preserves one winner')
    for i in range(10):data('create paginated idea '+str(i),bb+'/ideas','POST',{'title':'API practice '+str(i)},st)
    page=call('ideas use standard pagination',bb+'/ideas?current=2&pageSize=10',token=st);assert page['total']==11 and len(page['rows'])==1
    p=data('new cost plan is unsaved',bb+'/cost-plan',token=st);assert p['saved'] is False and p['version']==0
    amounts={'materialsCost':'0.10','otherCosts':'0.20','quantity':'3','pricePerItem':'0.10'}
    p=data('exact decimal calculation',bb+'/cost-plan/calculate','POST',{**amounts,'totalCost':'9999'},st);assert p['totalCost']=='0.30' and p['expectedRevenue']=='0.30' and p['estimatedProfit']=='0.00'
    assert not data('calculation does not save',bb+'/cost-plan',token=st)['saved']
    saved=data('save cost plan',bb+'/cost-plan','PUT',{**amounts,'version':0},st);assert saved['saved'] and saved['version']==1
    assert not data('other student has independent plan',bb+'/cost-plan',token=other)['saved']
    call('stale first save rejected',bb+'/cost-plan','PUT',{**amounts,'version':0},st,409)
    with concurrent.futures.ThreadPoolExecutor(2) as pool:
        results=list(pool.map(lambda x:a.request(bb+'/cost-plan','PUT',{**amounts,'otherCosts':x,'version':1},st)[1]['code'],['1.00','2.00']))
    assert sorted(results)==[200,409];checks.append('concurrent budget edit preserves one winner')
    for label,patch in [('negative cost',{'materialsCost':'-1'}),('excess precision',{'materialsCost':'0.001'}),('zero quantity',{'quantity':0}),('fractional quantity',{'quantity':1.5}),('large quantity',{'quantity':1000001}),('missing amount',{'pricePerItem':None})]:
        call(label+' rejected',bb+'/cost-plan/calculate','POST',{**amounts,**patch},st,400)
    p=data('loss is calculated',bb+'/cost-plan/calculate','POST',{'materialsCost':'10','otherCosts':'5','quantity':'2','pricePerItem':'5'},st);assert p['estimatedProfit']=='-5.00'
    p=data('large result keeps decimal cents',bb+'/cost-plan/calculate','POST',{'materialsCost':'0','otherCosts':'0','quantity':'1000000','pricePerItem':'9999999999.99'},st);assert p['expectedRevenue']=='9999999999990000.00'
    for i in range(10):data('create paginated note '+str(i),nb,'POST',{'content':'API private observation '+str(i)},mt)
    page=call('notes use standard pagination',nb+'?current=2&pageSize=10',token=mt);assert page['total']==11 and len(page['rows'])==1
    f={'actors':{k:{key:value for key,value in actor.items() if key!='token'} for k,actor in actors.items()},'courseId':cid,'studentId':sid,'ideaId':iid,'admin':{'username':'admin01','password':pwd}}
    fixture=W/'browser-fixtures.json';fixture.write_text(json.dumps(f,indent=2),encoding='utf-8')
    if '--browser' in sys.argv:
        node=Path(json.loads((ROOT/'.local/development-tools.json').read_text(encoding='utf-8-sig'))['nodeHome'])/'node.exe'
        env=os.environ.copy();env['UPSELF_BUSINESS_FIXTURE']=str(fixture);env['UPSELF_SCREENSHOTS']=str(W)
        with (W/'browser.log').open('w',encoding='utf-8') as log:
            p=subprocess.run([str(node),str(ROOT/'frontend/node_modules/@playwright/test/cli.js'),'test','business-notes.spec.ts'],cwd=ROOT/'frontend',env=env,stdout=log,stderr=subprocess.STDOUT)
        assert p.returncode==0,'Chrome verification failed; see .local/mvp-verification/browser.log'
        print('PASS Chrome role workflows and all new controls',flush=True)
    (W/'result.json').write_text(json.dumps({'apiChecks':len(checks),'browser':'passed' if '--browser' in sys.argv else 'not run','fixturePrefix':prefix},indent=2),encoding='utf-8')
    print('API checks:',len(checks),flush=True)
try:
    main()
finally:
    if users:
        ids=','.join(str(int(x)) for x in users)
        a.sql(f'DELETE FROM mentor_note WHERE mentor_id IN ({ids}) OR student_id IN ({ids});DELETE FROM business_idea WHERE student_id IN ({ids});DELETE FROM student_cost_plan WHERE student_id IN ({ids});')
        if courses:
            cs=','.join(str(int(x)) for x in courses)
            a.sql(f'DELETE FROM learning_enrollment WHERE course_id IN ({cs});DELETE FROM learning_course WHERE id IN ({cs});')
        a.sql(f'DELETE FROM sys_audit_log WHERE actor_id IN ({ids});DELETE FROM sys_user WHERE id IN ({ids});')
    for token,student in tokens:a.request('/client/logout' if student else '/logout','POST',token=token)
    fixture=W/'browser-fixtures.json'
    if fixture.exists():fixture.unlink()
