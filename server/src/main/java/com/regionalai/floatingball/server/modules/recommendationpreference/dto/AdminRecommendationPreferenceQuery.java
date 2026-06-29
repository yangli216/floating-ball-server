package com.regionalai.floatingball.server.modules.recommendationpreference.dto;

import lombok.Data;

@Data
public class AdminRecommendationPreferenceQuery {
    private long current = 1;
    private long size = 10;
    private String recommendationType;
    private String scope;
    private String idRegion;
    private String idOrg;
    private String idDept;
    private String idDoctor;
    private String keyword;
    private String dateFrom;
    private String dateTo;
}
