package com.upself.web;

import static org.junit.jupiter.api.Assertions.*;

import cn.hutool.core.codec.Base32;
import cn.hutool.crypto.digest.otp.HOTP;

import com.upself.common.utils.PasswordUtils;
import com.upself.framework.web.service.TotpService;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class AuthAlgorithmTest {
    @Test
    void loginUserRedisJsonRoundTripPreservesIdentityWithoutSerializingAuthorities() {
        com.upself.common.core.domain.model.LoginUser user =
                new com.upself.common.core.domain.model.LoginUser();
        user.setUserId(10L);
        user.setUsername("test-user");
        user.setChannel("student");
        user.getPermissions().add("example:resource:query");
        String json = com.alibaba.fastjson2.JSON.toJSONString(user);
        assertFalse(json.contains("authorities"));
        com.upself.common.core.domain.model.LoginUser restored =
                com.alibaba.fastjson2.JSON.parseObject(
                        json, com.upself.common.core.domain.model.LoginUser.class);
        assertEquals(user.getUserId(), restored.getUserId());
        assertEquals(user.getUsername(), restored.getUsername());
        assertEquals("student", restored.getChannel());
        assertEquals(user.getPermissions(), restored.getPermissions());
        assertEquals("student", restored.getAuthorities().iterator().next().getAuthority());
    }

    private TotpService service() throws Exception {
        TotpService service = new TotpService();
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(
                service, "encryptKey", "unit-test-only-independent-key-0123456789");
        ReflectionTestUtils.setField(service, "issuer", "UP-SELF");
        service.init();
        return service;
    }

    @Test
    void passwordFlowUsesOneMd5PreprocessingBeforeBcrypt() {
        String password = "Test-Password-2026";
        String stored = PasswordUtils.encode(password);
        assertTrue(PasswordUtils.matches(password, stored));
        assertFalse(PasswordUtils.matches(PasswordUtils.md5(password), stored));
        assertFalse(PasswordUtils.matches("wrong-password", stored));
        assertNotEquals(stored, PasswordUtils.encode(password));
    }

    @Test
    void totpAcceptsCurrentStepAndRejectsMalformedCodes() throws Exception {
        TotpService service = service();
        String secret = service.generateSecret();
        long step = System.currentTimeMillis() / 30_000;
        String code = String.format("%06d", new HOTP(6, Base32.decode(secret)).generate(step));
        assertEquals(step, service.matchTimeStep(secret, code));
        assertEquals(-1, service.matchTimeStep(secret, "123"));
        assertEquals(-1, service.matchTimeStep(secret, "abcdef"));
        assertTrue(
                service.buildOtpAuthUrl(secret, "test@example.com").startsWith("otpauth://totp/"));
    }

    @Test
    void encryptedSecretsRoundTripWithRandomIvAndRejectTampering() throws Exception {
        TotpService service = service();
        String secret = service.generateSecret();
        String stored = service.encrypt(secret);
        assertEquals(secret, service.decrypt(stored));
        assertNotEquals(stored, service.encrypt(secret));
        byte[] bytes = java.util.Base64.getDecoder().decode(stored);
        bytes[bytes.length - 1] ^= 1;
        assertThrows(
                IllegalStateException.class,
                () -> service.decrypt(java.util.Base64.getEncoder().encodeToString(bytes)));
    }
}
