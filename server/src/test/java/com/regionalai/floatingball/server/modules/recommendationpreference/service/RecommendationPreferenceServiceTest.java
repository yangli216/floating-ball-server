package com.regionalai.floatingball.server.modules.recommendationpreference.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.regionalai.floatingball.server.modules.config.dto.ResolvedAiConfig;
import com.regionalai.floatingball.server.modules.config.service.ConfigService;
import com.regionalai.floatingball.server.modules.device.entity.AiDevice;
import com.regionalai.floatingball.server.modules.recommendationpreference.dto.RecommendationPreferenceBatchRequest;
import com.regionalai.floatingball.server.modules.recommendationpreference.dto.RecommendationPreferenceBatchResponse;
import com.regionalai.floatingball.server.modules.recommendationpreference.dto.RecommendationPreferenceRankRequest;
import com.regionalai.floatingball.server.modules.recommendationpreference.dto.RecommendationPreferenceRankResponse;
import com.regionalai.floatingball.server.modules.recommendationpreference.entity.AiRecommendationPreferenceAggregate;
import com.regionalai.floatingball.server.modules.recommendationpreference.entity.AiRecommendationPreferenceEvent;
import com.regionalai.floatingball.server.modules.recommendationpreference.mapper.AiRecommendationPreferenceAggregateMapper;
import com.regionalai.floatingball.server.modules.recommendationpreference.mapper.AiRecommendationPreferenceEventMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationPreferenceServiceTest {

    @Mock
    private AiRecommendationPreferenceEventMapper eventMapper;

    @Mock
    private AiRecommendationPreferenceAggregateMapper aggregateMapper;

    @Mock
    private ConfigService configService;

    private RecommendationPreferenceService service;
    private AiDevice device;

    @BeforeEach
    void setUp() {
        service = new RecommendationPreferenceService(eventMapper, aggregateMapper, configService, new ObjectMapper());
        device = new AiDevice();
        device.setIdDevice("DEV001");
        device.setIdOrg("ORG001");
        device.setIdRegion("REG001");

        ResolvedAiConfig config = new ResolvedAiConfig();
        Map<String, Boolean> features = new HashMap<String, Boolean>();
        features.put("recommendationPreferenceCollection", true);
        features.put("recommendationPreferenceRerank", true);
        config.setFeatures(features);
        when(configService.resolveByDevice(device)).thenReturn(config);
    }

    @Test
    void saveBatchShouldInsertEventAndThreeScopeAggregates() {
        when(eventMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(aggregateMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        RecommendationPreferenceBatchRequest request = new RecommendationPreferenceBatchRequest();
        request.setEvents(Collections.singletonList(event("EVENT-001", "diagnosis", "final_select", "diagnosis:D001")));

        RecommendationPreferenceBatchResponse response = service.saveBatch(device, request);

        assertEquals(1, response.getAccepted());
        assertEquals(0, response.getSkipped());
        assertEquals(0, response.getRejected());

        ArgumentCaptor<AiRecommendationPreferenceEvent> eventCaptor = ArgumentCaptor.forClass(AiRecommendationPreferenceEvent.class);
        verify(eventMapper).insert(eventCaptor.capture());
        AiRecommendationPreferenceEvent saved = eventCaptor.getValue();
        assertEquals("EVENT-001", saved.getIdEvent());
        assertEquals("DEV001", saved.getIdDevice());
        assertEquals("diagnosis", saved.getRecommendationType());
        assertEquals("final_select", saved.getActionCode());
        assertEquals("diagnosis:D001", saved.getItemKey());
        assertEquals("1", saved.getFgSelected());
        assertEquals("1", saved.getFgPrimary());

        ArgumentCaptor<AiRecommendationPreferenceAggregate> aggregateCaptor = ArgumentCaptor.forClass(AiRecommendationPreferenceAggregate.class);
        verify(aggregateMapper, org.mockito.Mockito.times(3)).insert(aggregateCaptor.capture());
        assertTrue(aggregateCaptor.getAllValues().stream().anyMatch(item -> item.getIdDept() == null && item.getIdDoctor() == null));
        assertTrue(aggregateCaptor.getAllValues().stream().anyMatch(item -> "DEPT001".equals(item.getIdDept()) && item.getIdDoctor() == null));
        assertTrue(aggregateCaptor.getAllValues().stream().anyMatch(item -> "DOC001".equals(item.getIdDoctor())));
    }

    @Test
    void saveBatchShouldSkipDuplicateIdempotencyKey() {
        when(eventMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        RecommendationPreferenceBatchRequest request = new RecommendationPreferenceBatchRequest();
        request.setEvents(Collections.singletonList(event("EVENT-001", "diagnosis", "final_select", "diagnosis:D001")));

        RecommendationPreferenceBatchResponse response = service.saveBatch(device, request);

        assertEquals(0, response.getAccepted());
        assertEquals(1, response.getSkipped());
        assertEquals(0, response.getRejected());
        verify(eventMapper, never()).insert(any(AiRecommendationPreferenceEvent.class));
    }

    @Test
    void saveBatchShouldUseEventIdWhenIdempotencyKeyIsMissing() {
        when(eventMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(aggregateMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        RecommendationPreferenceBatchRequest.RecommendationPreferenceEventRequest event =
            event("EVENT-ONLY", "diagnosis", "final_select", "diagnosis:D001");
        event.setIdempotencyKey(null);

        RecommendationPreferenceBatchRequest request = new RecommendationPreferenceBatchRequest();
        request.setEvents(Collections.singletonList(event));

        RecommendationPreferenceBatchResponse response = service.saveBatch(device, request);

        assertEquals(1, response.getAccepted());
        ArgumentCaptor<AiRecommendationPreferenceEvent> eventCaptor = ArgumentCaptor.forClass(AiRecommendationPreferenceEvent.class);
        verify(eventMapper).insert(eventCaptor.capture());
        assertEquals("event:EVENT-ONLY", eventCaptor.getValue().getIdempotencyKey());
    }

    @Test
    void saveBatchShouldRejectWhenBothEventIdAndIdempotencyKeyAreMissing() {
        RecommendationPreferenceBatchRequest.RecommendationPreferenceEventRequest event =
            event("EVENT-001", "diagnosis", "final_select", "diagnosis:D001");
        event.setEventId(null);
        event.setIdempotencyKey(null);

        RecommendationPreferenceBatchRequest request = new RecommendationPreferenceBatchRequest();
        request.setEvents(Collections.singletonList(event));

        RecommendationPreferenceBatchResponse response = service.saveBatch(device, request);

        assertEquals(0, response.getAccepted());
        assertEquals(1, response.getRejected());
        assertEquals("eventId 或 idempotencyKey 不能为空", response.getRejections().get(0).getReason());
        verify(eventMapper, never()).insert(any(AiRecommendationPreferenceEvent.class));
    }

    @Test
    void saveBatchShouldRejectUnsupportedTypeAndAction() {
        RecommendationPreferenceBatchRequest request = new RecommendationPreferenceBatchRequest();
        request.setEvents(Collections.singletonList(event("EVENT-001", "template", "final_select", "template:T001")));

        RecommendationPreferenceBatchResponse response = service.saveBatch(device, request);

        assertEquals(0, response.getAccepted());
        assertEquals(1, response.getRejected());
        assertEquals("recommendationType 不支持", response.getRejections().get(0).getReason());
        verify(eventMapper, never()).insert(any(AiRecommendationPreferenceEvent.class));
    }

    @Test
    void saveBatchShouldRejectNonStandardItemKey() {
        RecommendationPreferenceBatchRequest request = new RecommendationPreferenceBatchRequest();
        request.setEvents(Collections.singletonList(event("EVENT-001", "medicine", "final_select", "raw:布洛芬")));

        RecommendationPreferenceBatchResponse response = service.saveBatch(device, request);

        assertEquals(0, response.getAccepted());
        assertEquals(1, response.getRejected());
        assertEquals("itemKey 与 recommendationType 不匹配", response.getRejections().get(0).getReason());
        verify(eventMapper, never()).insert(any(AiRecommendationPreferenceEvent.class));
    }

    @Test
    void saveBatchShouldTreatUniqueConstraintAsSkipped() {
        when(eventMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(eventMapper.insert(any(AiRecommendationPreferenceEvent.class))).thenThrow(new DuplicateKeyException("duplicate"));

        RecommendationPreferenceBatchRequest request = new RecommendationPreferenceBatchRequest();
        request.setEvents(Collections.singletonList(event("EVENT-001", "diagnosis", "final_select", "diagnosis:D001")));

        RecommendationPreferenceBatchResponse response = service.saveBatch(device, request);

        assertEquals(0, response.getAccepted());
        assertEquals(1, response.getSkipped());
        assertEquals(0, response.getRejected());
    }

    @Test
    void rankShouldPreferDoctorScopeOverDeptAndOrg() {
        when(aggregateMapper.selectOne(any(Wrapper.class)))
            .thenReturn(aggregate("doctor", "DEPT001", "DOC001", 10, 0.8D));

        RecommendationPreferenceRankResponse response = service.rank(device, rankRequest("diagnosis:D001"));

        assertTrue(response.isEnabled());
        assertEquals(1, response.getItems().size());
        assertEquals("doctor", response.getItems().get(0).getScope());
        assertEquals(0.8D, response.getItems().get(0).getPreferenceScore());
        assertEquals(0.12D, response.getItems().get(0).getBoost());
    }

    @Test
    void rankShouldReturnZeroBoostWhenSamplesAreInsufficient() {
        when(aggregateMapper.selectOne(any(Wrapper.class))).thenReturn(aggregate("doctor", "DEPT001", "DOC001", 2, 0.9D));

        RecommendationPreferenceRankResponse response = service.rank(device, rankRequest("diagnosis:D001"));

        assertTrue(response.isEnabled());
        assertEquals("none", response.getItems().get(0).getScope());
        assertEquals(0D, response.getItems().get(0).getBoost());
        assertEquals("insufficient_samples", response.getItems().get(0).getReason());
    }

    private RecommendationPreferenceBatchRequest.RecommendationPreferenceEventRequest event(
        String eventId,
        String type,
        String action,
        String itemKey
    ) {
        RecommendationPreferenceBatchRequest.RecommendationPreferenceEventRequest event =
            new RecommendationPreferenceBatchRequest.RecommendationPreferenceEventRequest();
        event.setEventId(eventId);
        event.setIdempotencyKey("pref:" + eventId);
        event.setRecommendationType(type);
        event.setAction(action);
        event.setItemKey(itemKey);
        event.setItemId("D001");
        event.setItemCode("J06.900");
        event.setItemName("急性上呼吸道感染");
        event.setSelected(true);
        event.setPrimary(true);
        event.setDoctorId("DOC001");
        event.setDoctorName("张医生");
        event.setDeptId("DEPT001");
        event.setDeptName("全科");
        return event;
    }

    private RecommendationPreferenceRankRequest rankRequest(String itemKey) {
        RecommendationPreferenceRankRequest request = new RecommendationPreferenceRankRequest();
        request.setRecommendationType("diagnosis");
        request.setDoctorId("DOC001");
        request.setDeptId("DEPT001");
        RecommendationPreferenceRankRequest.Candidate candidate = new RecommendationPreferenceRankRequest.Candidate();
        candidate.setItemKey(itemKey);
        candidate.setItemId("D001");
        candidate.setOriginalRank(0);
        request.setCandidates(Collections.singletonList(candidate));
        return request;
    }

    private AiRecommendationPreferenceAggregate aggregate(String scope, String deptId, String doctorId, int count, double score) {
        AiRecommendationPreferenceAggregate aggregate = new AiRecommendationPreferenceAggregate();
        aggregate.setIdAgg(scope);
        aggregate.setIdOrg("ORG001");
        aggregate.setIdDept(deptId);
        aggregate.setIdDoctor(doctorId);
        aggregate.setRecommendationType("diagnosis");
        aggregate.setItemKey("diagnosis:D001");
        aggregate.setSelectedCount(count);
        aggregate.setConfirmCount(0);
        aggregate.setManualMatchCount(0);
        aggregate.setPreferenceScore(BigDecimal.valueOf(score));
        aggregate.setFgActive("1");
        return aggregate;
    }
}
