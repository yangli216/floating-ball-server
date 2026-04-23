package com.regionalai.floatingball.server.modules.stats.controller;

import com.regionalai.floatingball.server.common.exception.GlobalExceptionHandler;
import com.regionalai.floatingball.server.modules.stats.dto.OverviewStatsVO;
import com.regionalai.floatingball.server.modules.stats.service.OverviewStatsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminStatsControllerTest {

    @Mock
    private OverviewStatsService overviewStatsService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminStatsController(overviewStatsService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    void overviewShouldReturnWrappedStats() throws Exception {
        OverviewStatsVO overview = new OverviewStatsVO();
        overview.setRegionCount(2L);
        overview.setOrgCount(3L);
        overview.setDeviceCount(5L);
        overview.setConfigCount(7L);
        overview.setPromptCount(11L);
        overview.setSymptomTemplateCount(12L);
        overview.setDataPackageCount(13L);
        overview.setLogCount(17L);
        overview.setUserCount(19L);
        overview.setRoleCount(23L);

        when(overviewStatsService.getOverview()).thenReturn(overview);

        mockMvc.perform(get("/admin/api/stats/overview")
                .header("X-Request-Id", "RID-stats-overview"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.requestId").value("RID-stats-overview"))
            .andExpect(jsonPath("$.data.regionCount").value(2))
            .andExpect(jsonPath("$.data.symptomTemplateCount").value(12))
            .andExpect(jsonPath("$.data.roleCount").value(23));
    }
}
