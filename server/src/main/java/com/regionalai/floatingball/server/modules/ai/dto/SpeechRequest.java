package com.regionalai.floatingball.server.modules.ai.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class SpeechRequest {

    @NotBlank(message = "音频内容不能为空")
    private String audio;
    private String traceId;
    private String sourceModule;
    private String sessionId;
    private String mimeType;
    private String format;
    private String fileName;
    private String scene;
}
