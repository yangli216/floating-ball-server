package com.regionalai.floatingball.server.modules.auth.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class AdminPasswordChangeRequest {

    @NotBlank(message = "当前密码不能为空")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    private String newPassword;

    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;
}
