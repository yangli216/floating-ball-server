package com.regionalai.floatingball.server.modules.release.dto;

import lombok.Data;

@Data
public class ReleasePolicyUpdateRequest {

    private String channel;
    private Boolean forceUpdate;
}
