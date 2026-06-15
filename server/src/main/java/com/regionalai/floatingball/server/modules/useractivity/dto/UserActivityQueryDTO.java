package com.regionalai.floatingball.server.modules.useractivity.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserActivityQueryDTO {

    private String dateFrom;
    private String dateTo;
    private LocalDateTime dateFromTime;
    private LocalDateTime dateToExclusiveTime;
    private String idRegion;
    private String idOrg;
    private String timeRange;
    private String activeStatus;
}
