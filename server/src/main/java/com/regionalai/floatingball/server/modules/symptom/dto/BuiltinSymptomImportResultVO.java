package com.regionalai.floatingball.server.modules.symptom.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BuiltinSymptomImportResultVO {

    private String medicalMode;

    private int createdCount;

    private int updatedCount;
}
