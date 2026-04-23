package com.regionalai.floatingball.server.common.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.filter.CorsFilter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.singletonList;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CorsConfigTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        CorsProperties corsProperties = new CorsProperties();
        corsProperties.setAllowedOrigins(singletonList("http://localhost:5174"));

        FilterRegistrationBean<CorsFilter> registrationBean = new CorsConfig()
            .corsFilterRegistration(corsProperties);

        mockMvc = MockMvcBuilders.standaloneSetup(new CorsProbeController())
            .addFilters(registrationBean.getFilter())
            .build();
    }

    @Test
    void preflightRequestShouldReturnCorsHeaders() throws Exception {
        mockMvc.perform(options("/admin/api/auth/login")
                .header("Origin", "http://localhost:5174")
                .header("Access-Control-Request-Method", "POST"))
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5174"))
            .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    void actualRequestShouldReturnCorsHeaders() throws Exception {
        mockMvc.perform(post("/admin/api/auth/login")
                .header("Origin", "http://localhost:5174")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cdUser\":\"admin\",\"password\":\"123456\"}"))
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5174"))
            .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @RestController
    static class CorsProbeController {

        @PostMapping("/admin/api/auth/login")
        public Map<String, Object> login(@RequestBody Map<String, Object> payload) {
            Map<String, Object> result = new HashMap<String, Object>();
            result.put("status", "ok");
            result.put("payload", payload == null ? Collections.emptyMap() : payload);
            return result;
        }
    }
}
