package com.regionalai.floatingball.server.modules.recommendationpreference.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class RecommendationPreferenceBatchRequest {

    private List<RecommendationPreferenceEventRequest> events;

    @Data
    public static class RecommendationPreferenceEventRequest {
        private String eventId;
        private String idempotencyKey;
        private String recommendationType;
        private String action;
        private String itemKey;
        private String itemId;
        private String itemCode;
        private String itemName;
        private Boolean selected;
        private Boolean primary;
        private String traceId;
        private String consultationId;
        private String sessionId;
        private String sourceModule;
        private String scene;
        private String doctorId;
        private String doctorName;
        private String deptId;
        private String deptName;
        private String promptVersion;
        private String templateVersion;
        private String modelVersion;
        private Map<String, Object> payload;
        private Long timestamp;
    }
}
