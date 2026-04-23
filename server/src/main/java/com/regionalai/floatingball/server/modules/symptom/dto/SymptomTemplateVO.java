package com.regionalai.floatingball.server.modules.symptom.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Data
public class SymptomTemplateVO {

    private String id;

    private String medicalMode;

    private String key;

    private String name;

    private String description;

    @JsonProperty("isCommonSymptom")
    private Boolean commonSymptom;

    private List<String> systemCategory = Collections.emptyList();

    private List<String> bodyParts = Collections.emptyList();

    private String customScript;

    private Map<String, Object> config = Collections.emptyMap();

    private Map<String, Object> applicablePopulation = Collections.emptyMap();

    private Map<String, Object> tcmMetadata;

    private Integer sortOrder;

    private String sdStatus;

    private String idRegion;

    private String idOrg;

    private Long createdAt;

    private Long updatedAt;
}
