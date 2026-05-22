package com.regionalai.floatingball.server.modules.symptom.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

@Data
public class SymptomTemplateChangeLogVO {

    private String idLog;

    private String idTemplate;

    private String symptomKey;

    private String symptomName;

    private String medicalMode;

    private String idOrg;

    private String idRegion;

    private String operationType;

    private String operatorId;

    private String operatorCode;

    private String operatorName;

    private String changeSummary;

    private Map<String, Object> beforeSnapshot = Collections.emptyMap();

    private Map<String, Object> afterSnapshot = Collections.emptyMap();

    private Map<String, Object> diff = Collections.emptyMap();

    private LocalDateTime operationTime;

    private Long createdAt;

    private Long updatedAt;
}
