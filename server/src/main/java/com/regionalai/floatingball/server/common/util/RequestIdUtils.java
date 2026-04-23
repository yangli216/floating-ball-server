package com.regionalai.floatingball.server.common.util;

import javax.servlet.http.HttpServletRequest;
import java.util.UUID;

public final class RequestIdUtils {

    private RequestIdUtils() {
    }

    public static String resolve(HttpServletRequest request) {
        String requestId = request.getHeader("X-Request-Id");
        return requestId == null || requestId.trim().isEmpty() ? UUID.randomUUID().toString() : requestId;
    }
}
