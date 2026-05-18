package com.regionalai.floatingball.server.modules.analytics.service;

import com.regionalai.floatingball.server.modules.analytics.dto.FunctionUsageItemVO;
import com.regionalai.floatingball.server.modules.analytics.dto.FunctionUsageQueryDTO;
import com.regionalai.floatingball.server.modules.analytics.dto.FunctionUsageResponseVO;
import com.regionalai.floatingball.server.modules.analytics.mapper.AnalyticsMapper;
import com.regionalai.floatingball.server.modules.audit.service.AuditLogDisplayCatalog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private AnalyticsMapper analyticsMapper;

    private AnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        analyticsService = new AnalyticsService(analyticsMapper, new AuditLogDisplayCatalog());
    }

    @Test
    void getFunctionUsageShouldUseFeatureDimensionAndAcceptLegacyModuleFilter() {
        FunctionUsageItemVO rankingItem = new FunctionUsageItemVO();
        rankingItem.setModuleName("语音问诊");
        rankingItem.setCallCount(12L);
        rankingItem.setDoctorCount(3L);
        rankingItem.setAvgPerDoctor(4L);

        FunctionUsageItemVO previousItem = new FunctionUsageItemVO();
        previousItem.setModuleName("语音问诊");
        previousItem.setCallCount(6L);

        Map<String, Object> trendRow = new LinkedHashMap<String, Object>();
        trendRow.put("MODULENAME", "语音问诊");
        trendRow.put("DAYSTR", "2026-05-01");
        trendRow.put("CNT", 12L);

        when(analyticsMapper.queryFunctionUsageRanking(any(FunctionUsageQueryDTO.class)))
            .thenReturn(Collections.singletonList(rankingItem));
        when(analyticsMapper.queryFunctionUsagePreviousRanking(any(FunctionUsageQueryDTO.class)))
            .thenReturn(Collections.singletonList(previousItem));
        when(analyticsMapper.queryFunctionUsageTrend(any(FunctionUsageQueryDTO.class)))
            .thenReturn(Collections.singletonList(trendRow));
        when(analyticsMapper.queryDistinctModules())
            .thenReturn(Arrays.asList("consultation_reference", "voice_capsule"));

        FunctionUsageQueryDTO query = new FunctionUsageQueryDTO();
        query.setDateFrom("2026-05-01");
        query.setDateTo("2026-05-01");
        query.setFunctionModules(Collections.singletonList("语音问诊 AI"));

        FunctionUsageResponseVO response = analyticsService.getFunctionUsage(query);

        assertEquals("语音问诊", response.getRanking().get(0).getModuleName());
        assertEquals("100", response.getRanking().get(0).getGrowthRate());
        assertIterableEquals(Collections.singletonList("语音问诊"), response.getTrend().getModules());
        assertIterableEquals(Collections.singletonList("2026-05-01"), response.getTrend().getDays());
        verify(analyticsMapper).queryFunctionUsageRanking(argThat(arg -> arg.getFunctionModules() != null
            && arg.getFunctionModules().contains("语音问诊")));
    }

    @Test
    void getFunctionModuleOptionsShouldReturnFeatureNames() {
        when(analyticsMapper.queryDistinctModules())
            .thenReturn(Arrays.asList("语音问诊", "智能问诊", "语音问诊"));

        List<String> options = analyticsService.getFunctionModuleOptions();

        assertIterableEquals(Arrays.asList("语音问诊", "智能问诊"), options);
    }
}
