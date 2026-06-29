package com.regionalai.floatingball.server.modules.recommendationpreference.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.regionalai.floatingball.server.common.api.PageResponse;
import com.regionalai.floatingball.server.modules.recommendationpreference.dto.AdminRecommendationPreferenceAggregateVO;
import com.regionalai.floatingball.server.modules.recommendationpreference.dto.AdminRecommendationPreferenceEventVO;
import com.regionalai.floatingball.server.modules.recommendationpreference.dto.AdminRecommendationPreferenceQuery;
import com.regionalai.floatingball.server.modules.recommendationpreference.dto.AdminRecommendationPreferenceSummaryVO;
import com.regionalai.floatingball.server.modules.recommendationpreference.entity.AiRecommendationPreferenceAggregate;
import com.regionalai.floatingball.server.modules.recommendationpreference.entity.AiRecommendationPreferenceEvent;
import com.regionalai.floatingball.server.modules.recommendationpreference.mapper.AiRecommendationPreferenceAggregateMapper;
import com.regionalai.floatingball.server.modules.recommendationpreference.mapper.AiRecommendationPreferenceEventMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminRecommendationPreferenceServiceTest {

    @Mock
    private AiRecommendationPreferenceAggregateMapper aggregateMapper;

    @Mock
    private AiRecommendationPreferenceEventMapper eventMapper;

    private AdminRecommendationPreferenceService service;

    @BeforeEach
    void setUp() {
        service = new AdminRecommendationPreferenceService(aggregateMapper, eventMapper);
    }

    @Test
    void summaryShouldCountScopesAndAveragePreferenceScore() {
        when(aggregateMapper.selectList(any(Wrapper.class))).thenReturn(Arrays.asList(
            aggregate("ORG", null, null, 2, 0.2D),
            aggregate("DEPT", "DEPT001", null, 4, 0.4D),
            aggregate("DOC", "DEPT001", "DOC001", 6, 0.6D)
        ));
        when(eventMapper.selectCount(any(Wrapper.class))).thenReturn(12L);

        AdminRecommendationPreferenceSummaryVO summary = service.summary(new AdminRecommendationPreferenceQuery());

        assertEquals(3, summary.getAggregateCount());
        assertEquals(12, summary.getEventCount());
        assertEquals(1, summary.getOrgScopeCount());
        assertEquals(1, summary.getDeptScopeCount());
        assertEquals(1, summary.getDoctorScopeCount());
        assertEquals(new BigDecimal("0.4000"), summary.getAveragePreferenceScore());
    }

    @Test
    void aggregatesShouldMapScopeAndSampleCount() {
        Page<AiRecommendationPreferenceAggregate> page = new Page<AiRecommendationPreferenceAggregate>(1, 10);
        page.setTotal(1);
        page.setRecords(Collections.singletonList(aggregate("DOC", "DEPT001", "DOC001", 6, 0.6D)));
        when(aggregateMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(page);

        PageResponse<AdminRecommendationPreferenceAggregateVO> response =
            service.aggregates(new AdminRecommendationPreferenceQuery());

        assertEquals(1, response.getTotal());
        assertEquals("doctor", response.getRecords().get(0).getScope());
        assertEquals("diagnosis:DOC", response.getRecords().get(0).getItemKey());
        assertEquals(6, response.getRecords().get(0).getSelectedCount());
        assertEquals(8, response.getRecords().get(0).getSampleCount());
        assertEquals(BigDecimal.valueOf(0.6D), response.getRecords().get(0).getPreferenceScore());
    }

    @Test
    void eventsShouldMapBooleansAndScope() {
        Page<AiRecommendationPreferenceEvent> page = new Page<AiRecommendationPreferenceEvent>(1, 10);
        page.setTotal(1);
        page.setRecords(Collections.singletonList(event()));
        when(eventMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(page);

        PageResponse<AdminRecommendationPreferenceEventVO> response =
            service.events(new AdminRecommendationPreferenceQuery());

        assertEquals(1, response.getTotal());
        assertEquals("doctor", response.getRecords().get(0).getScope());
        assertEquals("EVENT001", response.getRecords().get(0).getIdEvent());
        assertEquals("manual_match", response.getRecords().get(0).getActionCode());
        assertEquals("order:medicine:M001", response.getRecords().get(0).getItemKey());
        assertEquals(true, response.getRecords().get(0).isSelected());
        assertEquals(false, response.getRecords().get(0).isPrimary());
        assertEquals("TRACE001", response.getRecords().get(0).getTraceId());
    }

    private AiRecommendationPreferenceAggregate aggregate(String id,
                                                          String deptId,
                                                          String doctorId,
                                                          int selectedCount,
                                                          double score) {
        AiRecommendationPreferenceAggregate aggregate = new AiRecommendationPreferenceAggregate();
        aggregate.setIdAgg(id);
        aggregate.setIdOrg("ORG001");
        aggregate.setIdRegion("REG001");
        aggregate.setIdDept(deptId);
        aggregate.setIdDoctor(doctorId);
        aggregate.setRecommendationType("diagnosis");
        aggregate.setItemKey("diagnosis:" + id);
        aggregate.setItemId(id);
        aggregate.setItemCode("J06.900");
        aggregate.setItemName("急性上呼吸道感染");
        aggregate.setSelectedCount(selectedCount);
        aggregate.setConfirmCount(1);
        aggregate.setManualMatchCount(1);
        aggregate.setPreferenceScore(BigDecimal.valueOf(score));
        aggregate.setLastEventTime(LocalDateTime.of(2026, 6, 24, 10, 0));
        aggregate.setFgActive("1");
        return aggregate;
    }

    private AiRecommendationPreferenceEvent event() {
        AiRecommendationPreferenceEvent event = new AiRecommendationPreferenceEvent();
        event.setIdEvent("EVENT001");
        event.setIdDevice("DEV001");
        event.setIdOrg("ORG001");
        event.setIdRegion("REG001");
        event.setRecommendationType("medicine");
        event.setActionCode("manual_match");
        event.setIdempotencyKey("pref:EVENT001");
        event.setItemKey("order:medicine:M001");
        event.setItemId("M001");
        event.setItemCode("MED001");
        event.setItemName("布洛芬");
        event.setFgSelected("1");
        event.setFgPrimary("0");
        event.setTraceId("TRACE001");
        event.setConsultationId("CONSULT001");
        event.setSessionId("SESSION001");
        event.setSourceModule("voice_consultation");
        event.setSceneCode("voice-consultation");
        event.setIdDoctor("DOC001");
        event.setNaDoctor("张医生");
        event.setIdDept("DEPT001");
        event.setNaDept("全科");
        event.setEventTime(LocalDateTime.of(2026, 6, 24, 10, 0));
        event.setFgActive("1");
        return event;
    }
}
