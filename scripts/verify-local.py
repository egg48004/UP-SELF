"""Local-only HTTP/database/Redis verification. Never targets the reference project.

Run with the application stopped: this script temporarily starts its packaged JAR,
tests disposable accounts, then stops it and removes test accounts/keys. Requires
Python 3 (standard library only), the installed Java 8, MySQL and Redis.
"""
import base64
import concurrent.futures
import hashlib
import hmac
import json
import os
from pathlib import Path
import secrets
import socket
import struct
import subprocess
import time
import urllib.error
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
ENV = Path(r"C:\Users\eggma\Documents\Codex\local-env\up-self")
CREDS = json.loads((ENV / "secrets/local-credentials.json").read_text())
BASE = "http://127.0.0.1:8080"
CHECKS = []
PREFIX = "verify_" + secrets.token_hex(4)
PASSWORD = secrets.token_urlsafe(16)
TOKENS = []


def sql(statement):
    process = subprocess.run(
        [str(ENV / "mysql/bin/mysql.exe"),
         "--defaults-extra-file=" + str(ENV / "secrets/mysql-migration-client.cnf"),
         "-N", "up_self_dev"], input=statement, capture_output=True, text=True)
    if process.returncode:
        raise RuntimeError("Local SQL verification failed; credentials were not logged")
    return process.stdout.strip()


def redis(*parts):
    with socket.create_connection(("127.0.0.1", 6379), 3) as connection:
        stream = connection.makefile("rb")

        def command(items):
            items = [str(x).encode() for x in items]
            connection.sendall(b"*" + str(len(items)).encode() + b"\r\n" + b"".join(
                b"$" + str(len(x)).encode() + b"\r\n" + x + b"\r\n" for x in items))

            def read():
                line = stream.readline()
                kind, value = line[:1], line[1:-2]
                if kind == b"$":
                    length = int(value)
                    return stream.read(length + 2)[:-2].decode() if length >= 0 else None
                if kind == b"*":
                    return [read() for _ in range(int(value))]
                if kind == b"-":
                    raise RuntimeError("Local Redis command failed")
                return value.decode()

            return read()

        command(["AUTH", CREDS["redis_password"]])
        command(["SELECT", 2])
        return command(parts)


def request(path, method="GET", data=None, token=None):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    req = urllib.request.Request(BASE + path, headers=headers, method=method,
                                 data=None if data is None else json.dumps(data).encode())
    try:
        response = urllib.request.urlopen(req, timeout=15)
    except urllib.error.HTTPError as error:
        response = error
    with response:
        return response.status, json.load(response)


def expect(name, result, code=200):
    assert result[1].get("code") == code, name + ": unexpected response code " + str(result[1].get("code"))
    CHECKS.append(name)
    print("PASS", name, flush=True)
    return result[1]


def login(name, role, password=PASSWORD):
    path = "/client/login" if role == "student" else "/empLogin"
    return request(path, "POST", {"username": name, "password": password})


def otp(secret, step=None):
    if step is None:
        step = int(time.time()) // 30
    digest = hmac.new(base64.b32decode(secret), struct.pack(">Q", step), hashlib.sha1).digest()
    offset = digest[-1] & 15
    return str((struct.unpack(">I", digest[offset:offset + 4])[0] & 0x7fffffff) % 1000000).zfill(6)


def clear_our_rate_limits():
    # Only this application's three local login limiter keys; never FLUSHDB/FLUSHALL.
    for path, method in (("/empLogin", "login"), ("/empLogin/totp", "totp"), ("/client/login", "studentLogin")):
        redis("DEL", "upself:rate_limit:" + path + ":127.0.0.1-com.upself.web.controller.system.SysLoginController-" + method)


def key_for(token):
    payload = token.split(".")[1]
    data = json.loads(base64.urlsafe_b64decode(payload + "=" * (-len(payload) % 4)))
    return "upself:login_tokens:" + data["channel"] + ":" + data["login_user_key"]


