package com.regionalai.floatingball.server.modules.patientmemory.dto;

import lombok.Data;

@Data
public class PatientMemoryResolveRequest {
    private String patientId;
    private String hisOrgId;
    private Long knownMemoryVersion;
}
