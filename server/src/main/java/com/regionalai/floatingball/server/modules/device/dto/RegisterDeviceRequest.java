package com.regionalai.floatingball.server.modules.device.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class RegisterDeviceRequest {

    @NotBlank(message = "设备编码不能为空")
    private String cdDevice;

    private String naDevice;

    @NotBlank(message = "机构编码不能为空")
    private String cdOrg;

    private String clientVersion;

    private String updateChannel;

    private String osInfo;
}
