package com.regionalai.floatingball.server.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@ConfigurationProperties(prefix = "floating-ball.cors")
public class CorsProperties {

    private static final List<String> DEFAULT_ALLOWED_ORIGINS = Arrays.asList(
        "http://localhost:*",
        "http://127.0.0.1:*",
        "https://tauri.localhost",
        "http://tauri.localhost",
        "tauri://localhost",
        "asset://localhost"
    );

    private List<String> allowedOrigins = new ArrayList<String>(DEFAULT_ALLOWED_ORIGINS);

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        Set<String> merged = new LinkedHashSet<String>(DEFAULT_ALLOWED_ORIGINS);
        if (allowedOrigins != null) {
            for (String origin : allowedOrigins) {
                if (origin == null) {
                    continue;
                }
                String text = origin.trim();
                if (!text.isEmpty()) {
                    merged.add(text);
                }
            }
        }
        this.allowedOrigins = new ArrayList<String>(merged);
    }
}
