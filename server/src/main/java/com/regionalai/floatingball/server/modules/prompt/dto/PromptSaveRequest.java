package com.regionalai.floatingball.server.modules.prompt.dto;

import lombok.Data;

@Data
public class PromptSaveRequest {

    private String cdPrompt;
    private String naPrompt;
    private String sysPrompt;
    private String userTemplate;
    private String versionNum;
    private String sdPromptType;
    private String sdStatus;
    private String idOrg;
    private String idRegion;
}
