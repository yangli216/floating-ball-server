package com.regionalai.floatingball.server.modules.release.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Data
public class ReleaseBatchUploadRequest {

    private List<String> channels = new ArrayList<String>();
    private String channel;
    private String version;
    private String notes;
    private String pubDate;
    private Boolean forceUpdate;
    private MultipartFile metadataFile;
    private List<MultipartFile> files = new ArrayList<MultipartFile>();
    private MultipartFile file;
}
