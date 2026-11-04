"""Verify global questionnaire links on local development services.
Creates only qtest_ accounts/links and removes only those fixtures. Audit records
are retained. Use --browser for the prepared Chrome acceptance workflow.
"""
from pathlib import Path
import argparse, concurrent.futures, json, os, re, secrets, subprocess, sys, urllib.error, urllib.request

R = Path(__file__).resolve().parents[1]
W = R / '.local/questionnaire-verification'
W.mkdir(parents=True, exist_ok=True)
p = argparse.ArgumentParser()
p.add_argument('--pymysql-path', help='Optional existing test-only client directory')
p.add_argument('--browser', action='store_true')
args = p.parse_args()
C = json.loads((R / 'migration/local-snapshot/local-credentials.json').read_text(encoding='utf-8-sig'))
if args.pymysql_path:
    sys.path.insert(0, args.pymysql_path)
    import pymysql
BASE = 'http://127.0.0.1:8080'
prefix = 'qtest_' + secrets.token_hex(4) + '_'
checks, users, actors = [], [], {}
last_headers = {}

def check(label, condition):
    assert condition, label
    checks.append(label)
    print('PASS', label, flush=True)

def request(path, method='GET', body=None, token=None):
    global last_headers
    headers = {'Content-Type': 'application/json'}
    if token:
        headers['Authorization'] = 'Bearer ' + token
    req = urllib.request.Request(BASE + path, data=json.dumps(body).encode() if body is not None else None, method=method, headers=headers)
    try:
        response = urllib.request.urlopen(req, timeout=25)
    except urllib.error.HTTPError as error:
        response = error
    with response:
        last_headers = dict(response.headers)
        return json.load(response)

def call(label, path, method='GET', body=None, token=None, code=200):
    result = request(path, method, body, token)
    assert result.get('code') == code, (label, result.get('code'), result.get('msg'))
    check(label, True)
    return result

def sql(statement):
    if args.pymysql_path:
        conn = pymysql.connect(host='127.0.0.1', port=3306, user='root', password=C['mysql_root_password'], database='up_self_dev', charset='utf8mb4', autocommit=True)
        try:
            with conn.cursor() as cursor:
                cursor.execute(statement)
                return list(cursor.fetchall())
        finally:
            conn.close()
    saved = R / '.local/service-tools.json'
    if saved.exists():
        home = Path(json.loads(saved.read_text(encoding='utf-8-sig'))['mysqlHome'])
    else:
        home = Path(json.loads((R / '.local/development-tools.json').read_text(encoding='utf-8-sig'))['serviceHome']) / 'mysql'
    cfg = W / 'root-client.cnf'
    cfg.write_text('[client]\nuser=root\npassword="' + C['mysql_root_password'].replace('\\', '\\\\').replace('"', '\\"') + '"\nhost=127.0.0.1\nport=3306\nprotocol=tcp\n', encoding='utf-8')
    result = subprocess.run([str(home / 'bin/mysql.exe'), '--defaults-extra-file=' + str(cfg), '--batch', '--skip-column-names', '--default-character-set=utf8mb4', 'up_self_dev', '-e', statement], capture_output=True, text=True, encoding='utf-8')
    if result.returncode:
        raise RuntimeError('Local SQL check failed; no credentials were printed')
    return [tuple(row.split('\t')) for row in result.stdout.splitlines()]

def scalar(statement):
    return int(sql(statement)[0][0])

