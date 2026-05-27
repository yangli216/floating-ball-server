package com.regionalai.floatingball.server.modules.analytics.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AnalyticsQueryDTO {
    private String dateFrom;
    private String dateTo;
    private String idRegion;
    private String idOrg;
    private String timeRange;
    private LocalDateTime dateFromValue;
    private LocalDateTime dateToExclusive;
}
