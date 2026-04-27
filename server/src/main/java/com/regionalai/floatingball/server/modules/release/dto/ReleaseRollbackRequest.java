package com.regionalai.floatingball.server.modules.release.dto;

import lombok.Data;

@Data
public class ReleaseRollbackRequest {

    private String channel;
    private String version;
}
