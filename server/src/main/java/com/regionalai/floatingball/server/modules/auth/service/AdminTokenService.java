package com.regionalai.floatingball.server.modules.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regionalai.floatingball.server.common.util.AesUtils;
import com.regionalai.floatingball.server.modules.auth.dto.AdminCurrentUser;
import com.regionalai.floatingball.server.modules.auth.dto.AdminLoginResponse;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

@Service
public class AdminTokenService {

    private static final Duration TOKEN_TTL = Duration.ofHours(12);

    private final AesUtils aesUtils;
    private final ObjectMapper objectMapper;

    public AdminTokenService(AesUtils aesUtils, ObjectMapper objectMapper) {
        this.aesUtils = aesUtils;
        this.objectMapper = objectMapper;
    }

    public AdminLoginResponse issue(AdminCurrentUser user) {
        long expiresAt = Instant.now().plus(TOKEN_TTL).toEpochMilli();
        TokenPayload payload = new TokenPayload();
        payload.setIdUser(user.getIdUser());
        payload.setCdUser(user.getCdUser());
        payload.setNaUser(user.getNaUser());
        payload.setIdOrg(user.getIdOrg());
        payload.setRoles(user.getRoles());
        payload.setExpiresAt(expiresAt);

        AdminLoginResponse response = new AdminLoginResponse();
        response.setToken(encrypt(payload));
        response.setExpiresAt(expiresAt);
        response.setUser(user);
        return response;
    }

    public AdminCurrentUser parse(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }
        try {
            TokenPayload payload = objectMapper.readValue(aesUtils.decrypt(token), TokenPayload.class);
            if (payload == null || payload.getExpiresAt() == null || payload.getExpiresAt() < Instant.now().toEpochMilli()) {
                return null;
            }
            AdminCurrentUser user = new AdminCurrentUser();
            user.setIdUser(payload.getIdUser());
            user.setCdUser(payload.getCdUser());
            user.setNaUser(payload.getNaUser());
            user.setIdOrg(payload.getIdOrg());
            user.setRoles(payload.getRoles() == null ? Collections.<String>emptyList() : payload.getRoles());
            return user;
        } catch (Exception ex) {
            return null;
        }
    }

    private String encrypt(TokenPayload payload) {
        try {
            return aesUtils.encrypt(objectMapper.writeValueAsString(payload));
        } catch (Exception ex) {
            throw new IllegalStateException("admin token issue failed", ex);
        }
    }

    @lombok.Data
    private static class TokenPayload {
        private String idUser;
        private String cdUser;
        private String naUser;
        private String idOrg;
        private List<String> roles;
        private Long expiresAt;
    }
}
