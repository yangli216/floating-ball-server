package com.regionalai.floatingball.server.modules.businessdebug.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BusinessDebugConsultationItem {

    private String idRun;
    private String consultationId;
    private String scene;
    private String sceneName;
    private String patientName;
    private String patientGender;
    private String patientAge;
    private String doctorName;
    private String orgName;
    private String status;
    private LocalDateTime startedAt;
    private Boolean hasSpeechText;
}
