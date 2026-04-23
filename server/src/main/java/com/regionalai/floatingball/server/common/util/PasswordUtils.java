package com.regionalai.floatingball.server.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class PasswordUtils {

    private PasswordUtils() {
    }

    public static String sha256(String value) {
        if (value == null) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte item : bytes) {
                builder.append(String.format("%02x", item));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not supported", ex);
        }
    }

    public static boolean matches(String rawPassword, String passwordHash) {
        return sha256(rawPassword).equals(passwordHash);
    }
}
