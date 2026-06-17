package com.regionalai.floatingball.server.common.util;

import java.security.SecureRandom;
import java.util.concurrent.atomic.AtomicInteger;

public final class ObjectIdUtils {

    private static final char[] HEX = "0123456789abcdef".toCharArray();
    private static final byte[] PROCESS_RANDOM = new byte[5];
    private static final AtomicInteger COUNTER;

    static {
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(PROCESS_RANDOM);
        COUNTER = new AtomicInteger(secureRandom.nextInt());
    }

    private ObjectIdUtils() {
    }

    public static String next() {
        byte[] bytes = new byte[12];
        int timestamp = (int) (System.currentTimeMillis() / 1000L);
        bytes[0] = (byte) (timestamp >>> 24);
        bytes[1] = (byte) (timestamp >>> 16);
        bytes[2] = (byte) (timestamp >>> 8);
        bytes[3] = (byte) timestamp;
        System.arraycopy(PROCESS_RANDOM, 0, bytes, 4, PROCESS_RANDOM.length);
        int counter = COUNTER.getAndIncrement() & 0x00FFFFFF;
        bytes[9] = (byte) (counter >>> 16);
        bytes[10] = (byte) (counter >>> 8);
        bytes[11] = (byte) counter;

        char[] chars = new char[24];
        for (int i = 0; i < bytes.length; i++) {
            int value = bytes[i] & 0xFF;
            chars[i * 2] = HEX[value >>> 4];
            chars[i * 2 + 1] = HEX[value & 0x0F];
        }
        return new String(chars);
    }
}
