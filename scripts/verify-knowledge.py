"""Local API and optional Chrome regression for knowledge checks and diary sharing.
Only uniquely named fixtures created by this invocation are removed.
"""
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
    raise RuntimeError("Only local test backends are allowed")
prefix = "knowledge_check_" + secrets.token_hex(4)
users, courses, tokens, checks = [], [], [], []
work = ROOT / ".local/knowledge-verification"
work.mkdir(parents=True, exist_ok=True)


def call(label, path, method="GET", body=None, token=None, code=200):
    result = a.request(path, method, body, token)[1]
    assert result.get("code") == code, (label, result.get("code"), result.get("msg"))
    checks.append(label)
    print("PASS", label, flush=True)
    return result


def main():
    config = (ROOT / "config/application-local.yml").read_text(encoding="utf-8-sig")
    def password(name):
        return re.search(r"^\s{4}" + name + r":\s*(.*?)\s*$", config, re.M).group(1).strip("\"'")
    a.clear_our_rate_limits()
    admin = call("administrator login", "/empLogin", "POST", {"username": "admin01", "password": password("admin01")})["token"]
    internal = call("internal login", "/empLogin", "POST", {"username": "maintenance", "password": password("maintenance")})["token"]
    tokens.extend([(admin, False), (internal, False)])
    actors = {}
    for name, role in [("mentor", 20), ("othermentor", 20), ("student", 10), ("otherstudent", 10), ("fresh", 10)]:
        username, pwd = prefix + name, secrets.token_urlsafe(18)
        uid = call("create " + name, "/system/user", "POST", {"userName": username, "nickName": "DEMO knowledge " + name, "roleId": role, "password": pwd}, admin)["data"]
        users.append(uid)
        token = call("login " + name, "/client/login" if role == 10 else "/empLogin", "POST", {"username": username, "password": pwd})["token"]
        actors[name] = {"id": uid, "username": username, "password": pwd, "token": token}
        tokens.append((token, role == 10))
    mentor, othermentor = actors["mentor"], actors["othermentor"]
    student, otherstudent, fresh = actors["student"], actors["otherstudent"], actors["fresh"]
    mt, ot, st = mentor["token"], othermentor["token"], student["token"]
    for name, owner, learners, status in [("main", mentor, [student, fresh], "PUBLISHED"), ("draft", mentor, [student], "DRAFT"), ("other", othermentor, [otherstudent], "PUBLISHED")]:
        cid = call("create " + name + " course", "/learning/courses", "POST", {"title": prefix + " " + name, "description": "Disposable knowledge verification", "difficulty": "BEGINNER", "estimatedMinutes": 10, "languageCode": "en"}, admin)["data"]
        courses.append(cid)
        cp = "/learning/courses/" + str(cid)
        call("assign mentor", cp + "/mentor", "PUT", {"mentorId": owner["id"]}, admin)
        for learner in learners: call("enroll student", cp + "/students", "POST", {"studentId": learner["id"]}, admin)
        if status == "PUBLISHED": call("publish course", cp + "/status", "PUT", {"status": status}, admin)
    cid, draftid, otherid = courses
    def create(course, text, token=mt, status="PUBLISHED"):
        return call("create question", f"/knowledge/courses/{course}/questions", "POST", {"questionText": text, "status": status}, token)["data"]
    qid = create(cid, "Can you explain a small learning step?")
    draftq = create(cid, "Unpublished question", status="DRAFT")
    draftcourseq = create(draftid, "Question in a draft course")
    otherq = create(otherid, "Another mentor's question", ot)
    own = "/client/knowledge/questions"
    manage = "/knowledge/questions/manage"
    path = "/knowledge/questions/" + str(qid)
    answerpath = "/client/knowledge/questions/" + str(qid) + "/answer"
    call("anonymous cannot read", own, code=401)
    assert call("student sees only joined published questions", own, token=st)["total"] == 1
    assert call("mentor list is scoped", manage, token=mt)["total"] == 3
    for token in [admin, internal]:
        call("management token cannot enter student channel", own, token=token, code=401)
        call("admin and maintenance cannot read student questions", "/knowledge/questions", token=token, code=403)
        call("admin and maintenance cannot manage knowledge", manage, token=token, code=403)
        call("admin and maintenance cannot read answers", path + "/answers", token=token, code=403)
    call("student cannot manage", "/client" + manage, token=st, code=403)
    call("student cannot read peer answers", "/client" + path + "/answers", token=st, code=403)
    call("mentor cannot submit as student", path + "/answer", "PUT", {"choice": "YES", "questionVersion": 0}, mt, 403)
    call("unrelated mentor cannot edit", path, "PUT", {"questionText": "Changed", "status": "PUBLISHED", "version": 0}, ot, 403)
    call("unrelated mentor cannot create", f"/knowledge/courses/{cid}/questions", "POST", {"questionText": "Not mine"}, ot, 403)
    call("unrelated mentor cannot read answers", path + "/answers", token=ot, code=403)
    call("unenrolled student cannot answer", answerpath, "PUT", {"choice": "YES", "questionVersion": 0}, otherstudent["token"], 403)
    call("draft question cannot be answered", f"/client/knowledge/questions/{draftq}/answer", "PUT", {"choice": "YES", "questionVersion": 0}, st, 409)
    call("draft course cannot be answered", f"/client/knowledge/questions/{draftcourseq}/answer", "PUT", {"choice": "YES", "questionVersion": 0}, st, 403)
    for body in [{"choice": "MAYBE", "questionVersion": 0}, {"choice": "YES"}, {"choice": None, "questionVersion": 0}]:
        call("invalid choice or missing version rejected", answerpath, "PUT", body, st, 400)
    call("stale question rejected", answerpath, "PUT", {"choice": "YES", "questionVersion": 99}, st, 409)
    result = call("student saves own answer", answerpath, "PUT", {"choice": "YES", "questionVersion": 0, "studentId": otherstudent["id"]}, st)["data"]
    assert result["studentId"] == student["id"]
    with concurrent.futures.ThreadPoolExecutor(2) as pool:
        results = list(pool.map(lambda _: a.request(answerpath, "PUT", {"choice": "NOT_SURE", "questionVersion": 0}, st)[1], range(2)))
    assert all(r["code"] == 200 for r in results)
    assert a.sql(f"SELECT COUNT(*) FROM knowledge_answer WHERE question_id={qid} AND student_id={student['id']}") == "1"
    checks.append("concurrent saves preserve one answer")
    assert call("answer persists after reload", own, token=st)["rows"][0]["choice"] == "NOT_SURE"
    assert call("mentor reads current student answer", path + "/answers", token=mt)["rows"][0]["choice"] == "NOT_SURE"
    call("answered wording cannot change", path, "PUT", {"questionText": "Different question", "status": "PUBLISHED", "version": 0}, mt, 409)
    call("answered question cannot be deleted", path + "?version=0", "DELETE", token=mt, code=409)
    call("archive answered question", path, "PUT", {"questionText": "Can you explain a small learning step?", "status": "ARCHIVED", "version": 0}, mt)
    assert call("archived question hidden from students", own, token=st)["total"] == 0
    call("republish question", path, "PUT", {"questionText": "Can you explain a small learning step?", "status": "PUBLISHED", "version": 1}, mt)
    call("old version rejected after republish", answerpath, "PUT", {"choice": "NO", "questionVersion": 0}, st, 409)
    call("change saved answer", answerpath, "PUT", {"choice": "NO", "questionVersion": 2}, st)
    call("edit unanswered draft", f"/knowledge/questions/{draftq}", "PUT", {"questionText": "Updated draft question?", "status": "DRAFT", "version": 0}, mt)
    call("delete unanswered question", f"/knowledge/questions/{draftq}?version=1", "DELETE", token=mt)
    for n in range(11): create(cid, f"Can you describe learning step {n + 1}?")
    paged = call("student questions are paginated", own + "?current=2&pageSize=10", token=st)
    assert paged["total"] == 12 and len(paged["rows"]) == 2
    assert call("other student sees own course only", own, token=otherstudent["token"])["total"] == 1
    assert call("query parameter cannot impersonate another student", own + f"?studentId={student['id']}", token=otherstudent["token"])["rows"][0]["id"] == otherq

    # All diaries, including earlier test notes, follow the current mentor relationship.
    mood = "/client/moods"
    current = call("read updated diary notice", mood + "/current", token=st)["data"]
    previous_week = (datetime.date.fromisoformat(current["weekStart"]) - datetime.timedelta(days=7)).isoformat()
    old_private = "OLD_PRIVATE_" + prefix
    a.sql(f"INSERT INTO mood_notice_ack(student_id,notice_version) VALUES({student['id']},'2026-10-22-v1'); INSERT INTO mood_entry(student_id,week_start,mood_level,private_note) VALUES({student['id']},'{previous_week}',3,'{old_private}');")
    assert not call("old notice requires fresh confirmation", mood + "/current", token=st)["data"]["acknowledged"]
    call("cannot record diary with old consent", mood + "/entries", "POST", {"weekStart": current["weekStart"], "moodLevel": 4, "privateNote": "Not shared"}, st, 403)
    call("confirm updated diary sharing", mood + "/acknowledgment", "POST", {"accepted": True, "noticeVersion": current["noticeVersion"]}, st)
    assert call("student retains earlier private note", mood + "/history", token=st)["rows"][0]["privateNote"] == old_private
    shared = "Shared diary: <b>A quiet learning space helps me.</b>"
    record = call("save newly shared diary", mood + "/entries", "POST", {"weekStart": current["weekStart"], "moodLevel": 4, "privateNote": shared}, st)["data"]["entry"]
    assert record["noteShared"] is True
    historypath = f"/moods/students/{student['id']}/history"
    history = call("current mentor reads new diary only", historypath, token=mt)
    assert history["total"] == 2 and history["rows"][0]["diaryNote"] == shared
    assert history["rows"][1]["diaryNote"] == old_private and "privateNote" not in json.dumps(history)
    call("unrelated mentor cannot read diaries", historypath, token=ot, code=403)
    for token in [admin, internal]: call("other roles cannot read diaries", historypath, token=token, code=403)
    call("other student cannot read diaries", "/client" + historypath, token=otherstudent["token"], code=403)

    cp = "/learning/courses/" + str(cid)
    call("remove enrollment", cp + "/students/" + str(student["id"]), "DELETE", token=admin)
    call("removed student cannot answer", answerpath, "PUT", {"choice": "YES", "questionVersion": 2}, st, 403)
    assert call("removed student is excluded from response list", path + "/answers", token=mt)["total"] == 0
    call("restore enrollment", cp + "/students", "POST", {"studentId": student["id"]}, admin)
    call("reassign course mentor", cp + "/mentor", "PUT", {"mentorId": othermentor["id"]}, admin)
    call("old mentor loses diary access", historypath, token=mt, code=403)
    call("old mentor loses question writes", path, "PUT", {"questionText": "Changed", "status": "PUBLISHED", "version": 2}, mt, 403)
    call("old mentor loses answer access", path + "/answers", token=mt, code=403)
    assert call("new mentor sees current student answers", path + "/answers", token=ot)["total"] == 1
    assert old_private in json.dumps(call("new mentor can read earlier diaries", historypath, token=ot))
    call("restore original mentor", cp + "/mentor", "PUT", {"mentorId": mentor["id"]}, admin)
    call("archive course", cp + "/status", "PUT", {"status": "ARCHIVED"}, admin)
    call("archived course blocks response access", path + "/answers", token=mt, code=403)
    call("archived course blocks question updates", path, "PUT", {"questionText": "Changed", "status": "PUBLISHED", "version": 2}, mt, 409)
    assert call("archived course questions hidden", own, token=st)["total"] == 0
    call("restore published course", cp + "/status", "PUT", {"status": "PUBLISHED"}, admin)
    assert a.sql("SELECT COUNT(*) FROM flyway_schema_history WHERE version='10' AND success=1") == "1"
    print("Knowledge and diary API checks:", len(checks), flush=True)
    if "--browser" in sys.argv:
        fixture = {"actors": actors, "courseId": cid, "courseTitle": prefix + " main", "oldPrivate": old_private, "sharedDiary": shared, "adminPassword": password("admin01")}
        pathfile = work / "fixture.json"
        pathfile.write_text(json.dumps(fixture), encoding="utf-8")
        env = os.environ.copy()
        env["UPSELF_KNOWLEDGE_FIXTURE"] = str(pathfile)
        env["UPSELF_SCREENSHOTS"] = str(work)
        runtime = Path(json.loads((ROOT / ".local/development-tools.json").read_text(encoding="utf-8-sig"))["nodeHome"])
        env["PATH"] = str(runtime) + os.pathsep + env.get("PATH", "")
        try:
            result = subprocess.run([str(runtime / "node.exe"), str(ROOT / "frontend/node_modules/@playwright/test/cli.js"), "test", "tests/e2e/knowledge.spec.ts"], cwd=ROOT / "frontend", env=env)
            if result.returncode: raise RuntimeError("Chrome knowledge verification failed")
        finally:
            pathfile.unlink(missing_ok=True)


def cleanup():
    for token, student in tokens:
        try: a.request("/client/logout" if student else "/logout", "POST", token=token)
        except Exception: pass
    for cid in courses:
        if a.sql(f"SELECT COUNT(*) FROM learning_course WHERE id={cid} AND title LIKE '{prefix}%'") == "1":
            a.sql(f"DELETE FROM knowledge_answer WHERE question_id IN(SELECT id FROM knowledge_question WHERE course_id={cid}); DELETE FROM knowledge_question WHERE course_id={cid}; DELETE FROM learning_enrollment WHERE course_id={cid}; DELETE FROM learning_course WHERE id={cid};")
    for uid in users:
        if a.sql(f"SELECT COUNT(*) FROM sys_user WHERE id={uid} AND user_name LIKE '{prefix}%'") == "1":
            a.sql(f"DELETE FROM mood_entry WHERE student_id={uid}; DELETE FROM mood_notice_ack WHERE student_id={uid}; DELETE FROM sys_user WHERE id={uid};")


if __name__ == "__main__":
    try: main()
    finally: cleanup()
