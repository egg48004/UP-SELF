package com.upself.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.upself.common.core.domain.model.LoginUser;
import com.upself.common.exception.ServiceException;
import com.upself.framework.web.service.SysLoginService;
import com.upself.framework.web.service.TokenService;
import com.upself.framework.web.service.TotpService;
import com.upself.system.mapper.SysAuditMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

class PasswordOnlyLoginTest {
    private SysLoginService service;
    private AuthenticationManager authentication;
    private TokenService tokens;
    private TotpService totp;
    private StringRedisTemplate redis;

    @BeforeEach
    void setup() {
        service = new SysLoginService();
        authentication = mock(AuthenticationManager.class);
        tokens = mock(TokenService.class);
        totp = mock(TotpService.class);
        redis = mock(StringRedisTemplate.class);
        ReflectionTestUtils.setField(service, "authentication", authentication);
        ReflectionTestUtils.setField(service, "tokens", tokens);
        ReflectionTestUtils.setField(service, "totp", totp);
        ReflectionTestUtils.setField(service, "redis", redis);
        ReflectionTestUtils.setField(service, "audit", mock(SysAuditMapper.class));
        when(redis.execute(
                        org.mockito.ArgumentMatchers
                                .<org.springframework.data.redis.core.script.RedisScript<Long>>
                                        any(),
                        anyList()))
                .thenReturn(1L);
        when(totp.isEnabled()).thenReturn(false);
    }

    private LoginUser authenticated(String role) {
        LoginUser user = new LoginUser();
        user.setUserId(2L);
        user.setUsername("test-user");
        user.setRoleKey(role);
        user.setChannel(role.equals("student") ? "student" : "admin");
        when(authentication.authenticate(any()))
                .thenReturn(
                        new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
        when(tokens.createToken(any(), anyString())).thenReturn("test-token");
        return user;
    }

    @Test
    void allThreeRolesReceiveTokenAfterPasswordWhenTotpDisabled() {
        for (String role : new String[] {"student", "mentor", "platform_admin"}) {
            authenticated(role);
            String channel = role.equals("student") ? "student" : "admin";
            Map<String, Object> result = service.login("test-user", "password", channel);
            assertEquals("test-token", result.get("token"));
            assertFalse(result.containsKey("ticket"));
            assertFalse(result.containsKey("secret"));
            assertFalse(result.containsKey("needTotp"));
        }
        verify(totp, never()).generateSecret();
        verify(redis, times(3)).delete("upself:pwd_err_cnt:test-user");
    }

    @Test
    void wrongLoginEntryStillRejected() {
        authenticated("student");
        ServiceException error =
                assertThrows(
                        ServiceException.class,
                        () -> service.login("test-user", "password", "admin"));
        assertEquals(403, error.getCode());
        verifyNoInteractions(tokens);
    }

    @Test
    void wrongPasswordNeverCreatesToken() {
        when(authentication.authenticate(any())).thenThrow(new BadCredentialsException("bad"));
        assertThrows(ServiceException.class, () -> service.login("test-user", "wrong", "admin"));
        verifyNoInteractions(tokens);
        verify(redis, never()).delete(anyString());
    }

    @Test
    void oldTotpTicketsCannotBeUsedWhileDisabled() {
        ServiceException error =
                assertThrows(
                        ServiceException.class, () -> service.loginTotp("old-ticket", "123456"));
        assertEquals(403, error.getCode());
        verifyNoInteractions(redis, tokens, authentication);
    }
}
