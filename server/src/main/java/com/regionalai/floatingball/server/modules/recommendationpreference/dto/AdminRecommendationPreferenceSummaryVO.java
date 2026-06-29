package com.regionalai.floatingball.server.modules.recommendationpreference.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdminRecommendationPreferenceSummaryVO {
    private long aggregateCount;
    private long eventCount;
    private long doctorScopeCount;
    private long deptScopeCount;
    private long orgScopeCount;
    private BigDecimal averagePreferenceScore;
}
