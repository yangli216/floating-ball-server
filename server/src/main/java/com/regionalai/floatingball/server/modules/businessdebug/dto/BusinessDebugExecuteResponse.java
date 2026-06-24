package com.regionalai.floatingball.server.modules.businessdebug.dto;

import lombok.Data;

@Data
public class BusinessDebugExecuteResponse {

    private String nodeCode;
    private String traceId;
    private String content;
    private Object parsedJson;
    private Long durationMs;
}
