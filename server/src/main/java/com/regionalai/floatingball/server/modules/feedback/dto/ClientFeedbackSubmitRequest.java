package com.regionalai.floatingball.server.modules.feedback.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import java.util.Map;

@Data
public class ClientFeedbackSubmitRequest {

    private String sessionId;
    private String traceId;
    private String sourceModule;

    @Min(value = 1, message = "评分不能低于 1")
    @Max(value = 5, message = "评分不能高于 5")
    private Integer score;

    @NotBlank(message = "反馈说明不能为空")
    private String comment;

    @Valid
    private ScreenshotPayload screenshot;

    private Map<String, Object> chainContext;

    @Data
    public static class ScreenshotPayload {
        private String fileName;
        private String mimeType;
        private String dataUrl;
    }
}
