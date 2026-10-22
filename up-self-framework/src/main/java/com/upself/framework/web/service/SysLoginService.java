package com.upself.framework.web.service;

import com.alibaba.fastjson2.JSON;
import com.upself.common.core.domain.entity.SysUser;
import com.upself.common.core.domain.model.LoginUser;
import com.upself.common.exception.ServiceException;
import com.upself.system.mapper.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.security.authentication.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class SysLoginService {
    @Autowired private AuthenticationManager authentication;
    @Autowired private UserDetailsServiceImpl details;
    @Autowired private TokenService tokens;
    @Autowired private TotpService totp;
    @Autowired private StringRedisTemplate redis;
    @Autowired private SysUserMapper users;
    @Autowired private SysAuditMapper audit;
    private static final DefaultRedisScript<Long> INCR =
            new DefaultRedisScript<>(
                    "local c=redis.call('incr',KEYS[1]); if c==1 then"
                            + " redis.call('expire',KEYS[1],600) end; if c==5 then"
                            + " redis.call('expire',KEYS[1],1800) end; return c",
                    Long.class);
    private static final DefaultRedisScript<Long> RELEASE =
            new DefaultRedisScript<>(
                    "if redis.call('exists',KEYS[1])==1 then return redis.call('decr',KEYS[1]) end;"
                            + " return 0",
                    Long.class);

    private String failureKey(String name) {
        return "upself:pwd_err_cnt:" + name.trim().toLowerCase(Locale.ROOT);
    }

    private long reserve(String key) {
        long n = redis.execute(INCR, Collections.singletonList(key));
        if (n > 5)
            throw new ServiceException(1010, "Login is locked. Please try again in 30 minutes.");
        return n;
    }

    private void failed(String key, long n, String name) {
        audit.insert(null, "LOGIN", name, "FAIL");
        throw new ServiceException(
                1010,
                n >= 5
                        ? "Login is locked. Please try again in 30 minutes."
                        : "The username or password is incorrect.");
    }

    private void checkRole(LoginUser u, String channel) {
        if (channel.equals("student")
                ? !u.getRoleKey().equals("student")
                : !(u.getRoleKey().equals("mentor")
                        || u.getRoleKey().equals("platform_admin")
                        || u.getRoleKey().equals("admin")))
            throw new ServiceException(403, "This account cannot use the selected login entry.");
    }

    public Map<String, Object> login(String name, String password, String channel) {
        String key = failureKey(name);
        long n = reserve(key);
        LoginUser u;
        try {
            u =
                    (LoginUser)
                            authentication
                                    .authenticate(
                                            new UsernamePasswordAuthenticationToken(
                                                    name,
                                                    com.upself.common.utils.PasswordUtils.md5(
                                                            password)))
                                    .getPrincipal();
        } catch (AuthenticationException e) {
            failed(key, n, name);
            throw e;
        }
        try {
            checkRole(u, channel);
        } catch (ServiceException e) {
            redis.execute(RELEASE, Collections.singletonList(key));
            throw e;
        }
        if (channel.equals("student") || !totp.isEnabled()) {
            redis.delete(key);
            return finish(u, channel);
        }
        redis.execute(RELEASE, Collections.singletonList(key));
        SysUser db = users.selectById(u.getUserId());
        Map<String, Object> t = new HashMap<>();
        t.put("userId", u.getUserId());
        t.put("userName", name);
        t.put("authVersion", u.getAuthVersion());
        Map<String, Object> r = new HashMap<>();
        if (db.getTotpSecret() == null) {
            String s = totp.generateSecret();
            t.put("bindSecret", totp.encrypt(s));
            r.put("needBind", true);
            r.put("secret", s);
            r.put("otpauthUrl", totp.buildOtpAuthUrl(s, name));
        } else r.put("needTotp", true);
        String ticket = UUID.randomUUID().toString();
        redis.opsForValue()
                .set(
                        "upself:login_totp_ticket:" + ticket,
                        JSON.toJSONString(t),
                        5,
                        TimeUnit.MINUTES);
        r.put("ticket", ticket);
        return r;
    }

    public Map<String, Object> loginTotp(String ticket, String code) {
        if (!totp.isEnabled()) {
            throw new ServiceException(
                    403,
                    "Two-step verification is disabled. Sign in with your username and password.");
        }
        String ticketKey = "upself:login_totp_ticket:" + ticket;
        String raw = redis.opsForValue().get(ticketKey);
        if (raw == null)
            throw new ServiceException(1018, "The login ticket has expired. Please sign in again.");
        com.alibaba.fastjson2.JSONObject t = JSON.parseObject(raw);
        String name = t.getString("userName");
        String key = failureKey(name);
        long n = reserve(key);
        SysUser db = users.selectById(t.getLong("userId"));
        String bind = t.getString("bindSecret");
        if (db == null
                || !"0".equals(db.getStatus())
                || db.getAuthVersion() != t.getLongValue("authVersion")
                || (bind != null) != (db.getTotpSecret() == null)) {
            redis.delete(ticketKey);
            redis.execute(RELEASE, Collections.singletonList(key));
            throw new ServiceException(1018, "The login ticket has expired. Please sign in again.");
        }
        LoginUser u = details.from(db);
        checkRole(u, "admin");
        long step =
                totp.matchTimeStep(totp.decrypt(bind != null ? bind : db.getTotpSecret()), code);
        boolean fresh =
                step >= 0
                        && Boolean.TRUE.equals(
                                redis.opsForValue()
                                        .setIfAbsent(
                                                "upself:totp_used:" + db.getId() + ":" + step,
                                                "1",
                                                120,
                                                TimeUnit.SECONDS));
        if (!fresh) {
            audit.insert(db.getId(), "LOGIN_TOTP", name, "FAIL");
            throw new ServiceException(
                    1019,
                    n >= 5
                            ? "Login is locked. Please try again in 30 minutes."
                            : "The verification code is invalid or has already been used.");
        }
        if (!Boolean.TRUE.equals(redis.delete(ticketKey))) {
            redis.execute(RELEASE, Collections.singletonList(key));
            throw new ServiceException(1018, "This login ticket has already been used.");
        }
        if (bind != null && users.bindTotp(db.getId(), bind) != 1)
            throw new ServiceException(
                    1018, "The authenticator binding has changed. Please sign in again.");
        redis.delete(key);
        return finish(u, "admin");
    }

    private Map<String, Object> finish(LoginUser u, String channel) {
        Map<String, Object> r = new HashMap<>();
        r.put("token", tokens.createToken(u, channel));
        audit.insert(u.getUserId(), "LOGIN", u.getUsername(), "SUCCESS");
        return r;
    }
}
