"""Local-only learning API checks; fixtures and uploads are removed on completion.
Stop the application first, keep MySQL/Redis running, then run with Python 3.
"""
from pathlib import Path
import base64
import concurrent.futures
import importlib.util
import json
import os
import secrets
import shutil
import socket
import subprocess
import time
import urllib.error
import urllib.request
import zipfile

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location("auth_checks", ROOT / "scripts/verify-local.py")
auth = importlib.util.module_from_spec(spec)
spec.loader.exec_module(auth)
PREFIX = "learning_check_" + secrets.token_hex(4)
PASSWORD = secrets.token_urlsafe(18)
TOKENS = []
COURSES = []
USERS = []
TICKETS = []
CHECKS = []
BASE = "http://127.0.0.1:8080"


def call(path, method="GET", body=None, token=None):
    return auth.request(path, method, body, token)


def expect(label, result, code=200):
    payload = result[1]
    assert payload.get("code") == code, label + ": code=" + str(payload.get("code")) + " msg=" + str(payload.get("msg"))
    assert not any("\u4e00" <= char <= "\u9fff" for char in payload.get("msg", "")), label + ": non-English response"
    CHECKS.append(label)
    print("PASS", label, flush=True)
    return payload


def upload(chapter, token, kind, name, data, **fields):
    boundary = "----UpSelf" + secrets.token_hex(12)
    body = bytearray()
    for key, value in dict(kind=kind, title=name, languageCode="en", status="PUBLISHED", **fields).items():
        body.extend(("--" + boundary + '\r\nContent-Disposition: form-data; name="' + key + '"\r\n\r\n' + str(value) + "\r\n").encode())
    body.extend(("--" + boundary + '\r\nContent-Disposition: form-data; name="file"; filename="' + name + '"\r\nContent-Type: application/octet-stream\r\n\r\n').encode())
    body.extend(data)
    body.extend(("\r\n--" + boundary + "--\r\n").encode())
    request = urllib.request.Request(BASE + "/learning/chapters/%d/resources" % chapter,
        data=bytes(body), headers={"Content-Type": "multipart/form-data; boundary=" + boundary, "Authorization": "Bearer " + token})
    try:
        response = urllib.request.urlopen(request, timeout=30)
    except urllib.error.HTTPError as error:
        response = error
    with response:
        return response.status, json.load(response)


def content(path, token=None, range_header=None):
    headers = {}
    if token:
        headers["Authorization"] = "Bearer " + token
    if range_header:
        headers["Range"] = range_header
    request = urllib.request.Request(BASE + path, headers=headers)
    try:
        response = urllib.request.urlopen(request, timeout=15)
    except urllib.error.HTTPError as error:
        response = error
    with response:
        return response.status, response.headers, response.read()


def json_content_result(raw):
    return raw[0], json.loads(raw[2])


def media_link(resource_id, token, student=True):
    prefix = "/client/learning" if student else "/learning"
    result = expect("issue-scoped-media-link", call(prefix + "/resources/%d/access" % resource_id, "POST", token=token))
    url = result["data"]["url"]
    TICKETS.append(url.rsplit("/", 1)[1])
    assert result["data"]["expiresIn"] == 1800 and token not in url
    return url


