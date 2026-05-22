package com.regionalai.floatingball.server.modules.stats.service;

import com.regionalai.floatingball.server.modules.stats.dto.OverviewStatsVO;
import com.regionalai.floatingball.server.modules.stats.mapper.OverviewStatsMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OverviewStatsServiceTest {

    @Mock
    private OverviewStatsMapper overviewStatsMapper;

    private OverviewStatsService overviewStatsService;

    @BeforeEach
    void setUp() {
        overviewStatsService = new OverviewStatsService(overviewStatsMapper);
    }

    @Test
    void getOverviewShouldReturnAggregatedStatsFromMapper() {
        OverviewStatsVO mapperResult = new OverviewStatsVO();
        mapperResult.setRegionCount(2L);
        mapperResult.setOrgCount(3L);
        mapperResult.setDeviceCount(5L);
        mapperResult.setConfigCount(7L);
        mapperResult.setSymptomTemplateCount(12L);
        mapperResult.setLogCount(17L);
        mapperResult.setUserCount(19L);
        mapperResult.setRoleCount(23L);
        when(overviewStatsMapper.selectOverviewStats()).thenReturn(mapperResult);

        OverviewStatsVO result = overviewStatsService.getOverview();

        assertEquals(2L, result.getRegionCount());
        assertEquals(3L, result.getOrgCount());
        assertEquals(5L, result.getDeviceCount());
        assertEquals(7L, result.getConfigCount());
        assertEquals(12L, result.getSymptomTemplateCount());
        assertEquals(17L, result.getLogCount());
        assertEquals(19L, result.getUserCount());
        assertEquals(23L, result.getRoleCount());
        verify(overviewStatsMapper).selectOverviewStats();
    }

    @Test
    void getOverviewShouldReturnZeroedStatsWhenMapperReturnsNull() {
        when(overviewStatsMapper.selectOverviewStats()).thenReturn(null);

        OverviewStatsVO result = overviewStatsService.getOverview();

        assertNotNull(result);
        assertEquals(0L, result.getRegionCount());
        assertEquals(0L, result.getOrgCount());
        assertEquals(0L, result.getDeviceCount());
        assertEquals(0L, result.getConfigCount());
        assertEquals(0L, result.getSymptomTemplateCount());
        assertEquals(0L, result.getLogCount());
        assertEquals(0L, result.getUserCount());
        assertEquals(0L, result.getRoleCount());
        verify(overviewStatsMapper).selectOverviewStats();
    }
}
