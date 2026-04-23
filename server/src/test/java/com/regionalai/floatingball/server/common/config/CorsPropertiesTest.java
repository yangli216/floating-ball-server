package com.regionalai.floatingball.server.common.config;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CorsPropertiesTest {

    @Test
    void shouldKeepDesktopOriginsWhenCustomOriginsConfigured() {
        CorsProperties properties = new CorsProperties();

        properties.setAllowedOrigins(Arrays.asList(
            "http://localhost:5174",
            "http://127.0.0.1:5174"
        ));

        List<String> origins = properties.getAllowedOrigins();
        assertTrue(origins.contains("tauri://localhost"));
        assertTrue(origins.contains("asset://localhost"));
        assertTrue(origins.contains("https://tauri.localhost"));
        assertTrue(origins.contains("http://localhost:5174"));
        assertTrue(origins.contains("http://127.0.0.1:5174"));
    }
}
