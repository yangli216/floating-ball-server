package com.regionalai.floatingball.server.modules.businessdebug.dto;

import lombok.Data;

import java.util.List;

@Data
public class BusinessDebugNodeVO {

    private String nodeCode;
    private String title;
    private String description;
    private String promptCode;
    private String promptName;
    private String promptSource;
    private String versionNum;
    private String defaultConfigProfile;
    private Double defaultTemperature;
    private String systemPrompt;
    private String userPrompt;
    private List<String> inputPresets;
}
