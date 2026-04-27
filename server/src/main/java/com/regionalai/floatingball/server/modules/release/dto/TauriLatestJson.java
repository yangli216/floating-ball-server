package com.regionalai.floatingball.server.modules.release.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class TauriLatestJson {

    private String version;
    private String notes;

    @JsonProperty("pub_date")
    private String pubDate;

    private Map<String, PlatformInfo> platforms = new LinkedHashMap<String, PlatformInfo>();

    @Data
    public static class PlatformInfo {
        private String signature;
        private String url;
    }
}
