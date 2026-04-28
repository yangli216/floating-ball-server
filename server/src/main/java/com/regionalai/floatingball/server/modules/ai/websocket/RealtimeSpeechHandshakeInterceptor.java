package com.regionalai.floatingball.server.modules.ai.websocket;

import com.regionalai.floatingball.server.modules.device.entity.AiDevice;
import com.regionalai.floatingball.server.modules.device.service.DeviceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URI;
import java.net.URLDecoder;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class RealtimeSpeechHandshakeInterceptor implements HandshakeInterceptor {

    public static final String DEVICE_ATTRIBUTE = "aiDevice";

    private final DeviceService deviceService;

    public RealtimeSpeechHandshakeInterceptor(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        String token = resolveQueryParam(request.getURI(), "token");
        if (!StringUtils.hasText(token)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        AiDevice device = deviceService.findActiveByToken(token);
        if (device == null) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        attributes.put(DEVICE_ATTRIBUTE, device);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
    }

    private String resolveQueryParam(URI uri, String name) {
        String query = uri.getRawQuery();
        if (!StringUtils.hasText(query)) {
            return null;
        }
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int equalsIndex = pair.indexOf('=');
            String rawName = equalsIndex >= 0 ? pair.substring(0, equalsIndex) : pair;
            if (!name.equals(decode(rawName))) {
                continue;
            }
            String rawValue = equalsIndex >= 0 ? pair.substring(equalsIndex + 1) : "";
            return decode(rawValue);
        }
        return null;
    }

    private String decode(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException ex) {
            return value;
        }
    }
}
