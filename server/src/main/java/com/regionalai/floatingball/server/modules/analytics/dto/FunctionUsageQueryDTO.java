package com.regionalai.floatingball.server.modules.analytics.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class FunctionUsageQueryDTO {
    private String dateFrom;
    private String dateTo;
    private LocalDateTime dateFromTime;
    private LocalDateTime dateToExclusiveTime;
    private String idRegion;
    private String idOrg;
    private List<String> functionModules;
    private Integer current;
    private Integer size;
}
