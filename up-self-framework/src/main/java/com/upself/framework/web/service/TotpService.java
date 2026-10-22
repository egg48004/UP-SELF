package com.upself.framework.web.service;

import cn.hutool.core.codec.Base32;
import cn.hutool.crypto.digest.otp.HOTP;

import lombok.Getter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.annotation.PostConstruct;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/** 谷歌身份验证器（TOTP，RFC 6238：HmacSHA1、6 位、30 秒一个时间片） 只管算法和密钥加解密，登录流程见 SysLoginService */
@Component
public class TotpService {

    private static final int PERIOD_SECONDS = 30;
    private static final int DIGITS = 6;

    /** 允许前后各 1 个时间片的时钟误差 */
    private static final int WINDOW = 1;

    private static final int SECRET_BYTES = 20;
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_BITS = 128;

    private static final SecureRandom RANDOM = new SecureRandom();

    /** 总开关：false 时退回只用密码登录，给上线出问题时应急用 */
    @Getter
    @Value("${custom.totp.enabled:true}")
    private boolean enabled;

    @Value("${custom.totp.encryptKey:}")
    private String encryptKey;

    /** 显示在用户手机验证器 App 里的名称；只影响之后新绑定的账号，已绑定的 App 里不会变 */
    @Value("${custom.totp.issuer:UP-SELF}")
    private String issuer;

    private SecretKeySpec aesKey;

    /** 开着却没配 key 直接启动失败，宁可发版时发现，也不能悄悄关掉两步验证或用默认 key */
    @PostConstruct
    public void init() throws Exception {
        if (!enabled) {
            return;
        }
        if (!StringUtils.hasText(encryptKey) || encryptKey.length() < 32) {
            throw new IllegalStateException(
                    "When TOTP is enabled, custom.totp.encryptKey must contain at least 32 random"
                        + " characters.");
        }
        // 冒号是 otpauth 里 issuer 和用户名的分隔符，带了会被 App 拆错
        if (!StringUtils.hasText(issuer) || issuer.contains(":")) {
            throw new IllegalStateException(
                    "The TOTP issuer must not be empty or contain a colon: " + issuer);
        }
        byte[] key =
                MessageDigest.getInstance("SHA-256")
                        .digest(encryptKey.getBytes(StandardCharsets.UTF_8));
        aesKey = new SecretKeySpec(key, "AES");
    }

    /** 新密钥，Base32 明文（32 位），用户扫码或手动输入的就是它 */
    public String generateSecret() {
        byte[] bytes = new byte[SECRET_BYTES];
        RANDOM.nextBytes(bytes);
        return Base32.encode(bytes);
    }

    /** 绑定二维码内容，验证器 App 扫的就是这个串 */
    public String buildOtpAuthUrl(String secret, String userName) {
        try {
            String label = URLEncoder.encode(issuer + ":" + userName, "UTF-8").replace("+", "%20");
            String encodedIssuer = URLEncoder.encode(issuer, "UTF-8").replace("+", "%20");
            return "otpauth://totp/"
                    + label
                    + "?secret="
                    + secret
                    + "&issuer="
                    + encodedIssuer
                    + "&algorithm=SHA1&digits="
                    + DIGITS
                    + "&period="
                    + PERIOD_SECONDS;
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    /** 校验动态码，返回命中的时间片序号（调用方用它防重放）；不匹配返回 -1 */
    public long matchTimeStep(String secret, String code) {
        if (!StringUtils.hasText(secret) || code == null || !code.matches("\\d{" + DIGITS + "}")) {
            return -1;
        }
        int expected = Integer.parseInt(code);
        HOTP hotp = new HOTP(DIGITS, Base32.decode(secret));
        long current = System.currentTimeMillis() / 1000 / PERIOD_SECONDS;
        for (long step = current - WINDOW; step <= current + WINDOW; step++) {
            if (hotp.generate(step) == expected) {
                return step;
            }
        }
        return -1;
    }

    /** 加密后入库：Base64(IV + 密文 + GCM tag)，每次 IV 随机 */
    public String encrypt(String secret) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(secret.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder()
                    .encodeToString(
                            ByteBuffer.allocate(iv.length + encrypted.length)
                                    .put(iv)
                                    .put(encrypted)
                                    .array());
        } catch (Exception e) {
            throw new IllegalStateException("Could not encrypt the TOTP secret.", e);
        }
    }

    /** 解不开（库里数据被改、或换了 encryptKey）直接抛异常，不能当成「未绑定」放过去 */
    public String decrypt(String stored) {
        try {
            byte[] data = Base64.getDecoder().decode(stored);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    aesKey,
                    new GCMParameterSpec(GCM_TAG_BITS, data, 0, GCM_IV_LENGTH));
            return new String(
                    cipher.doFinal(data, GCM_IV_LENGTH, data.length - GCM_IV_LENGTH),
                    StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Could not decrypt the TOTP secret. Check the encryption key.", e);
        }
    }
}
