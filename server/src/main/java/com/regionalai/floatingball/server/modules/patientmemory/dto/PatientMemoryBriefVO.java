package com.regionalai.floatingball.server.modules.patientmemory.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class PatientMemoryBriefVO {

    private String memoryId;
    private Long memoryVersion;
    private String patientId;
    private String patientName;
    private String patientGender;
    private String patientAge;
    private String qualityStatus;
    private Integer conflictCount;
    private LocalDateTime lastSyncTime;
    private LocalDateTime lastSourceTime;
    private List<MemoryFactItem> allergies = new ArrayList<MemoryFactItem>();
    private List<MemoryFactItem> chronicConditions = new ArrayList<MemoryFactItem>();
    private List<MemoryFactItem> recentDiagnoses = new ArrayList<MemoryFactItem>();
    private List<MemoryFactItem> recentMedications = new ArrayList<MemoryFactItem>();
    private List<MemoryFactItem> otherFacts = new ArrayList<MemoryFactItem>();

    @Data
    public static class MemoryFactItem {
        private String factId;
        private String factType;
        private String code;
        private String name;
        private String valueText;
        private String status;
        private String confidence;
        private String evidenceText;
        private String sourceType;
        private String origin;
        private LocalDateTime lastObservedAt;
    }
}
