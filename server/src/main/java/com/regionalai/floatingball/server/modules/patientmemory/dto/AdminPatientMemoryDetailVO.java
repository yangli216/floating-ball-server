package com.regionalai.floatingball.server.modules.patientmemory.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class AdminPatientMemoryDetailVO {
    private AdminPatientMemoryListItem memory;
    private List<FactVO> facts = new ArrayList<FactVO>();
    private List<ObservationVO> observations = new ArrayList<ObservationVO>();
    private List<AuditVO> audits = new ArrayList<AuditVO>();

    @Data
    public static class FactVO {
        private String factId;
        private String factKey;
        private String factType;
        private String code;
        private String name;
        private String valueText;
        private String status;
        private String confidence;
        private String evidenceText;
        private String sourceType;
        private String sourceKey;
        private String origin;
        private boolean suppressed;
        private Integer revisionNo;
        private LocalDateTime firstObservedTime;
        private LocalDateTime lastObservedTime;
    }

    @Data
    public static class ObservationVO {
        private String observationId;
        private String sourceKey;
        private String sourceType;
        private String sourceVersion;
        private String operation;
        private String visitId;
        private String payloadHash;
        private Integer factCount;
        private boolean latest;
        private LocalDateTime occurredTime;
        private LocalDateTime receivedTime;
    }

    @Data
    public static class AuditVO {
        private String auditId;
        private String factId;
        private String action;
        private String note;
        private String operatorId;
        private String operatorName;
        private LocalDateTime operationTime;
    }
}
