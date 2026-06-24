package com.regionalai.floatingball.server.modules.businessdebug.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regionalai.floatingball.server.modules.ai.dto.ChatRequest;
import com.regionalai.floatingball.server.modules.ai.service.AiProxyService;
import com.regionalai.floatingball.server.modules.businessdebug.dto.BusinessDebugContextVO;
import com.regionalai.floatingball.server.modules.businessdebug.dto.BusinessDebugExecuteRequest;
import com.regionalai.floatingball.server.modules.businessdebug.dto.BusinessDebugExecuteResponse;
import com.regionalai.floatingball.server.modules.device.entity.AiDevice;
import com.regionalai.floatingball.server.modules.device.mapper.AiDeviceMapper;
import com.regionalai.floatingball.server.modules.prompt.dto.PromptView;
import com.regionalai.floatingball.server.modules.prompt.service.PromptService;
import com.regionalai.floatingball.server.modules.userlog.entity.AiUserConsultationLog;
import com.regionalai.floatingball.server.modules.userlog.mapper.AiUserConsultationLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusinessWorkflowDebugServiceTest {

    @Mock
    private AiUserConsultationLogMapper consultationLogMapper;

    @Mock
    private AiDeviceMapper deviceMapper;

    @Mock
    private PromptService promptService;

    @Mock
    private AiProxyService aiProxyService;

    private BusinessWorkflowDebugService service;

    @BeforeEach
    void setUp() {
        service = new BusinessWorkflowDebugService(
            consultationLogMapper,
            deviceMapper,
            promptService,
            aiProxyService,
            new ObjectMapper()
        );
    }

    @Test
    void contextShouldLoadVoiceConsultationAndResolvedNodes() {
        when(consultationLogMapper.selectById("LOG001")).thenReturn(voiceLog());
        when(deviceMapper.selectById("DEV001")).thenReturn(device());
        when(promptService.resolveEffectivePrompt(any(), any(), any())).thenAnswer(invocation -> prompt(invocation.getArgument(0)));

        BusinessDebugContextVO context = service.context("LOG001");

        assertThat(context.getRun().getSceneName()).isEqualTo("语音接诊");
        assertThat(context.getContext().get("speechText")).isEqualTo("患者发热一天");
        assertThat(context.getNodes()).extracting("nodeCode").contains("voice_transcript_calibration", "medical_record_generation");
        assertThat(context.getNodes().get(0).getSystemPrompt()).contains("system");
    }

    @Test
    void executeShouldUseBusinessDebugSceneAndConsultationId() {
        when(consultationLogMapper.selectById("LOG001")).thenReturn(voiceLog());
        when(deviceMapper.selectById("DEV001")).thenReturn(device());
        when(promptService.resolveEffectivePrompt(any(), any(), any())).thenAnswer(invocation -> prompt(invocation.getArgument(0)));
        when(aiProxyService.chat(any(AiDevice.class), any(ChatRequest.class))).thenReturn("{\"ok\":true}");

        BusinessDebugExecuteRequest request = new BusinessDebugExecuteRequest();
        request.setIdRun("LOG001");
        request.setNodeCode("medical_record_generation");
        request.setSystemPrompt("custom system");
        request.setUserPrompt("custom user");
        request.setConfigProfile("default");
        request.setTemperature(0.2);
        request.setInputPayload(Collections.<String, Object>singletonMap("input", "患者发热一天"));

        BusinessDebugExecuteResponse response = service.execute(request);

        assertThat(response.getParsedJson()).isInstanceOf(Map.class);
        ArgumentCaptor<ChatRequest> captor = ArgumentCaptor.forClass(ChatRequest.class);
        verify(aiProxyService).chat(any(AiDevice.class), captor.capture());
        ChatRequest chatRequest = captor.getValue();
        assertThat(chatRequest.getScene()).isEqualTo("business-workflow-debug-medical_record_generation");
        assertThat(chatRequest.getSourceModule()).isEqualTo("business_workflow_debug");
        assertThat(chatRequest.getConsultationId()).isEqualTo("CONSULT-001");
        assertThat(chatRequest.getMessages()).hasSize(2);
    }

    private AiUserConsultationLog voiceLog() {
        AiUserConsultationLog log = new AiUserConsultationLog();
        log.setIdLog("LOG001");
        log.setConsultationId("CONSULT-001");
        log.setConsultationType("voice");
        log.setIdDevice("DEV001");
        log.setIdOrg("ORG001");
        log.setFgActive("1");
        log.setPatientName("王某");
        log.setNaDoctor("张医生");
        log.setNaOrg("区域中心医院");
        log.setConsultationTime(LocalDateTime.of(2026, 6, 23, 9, 30));
        log.setSpeechText("患者发热一天");
        log.setStatus("completed");
        return log;
    }

    private AiDevice device() {
        AiDevice device = new AiDevice();
        device.setIdDevice("DEV001");
        device.setIdOrg("ORG001");
        device.setIdRegion("REG001");
        device.setFgActive("1");
        return device;
    }

    private PromptView prompt(String code) {
        PromptView prompt = new PromptView();
        prompt.setCdPrompt(code);
        prompt.setNaPrompt(code + "-name");
        prompt.setSysPrompt("system " + code);
        prompt.setUserTemplate("input={{input}}\nupstream={{upstreamOutput}}\nspeech={{speechText}}");
        prompt.setVersionNum("v1.0");
        prompt.setSource("built_in");
        prompt.setBuiltIn(Boolean.TRUE);
        return prompt;
    }
}
