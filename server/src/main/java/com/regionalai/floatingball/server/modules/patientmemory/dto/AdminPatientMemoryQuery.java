package com.regionalai.floatingball.server.modules.patientmemory.dto;

import lombok.Data;

@Data
public class AdminPatientMemoryQuery {
    private long current = 1;
    private long size = 20;
    private String keyword;
    private String idOrg;
    private String qualityStatus;
}
