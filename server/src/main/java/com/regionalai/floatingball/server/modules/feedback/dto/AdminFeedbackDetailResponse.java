package com.regionalai.floatingball.server.modules.feedback.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class AdminFeedbackDetailResponse {

    private FeedbackDetail feedback;
    private List<FeedbackTimelineItem> timeline;

    @Data
    public static class FeedbackDetail {
        private String feedbackId;
        private Integer score;
        private String comment;
        private String sourceModule;
        private String traceId;
        private String sessionId;
        private String idDevice;
        private String idOrg;
        private String screenshotFileName;
        private String screenshotMimeType;
        private String screenshotDataUrl;
        private Map<String, Object> chainContext;
        private LocalDateTime createdAt;
    }
}
