package com.upself.common.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class PasswordUtils {
    private PasswordUtils() {}

    public static String md5(String raw) {
        try {
            byte[] d =
                    MessageDigest.getInstance("MD5").digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder b = new StringBuilder();
            for (byte v : d) b.append(String.format("%02x", v & 255));
            return b.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static String encode(String raw) {
        return new BCryptPasswordEncoder().encode(md5(raw));
    }

    public static boolean matches(String raw, String stored) {
        return new BCryptPasswordEncoder().matches(md5(raw), stored);
    }
}
