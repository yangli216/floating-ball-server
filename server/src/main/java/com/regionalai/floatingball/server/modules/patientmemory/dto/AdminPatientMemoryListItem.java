package com.regionalai.floatingball.server.modules.patientmemory.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminPatientMemoryListItem {
    private String memoryId;
    private String idOrg;
    private String idHisOrg;
    private String patientId;
    private String patientName;
    private String patientGender;
    private String patientAge;
    private Long memoryVersion;
    private String qualityStatus;
    private Integer factCount;
    private Integer allergyCount;
    private Integer diagnosisCount;
    private Integer medicationCount;
    private Integer conflictCount;
    private LocalDateTime lastSyncTime;
    private LocalDateTime lastSourceTime;
}
