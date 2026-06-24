package com.regionalai.floatingball.server.modules.businessdebug.dto;

import lombok.Data;

import java.util.Map;

@Data
public class BusinessDebugExecuteRequest {

    private String idRun;
    private String nodeCode;
    private String systemPrompt;
    private String userPrompt;
    private String configProfile;
    private Double temperature;
    private Map<String, Object> inputPayload;
}
