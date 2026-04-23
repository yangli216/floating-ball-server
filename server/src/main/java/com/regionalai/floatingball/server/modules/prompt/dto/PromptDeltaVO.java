package com.regionalai.floatingball.server.modules.prompt.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class PromptDeltaVO {

    private String version;
    private List<RemotePrompt> prompts;

    @Data
    @AllArgsConstructor
    public static class RemotePrompt {
        private String cdPrompt;
        private String sysPrompt;
        private String userTemplate;
        private String versionNum;
    }
}