def main():
    check('V19 recorded successfully', scalar("SELECT COUNT(*) FROM flyway_schema_history WHERE version='19' AND success=1") == 1)
    password = re.search(r'username: admin01\s+password: "([^"]+)"', (R / 'config/demo-accounts.yml').read_text(encoding='utf-8-sig')).group(1)
    admin = call('administrator login', '/empLogin', 'POST', {'username': 'admin01', 'password': password})['token']
    for name, role in [('mentor', 20), ('othermentor', 20), ('student', 10), ('otherstudent', 10)]:
        pwd = secrets.token_urlsafe(18)
        uid = call('create ' + name, '/system/user', 'POST', {'userName': prefix + name, 'nickName': 'Questionnaire test ' + name, 'roleId': role, 'password': pwd}, admin)['data']
        users.append(uid)
        token = call('login ' + name, '/client/login' if role == 10 else '/empLogin', 'POST', {'username': prefix + name, 'password': pwd})['token']
        actors[name] = {'id': uid, 'username': prefix + name, 'password': pwd, 'token': token}
    mentor, other = actors['mentor']['token'], actors['othermentor']['token']
    student, second_student = actors['student']['token'], actors['otherstudent']['token']
    check('both students have zero enrollments', scalar('SELECT COUNT(*) FROM learning_enrollment WHERE student_id IN (' + str(actors['student']['id']) + ',' + str(actors['otherstudent']['id']) + ')') == 0)
    call('anonymous student list denied', '/client/questionnaires', code=401)
    call('anonymous management denied', '/questionnaires/manage', code=401)
    call('student cannot manage questionnaires', '/client/questionnaires/manage', token=student, code=403)
    call('admin cannot use mentor management', '/questionnaires/manage', token=admin, code=403)
    call('mentor cannot impersonate student reader', '/questionnaires', token=mentor, code=403)
    call('management token cannot enter student channel', '/client/questionnaires', token=mentor, code=401)
    body = {'title': prefix + 'General questionnaire', 'url': 'https://forms.example.org/general?key=test#page', 'createdBy': actors['othermentor']['id']}
    qid = call('mentor adds hidden platform link', '/questionnaires', 'POST', body, mentor)['data']
    base = '/questionnaires/' + str(qid)
    own = call('mentor lists own links', '/questionnaires/manage', token=mentor)
    check('author comes from session and new links default hidden', scalar('SELECT COUNT(*) FROM questionnaire_link WHERE id=' + str(qid) + ' AND created_by=' + str(actors['mentor']['id']) + ' AND visible=0') == 1)
    check('owned list includes the created link and version', any(row['id'] == qid and row['version'] == 0 and row['visible'] is False for row in own['rows']))
    visible = call('hidden link is absent for student', '/client/questionnaires', token=student)
    check('hidden link not leaked in rows', all(row['id'] != qid for row in visible['rows']))
    call('hidden detail is 404', '/client/questionnaires/' + str(qid), token=student, code=404)
    check('student responses disable storage caching', last_headers.get('Cache-Control', '').lower() == 'no-store')
    check('other mentor list excludes this author', call('other mentor list', '/questionnaires/manage', token=other)['total'] == 0)
    call('other mentor cannot edit', base, 'PUT', {**body, 'visible': True, 'version': 0}, other, 404)
    call('other mentor cannot reveal', base + '/visibility', 'PUT', {'visible': True, 'version': 0}, other, 404)
    call('other mentor cannot delete', base + '?version=0', 'DELETE', token=other, code=404)
    call('student cannot add', '/client/questionnaires', 'POST', body, student, 403)
    call('student cannot edit', '/client/questionnaires/' + str(qid), 'PUT', {**body, 'visible': True, 'version': 0}, student, 403)
    call('student cannot reveal', '/client/questionnaires/' + str(qid) + '/visibility', 'PUT', {'visible': True, 'version': 0}, student, 403)
    call('student cannot delete', '/client/questionnaires/' + str(qid) + '?version=0', 'DELETE', token=student, code=403)
    for url in ['http://example.org', 'javascript:alert(1)', 'data:text/html,test', '//example.org/form', 'https:example.org', 'https://user:pass@example.org', 'https://@example.org', 'https://example.org:70000', 'https://example.org/has space', 'https://']:
        call('reject unsafe URL ' + url.split(':')[0], '/questionnaires', 'POST', {**body, 'url': url}, mentor, 400)
    call('reject blank title', '/questionnaires', 'POST', {**body, 'title': '   '}, mentor, 400)
    bad = call('reject oversized title', '/questionnaires', 'POST', {**body, 'title': 'a' * 151}, mentor, 400)
    check('validation message is resolved English', not re.search(r'\{field\.|[\u4e00-\u9fff]', bad['msg']))
    call('reject missing visibility flag', base + '/visibility', 'PUT', {'version': 0}, mentor, 400)
    call('reject missing version', base + '/visibility', 'PUT', {'visible': True}, mentor, 400)
    call('reject negative delete version', base + '?version=-1', 'DELETE', token=mentor, code=400)
    call('mentor reveals questionnaire', base + '/visibility', 'PUT', {'visible': True, 'version': 0}, mentor)
    for who, token in [('student', student), ('other student', second_student)]:
        result = call(who + ' sees global link without enrollment', '/client/questionnaires', token=token)
        check(who + ' list contains platform questionnaire', any(row['id'] == qid for row in result['rows']))
    detail = call('student opens visible detail', '/client/questionnaires/' + str(qid), token=student)['data']
    check('student projection excludes editing metadata', set(detail) == {'id', 'title', 'url'})
    call('mentor hides link', base + '/visibility', 'PUT', {'visible': False, 'version': 1}, mentor)
    call('hidden detail unavailable immediately', '/client/questionnaires/' + str(qid), token=student, code=404)
    call('stale edit cannot unhide or overwrite', base, 'PUT', {**body, 'visible': True, 'version': 1}, mentor, 409)
    call('stale delete cannot remove changed record', base + '?version=1', 'DELETE', token=mentor, code=409)
    check('stale writes preserve hidden state', scalar('SELECT COUNT(*) FROM questionnaire_link WHERE id=' + str(qid) + ' AND visible=0 AND version=2') == 1)
    edited = {'title': prefix + 'Updated title', 'url': 'https://example.org/updated?test=1', 'visible': True, 'version': 2}
    call('mentor edits title and URL', base, 'PUT', edited, mentor)
    detail = call('students receive current edited fields', '/client/questionnaires/' + str(qid), token=student)['data']
    check('edited link matches submitted fields', detail['title'] == edited['title'] and detail['url'] == edited['url'])
    with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
        results = list(pool.map(lambda flag: request(base + '/visibility', 'PUT', {'visible': flag, 'version': 3}, mentor), [True, False]))
    check('concurrent updates have one winner and one conflict', sorted(r['code'] for r in results) == [200, 409])
    check('concurrent visibility increments version once', scalar('SELECT version FROM questionnaire_link WHERE id=' + str(qid)) == 4)
    call('delete current questionnaire', base + '?version=4', 'DELETE', token=mentor)
    call('deleted link detail is 404', '/client/questionnaires/' + str(qid), token=student, code=404)
    paging_ids = []
    for i in range(12):
        paging_ids.append(call('create paging link ' + str(i), '/questionnaires', 'POST', {'title': prefix + 'Questionnaire ' + str(i), 'url': 'https://example.org/form/' + str(i), 'visible': True}, mentor)['data'])
    call('other mentor publishes platform questionnaire', '/questionnaires', 'POST', {'title': prefix + 'Other mentor', 'url': 'https://example.org/other', 'visible': True}, other)
    total = scalar('SELECT COUNT(*) FROM questionnaire_link WHERE visible=1')
    first = call('student list page one', '/client/questionnaires?current=1&pageSize=10', token=student)
    second = call('student list page two', '/client/questionnaires?current=2&pageSize=10', token=student)
    check('student paging counts all visible authors', first['total'] == total and len(first['rows']) == 10 and len(second['rows']) > 0 and not set(r['id'] for r in first['rows']) & set(r['id'] for r in second['rows']))
    own = call('mentor own page two', '/questionnaires/manage?current=2&pageSize=10', token=mentor)
    check('management pagination keeps ownership and total', own['total'] == 12 and len(own['rows']) == 2 and all(row['id'] in paging_ids for row in own['rows']))
    check('audit events contain only record IDs', all(str(row[0]).isdigit() for row in sql("SELECT target FROM sys_audit_log WHERE actor_id=" + str(actors['mentor']['id']) + " AND action LIKE 'QUESTIONNAIRE_%'")))
    if args.browser:
        fixture = W / 'fixture.json'
        fixture.write_text(json.dumps({'actors': actors, 'admin': {'username': 'admin01', 'password': password}, 'prefix': prefix}, indent=2), encoding='utf-8')
        tools = json.loads((R / '.local/development-tools.json').read_text(encoding='utf-8-sig'))
        env = os.environ.copy()
        env['UPSELF_QUESTIONNAIRE_FIXTURE'] = str(fixture)
        env['UPSELF_SCREENSHOTS'] = str(W)
        result = subprocess.run([str(Path(tools['nodeHome']) / 'node.exe'), str(R / 'frontend/node_modules/@playwright/test/cli.js'), 'test', 'tests/e2e/questionnaires.spec.ts'], cwd=R / 'frontend', env=env)
        check('Chrome questionnaire workflow', result.returncode == 0)
    (W / 'api-result.json').write_text(json.dumps({'passed': True, 'checks': checks, 'browser': 'passed' if args.browser else 'not run'}, indent=2), encoding='utf-8')
    print('PASS:', len(checks), 'API checks.', flush=True)

def cleanup():
    for name, actor in actors.items():
        try:
            request('/client/logout' if 'student' in name else '/logout', 'POST', {}, actor['token'])
        except Exception:
            pass
    if users:
        ids = ','.join(str(int(uid)) for uid in users)
        owned = sql("SELECT id FROM sys_user WHERE id IN (" + ids + ") AND user_name LIKE '" + prefix + "%'")
        assert len(owned) == len(users), 'Unexpected fixture ownership; no cleanup performed'
        sql('DELETE FROM questionnaire_link WHERE created_by IN (' + ids + ')')
        sql("DELETE FROM sys_user WHERE id IN (" + ids + ") AND user_name LIKE '" + prefix + "%'")
    fixture = W / 'fixture.json'
    if fixture.exists() and json.loads(fixture.read_text(encoding='utf-8')).get('prefix') == prefix:
        fixture.unlink()

if __name__ == '__main__':
    try:
        main()
    finally:
        cleanup()
