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
        private String kind;
        private String severity;
        private List<String> tags;
        private Boolean hasCorrection;
        private Boolean hasTrace;
        private String traceId;
        private String sessionId;

        /** 反馈目标摘要（如"推荐诊断：高血压"、"病例字段：主诉"），由 chainContext 解析得出 */
        private String targetSummary;
        /** 反馈目标类型 */
        private String targetType;

        private String idDoctor;
        private String naDoctor;
        private String idDept;
        private String naDept;
        private String idOrg;
        private String naOrg;

        private String idDevice;
        private String screenshotFileName;
        private String screenshotMimeType;
        private String screenshotDataUrl;
        private Map<String, Object> chainContext;
        private LocalDateTime createdAt;
    }
}
