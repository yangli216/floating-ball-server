package com.regionalai.floatingball.server.modules.release.dto;

import lombok.Data;

@Data
public class ReleasePolicyView {

    private String channel;
    private String latestVersion;
    private Boolean forceUpdate;
    private String minSupportedVersion;
    private String latestJsonUrl;
    private String notes;
    private String pubDate;
    private Long updatedAt;
}
