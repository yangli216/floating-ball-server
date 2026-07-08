package com.regionalai.floatingball.server.modules.release.dto;

import lombok.Data;

@Data
public class ReleasePlatformView {

    private String target;
    private String fileName;
    private Long fileSize;
    private String downloadUrl;
}
