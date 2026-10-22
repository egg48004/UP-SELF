package com.upself.framework.web.service;

import com.alibaba.fastjson2.JSON;
import com.upself.common.core.domain.entity.SysUser;
import com.upself.common.core.domain.model.LoginUser;
import com.upself.system.mapper.SysUserMapper;

import io.jsonwebtoken.*;

import org.springframework.beans.factory.annotation.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

@Service
public class TokenService {
    @Autowired private StringRedisTemplate redis;
    @Autowired private SysUserMapper users;
    @Autowired private UserDetailsServiceImpl details;

    @Value("${token.admin.secret}")
    private String adminSecret;

    @Value("${token.client.secret}")
    private String clientSecret;

    @Value("${token.admin.expireTime:120}")
    private int adminMinutes;

    @Value("${token.client.expireTime:120}")
    private int clientMinutes;

    @PostConstruct
    public void check() {
        if (adminSecret.length() < 32
                || clientSecret.length() < 32
                || adminSecret.equals(clientSecret))
            throw new IllegalStateException(
                    "Both login channels require independent random secrets of at least 32"
                        + " characters.");
    }

    private String key(String channel, String uuid) {
        return "upself:login_tokens:" + channel + ":" + uuid;
    }

    private String secret(String channel) {
        return channel.equals("admin") ? adminSecret : clientSecret;
    }

    public String createToken(LoginUser user, String channel) {
        user.setChannel(channel);
        user.setPassword(null);
        user.setToken(UUID.randomUUID().toString());
        refreshToken(user);
        return Jwts.builder()
                .claim("login_user_key", user.getToken())
                .claim("channel", channel)
                .signWith(SignatureAlgorithm.HS512, secret(channel))
                .compact();
    }

    public LoginUser getLoginUser(String token, String channel) {
        if (token == null || token.length() > 4096) return null;
        Claims c;
        try {
            c = Jwts.parser().setSigningKey(secret(channel)).parseClaimsJws(token).getBody();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
        if (!channel.equals(c.get("channel"))) return null;
        String id = c.get("login_user_key", String.class);
        return getLoginUserBySession(id, channel);
    }

    /** Reuse current login-state checks for narrowly scoped media tickets. No renewal here. */
    public LoginUser getLoginUserBySession(String id, String channel) {
        if (id == null || !("admin".equals(channel) || "student".equals(channel))) return null;
        String data = redis.opsForValue().get(key(channel, id));
        if (data == null) return null;
        LoginUser u = JSON.parseObject(data, LoginUser.class);
        SysUser current = users.selectById(u.getUserId());
        if (current == null
                || !"0".equals(current.getStatus())
                || current.getAuthVersion() != u.getAuthVersion()
                || !current.getRoleId().equals(u.getRoleId())) {
            redis.delete(key(channel, id));
            return null;
        }
        boolean student = "student".equals(current.getRoleKey());
        if (student != channel.equals("student")) {
            redis.delete(key(channel, id));
            return null;
        }
        u.setPermissions(details.from(current).getPermissions());
        return u;
    }

    public void verifyToken(LoginUser user) {
        refreshToken(user);
    }

    public void refreshToken(LoginUser user) {
        int minutes = user.getChannel().equals("admin") ? adminMinutes : clientMinutes;
        user.setLoginTime(System.currentTimeMillis());
        user.setExpireTime(user.getLoginTime() + minutes * 60000L);
        redis.opsForValue()
                .set(
                        key(user.getChannel(), user.getToken()),
                        JSON.toJSONString(user),
                        minutes,
                        TimeUnit.MINUTES);
    }

    public void delLoginUser(LoginUser user) {
        redis.delete(key(user.getChannel(), user.getToken()));
    }
}
