package com.regionalai.floatingball.server.modules.patientmemory.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class PatientMemorySyncRequest {

    private String schemaVersion;
    private String syncId;
    private Long knownMemoryVersion;
    private PatientIdentity patient;
    private List<Observation> observations = new ArrayList<Observation>();

    @Data
    public static class PatientIdentity {
        private String patientId;
        private String hisOrgId;
        private String name;
        private String gender;
        private String ageText;
        private String birthDate;
    }

    @Data
    public static class Observation {
        private String sourceKey;
        private String sourceType;
        private String sourceVersion;
        private String operation;
        private Long occurredAt;
        private String visitId;
        private Map<String, Object> payload = new LinkedHashMap<String, Object>();
        private List<ClinicalFact> facts = new ArrayList<ClinicalFact>();
    }

    @Data
    public static class ClinicalFact {
        private String factKey;
        private String factType;
        private String code;
        private String name;
        private String valueText;
        private String status;
        private String confidence;
        private String evidenceText;
    }
}
