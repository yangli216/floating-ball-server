package com.regionalai.floatingball.server.modules.release.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class ReleaseUploadRequest {

    private String channel;
    private String version;
    private String target;
    private String signature;
    private String notes;
    private String pubDate;
    private MultipartFile metadataFile;
    private MultipartFile file;
}
