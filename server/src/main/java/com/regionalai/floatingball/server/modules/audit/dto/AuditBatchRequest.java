package com.regionalai.floatingball.server.modules.audit.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class AuditBatchRequest {

    private List<AuditEvent> events;

    @Data
    public static class AuditEvent {
        private String eventId;
        private String eventType;
        private String hisOrgId;
        private String hisOrgName;
        private Map<String, Object> payload;
        private Long timestamp;
    }
}