def main():
    with socket.socket() as test:
        if test.connect_ex(("127.0.0.1", 8080)) == 0:
            raise RuntimeError("Stop the running application first; this check owns its process")
    logs = ROOT / "logs"
    logs.mkdir(exist_ok=True)
    output = open(logs / "verification-launch.log", "w", encoding="utf-8")
    app = subprocess.Popen([
        r"C:\Program Files\Java\jdk1.8.0_231\bin\java.exe", "-Dfile.encoding=UTF-8",
        "-jar", str(ROOT / "up-self-admin/target/up-self-admin.jar"),
        "--spring.config.additional-location=file:./config/"], cwd=ROOT,
        stdout=output, stderr=subprocess.STDOUT, creationflags=subprocess.CREATE_NO_WINDOW)
    ids = []
    try:
        for _ in range(90):
            if app.poll() is not None:
                raise RuntimeError("Application startup failed; see logs/verification-launch.log")
            try:
                request("/getInfo")
                break
            except (OSError, ValueError):
                time.sleep(1)
        else:
            raise RuntimeError("Application did not become ready")
        clear_our_rate_limits()
        expect("unauthenticated-admin", request("/getInfo"), 401)
        expect("unauthenticated-student", request("/client/getInfo"), 401)
        expect("invalid-token", request("/getInfo", token="not-a-token"), 401)
        assert sql("SELECT COUNT(*) FROM flyway_schema_history WHERE success=1 AND type='SQL' AND version IN ('1','2');") == "2"
        CHECKS.append("flyway-auth-migrations")
        # Local fixture hashes are computed by Java's exact BCrypt(MD5(raw)) flow.
        source = ROOT / ".local/FixturePassword.java"
        source.write_text('import com.upself.common.utils.PasswordUtils; public class FixturePassword {public static void main(String[] args)throws Exception{System.out.print(PasswordUtils.encode(new java.io.BufferedReader(new java.io.InputStreamReader(System.in)).readLine()));}}')
        jar = ROOT / "up-self-admin/target/up-self-admin.jar"
        # Extract only our local executable JAR into verification scratch, no reference code/data.
        import zipfile
        scratch = ROOT / ".local/verification-classpath"
        scratch.mkdir(exist_ok=True)
        with zipfile.ZipFile(jar) as archive:
            for member in archive.namelist():
                if member.startswith("BOOT-INF/lib/") and member.endswith(".jar"):
                    destination = scratch / Path(member).name
                    if not destination.exists():
                        destination.write_bytes(archive.read(member))
        classpath = str(scratch / "*") + os.pathsep + str(ROOT / ".local")
        subprocess.run([r"C:\Program Files\Java\jdk1.8.0_231\bin\javac.exe", "-cp", classpath, str(source)], check=True, capture_output=True)
        hashed = subprocess.run([r"C:\Program Files\Java\jdk1.8.0_231\bin\java.exe", "-cp", classpath, "FixturePassword"], input=PASSWORD, capture_output=True, text=True, check=True).stdout
        for suffix, role in (("s", 10), ("m", 20), ("a", 30)):
            name = PREFIX + suffix
            sql("INSERT INTO sys_user(user_name,nick_name,password,role_id) VALUES('%s','Verification','%s',%d);" % (name, hashed, role))
            ids.append(int(sql("SELECT id FROM sys_user WHERE user_name='%s';" % name)))
        student, mentor, admin = PREFIX + "s", PREFIX + "m", PREFIX + "a"
        st = expect("student-login", login(student, "student"))["token"]
        TOKENS.append(st)
        info = expect("student-own-info", request("/client/getInfo", token=st))
        assert info["roles"] == ["student"] and "password" not in info["user"] and "totpSecret" not in info["user"]
        expect("student-cannot-enter-admin", request("/system/user/list", token=st), 401)
        expect("student-wrong-login-entry", login(student, "admin"), 403)
        expect("mentor-wrong-login-entry", login(mentor, "student"), 403)
        bound = {}
        for name, label in ((mentor, "mentor"), (admin, "admin")):
            first = expect(label + "-password-login", login(name, "admin"))
            if first.get("token"):
                assert not any(key in first for key in ("ticket", "needBind", "needTotp", "secret"))
                TOKENS.append(first["token"])
                if label == "mentor":
                    mt = first["token"]
                else:
                    at = first["token"]
                expect(label + "-totp-disabled", request("/empLogin/totp", "POST", {"ticket": "old-ticket", "code": "123456"}), 403)
                continue
            assert first.get("needBind") and "token" not in first
            expect(label + "-ticket-is-not-token", request("/getInfo", token=first["ticket"]), 401)
            complete = expect(label + "-totp-bind-login", request("/empLogin/totp", "POST", {"ticket": first["ticket"], "code": otp(first["secret"])}))
            bound[name] = first["secret"]
            TOKENS.append(complete["token"])
            if label == "mentor":
                mt = complete["token"]
            else:
                at = complete["token"]
            expect(label + "-ticket-single-use", request("/empLogin/totp", "POST", {"ticket": first["ticket"], "code": otp(first["secret"])}), 1018)
        expect("mentor-no-account-admin-permission", request("/system/user/list", token=mt), 403)
        expect("admin-cannot-impersonate-student", request("/client/getInfo", token=at), 401)
        listed = expect("admin-paginated-user-list", request("/system/user/list?pageNum=1&pageSize=2", token=at))
        assert len(listed["rows"]) == 2 and listed["total"] >= 6
        assert all("password" not in u and "totpSecret" not in u for u in listed["rows"])
        expect("no-super-role-option", request("/system/user/roles", token=at))
        expect("reject-super-role-create", request("/system/user", "POST", {"userName": PREFIX + "bad", "nickName": "Bad", "roleId": 1, "password": PASSWORD}, at), 403)
        expect("reject-protected-maintenance-edit", request("/system/user", "PUT", {"id": 1, "nickName": "Bad", "roleId": 30, "status": "0"}, at), 403)
        expect("reject-other-admin-demotion", request("/system/user", "PUT", {"id": 4, "nickName": "Bad", "roleId": 10, "status": "0"}, at), 403)
        expect("reject-ordinary-admin-totp-reset", request("/system/user/%d/resetTotp" % ids[1], "PUT", token=at), 403)
        created = expect("admin-create-account", request("/system/user", "POST", {"userName": PREFIX + "new", "nickName": "New", "roleId": 10, "password": PASSWORD}, at))["data"]
        ids.append(created)
        expect("reject-duplicate-account", request("/system/user", "POST", {"userName": PREFIX + "new", "nickName": "New", "roleId": 10, "password": PASSWORD}, at), 400)
        clear_our_rate_limits()
        if bound:
            first = expect("bound-mentor-needs-code", login(mentor, "admin"))
            assert first.get("needTotp") and "secret" not in first
            expect("totp-time-step-no-replay", request("/empLogin/totp", "POST", {"ticket": first["ticket"], "code": otp(bound[mentor])}), 1019)
        expect("account-disable", request("/system/user", "PUT", {"id": ids[0], "nickName": "Disabled", "roleId": 10, "status": "1"}, at))
        expect("disabled-old-token-rejected", request("/client/getInfo", token=st), 401)
        expect("disabled-login-rejected", login(student, "student"), 1010)
        expect("account-enable", request("/system/user", "PUT", {"id": ids[0], "nickName": "Student", "roleId": 10, "status": "0"}, at))
        st = expect("enabled-account-login", login(student, "student"))["token"]
        TOKENS.append(st)
        expect("reset-password", request("/system/user/resetPwd", "PUT", {"id": ids[0], "password": PASSWORD + "New"}, at))
        expect("reset-invalidates-old-token", request("/client/getInfo", token=st), 401)
        expect("old-password-rejected", login(student, "student"), 1010)
        st = expect("new-password-login", login(student, "student", PASSWORD + "New"))["token"]
        TOKENS.append(st)
        redis("EXPIRE", key_for(st), 5)
        expect("sliding-expiry-request", request("/client/getInfo", token=st))
        assert int(redis("TTL", key_for(st))) > 7100
        expect("student-logout", request("/client/logout", "POST", token=st))
        expect("logout-rejects-old-token", request("/client/getInfo", token=st), 401)
        st = expect("relogin-for-change-password", login(student, "student", PASSWORD + "New"))["token"]
        TOKENS.append(st)
        expect("self-password-change", request("/client/profile/password", "PUT", {"oldPassword": PASSWORD + "New", "newPassword": PASSWORD + "Changed"}, st))
        expect("self-password-change-invalidates-token", request("/client/getInfo", token=st), 401)
        clear_our_rate_limits()
        # Verify concurrent failures cannot bypass the reference atomic threshold.
        def wrong(_):
            return login(PREFIX + "locked", "student", "wrong-password")[1]["code"]
        with concurrent.futures.ThreadPoolExecutor(max_workers=8) as pool:
            assert all(code == 1010 for code in pool.map(wrong, range(8)))
        assert int(redis("TTL", "upself:pwd_err_cnt:" + PREFIX + "locked")) > 1700
        CHECKS.append("concurrent-login-lockout")
        clear_our_rate_limits()
        status, result = 0, {}
        for _ in range(21):
            status, result = request("/client/login", "POST", {"username": PREFIX + "rate", "password": "wrong-password"})
        assert result["code"] == 429
        CHECKS.append("ip-login-rate-limit")
        report = {"passed": len(CHECKS), "checks": CHECKS, "business_modules_implemented": False}
        (ROOT / "docs/verification-results.json").write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
        print("ALL PASSED:", len(CHECKS), flush=True)
    finally:
        # Stop only the process created by this script, release JAR for future F5/builds.
        app.terminate()
        try:
            app.wait(timeout=20)
        except subprocess.TimeoutExpired:
            app.kill()
            app.wait()
        output.close()
        if ids:
            sql("DELETE FROM sys_user WHERE id IN (%s) AND user_name LIKE '%s%%';" % (",".join(map(str, ids)), PREFIX))
        for token in TOKENS:
            redis("DEL", key_for(token))
        cursor = "0"
        while True:
            cursor, keys = redis("SCAN", cursor, "MATCH", "upself:*", "COUNT", 200)
            for key in keys:
                if PREFIX in key:
                    redis("DEL", key)
                elif key.startswith("upself:login_totp_ticket:"):
                    raw = redis("GET", key)
                    if raw and PREFIX in raw:
                        redis("DEL", key)
                elif any(key.startswith("upself:totp_used:" + str(i) + ":") for i in ids):
                    redis("DEL", key)
            if cursor == "0":
                break
        clear_our_rate_limits()


if __name__ == "__main__":
    main()
