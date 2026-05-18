package com.regionalai.floatingball.server.modules.device.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class RegisterDeviceResponse {

    private String idDevice;
    private String deviceToken;
    private Integer heartbeatInterval;
    private Boolean hasPublicKey;

    public RegisterDeviceResponse(String idDevice, String deviceToken, Integer heartbeatInterval) {
        this.idDevice = idDevice;
        this.deviceToken = deviceToken;
        this.heartbeatInterval = heartbeatInterval;
        this.hasPublicKey = false;
    }

    public RegisterDeviceResponse(String idDevice, String deviceToken, Integer heartbeatInterval, Boolean hasPublicKey) {
        this.idDevice = idDevice;
        this.deviceToken = deviceToken;
        this.heartbeatInterval = heartbeatInterval;
        this.hasPublicKey = hasPublicKey;
    }
}
