package com.regionalai.floatingball.server.modules.userlog.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regionalai.floatingball.server.common.exception.BusinessException;
import com.regionalai.floatingball.server.modules.audit.service.AudioLogStorageService;
import com.regionalai.floatingball.server.modules.audit.mapper.AiOpLogMapper;
import com.regionalai.floatingball.server.modules.device.entity.AiDevice;
import com.regionalai.floatingball.server.modules.userlog.dto.UserConsultationLogRequest;
import com.regionalai.floatingball.server.modules.userlog.entity.AiUserConsultationLog;
import com.regionalai.floatingball.server.modules.userlog.mapper.AiUserConsultationLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserConsultationLogServiceTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Mock
    private AiUserConsultationLogMapper mapper;

    @Mock
    private AiOpLogMapper opLogMapper;

    @Mock
    private AudioLogStorageService audioLogStorageService;

    private UserConsultationLogService service;

    @BeforeEach
    void setUp() {
        service = new UserConsultationLogService(mapper, opLogMapper, new ObjectMapper(), audioLogStorageService);
    }

    @Test
    void saveShouldInsertFirstSnapshot() throws Exception {
        when(mapper.selectOne(any())).thenReturn(null);
        AiDevice device = new AiDevice();
        device.setIdDevice("DEV001");
        device.setIdOrg("ORG001");

        Map<String, Object> firstSnapshot = new HashMap<String, Object>();
        firstSnapshot.put("chiefComplaint", "咳嗽3天");
        firstSnapshot.put("diagnoses", Collections.singletonList(Collections.singletonMap("name", "急性支气管炎")));

        UserConsultationLogRequest request = new UserConsultationLogRequest();
        request.setConsultationId("CONSULT-001");
        request.setConsultationType("voice");
        request.setPatientId("P001");
        request.setPatientName("王某");
        request.setDoctorId("D001");
        request.setDoctorName("张医生");
        request.setOrgName("区域中心医院");
        request.setFirstSnapshot(firstSnapshot);

        service.save(device, request);

        ArgumentCaptor<AiUserConsultationLog> captor = ArgumentCaptor.forClass(AiUserConsultationLog.class);
        verify(mapper).insert(captor.capture());
        verify(mapper, never()).updateById(any());

        AiUserConsultationLog saved = captor.getValue();
        assertEquals("CONSULT-001", saved.getConsultationId());
        assertEquals("voice", saved.getConsultationType());
        assertEquals("DEV001", saved.getIdDevice());
        assertEquals("ORG001", saved.getIdOrg());
        assertEquals("区域中心医院", saved.getNaOrg());
        assertEquals("D001", saved.getIdDoctor());
        assertEquals("张医生", saved.getNaDoctor());
        assertEquals("王某", saved.getPatientName());
        assertEquals("generated", saved.getStatus());
        assertEquals(OBJECT_MAPPER.valueToTree(firstSnapshot), OBJECT_MAPPER.readTree(saved.getFirstSnapshotJson()));
    }

    @Test
    void saveShouldPersistSpeechTextAndAudioMetadata() throws Exception {
        when(mapper.selectOne(any())).thenReturn(null);
        when(audioLogStorageService.store(any(byte[].class), any(String.class), any(String.class)))
            .thenReturn("/tmp/floating-ball-server/speech-audit/voice.wav");
        AiDevice device = new AiDevice();
        device.setIdDevice("DEV001");
        device.setIdOrg("ORG001");

        UserConsultationLogRequest request = new UserConsultationLogRequest();
        request.setConsultationId("CONSULT-001");
        request.setConsultationType("voice");
        request.setSpeechText("患者发热一天");
        request.setAudio(Base64.getEncoder().encodeToString("audio-bytes".getBytes("UTF-8")));
        request.setAudioMimeType("audio/wav");
        request.setAudioFileName("voice.wav");

        service.save(device, request);

        ArgumentCaptor<AiUserConsultationLog> captor = ArgumentCaptor.forClass(AiUserConsultationLog.class);
        verify(mapper).insert(captor.capture());

        AiUserConsultationLog saved = captor.getValue();
        assertEquals("患者发热一天", saved.getSpeechText());
        assertEquals("/tmp/floating-ball-server/speech-audit/voice.wav", saved.getAudioFilePath());
        assertEquals("voice.wav", saved.getAudioFileName());
        assertEquals("audio/wav", saved.getAudioMimeType());
        assertEquals(Long.valueOf("audio-bytes".getBytes("UTF-8").length), saved.getAudioSize());
        assertEquals(Boolean.TRUE, saved.getHasAudio());
        assertEquals(Boolean.TRUE, saved.getHasSpeechText());
    }

    @Test
    void saveShouldUpdateFinalSnapshotOnExistingRecord() throws Exception {
        AiUserConsultationLog existing = new AiUserConsultationLog();
        existing.setIdLog("LOG001");
        existing.setConsultationId("CONSULT-001");
        existing.setConsultationType("voice");
        existing.setIdDevice("DEV001");
        existing.setFgActive("1");
        existing.setStatus("generated");
        existing.setFirstSnapshotJson("{\"chiefComplaint\":\"咳嗽3天\"}");
        when(mapper.selectOne(any())).thenReturn(existing);

        AiDevice device = new AiDevice();
        device.setIdDevice("DEV001");
        device.setIdOrg("ORG001");

        Map<String, Object> finalSnapshot = new HashMap<String, Object>();
        finalSnapshot.put("chiefComplaint", "咳嗽、咳痰3天");
        Map<String, Object> selectionSnapshot = new HashMap<String, Object>();
        selectionSnapshot.put("selectedMedicineNames", Collections.singletonList("氨溴索"));

        UserConsultationLogRequest request = new UserConsultationLogRequest();
        request.setConsultationId("CONSULT-001");
        request.setConsultationType("voice");
        request.setFinalSnapshot(finalSnapshot);
        request.setSelectionSnapshot(selectionSnapshot);

        service.save(device, request);

        ArgumentCaptor<AiUserConsultationLog> captor = ArgumentCaptor.forClass(AiUserConsultationLog.class);
        verify(mapper, never()).insert(any());
        verify(mapper).updateById(captor.capture());

        AiUserConsultationLog updated = captor.getValue();
        assertEquals("LOG001", updated.getIdLog());
        assertEquals("completed", updated.getStatus());
        assertEquals(OBJECT_MAPPER.readTree("{\"chiefComplaint\":\"咳嗽3天\"}"), OBJECT_MAPPER.readTree(updated.getFirstSnapshotJson()));
        assertEquals(OBJECT_MAPPER.valueToTree(finalSnapshot), OBJECT_MAPPER.readTree(updated.getFinalSnapshotJson()));
        assertEquals(OBJECT_MAPPER.valueToTree(selectionSnapshot), OBJECT_MAPPER.readTree(updated.getSelectionJson()));
    }

    @Test
    void saveShouldRejectUnsupportedConsultationType() {
        UserConsultationLogRequest request = new UserConsultationLogRequest();
        request.setConsultationId("CONSULT-001");
        request.setConsultationType("raw");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.save(null, request));

        assertEquals("问诊类型非法", ex.getMessage());
    }

    @Test
    void listShouldRejectInvalidDateFormat() {
        BusinessException ex = assertThrows(
            BusinessException.class,
            () -> service.list(1, 10, null, null, null, null, null, "bad-date", null)
        );

        assertEquals("用户日志查询时间格式非法", ex.getMessage());
    }
}
