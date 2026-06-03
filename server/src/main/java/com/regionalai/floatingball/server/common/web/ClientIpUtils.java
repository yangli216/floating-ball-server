package com.regionalai.floatingball.server.common.web;

import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;

public final class ClientIpUtils {

    private ClientIpUtils() {
    }

    public static String resolve(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String ip = firstIp(request.getHeader("X-Forwarded-For"));
        if (StringUtils.hasText(ip)) {
            return ip;
        }
        ip = firstIp(request.getHeader("X-Real-IP"));
        if (StringUtils.hasText(ip)) {
            return ip;
        }
        ip = request.getRemoteAddr();
        return StringUtils.hasText(ip) ? ip : "unknown";
    }

    private static String firstIp(String headerValue) {
        if (!StringUtils.hasText(headerValue)) {
            return null;
        }
        String[] parts = headerValue.split(",");
        for (String part : parts) {
            String ip = part == null ? null : part.trim();
            if (StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) {
                return ip;
            }
        }
        return null;
    }
}
