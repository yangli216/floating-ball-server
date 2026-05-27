package com.regionalai.floatingball.server.modules.stats.mapper;

import com.regionalai.floatingball.server.modules.stats.dto.OverviewStatsVO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OverviewStatsMapper {

    OverviewStatsVO selectOverviewStats();
}
