package com.regionalai.floatingball.server.modules.useractivity.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserActivityQueryDTO {

    private String dateFrom;
    private String dateTo;
    private String idRegion;
    private String idOrg;
    private String timeRange;
    private String activeStatus;
    private LocalDateTime dateFromValue;
    private LocalDateTime dateToExclusive;
}
