package com.regionalai.floatingball.server.modules.recommendationpreference.controller;

import com.regionalai.floatingball.server.common.api.PageResponse;
import com.regionalai.floatingball.server.common.exception.GlobalExceptionHandler;
import com.regionalai.floatingball.server.modules.recommendationpreference.dto.AdminRecommendationPreferenceAggregateVO;
import com.regionalai.floatingball.server.modules.recommendationpreference.dto.AdminRecommendationPreferenceQuery;
import com.regionalai.floatingball.server.modules.recommendationpreference.dto.AdminRecommendationPreferenceSummaryVO;
import com.regionalai.floatingball.server.modules.recommendationpreference.service.AdminRecommendationPreferenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminRecommendationPreferenceControllerTest {

    @Mock
    private AdminRecommendationPreferenceService recommendationPreferenceService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .standaloneSetup(new AdminRecommendationPreferenceController(recommendationPreferenceService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    void summaryShouldBindQueryAndWrapResponse() throws Exception {
        AdminRecommendationPreferenceSummaryVO summary = new AdminRecommendationPreferenceSummaryVO();
        summary.setAggregateCount(3);
        summary.setEventCount(12);
        summary.setDoctorScopeCount(1);
        summary.setAveragePreferenceScore(new BigDecimal("0.4000"));
        when(recommendationPreferenceService.summary(any(AdminRecommendationPreferenceQuery.class))).thenReturn(summary);

        mockMvc.perform(get("/admin/api/recommendation-preferences/summary")
                .param("recommendationType", "diagnosis")
                .param("scope", "doctor")
                .param("idOrg", "ORG001")
                .header("X-Request-Id", "RID-pref-summary"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.requestId").value("RID-pref-summary"))
            .andExpect(jsonPath("$.data.aggregateCount").value(3))
            .andExpect(jsonPath("$.data.eventCount").value(12))
            .andExpect(jsonPath("$.data.averagePreferenceScore").value(0.4));

        ArgumentCaptor<AdminRecommendationPreferenceQuery> queryCaptor =
            ArgumentCaptor.forClass(AdminRecommendationPreferenceQuery.class);
        verify(recommendationPreferenceService).summary(queryCaptor.capture());
        assertEquals("diagnosis", queryCaptor.getValue().getRecommendationType());
        assertEquals("doctor", queryCaptor.getValue().getScope());
        assertEquals("ORG001", queryCaptor.getValue().getIdOrg());
    }

    @Test
    void aggregatesShouldReturnPagedRecords() throws Exception {
        AdminRecommendationPreferenceAggregateVO item = new AdminRecommendationPreferenceAggregateVO();
        item.setIdAgg("AGG001");
        item.setScope("doctor");
        item.setRecommendationType("diagnosis");
        item.setItemKey("diagnosis:D001");
        item.setItemName("急性上呼吸道感染");
        item.setSampleCount(8);
        item.setPreferenceScore(new BigDecimal("0.6000"));

        when(recommendationPreferenceService.aggregates(any(AdminRecommendationPreferenceQuery.class)))
            .thenReturn(new PageResponse<AdminRecommendationPreferenceAggregateVO>(1, 10, 1, Collections.singletonList(item)));

        mockMvc.perform(get("/admin/api/recommendation-preferences/aggregates")
                .param("current", "1")
                .param("size", "10")
                .header("X-Request-Id", "RID-pref-aggregates"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.requestId").value("RID-pref-aggregates"))
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.records[0].idAgg").value("AGG001"))
            .andExpect(jsonPath("$.data.records[0].scope").value("doctor"))
            .andExpect(jsonPath("$.data.records[0].itemName").value("急性上呼吸道感染"));
    }
}
