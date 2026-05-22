package com.regionalai.floatingball.server.modules.useractivity.dto;

import lombok.Data;

@Data
public class UserActivitySummaryVO {

    private long activeUsers;
    private long inactiveUsers;
    private String activityRate;
    private String effectiveConsultationRate;

    private String activeUsersGrowth;
    private String inactiveUsersGrowth;
    private String activityRateGrowth;
    private String effectiveConsultationRateGrowth;
}
