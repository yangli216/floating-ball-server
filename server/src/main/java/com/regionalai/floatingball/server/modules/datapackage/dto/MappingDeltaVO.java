package com.regionalai.floatingball.server.modules.datapackage.dto;

import lombok.Data;

@Data
public class MappingDeltaVO {

    private String version;
    private String diagnoses;
    private String medicines;
    private String items;
    private String tcmDiagnoses;
    private String tcmSyndromes;
    private String tcmTreatments;
}
