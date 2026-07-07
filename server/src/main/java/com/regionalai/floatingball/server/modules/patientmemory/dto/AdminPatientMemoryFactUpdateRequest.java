package com.regionalai.floatingball.server.modules.patientmemory.dto;

import lombok.Data;

@Data
public class AdminPatientMemoryFactUpdateRequest {
    private String name;
    private String valueText;
    private String status;
    private String confidence;
    private String correctionNote;
}
