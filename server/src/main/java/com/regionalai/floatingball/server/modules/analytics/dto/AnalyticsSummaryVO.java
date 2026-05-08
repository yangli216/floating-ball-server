package com.regionalai.floatingball.server.modules.analytics.dto;

import lombok.Data;

@Data
public class AnalyticsSummaryVO {

    private long aiServiceTotal;
    private long avgDailyAiService;
    private String aiAdoptionRate;
    private String diagnosisMatchRate;
    private long activeDoctorCount;
    private long consultationTotal;

    private String aiServiceGrowth;
    private String avgDailyGrowth;
    private String adoptionRateGrowth;
    private String matchRateGrowth;
    private String activeDoctorGrowth;
    private String consultationGrowth;
}
