package com.regionalai.floatingball.server.modules.stats.service;

import com.regionalai.floatingball.server.modules.stats.dto.OverviewStatsVO;
import com.regionalai.floatingball.server.modules.stats.mapper.OverviewStatsMapper;
import org.springframework.stereotype.Service;

@Service
public class OverviewStatsService {

    private final OverviewStatsMapper overviewStatsMapper;

    public OverviewStatsService(OverviewStatsMapper overviewStatsMapper) {
        this.overviewStatsMapper = overviewStatsMapper;
    }

    public OverviewStatsVO getOverview() {
        OverviewStatsVO stats = overviewStatsMapper.selectOverviewStats();
        return stats == null ? new OverviewStatsVO() : stats;
    }
}
