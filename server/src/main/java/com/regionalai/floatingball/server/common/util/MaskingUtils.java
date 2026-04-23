package com.regionalai.floatingball.server.common.util;

public final class MaskingUtils {

    private MaskingUtils() {
    }

    public static String maskSecret(String secret) {
        if (secret == null || secret.trim().isEmpty()) {
            return secret;
        }
        if (secret.length() < 8) {
            return "****";
        }
        return secret.substring(0, 4) + "****" + secret.substring(secret.length() - 4);
    }
}
