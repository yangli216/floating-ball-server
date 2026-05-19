package com.regionalai.floatingball.server.modules.featureevent.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.regionalai.floatingball.server.modules.device.entity.AiDevice;
import com.regionalai.floatingball.server.modules.featureevent.dto.FeatureEventBatchRequest;
import com.regionalai.floatingball.server.modules.featureevent.dto.FeatureEventBatchResponse;
import com.regionalai.floatingball.server.modules.featureevent.entity.AiFeatureEvent;
import com.regionalai.floatingball.server.modules.featureevent.mapper.AiFeatureEventMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeatureEventServiceTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Mock
    private AiFeatureEventMapper featureEventMapper;

    private FeatureEventService service;

    @BeforeEach
    void setUp() {
        service = new FeatureEventService(featureEventMapper, new ObjectMapper());
    }

    @Test
    void saveBatchShouldInsertKnownFeatureEvent() throws Exception {
        when(featureEventMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        AiDevice device = new AiDevice();
        device.setIdDevice("DEV001");
        device.setIdOrg("ORG001");
        device.setIdRegion("REG001");

        FeatureEventBatchRequest.FeatureEventRequest event = new FeatureEventBatchRequest.FeatureEventRequest();
        event.setEventId("EVENT-001");
        event.setFeatureCode(FeatureEventCatalog.VOICE_CONSULTATION);
        event.setEventAction("submit_voice_consultation");
        event.setIdempotencyKey("consultation:voice:CONSULT-001");
        event.setTraceId("TRACE-001");
        event.setConsultationId("CONSULT-001");
        event.setSessionId("SESSION-001");
        event.setSourceModule("voice_consultation");
        event.setScene("voice-consultation");
        event.setDoctorId("DOC001");
        event.setDoctorName("张医生");
        event.setDeptId("DEPT001");
        event.setDeptName("全科");
        event.setTimestamp(1770000000000L);
        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("patientId", "PAT001");
        event.setPayload(payload);

        FeatureEventBatchRequest request = new FeatureEventBatchRequest();
        request.setEvents(Collections.singletonList(event));

        FeatureEventBatchResponse response = service.saveBatch(device, request);

        assertEquals(1, response.getAccepted());
        assertEquals(0, response.getSkipped());

        ArgumentCaptor<AiFeatureEvent> captor = ArgumentCaptor.forClass(AiFeatureEvent.class);
        verify(featureEventMapper).insert(captor.capture());
        AiFeatureEvent saved = captor.getValue();
        assertEquals("EVENT-001", saved.getIdEvent());
        assertEquals("DEV001", saved.getIdDevice());
        assertEquals("ORG001", saved.getIdOrg());
        assertEquals("REG001", saved.getIdRegion());
        assertEquals(FeatureEventCatalog.VOICE_CONSULTATION, saved.getFeatureCode());
        assertEquals("语音问诊", saved.getFeatureName());
        assertEquals("submit_voice_consultation", saved.getEventAction());
        assertEquals("consultation:voice:CONSULT-001", saved.getIdempotencyKey());
        assertEquals("TRACE-001", saved.getTraceId());
        assertEquals("CONSULT-001", saved.getConsultationId());
        assertEquals("SESSION-001", saved.getSessionId());
        assertEquals("voice_consultation", saved.getSourceModule());
        assertEquals("voice-consultation", saved.getSceneCode());
        assertEquals("DOC001", saved.getIdDoctor());
        assertEquals("张医生", saved.getNaDoctor());
        assertEquals("DEPT001", saved.getIdDept());
        assertEquals("全科", saved.getNaDept());
        assertEquals("success", saved.getEventStatus());
        assertEquals("1", saved.getFgActive());
        assertNotNull(saved.getEventTime());
        assertEquals(OBJECT_MAPPER.valueToTree(payload), OBJECT_MAPPER.readTree(saved.getPayloadJson()));
    }

    @Test
    void saveBatchShouldSkipDuplicateIdempotencyKey() {
        when(featureEventMapper.selectCount(any(Wrapper.class))).thenReturn(1L);
        AiDevice device = new AiDevice();
        device.setIdDevice("DEV001");

        FeatureEventBatchRequest.FeatureEventRequest event = new FeatureEventBatchRequest.FeatureEventRequest();
        event.setFeatureCode(FeatureEventCatalog.CHAT);
        event.setIdempotencyKey("chat:MSG001");

        FeatureEventBatchRequest request = new FeatureEventBatchRequest();
        request.setEvents(Collections.singletonList(event));

        FeatureEventBatchResponse response = service.saveBatch(device, request);

        assertEquals(0, response.getAccepted());
        assertEquals(1, response.getSkipped());
        verify(featureEventMapper, never()).insert(any(AiFeatureEvent.class));
    }

    @Test
    void saveBatchShouldSkipUnsupportedFeatureCode() {
        FeatureEventBatchRequest.FeatureEventRequest event = new FeatureEventBatchRequest.FeatureEventRequest();
        event.setFeatureCode("raw_ai_operation");

        FeatureEventBatchRequest request = new FeatureEventBatchRequest();
        request.setEvents(Collections.singletonList(event));

        FeatureEventBatchResponse response = service.saveBatch(null, request);

        assertEquals(0, response.getAccepted());
        assertEquals(1, response.getSkipped());
        verify(featureEventMapper, never()).insert(any(AiFeatureEvent.class));
    }

    @Test
    void saveBatchShouldTreatUniqueConstraintAsSkipped() {
        when(featureEventMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(featureEventMapper.insert(any(AiFeatureEvent.class))).thenThrow(new DuplicateKeyException("duplicate"));
        AiDevice device = new AiDevice();
        device.setIdDevice("DEV001");

        FeatureEventBatchRequest.FeatureEventRequest event = new FeatureEventBatchRequest.FeatureEventRequest();
        event.setFeatureCode(FeatureEventCatalog.SMART_CONSULTATION);
        event.setIdempotencyKey("consultation:smart:CONSULT-001");

        FeatureEventBatchRequest request = new FeatureEventBatchRequest();
        request.setEvents(Collections.singletonList(event));

        FeatureEventBatchResponse response = service.saveBatch(device, request);

        assertEquals(0, response.getAccepted());
        assertEquals(1, response.getSkipped());
    }
}
