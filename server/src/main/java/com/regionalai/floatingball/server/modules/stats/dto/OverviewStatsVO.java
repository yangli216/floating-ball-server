package com.regionalai.floatingball.server.modules.stats.dto;

import lombok.Data;

@Data
public class OverviewStatsVO {

    private long regionCount;
    private long orgCount;
    private long deviceCount;
    private long configCount;
    private long symptomTemplateCount;
    private long logCount;
    private long userCount;
    private long roleCount;
}
