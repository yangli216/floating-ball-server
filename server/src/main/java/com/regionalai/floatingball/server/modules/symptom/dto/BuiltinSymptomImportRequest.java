package com.regionalai.floatingball.server.modules.symptom.dto;

import lombok.Data;

@Data
public class BuiltinSymptomImportRequest {

    private String medicalMode;

    private String idRegion;

    private String idOrg;

    private Boolean overwriteExisting;
}
