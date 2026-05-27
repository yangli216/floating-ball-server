package com.regionalai.floatingball.server.modules.analytics.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class FunctionUsageQueryDTO {
    private String dateFrom;
    private String dateTo;
    private String idRegion;
    private String idOrg;
    private List<String> functionModules;
    private Integer current;
    private Integer size;
    private LocalDateTime dateFromValue;
    private LocalDateTime dateToExclusive;
}
