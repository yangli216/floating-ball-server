package com.regionalai.floatingball.server.common.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class AesUtils {

    private final String aesKey;

    public AesUtils(@Value("${floating-ball.aes-key}") String aesKey) {
        if (!StringUtils.hasText(aesKey)) {
            throw new IllegalStateException("floating-ball.aes-key must be provided via FB_AES_KEY");
        }
        this.aesKey = aesKey.trim();
    }

    public String encrypt(String plainText) {
        if (plainText == null || plainText.trim().isEmpty()) {
            return plainText;
        }
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            SecretKeySpec keySpec = new SecretKeySpec(normalizeKey(aesKey), "AES");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception ex) {
            throw new IllegalStateException("AES encrypt failed", ex);
        }
    }

    public String decrypt(String cipherText) {
        if (cipherText == null || cipherText.trim().isEmpty()) {
            return cipherText;
        }
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            SecretKeySpec keySpec = new SecretKeySpec(normalizeKey(aesKey), "AES");
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] decoded = Base64.getDecoder().decode(cipherText);
            return new String(cipher.doFinal(decoded), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("AES decrypt failed", ex);
        }
    }

    private byte[] normalizeKey(String rawKey) {
        byte[] source = rawKey.getBytes(StandardCharsets.UTF_8);
        byte[] target = new byte[16];
        for (int i = 0; i < target.length; i++) {
            target[i] = i < source.length ? source[i] : (byte) '0';
        }
        return target;
    }
}
