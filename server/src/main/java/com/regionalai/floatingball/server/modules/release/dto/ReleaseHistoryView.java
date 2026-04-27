package com.regionalai.floatingball.server.modules.release.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ReleaseHistoryView {

    private String channel;
    private String version;
    private Boolean active;
    private Boolean forceUpdate;
    private String minSupportedVersion;
    private List<String> targets = new ArrayList<String>();
    private List<String> fileNames = new ArrayList<String>();
    private String latestJsonUrl;
    private String notes;
    private String pubDate;
    private Long updatedAt;
}
