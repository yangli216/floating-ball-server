package com.regionalai.floatingball.server.modules.useractivity.dto;

import lombok.Data;

@Data
public class UserActivityQueryDTO {

    private String dateFrom;
    private String dateTo;
    private String idRegion;
    private String timeRange;
    private String activeStatus;
}
