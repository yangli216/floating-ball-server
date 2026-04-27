package com.regionalai.floatingball.server.modules.release.dto;

import lombok.Data;

@Data
public class ReleaseView {

    private String channel;
    private String version;
    private String target;
    private String fileName;
    private Long fileSize;
    private String downloadUrl;
    private String latestJsonUrl;
    private String pubDate;
    private String notes;
    private Long updatedAt;
}
