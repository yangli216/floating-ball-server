package com.regionalai.floatingball.server.modules.stats.mapper;

import com.regionalai.floatingball.server.modules.stats.dto.OverviewStatsVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.SelectProvider;

@Mapper
public interface OverviewStatsMapper {

    @SelectProvider(type = OverviewStatsSqlProvider.class, method = "selectOverviewStats")
    OverviewStatsVO selectOverviewStats();
}
