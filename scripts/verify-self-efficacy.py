"""FR-25 local API/Chrome verification. Creates and removes only its own fixtures."""
from pathlib import Path
import concurrent.futures
import datetime
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
    raise RuntimeError("Only local reviewed test ports are allowed")
prefix = "eff_check_" + secrets.token_hex(4)
checks, users, courses, tokens = [], [], [], []
work = ROOT / ".local/efficacy-verification"
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
    maintenance = call("internal login", "/empLogin", "POST", {"username": "maintenance", "password": password("maintenance")})["token"]
    tokens.extend([admin, maintenance])
    actors = {}
    for name, role in [("history", 10), ("student", 10), ("mentor", 20), ("othermentor", 20)]:
        username, pwd = prefix + name, secrets.token_urlsafe(18)
        uid = call("create " + name, "/system/user", "POST", {"userName": username, "nickName": "DEMO self-belief " + name, "password": pwd, "roleId": role}, admin)["data"]
        users.append(uid)
        token = call("login " + name, "/client/login" if role == 10 else "/empLogin", "POST", {"username": username, "password": pwd})["token"]
        tokens.append(token)
        actors[name] = {"id": uid, "username": username, "password": pwd, "token": token}
    s, sid = actors["history"]["token"], actors["history"]["id"]
    other, oid = actors["student"]["token"], actors["student"]["id"]
    mentor, mt = actors["mentor"]["id"], actors["mentor"]["token"]
    second, st = actors["othermentor"]["id"], actors["othermentor"]["token"]
    base = "/client/self-efficacy"
    versions = a.sql("SELECT GROUP_CONCAT(version ORDER BY installed_rank) FROM flyway_schema_history WHERE success=1 AND version IS NOT NULL").split(",")
    assert versions[:9] == [str(i) for i in range(1, 10)]
    checks.append("Flyway V1-V9 applied")
    cid = call("create course", "/learning/courses", "POST", {"title": prefix + " course", "description": "Disposable self-belief verification."}, admin)["data"]
    courses.append(cid)
    cp = "/learning/courses/" + str(cid)
    call("assign current mentor", cp + "/mentor", "PUT", {"mentorId": mentor}, admin)
    for uid in [sid, oid]: call("enroll student", cp + "/students", "POST", {"studentId": uid}, admin)
    call("publish course", cp + "/status", "PUT", {"status": "PUBLISHED"}, admin)
    c = call("current metadata before independent notice", base + "/current", token=s)["data"]
    today, instrument, notice = c["today"], c["instrument"]["version"], c["noticeVersion"]
    assert not c["acknowledged"] and not c["available"] and c["latest"] is None
    assert c["intervalDays"] == 28 and c["timeZone"] == "Asia/Kuala_Lumpur"
    assert len(c["instrument"]["questions"]) == 3 and len(c["instrument"]["responses"]) == 5
    assert c["instrument"]["license"] == "CC BY-NC-SA 4.0" and {p["id"] for p in c["viewers"]} == {mentor}
    body = {"assessmentDate": today, "instrumentVersion": instrument, "answers": [1, 2, 2]}
    shared = "/self-efficacy/students/" + str(sid)
    for path in ["/current", "/history", "/trend"]: call("anonymous denied " + path, base + path, code=401)
    call("anonymous record denied", base + "/entries", "POST", body, code=401)
    for name, token in [("mentor", mt), ("administrator", admin), ("maintenance wildcard", maintenance)]:
        for path in ["/current", "/history", "/trend"]: call(name + " cannot read owner " + path, "/self-efficacy" + path, token=token, code=403)
        call(name + " cannot record", "/self-efficacy/entries", "POST", body, token, 403)
    for name, token in [("administrator", admin), ("maintenance wildcard", maintenance), ("unrelated mentor", st)]:
        for path in ["", "/history", "/trend"]: call(name + " cannot read shared " + path, shared + path, token=token, code=403)
    call("student cannot use mentor path", "/client/self-efficacy/students/" + str(oid) + "/history", token=s, code=403)
    call("no notice cannot record", base + "/entries", "POST", body, s, 403)
    call("no notice cannot read history", base + "/history", token=s, code=403)
    call("no notice cannot read trend", base + "/trend", token=s, code=403)
    assert call("mentor sees no unshared history", shared + "/history", token=mt)["total"] == 0
    call("declined notice rejected", base + "/acknowledgment", "POST", {"noticeVersion": notice, "accepted": False}, s, 400)
    call("old notice rejected", base + "/acknowledgment", "POST", {"noticeVersion": "old", "accepted": True}, s, 400)
    call("independent notice accepted", base + "/acknowledgment", "POST", {"noticeVersion": notice, "accepted": True}, s)
    accepted_at = a.sql("SELECT accepted_at FROM self_efficacy_notice_ack WHERE student_id=" + str(sid))
    call("notice repeated safely", base + "/acknowledgment", "POST", {"noticeVersion": notice, "accepted": True}, s)
    assert a.sql("SELECT accepted_at FROM self_efficacy_notice_ack WHERE student_id=" + str(sid)) == accepted_at
    assert a.sql("SELECT COUNT(*) FROM mood_notice_ack WHERE student_id=" + str(sid)) == "0"
    for answers in [[], [1, 2], [1, 2, 3, 4], [None, 2, 3], [0, 2, 3], [6, 2, 3], [1, 2.5, 3]]:
        call("incomplete or non-ordinal answers rejected", base + "/entries", "POST", {**body, "answers": answers}, s, 400)
    call("stale questionnaire rejected", base + "/entries", "POST", {**body, "instrumentVersion": "old"}, s, 409)
    call("backdated record rejected", base + "/entries", "POST", {**body, "assessmentDate": "2020-01-01"}, s, 409)
    assert a.sql("SELECT COUNT(*) FROM self_efficacy_entry WHERE student_id=" + str(sid)) == "0"
    with concurrent.futures.ThreadPoolExecutor(2) as pool:
        results = list(pool.map(lambda _: a.request(base + "/entries", "POST", {**body, "score": 5, "studentId": oid}, s)[1]["code"], range(2)))
    assert sorted(results) == [200, 409]
    checks.append("concurrent saves create one record")
    latest = call("saved score calculated by server", base + "/current", token=s)["data"]
    assert latest["latest"]["score"] == 1.67 and latest["latest"]["studentId"] == sid and not latest["available"]
    assert a.sql("SELECT COUNT(*) FROM self_efficacy_entry WHERE student_id=" + str(sid)) == "1"
    call("same period cannot be overwritten", base + "/entries", "POST", {**body, "answers": [5, 5, 5]}, s, 409)
    day = datetime.date.fromisoformat(today)
    a.sql("UPDATE self_efficacy_entry SET assessment_date='" + (day - datetime.timedelta(days=27)).isoformat() + "' WHERE student_id=" + str(sid))
    assert not call("27 day boundary stays locked", base + "/current", token=s)["data"]["available"]
    call("27 days cannot record", base + "/entries", "POST", body, s, 409)
    a.sql("UPDATE self_efficacy_entry SET assessment_date='" + (day - datetime.timedelta(days=28)).isoformat() + "' WHERE student_id=" + str(sid))
    assert call("28 day boundary opens", base + "/current", token=s)["data"]["available"]
    call("new period record allowed", base + "/entries", "POST", {**body, "answers": [5, 5, 5]}, s)
    history = call("owner sees original answers", base + "/history", token=s)["rows"]
    assert history[0]["answerOne"] == 5 and history[1]["answerOne"] == 1
    shared_rows = call("current mentor sees readonly scores", shared + "/history", token=mt)["rows"]
    assert all(set(row) == {"assessmentDate", "score", "instrumentVersion"} for row in shared_rows)
    assert call("mentor trend has actual dates only", shared + "/trend", token=mt)["data"]["points"] == list(reversed(shared_rows))
    for weeks in [3, 53]: call("trend range bounded", base + "/trend?weeks=" + str(weeks), token=s, code=400)
    call("second student accepts own notice", base + "/acknowledgment", "POST", {"noticeVersion": notice, "accepted": True}, other)
    assert call("forged studentId does not read another owner", base + "/history?studentId=" + str(sid), token=other)["total"] == 0
    call("replace mentor", cp + "/mentor", "PUT", {"mentorId": second}, admin)
    call("old mentor immediately loses history", shared + "/history", token=mt, code=403)
    call("old mentor immediately loses trend", shared + "/trend", token=mt, code=403)
    assert call("new mentor gains existing shared history", shared + "/history", token=st)["total"] == 2
    assert {p["id"] for p in call("sharing viewers follow assignment", base + "/current", token=s)["data"]["viewers"]} == {second}
    call("archive course", cp + "/status", "PUT", {"status": "ARCHIVED"}, admin)
    call("archived relationship denied", shared + "/history", token=st, code=403)
    call("republish course", cp + "/status", "PUT", {"status": "PUBLISHED"}, admin)
    call("remove enrollment", cp + "/students/" + str(sid), "DELETE", token=admin)
    call("revoked student relationship denied", shared + "/history", token=st, code=403)
    call("restore enrollment", cp + "/students", "POST", {"studentId": sid}, admin)
    call("restore current mentor for browser", cp + "/mentor", "PUT", {"mentorId": mentor}, admin)
    # Explicitly synthetic history, limited to the disposable fixture account.
    a.sql("DELETE FROM self_efficacy_entry WHERE student_id=" + str(sid) + " AND assessment_date<>'" + today + "'")
    for n in range(1, 13):
        date = (day - datetime.timedelta(days=28 * n)).isoformat()
        value = 1 + n % 5
        a.sql("INSERT INTO self_efficacy_entry(student_id,assessment_date,instrument_version,answer_one,answer_two,answer_three,score) VALUES(" + str(sid) + ",'" + date + "','" + instrument + "'," + ",".join([str(value)] * 4) + ")")
    first = call("history page one", base + "/history?current=1&pageSize=10", token=s)
    second_page = call("history page two", base + "/history?current=2&pageSize=10", token=s)
    assert first["total"] == second_page["total"] == 13 and len(first["rows"]) == 10 and len(second_page["rows"]) == 3, (first["total"], second_page["total"], len(first["rows"]), len(second_page["rows"]))
    trend = call("year trend has saved points only", base + "/trend?weeks=52", token=s)["data"]
    assert len(trend["points"]) == 13 and len({p["assessmentDate"] for p in trend["points"]}) == 13
    # Reset only the second fixture's empty acknowledgment for first-use UI testing.
    a.sql("DELETE FROM self_efficacy_notice_ack WHERE student_id=" + str(oid))
    if "--browser" in sys.argv:
        runtime = json.loads((ROOT / ".local/development-tools.json").read_text(encoding="utf-8-sig"))["nodeHome"]
        env = os.environ.copy(); env["PATH"] = runtime + os.pathsep + env.get("PATH", "")
        for name, actor in actors.items():
            env["UPSELF_EFFICACY_" + name.upper()] = actor["username"]
            env["UPSELF_EFFICACY_" + name.upper() + "_PASSWORD"] = actor["password"]
            env["UPSELF_EFFICACY_" + name.upper() + "_ID"] = str(actor["id"])
        env["UPSELF_EFFICACY_ADMIN_PASSWORD"] = password("admin01")
        env["UPSELF_EFFICACY_COURSE"] = str(cid); env["UPSELF_EFFICACY_WORK"] = str(work)
        with (work / "browser.log").open("w", encoding="utf-8") as log:
            result = subprocess.run([str(Path(runtime) / "node.exe"), str(ROOT / "frontend/node_modules/@playwright/test/cli.js"), "test", "self-efficacy.spec.ts"], cwd=ROOT / "frontend", env=env, stdout=log, stderr=subprocess.STDOUT)
        print((work / "browser.log").read_text(encoding="utf-8").encode("ascii", errors="backslashreplace").decode(), flush=True)
        assert result.returncode == 0, "Self-efficacy browser verification failed"
    call("student logout", "/client/logout", "POST", token=s)
    call("logged out token denied", base + "/history", token=s, code=401)
    (work / "api-result.json").write_text(json.dumps({"passed": len(checks), "checks": checks}, indent=2), encoding="utf-8")
    print("TOTAL", len(checks), "self-efficacy API checks passed", flush=True)


try:
    main()
finally:
    for uid in users:
        uid = int(uid)
        if a.sql("SELECT COUNT(*) FROM sys_user WHERE id=" + str(uid) + " AND user_name LIKE '" + prefix + "%'") != "1": continue
        a.sql("DELETE FROM self_efficacy_entry WHERE student_id=" + str(uid) + ";DELETE FROM self_efficacy_notice_ack WHERE student_id=" + str(uid) + ";DELETE FROM mood_entry WHERE student_id=" + str(uid) + ";DELETE FROM mood_notice_ack WHERE student_id=" + str(uid))
    for cid in courses:
        cid = int(cid)
        if a.sql("SELECT COUNT(*) FROM learning_course WHERE id=" + str(cid) + " AND title LIKE '" + prefix + "%'") == "1":
            a.sql("DELETE FROM learning_enrollment WHERE course_id=" + str(cid) + ";DELETE FROM learning_course WHERE id=" + str(cid))
    for uid in users:
        a.sql("DELETE FROM sys_user WHERE id=" + str(int(uid)) + " AND user_name LIKE '" + prefix + "%'")
    for token in tokens: a.redis("DEL", a.key_for(token))
