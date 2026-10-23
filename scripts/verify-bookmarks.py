"""FR-07/08 real local API and optional Chrome checks; removes only its own fixtures.
Use UPSELF_TEST_API_URL/UPSELF_TEST_UI_URL for isolated local test ports.
"""
from pathlib import Path
import concurrent.futures
import importlib.util
import json
import os
import re
import secrets
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location("auth", ROOT / "scripts/verify-local.py")
a = importlib.util.module_from_spec(spec)
spec.loader.exec_module(a)
a.BASE = os.environ.get("UPSELF_TEST_API_URL", "http://127.0.0.1:8080")
if a.BASE not in ("http://127.0.0.1:8080", "http://127.0.0.1:8081"):
    raise RuntimeError("Only reviewed localhost test ports are supported")
prefix = "bookmark_check_" + secrets.token_hex(4)
checks, users, courses, tokens = [], [], [], []
work = ROOT / ".local/bookmark-verification"
work.mkdir(parents=True, exist_ok=True)


def call(label, path, method="GET", body=None, token=None, code=200):
    response = a.request(path, method, body, token)[1]
    assert response.get("code") == code, (label, response)
    checks.append(label)
    print("PASS", label, flush=True)
    return response


def main():
    config = (ROOT / "config/application-local.yml").read_text(encoding="utf-8-sig")
    password = lambda name: re.search(r"^\s{4}" + name + r":\s*(.*?)\s*$", config, re.M).group(1).strip("\"'")
    a.clear_our_rate_limits()
    admin = call("administrator login", "/empLogin", "POST", {"username": "admin01", "password": password("admin01")})["token"]
    tokens.append(admin)
    maintenance = call("internal maintenance login", "/empLogin", "POST", {"username": "maintenance", "password": password("maintenance")})["token"]
    tokens.append(maintenance)
    actors = {}
    for name, role in [("student", 10), ("other", 10), ("mentor", 20)]:
        pwd = secrets.token_urlsafe(18)
        username = prefix + name
        uid = call("create " + name, "/system/user", "POST", {"userName": username, "nickName": "Bookmark test " + name, "password": pwd, "roleId": role}, admin)["data"]
        users.append(uid)
        path = "/client/login" if role == 10 else "/empLogin"
        token = call("login " + name, path, "POST", {"username": username, "password": pwd})["token"]
        tokens.append(token)
        actors[name] = {"id": uid, "username": username, "password": pwd, "token": token}
    sid, s = actors["student"]["id"], actors["student"]["token"]
    other = actors["other"]["token"]
    base = "/client/learning/bookmarks"
    assert a.sql("SELECT GROUP_CONCAT(version ORDER BY installed_rank) FROM flyway_schema_history WHERE success=1 AND version IS NOT NULL;").split(",")[:8] == [str(i) for i in range(1, 9)]
    checks.append("Flyway V1-V8 applied")
    for i in range(12):
        cid = call("create course " + str(i), "/learning/courses", "POST", {"title": prefix + " course " + str(i), "description": "Private fixture description " + str(i)}, admin)["data"]
        courses.append(cid)
        path = "/learning/courses/" + str(cid)
        call("assign mentor " + str(i), path + "/mentor", "PUT", {"mentorId": actors["mentor"]["id"]}, admin)
        call("enroll student " + str(i), path + "/students", "POST", {"studentId": sid}, admin)
        if i == 0:
            call("draft course cannot be saved", base + "/" + str(cid), "PUT", token=s, code=403)
        call("publish course " + str(i), path + "/status", "PUT", {"status": "PUBLISHED"}, admin)
    cid = courses[0]
    cp = "/learning/courses/" + str(cid)
    call("second student joins same course", cp + "/students", "POST", {"studentId": actors["other"]["id"]}, admin)
    call("one published chapter", cp + "/chapters", "POST", {"title": "A small lesson", "body": "Learn at your own pace.", "sortOrder": 0, "status": "PUBLISHED"}, actors["mentor"]["token"])
    call("anonymous cannot list", base, code=401)
    call("anonymous cannot save", base + "/" + str(cid), "PUT", code=401)
    call("anonymous cannot remove", base + "/" + str(cid), "DELETE", code=401)
    for name, token in [("administrator", admin), ("mentor", actors["mentor"]["token"]), ("maintenance wildcard", maintenance)]:
        for method, url in [("GET", "/learning/bookmarks"), ("PUT", "/learning/bookmarks/" + str(cid)), ("DELETE", "/learning/bookmarks/" + str(cid))]:
            call(name + " cannot " + method, url, method, token=token, code=403)
    assert call("fresh bookmarks empty", base, token=s)["total"] == 0
    call("unenrolled course cannot be saved", base + "/" + str(courses[1]), "PUT", token=other, code=403)
    call("missing course cannot be saved", base + "/9223372036854775806", "PUT", token=s, code=404)
    call("save uses authenticated owner", base + "/" + str(cid), "PUT", {"studentId": actors["other"]["id"]}, s)
    saved = call("saved entry roundtrip", base, token=s)["rows"][0]
    assert saved["courseId"] == cid and saved["available"] is True and saved["course"]["bookmarked"] is True
    first_date = saved["savedAt"]
    call("duplicate save succeeds", base + "/" + str(cid), "PUT", token=s)
    with concurrent.futures.ThreadPoolExecutor(4) as pool:
        results = list(pool.map(lambda _: a.request(base + "/" + str(cid), "PUT", token=s)[1]["code"], range(4)))
    assert results == [200] * 4
    rows = call("concurrent saves remain unique", base, token=s)
    assert rows["total"] == 1 and rows["rows"][0]["savedAt"] == first_date
    assert call("course detail reflects saved flag", "/client/learning/courses/" + str(cid), token=s)["data"]["bookmarked"] is True
    assert call("other student detail has own flag", "/client/learning/courses/" + str(cid), token=other)["data"]["bookmarked"] is False
    assert call("query cannot select another owner", base + "?studentId=" + str(sid), token=other)["total"] == 0
    call("another student remove cannot touch owner", base + "/" + str(cid), "DELETE", {"studentId": sid}, other)
    assert call("original bookmark preserved", base, token=s)["total"] == 1
    call("same course independently saved by other student", base + "/" + str(cid), "PUT", token=other)
    call("other student removes only own bookmark", base + "/" + str(cid), "DELETE", token=other)
    assert call("first student still owns bookmark", base, token=s)["total"] == 1
    for item in courses[1:]: call("save pagination fixture", base + "/" + str(item), "PUT", token=s)
    first = call("saved page one", base + "?current=1&pageSize=10", token=s)
    second = call("saved page two", base + "?current=2&pageSize=10", token=s)
    assert first["total"] == second["total"] == 12 and len(first["rows"]) == 10 and len(second["rows"]) == 2
    assert len({g["courseId"] for g in first["rows"] + second["rows"]}) == 12
    listed = call("course list has current student saved flags", "/client/learning/courses?current=1&pageSize=100", token=s)
    assert len(listed["rows"]) == 12 and all(c["bookmarked"] for c in listed["rows"])
    revoked = courses[2]
    call("revoke course access", "/learning/courses/" + str(revoked) + "/students/" + str(sid), "DELETE", token=admin)
    unavailable = next(g for g in call("unavailable bookmark retained", base + "?pageSize=100", token=s)["rows"] if g["courseId"] == revoked)
    assert unavailable["available"] is False and unavailable["course"] is None
    assert "Private fixture description" not in json.dumps(unavailable)
    call("revoked bookmark cannot open course", "/client/learning/courses/" + str(revoked), token=s, code=403)
    call("revoked course cannot be saved again", base + "/" + str(revoked), "PUT", token=s, code=403)
    call("unavailable bookmark can be removed", base + "/" + str(revoked), "DELETE", token=s)
    call("repeated removal succeeds", base + "/" + str(revoked), "DELETE", token=s)
    assert call("remove keeps enrollment revoked", "/client/learning/courses?current=1&pageSize=100", token=s)["total"] == 11
    archived = courses[3]
    call("archive course", "/learning/courses/" + str(archived) + "/status", "PUT", {"status": "ARCHIVED"}, admin)
    row = next(g for g in call("archived course data hidden", base + "?pageSize=100", token=s)["rows"] if g["courseId"] == archived)
    assert row["course"] is None and row["available"] is False
    call("republish saved course", "/learning/courses/" + str(archived) + "/status", "PUT", {"status": "PUBLISHED"}, admin)
    row = next(g for g in call("republish restores access not duplicate", base + "?pageSize=100", token=s)["rows"] if g["courseId"] == archived)
    assert row["course"]["bookmarked"] and row["available"]
    deleted = courses[4]
    a.sql("UPDATE learning_course SET deleted=1 WHERE id=" + str(deleted))
    row = next(g for g in call("soft deleted course remains private", base + "?pageSize=100", token=s)["rows"] if g["courseId"] == deleted)
    assert row["course"] is None
    call("soft deleted bookmark removable", base + "/" + str(deleted), "DELETE", token=s)
    a.sql("UPDATE learning_course SET deleted=0 WHERE id=" + str(deleted))
    call("restore enrollment for browser fixture", "/learning/courses/" + str(revoked) + "/students", "POST", {"studentId": sid}, admin)
    a.sql("DELETE FROM learning_bookmark WHERE student_id IN(" + ",".join(str(i) for i in users) + ")")
    if "--browser" in sys.argv:
        env = os.environ.copy()
        runtime = json.loads((ROOT / ".local/development-tools.json").read_text(encoding="utf-8-sig"))["nodeHome"]
        env["PATH"] = runtime + os.pathsep + env.get("PATH", "")
        for key, name in [("STUDENT", "student"), ("OTHER", "other"), ("MENTOR", "mentor")]:
            env["UPSELF_BOOKMARK_" + key] = actors[name]["username"]
            env["UPSELF_BOOKMARK_" + key + "_PASSWORD"] = actors[name]["password"]
        env["UPSELF_BOOKMARK_ADMIN_PASSWORD"] = password("admin01")
        env["UPSELF_BOOKMARK_COURSE"] = str(cid)
        env["UPSELF_BOOKMARK_COURSES"] = json.dumps(courses)
        env["UPSELF_BOOKMARK_STUDENT_ID"] = str(sid)
        env["UPSELF_BOOKMARK_COURSE_TITLE"] = prefix + " course 0"
        env["UPSELF_BOOKMARK_WORK"] = str(work)
        with (work / "browser.log").open("w", encoding="utf-8") as log:
            result = subprocess.run([str(Path(runtime) / "node.exe"), str(ROOT / "frontend/node_modules/@playwright/test/cli.js"), "test", "bookmarks.spec.ts"], cwd=ROOT / "frontend", env=env, stdout=log, stderr=subprocess.STDOUT)
        print((work / "browser.log").read_text(encoding="utf-8").encode("ascii", errors="backslashreplace").decode(), flush=True)
        assert result.returncode == 0, "Browser check failed; see local browser.log"
    call("student logout", "/client/logout", "POST", token=s)
    call("logged out token denied", base, token=s, code=401)
    (work / "api-result.json").write_text(json.dumps({"passed": len(checks), "checks": checks}, indent=2), encoding="utf-8")
    print("TOTAL", len(checks), "API checks passed", flush=True)


try:
    main()
finally:
    for cid in courses:
        cid = int(cid)
        if a.sql("SELECT COUNT(*) FROM learning_course WHERE id=" + str(cid) + " AND title LIKE '" + prefix + "%'") != "1": continue
        a.sql("DELETE FROM learning_bookmark WHERE course_id=" + str(cid) + ";DELETE FROM learning_chapter_progress WHERE chapter_id IN(SELECT id FROM learning_chapter WHERE course_id=" + str(cid) + ");DELETE FROM learning_chapter WHERE course_id=" + str(cid) + ";DELETE FROM learning_enrollment WHERE course_id=" + str(cid) + ";DELETE FROM learning_course WHERE id=" + str(cid) + ";")
    for uid in users:
        uid = int(uid)
        a.sql("DELETE FROM sys_user WHERE id=" + str(uid) + " AND user_name LIKE '" + prefix + "%'")
    for token in tokens:
        a.redis("DEL", a.key_for(token))
