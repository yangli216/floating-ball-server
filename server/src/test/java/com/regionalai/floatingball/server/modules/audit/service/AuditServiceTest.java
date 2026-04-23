package com.regionalai.floatingball.server.modules.audit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regionalai.floatingball.server.common.exception.BusinessException;
import com.regionalai.floatingball.server.modules.audit.dto.AuditBatchRequest;
import com.regionalai.floatingball.server.modules.audit.entity.AiOpLog;
import com.regionalai.floatingball.server.modules.audit.mapper.AiOpLogMapper;
import com.regionalai.floatingball.server.modules.device.entity.AiDevice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Mock
    private AiOpLogMapper aiOpLogMapper;

    @TempDir
    Path tempDir;

    private AuditService auditService;

    @BeforeEach
    void setUp() {
        auditService = new AuditService(
            aiOpLogMapper,
            new ObjectMapper(),
            new AudioLogStorageService(tempDir.resolve("speech-audit").toString())
        );
    }

    @Test
    void listShouldRejectInvalidDateFormat() {
        BusinessException ex = assertThrows(
            BusinessException.class,
            () -> auditService.list(1, 10, null, null, null, null, "bad-date", null)
        );

        assertEquals("日志查询时间格式非法", ex.getMessage());
    }

    @Test
    void saveBatchShouldInsertSerializedPayload() throws Exception {
        AiDevice device = new AiDevice();
        device.setIdDevice("DEV001");
        device.setIdOrg("ORG001");

        AuditBatchRequest request = new AuditBatchRequest();
        AuditBatchRequest.AuditEvent event = new AuditBatchRequest.AuditEvent();
        event.setEventType("operation");
        event.setTimestamp(1770000000000L);
        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("module", "consultation");
        payload.put("action", "finish");
        payload.put("success", true);
        event.setPayload(payload);
        request.setEvents(Collections.singletonList(event));

        int accepted = auditService.saveBatch(device, request);

        assertEquals(1, accepted);

        ArgumentCaptor<AiOpLog> captor = ArgumentCaptor.forClass(AiOpLog.class);
        verify(aiOpLogMapper, times(1)).insert(captor.capture());
        AiOpLog log = captor.getValue();
        assertEquals("DEV001", log.getIdDevice());
        assertEquals("ORG001", log.getIdOrg());
        assertEquals("operation", log.getSdLogType());
        assertEquals("consultation", log.getNaModule());
        assertEquals("finish", log.getDesOp());
        assertEquals("1", log.getOpResult());
        assertNotNull(log.getOperationTime());
        assertEquals(
            OBJECT_MAPPER.readTree("{\"module\":\"consultation\",\"action\":\"finish\",\"success\":true}"),
            OBJECT_MAPPER.readTree(log.getPayloadJson())
        );
    }

    @Test
    void saveBatchShouldFallbackToLegacyOperationFields() throws Exception {
        AiDevice device = new AiDevice();
        device.setIdDevice("DEV002");
        device.setIdOrg("ORG002");

        AuditBatchRequest request = new AuditBatchRequest();
        AuditBatchRequest.AuditEvent event = new AuditBatchRequest.AuditEvent();
        event.setEventType("operation");
        event.setTimestamp(1770000000000L);
        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("operationType", "api_call");
        payload.put("operationName", "reference_feedback:diagnosis");
        payload.put("success", false);
        payload.put("details", Collections.singletonMap("consultationId", "CONSULT-001"));
        event.setPayload(payload);
        request.setEvents(Collections.singletonList(event));

        int accepted = auditService.saveBatch(device, request);

        assertEquals(1, accepted);

        ArgumentCaptor<AiOpLog> captor = ArgumentCaptor.forClass(AiOpLog.class);
        verify(aiOpLogMapper, times(1)).insert(captor.capture());
        AiOpLog log = captor.getValue();
        assertEquals("api_call", log.getNaModule());
        assertEquals("reference_feedback:diagnosis", log.getDesOp());
        assertEquals("0", log.getOpResult());
        assertEquals(
            OBJECT_MAPPER.readTree("{\"operationType\":\"api_call\",\"operationName\":\"reference_feedback:diagnosis\",\"success\":false,\"details\":{\"consultationId\":\"CONSULT-001\"}}"),
            OBJECT_MAPPER.readTree(log.getPayloadJson())
        );
    }

    @Test
    void saveSystemLogWithAudioFileShouldPersistAudioPath() throws Exception {
        AiDevice device = new AiDevice();
        device.setIdDevice("DEV003");
        device.setIdOrg("ORG003");

        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("scene", "chat-input");
        payload.put("requestBody", Collections.singletonMap("fileName", "speech.wav"));

        auditService.saveSystemLogWithAudioFile(
            device,
            "speech_proxy",
            "speech",
            "transcribe",
            payload,
            true,
            "hello-audio".getBytes(StandardCharsets.UTF_8),
            "speech.wav"
        );

        ArgumentCaptor<AiOpLog> captor = ArgumentCaptor.forClass(AiOpLog.class);
        verify(aiOpLogMapper, times(1)).insert(captor.capture());
        AiOpLog log = captor.getValue();
        assertEquals("speech_proxy", log.getSdLogType());
        assertNotNull(log.getAudioFilePath());
        assertEquals("hello-audio", new String(Files.readAllBytes(Paths.get(log.getAudioFilePath())), StandardCharsets.UTF_8));
        assertEquals(
            OBJECT_MAPPER.readTree("{\"scene\":\"chat-input\",\"requestBody\":{\"fileName\":\"speech.wav\"}}"),
            OBJECT_MAPPER.readTree(log.getPayloadJson())
        );
    }
}
