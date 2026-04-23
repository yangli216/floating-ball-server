package com.regionalai.floatingball.server.modules.auth.dto;

import lombok.Data;

@Data
public class AdminLoginResponse {

    private String token;
    private Long expiresAt;
    private AdminCurrentUser user;
}
