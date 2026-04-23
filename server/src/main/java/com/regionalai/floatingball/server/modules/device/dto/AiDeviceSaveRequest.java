package com.regionalai.floatingball.server.modules.device.dto;

import lombok.Data;

@Data
public class AiDeviceSaveRequest {

    private String cdDevice;
    private String naDevice;
    private String idOrg;
    private String clientVersion;
    private String osInfo;
    private String sdStatus;
}
