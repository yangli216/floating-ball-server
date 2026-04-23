package com.regionalai.floatingball.server.modules.auth.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class AdminLoginRequest {

    @NotBlank(message = "管理员账号不能为空")
    private String username;

    @NotBlank(message = "管理员密码不能为空")
    private String password;
}
