package com.regionalai.floatingball.server.modules.patientmemory.dto;

import lombok.Data;

@Data
public class PatientMemoryResolveResponse {
    private boolean found;
    private boolean notModified;
    private Long memoryVersion;
    private PatientMemoryBriefVO brief;
}