def main():
    with socket.socket() as test:
        if test.connect_ex(("127.0.0.1", 8080)) == 0:
            raise RuntimeError("Stop the running application before these isolated checks")
    scratch = ROOT / ".local" / PREFIX
    scratch.mkdir(parents=True)
    uploads = scratch / "uploads"
    log = open(scratch / "application.log", "w", encoding="utf-8")
    app = subprocess.Popen([r"C:\Program Files\Java\jdk1.8.0_231\bin\java.exe", "-Dfile.encoding=UTF-8", "-jar",
        str(ROOT / "up-self-admin/target/up-self-admin.jar"), "--spring.config.additional-location=file:./config/",
        "--learning.storage-path=" + str(uploads)], cwd=ROOT, stdout=log, stderr=subprocess.STDOUT,
        creationflags=subprocess.CREATE_NO_WINDOW)
    try:
        for _ in range(90):
            if app.poll() is not None:
                raise RuntimeError("Application startup failed: " + str(scratch / "application.log"))
            try:
                call("/getInfo")
                break
            except (OSError, ValueError):
                time.sleep(1)
        else:
            raise RuntimeError("Application startup timeout")
        auth.clear_our_rate_limits()
        assert auth.sql("SELECT COUNT(*) FROM flyway_schema_history WHERE version='3' AND success=1;") == "1"
        CHECKS.append("flyway-v3-migrated")
        cp = scratch / "cp"
        cp.mkdir()
        with zipfile.ZipFile(ROOT / "up-self-admin/target/up-self-admin.jar") as jar:
            for name in jar.namelist():
                if name.startswith("BOOT-INF/lib/") and name.endswith(".jar"):
                    (cp / Path(name).name).write_bytes(jar.read(name))
        source = scratch / "FixturePassword.java"
        source.write_text('import com.upself.common.utils.PasswordUtils; public class FixturePassword { public static void main(String[] args)throws Exception{System.out.print(PasswordUtils.encode(new java.io.BufferedReader(new java.io.InputStreamReader(System.in)).readLine()));}}', encoding="utf-8")
        classpath = str(cp / "*") + os.pathsep + str(scratch)
        subprocess.run([r"C:\Program Files\Java\jdk1.8.0_231\bin\javac.exe", "-cp", classpath, str(source)], check=True, capture_output=True)
        hashed = subprocess.run([r"C:\Program Files\Java\jdk1.8.0_231\bin\java.exe", "-cp", classpath, "FixturePassword"], input=PASSWORD, text=True, capture_output=True, check=True).stdout
        actors = {}
        for suffix, role in (("admin", 30), ("mentor", 20), ("othermentor", 20), ("student", 10), ("otherstudent", 10)):
            name = PREFIX + suffix
            auth.sql("INSERT INTO sys_user(user_name,nick_name,password,role_id) VALUES('%s','API check','%s',%d);" % (name, hashed, role))
            user_id = int(auth.sql("SELECT id FROM sys_user WHERE user_name='%s';" % name))
            USERS.append(user_id)
            channel = "student" if role == 10 else "admin"
            response = expect(suffix + "-password-only-login", call("/client/login" if role == 10 else "/empLogin", "POST", {"username": name, "password": PASSWORD}))
            token = response["token"]
            assert "ticket" not in response
            TOKENS.append(token)
            actors[suffix] = (user_id, token)
        aid, admin = actors["admin"]
        mid, mentor = actors["mentor"]
        mid2, mentor2 = actors["othermentor"]
        sid, student = actors["student"]
        sid2, student2 = actors["otherstudent"]
        expect("anonymous-cannot-list-courses", call("/client/learning/courses"), 401)
        invalid = expect("english-parameter-validation", call("/learning/courses", "POST", {"title": "", "description": ""}, admin), 400)
        assert invalid["msg"] != "{field.required}"
        c = expect("admin-create-draft-course", call("/learning/courses", "POST", {"title": PREFIX + "Course", "description": "Plain text course", "languageCode": "en"}, admin))["data"]
        COURSES.append(c)
        c2 = expect("admin-create-second-course", call("/learning/courses", "POST", {"title": PREFIX + "Other", "description": "Other course"}, admin))["data"]
        COURSES.append(c2)
        expect("student-cannot-create-course", call("/client/learning/courses", "POST", {"title": "Bad", "description": "Bad"}, student), 403)
        expect("mentor-cannot-create-course", call("/learning/courses", "POST", {"title": "Bad", "description": "Bad"}, mentor), 403)
        expect("mentor-cannot-assign-mentor", call("/learning/courses/%d/mentor" % c, "PUT", {"mentorId": mid}, mentor), 403)
        expect("reject-student-as-mentor", call("/learning/courses/%d/mentor" % c, "PUT", {"mentorId": sid}, admin), 400)
        expect("publish-requires-mentor", call("/learning/courses/%d/status" % c, "PUT", {"status": "PUBLISHED"}, admin), 400)
        expect("admin-assign-mentor", call("/learning/courses/%d/mentor" % c, "PUT", {"mentorId": mid}, admin))
        expect("admin-assign-other-mentor", call("/learning/courses/%d/mentor" % c2, "PUT", {"mentorId": mid2}, admin))
        expect("reject-mentor-enrollment", call("/learning/courses/%d/students" % c, "POST", {"studentId": mid}, admin), 400)
        expect("admin-enroll-student", call("/learning/courses/%d/students" % c, "POST", {"studentId": sid}, admin))
        expect("enrollment-idempotent", call("/learning/courses/%d/students" % c, "POST", {"studentId": sid}, admin))
        members = expect("course-members", call("/learning/courses/%d/students" % c, token=mentor))["data"]
        assert len(members) == 1
        expect("student-cannot-list-peer-members", call("/client/learning/courses/%d/students" % c, token=student), 403)
        expect("draft-course-hidden", call("/client/learning/courses/%d" % c, token=student), 403)
        assert expect("draft-course-not-in-student-list", call("/client/learning/courses", token=student))["total"] == 0
        assert expect("mentor-list-scoped-to-own-course", call("/learning/courses", token=mentor))["total"] == 1
        expect("other-mentor-cannot-read-course", call("/learning/courses/%d" % c, token=mentor2), 403)
        chapter_body = {"title": "First chapter", "body": "Learning text", "sortOrder": 2, "status": "PUBLISHED", "languageCode": "en"}
        ch = expect("assigned-mentor-create-chapter", call("/learning/courses/%d/chapters" % c, "POST", chapter_body, mentor))["data"]
        draft = expect("create-draft-chapter", call("/learning/courses/%d/chapters" % c, "POST", dict(chapter_body, title="Draft", status="DRAFT"), mentor))["data"]
        expect("other-mentor-cannot-edit-chapter", call("/learning/chapters/%d" % ch, "PUT", chapter_body, mentor2), 403)
        expect("other-mentor-cannot-create-chapter", call("/learning/courses/%d/chapters" % c, "POST", chapter_body, mentor2), 403)
        expect("student-cannot-create-chapter", call("/client/learning/courses/%d/chapters" % c, "POST", chapter_body, student), 403)
        expect("publish-course", call("/learning/courses/%d/status" % c, "PUT", {"status": "PUBLISHED"}, admin))
        expect("joined-student-course-detail", call("/client/learning/courses/%d" % c, token=student))
        assert expect("student-course-list-total", call("/client/learning/courses?pageNum=1&pageSize=1", token=student))["total"] == 1
        expect("unjoined-student-cannot-read", call("/client/learning/courses/%d" % c, token=student2), 403)
        chapters = expect("published-chapters-only", call("/client/learning/courses/%d/chapters" % c, token=student))["data"]
        assert len(chapters) == 1 and chapters[0]["id"] == ch
        expect("draft-chapter-detail-denied", call("/client/learning/chapters/%d" % draft, token=student), 403)
        image = base64.b64decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+j9ioAAAAASUVORK5CYII=")
        expect("image-requires-alternative-text", upload(ch, mentor, "IMAGE", "image.png", image), 400)
        expect("reject-disguised-svg", upload(ch, mentor, "IMAGE", "bad.png", b"<svg/>", altText="A diagram"), 400)
        expect("cross-course-upload-denied", upload(ch, mentor2, "IMAGE", "bad.png", image, altText="A diagram"), 403)
        img = expect("upload-image-with-alt-text", upload(ch, mentor, "IMAGE", "diagram.png", image, altText="A one-pixel diagram"))["data"]
        vtt = expect("upload-webvtt", upload(ch, mentor, "CAPTION", "english.vtt", b"WEBVTT\n\n00:00.000 --> 00:01.000\nHello\n"))["data"]
        video_bytes = b"\x00\x00\x00\x18ftypmp42\x00\x00\x00\x00mp42isom" + bytes(range(256))
        video = expect("upload-video-with-caption-and-transcript", upload(ch, mentor, "VIDEO", "lesson.mp4", video_bytes, captionResourceId=vtt, transcript="A text equivalent."))["data"]
        res = expect("resource-metadata", call("/client/learning/resources/%d" % video, token=student))["data"]
        assert "storageKey" not in res and res["captionResourceId"] == vtt and res["transcript"] == "A text equivalent."
        assert expect("image-alt-text-round-trip", call("/client/learning/resources/%d" % img, token=student))["data"]["altText"] == "A one-pixel diagram"
        expect("unjoined-resource-metadata-denied", call("/client/learning/resources/%d" % img, token=student2), 403)
        expect("caption-in-use-cannot-delete", call("/learning/resources/%d" % vtt, "DELETE", token=mentor), 400)
        status, headers, data = content("/client/learning/resources/%d/content" % img, student)
        assert status == 200 and data == image and headers["Content-Type"].startswith("image/png")
        CHECKS.append("authenticated-image-bytes")
        status, headers, data = content("/client/learning/resources/%d/content" % video, student, "bytes=0-15")
        assert status == 206 and data == video_bytes[:16] and headers["Content-Range"].startswith("bytes 0-15/")
        CHECKS.append("video-range-206")
        assert content("/client/learning/resources/%d/content" % video, student, "bytes=999999-1000000")[0] == 416
        CHECKS.append("invalid-range-416")
        expect("raw-resource-requires-login", json_content_result(content("/client/learning/resources/%d/content" % img)), 401)
        url = media_link(video, student)
        status, headers, data = content(url, range_header="bytes=16-31")
        assert status == 206 and data == video_bytes[16:32] and headers["Cache-Control"] == "private, no-store"
        CHECKS.append("html5-media-ticket-range")
        expect("random-media-ticket-rejected", json_content_result(content("/client/learning/media/" + "a" * 32)), 403)
        expect("media-channel-cannot-be-swapped", json_content_result(content(url.replace("/client/", "/"))), 403)
        post_body = {"title": "Weekly meeting", "body": "Please join our weekly session.", "type": "MEETING", "meetingUrl": "https://meet.example.com/course", "meetingStartsAt": "2026-11-01T10:00:00+08:00", "status": "PUBLISHED", "languageCode": "en"}
        expect("reject-javascript-meeting-link", call("/learning/courses/%d/posts" % c, "POST", dict(post_body, meetingUrl="javascript:alert(1)"), mentor), 400)
        expect("reject-ambiguous-meeting-time", call("/learning/courses/%d/posts" % c, "POST", dict(post_body, meetingStartsAt="2026-11-01T10:00:00"), mentor), 400)
        post = expect("mentor-publish-meeting-post", call("/learning/courses/%d/posts" % c, "POST", post_body, mentor))["data"]
        pd = expect("mentor-draft-announcement", call("/learning/courses/%d/posts" % c, "POST", dict(post_body, title="Draft announcement", type="ANNOUNCEMENT", status="DRAFT"), mentor))["data"]
        assert len(expect("student-published-posts-only", call("/client/learning/courses/%d/posts" % c, token=student))["data"]) == 1
        assert expect("meeting-link-and-time-round-trip", call("/client/learning/posts/%d" % post, token=student))["data"]["meetingStartsAt"].endswith("+08:00")
        expect("student-draft-post-denied", call("/client/learning/posts/%d" % pd, token=student), 403)
        expect("unjoined-post-denied", call("/client/learning/posts/%d" % post, token=student2), 403)
        expect("mentor-cannot-post-in-other-course", call("/learning/courses/%d/posts" % c2, "POST", post_body, mentor), 403)
        expect("student-cannot-post", call("/client/learning/courses/%d/posts" % c, "POST", post_body, student), 403)
        expect("admin-unpublish-post", call("/learning/posts/%d/offline" % post, "PUT", token=admin))
        expect("unpublished-post-detail-denied", call("/client/learning/posts/%d" % post, token=student), 403)
        expect("mentor-republish-own-post", call("/learning/posts/%d" % post, "PUT", post_body, mentor))
        expect("admin-reassign-mentor", call("/learning/courses/%d/mentor" % c, "PUT", {"mentorId": mid2}, admin))
        expect("old-mentor-loses-content-write", call("/learning/chapters/%d" % ch, "PUT", chapter_body, mentor), 403)
        expect("old-mentor-loses-resource-read", call("/learning/resources/%d" % img, token=mentor), 403)
        expect("new-mentor-maintains-chapters", call("/learning/chapters/%d" % ch, "PUT", chapter_body, mentor2))
        expect("new-mentor-cannot-impersonate-old-author", call("/learning/posts/%d" % post, "PUT", post_body, mentor2), 403)
        expect("unenroll-student", call("/learning/courses/%d/students/%d" % (c, sid), "DELETE", token=admin))
        expect("unenrollment-revokes-media-ticket", json_content_result(content(url)), 403)
        expect("unenrollment-revokes-content", call("/client/learning/chapters/%d" % ch, token=student), 403)
        expect("reenroll-student", call("/learning/courses/%d/students" % c, "POST", {"studentId": sid}, admin))
        url2 = media_link(img, student)
        expect("unpublish-parent-course", call("/learning/courses/%d/status" % c, "PUT", {"status": "DRAFT"}, admin))
        expect("parent-unpublish-revokes-media", json_content_result(content(url2)), 403)
        expect("republish-parent-course", call("/learning/courses/%d/status" % c, "PUT", {"status": "PUBLISHED"}, admin))
        expect("logout-student", call("/client/logout", "POST", token=student))
        expect("logout-revokes-media-link", json_content_result(content(url2)), 403)
        expect("archive-course", call("/learning/courses/%d/status" % c, "PUT", {"status": "ARCHIVED"}, admin))
        expect("archive-blocks-content-mutation", call("/learning/chapters/%d" % ch, "PUT", chapter_body, mentor2), 400)
        # Data permissions are checked before any SQL mutation; no partial record appeared.
        assert auth.sql("SELECT COUNT(*) FROM learning_chapter WHERE course_id=%d;" % c) == "2"
        print("ALL LEARNING CHECKS PASSED:", len(CHECKS), flush=True)
    finally:
        app.terminate()
        try:
            app.wait(timeout=20)
        except subprocess.TimeoutExpired:
            app.kill();app.wait()
        log.close()
        if COURSES:
            ids = ",".join(map(str, COURSES))
            auth.sql("UPDATE learning_resource r JOIN learning_chapter c ON r.chapter_id=c.id SET r.caption_resource_id=NULL WHERE c.course_id IN (%s);" % ids)
            auth.sql("DELETE r FROM learning_resource r JOIN learning_chapter c ON r.chapter_id=c.id WHERE c.course_id IN (%s);" % ids)
            auth.sql("DELETE FROM learning_post WHERE course_id IN (%s); DELETE FROM learning_chapter WHERE course_id IN (%s); DELETE FROM learning_enrollment WHERE course_id IN (%s); DELETE FROM learning_course WHERE id IN (%s);" % (ids, ids, ids, ids))
        if USERS:
            auth.sql("DELETE FROM sys_user WHERE id IN (%s) AND user_name LIKE '%s%%';" % (",".join(map(str, USERS)), PREFIX))
        for token in TOKENS:
            auth.redis("DEL", auth.key_for(token))
        for ticket in TICKETS:
            auth.redis("DEL", "upself:media:" + ticket)
        auth.clear_our_rate_limits()
        if scratch.resolve().is_relative_to((ROOT / ".local").resolve()):
            shutil.rmtree(scratch)


if __name__ == "__main__":
    main()
