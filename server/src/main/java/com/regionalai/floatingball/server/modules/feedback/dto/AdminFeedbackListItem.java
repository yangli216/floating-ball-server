package com.regionalai.floatingball.server.modules.feedback.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminFeedbackListItem {
    private String feedbackId;
    private Integer score;
    private String comment;
    private String sourceModule;
    private String traceId;
    private String sessionId;
    private Boolean hasScreenshot;
    private String idDevice;
    private String idOrg;
    private LocalDateTime createdAt;
}
