package com.regionalai.floatingball.server.modules.prompt.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PromptView {

    private String idPrompt;
    private String cdPrompt;
    private String naPrompt;
    private String sysPrompt;
    private String userTemplate;
    private String versionNum;
    private String sdPromptType;
    private String sdStatus;
    private String idOrg;
    private String idRegion;
    private String source;
    private Boolean builtIn;
    private LocalDateTime insertTime;
    private LocalDateTime updateTime;
}
