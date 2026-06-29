package com.regionalai.floatingball.server.modules.recommendationpreference.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AdminRecommendationPreferenceAggregateVO {
    private String idAgg;
    private String scope;
    private String idOrg;
    private String idRegion;
    private String idDept;
    private String idDoctor;
    private String recommendationType;
    private String itemKey;
    private String itemId;
    private String itemCode;
    private String itemName;
    private Integer selectedCount;
    private Integer confirmCount;
    private Integer manualMatchCount;
    private Integer sampleCount;
    private BigDecimal preferenceScore;
    private LocalDateTime lastEventTime;
    private LocalDateTime insertTime;
    private LocalDateTime updateTime;
}
