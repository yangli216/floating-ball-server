package com.regionalai.floatingball.server.modules.device.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RegisterDeviceResponse {

    private String idDevice;
    private String deviceToken;
    private Integer heartbeatInterval;
}
