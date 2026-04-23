package com.regionalai.floatingball.server.modules.device.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class HeartbeatResponse {

    private String status;
    private Long serverTime;
}
