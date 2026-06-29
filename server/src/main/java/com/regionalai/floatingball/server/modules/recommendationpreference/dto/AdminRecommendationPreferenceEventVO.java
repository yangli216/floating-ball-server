package com.regionalai.floatingball.server.modules.recommendationpreference.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminRecommendationPreferenceEventVO {
    private String idEvent;
    private String idDevice;
    private String idOrg;
    private String idRegion;
    private String scope;
    private String recommendationType;
    private String actionCode;
    private String idempotencyKey;
    private String itemKey;
    private String itemId;
    private String itemCode;
    private String itemName;
    private boolean selected;
    private boolean primary;
    private String traceId;
    private String consultationId;
    private String sessionId;
    private String sourceModule;
    private String sceneCode;
    private String idDoctor;
    private String naDoctor;
    private String idDept;
    private String naDept;
    private String promptVersion;
    private String templateVersion;
    private String modelVersion;
    private LocalDateTime eventTime;
    private LocalDateTime insertTime;
}
