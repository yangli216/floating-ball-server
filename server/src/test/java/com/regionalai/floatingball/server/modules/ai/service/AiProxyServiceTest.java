package com.regionalai.floatingball.server.modules.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regionalai.floatingball.server.common.exception.BusinessException;
import com.regionalai.floatingball.server.modules.ai.dto.ChatRequest;
import com.regionalai.floatingball.server.modules.ai.dto.SpeechRequest;
import com.regionalai.floatingball.server.modules.audit.service.AuditService;
import com.regionalai.floatingball.server.modules.config.dto.ResolvedAiConfig;
import com.regionalai.floatingball.server.modules.config.service.ConfigService;
import com.regionalai.floatingball.server.modules.device.entity.AiDevice;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiProxyServiceTest {

    @Test
    void resolveChatConfigUsesReviewerOverrides() {
        ConfigService configService = mock(ConfigService.class);
        AuditService auditService = mock(AuditService.class);
        AiProxyService service = new AiProxyService(configService, auditService, new RestTemplate(), new ObjectMapper());

        ResolvedAiConfig resolved = new ResolvedAiConfig();
        resolved.setBaseUrl("https://main.example/v1");
        resolved.setApiKey("main-key");
        resolved.setModel("main-model");
        resolved.setReviewerEnabled(Boolean.TRUE);
        resolved.setReviewerBaseUrl("https://reviewer.example/v1");
        resolved.setReviewerApiKey("reviewer-key");
        resolved.setReviewerModel("reviewer-model");
        when(configService.resolveByDevice(any(AiDevice.class))).thenReturn(resolved);

        ChatRequest request = new ChatRequest();
        request.setConfigProfile("reviewer");

        Object upstream = ReflectionTestUtils.invokeMethod(service, "resolveChatConfig", new AiDevice(), request);

        assertThat(ReflectionTestUtils.getField(upstream, "baseUrl")).isEqualTo("https://reviewer.example/v1");
        assertThat(ReflectionTestUtils.getField(upstream, "apiKey")).isEqualTo("reviewer-key");
        assertThat(ReflectionTestUtils.getField(upstream, "model")).isEqualTo("reviewer-model");
    }

    @Test
    void resolveChatConfigFallsBackToDefaultWhenReviewerOverridesMissing() {
        ConfigService configService = mock(ConfigService.class);
        AuditService auditService = mock(AuditService.class);
        AiProxyService service = new AiProxyService(configService, auditService, new RestTemplate(), new ObjectMapper());

        ResolvedAiConfig resolved = new ResolvedAiConfig();
        resolved.setBaseUrl("https://main.example/v1");
        resolved.setApiKey("main-key");
        resolved.setModel("main-model");
        resolved.setReviewerEnabled(Boolean.TRUE);
        when(configService.resolveByDevice(any(AiDevice.class))).thenReturn(resolved);

        ChatRequest request = new ChatRequest();
        request.setConfigProfile("reviewer");

        Object upstream = ReflectionTestUtils.invokeMethod(service, "resolveChatConfig", new AiDevice(), request);

        assertThat(ReflectionTestUtils.getField(upstream, "baseUrl")).isEqualTo("https://main.example/v1");
        assertThat(ReflectionTestUtils.getField(upstream, "apiKey")).isEqualTo("main-key");
        assertThat(ReflectionTestUtils.getField(upstream, "model")).isEqualTo("main-model");
    }

    @Test
    void resolveChatConfigRejectsReviewerProfileWhenDisabled() {
        ConfigService configService = mock(ConfigService.class);
        AuditService auditService = mock(AuditService.class);
        AiProxyService service = new AiProxyService(configService, auditService, new RestTemplate(), new ObjectMapper());

        ResolvedAiConfig resolved = new ResolvedAiConfig();
        resolved.setBaseUrl("https://main.example/v1");
        resolved.setApiKey("main-key");
        resolved.setModel("main-model");
        resolved.setReviewerEnabled(Boolean.FALSE);
        when(configService.resolveByDevice(any(AiDevice.class))).thenReturn(resolved);

        ChatRequest request = new ChatRequest();
        request.setConfigProfile("reviewer");

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(service, "resolveChatConfig", new AiDevice(), request))
            .isInstanceOf(BusinessException.class)
            .hasMessage("当前设备未开启独立审查 AI");
    }

    @Test
    @SuppressWarnings("unchecked")
    void buildSpeechLogPayloadShouldNotContainRawAudio() throws Exception {
        ConfigService configService = mock(ConfigService.class);
        AuditService auditService = mock(AuditService.class);
        AiProxyService service = new AiProxyService(configService, auditService, new RestTemplate(), new ObjectMapper());

        SpeechRequest request = new SpeechRequest();
        request.setAudio("BASE64_AUDIO_SHOULD_NOT_BE_LOGGED");
        request.setTraceId("TRACE001");
        request.setSourceModule("llm");
        request.setSessionId("SESSION001");
        request.setMimeType("audio/webm");
        request.setFormat("webm");
        request.setFileName("speech.webm");
        request.setScene("chat-input");

        Class<?> preparedSpeechFileClass = Class.forName("com.regionalai.floatingball.server.modules.ai.service.AiProxyService$PreparedSpeechFile");
        Constructor<?> constructor = preparedSpeechFileClass.getDeclaredConstructor(
            byte[].class,
            byte[].class,
            String.class,
            String.class,
            String.class,
            String.class,
            boolean.class
        );
        constructor.setAccessible(true);
        Object preparedFile = constructor.newInstance(
            "source-audio".getBytes(StandardCharsets.UTF_8),
            "upload-audio".getBytes(StandardCharsets.UTF_8),
            "audio/webm",
            "audio/webm",
            "speech.webm",
            "speech.webm",
            false
        );

        Map<String, Object> payload = (Map<String, Object>) ReflectionTestUtils.invokeMethod(
            service,
            "buildSpeechLogPayload",
            request,
            preparedFile,
            "https://speech.example/v1",
            "whisper-1",
            "ok",
            true,
            null,
            "{\"text\":\"ok\"}"
        );

        Map<String, Object> requestBody = (Map<String, Object>) payload.get("requestBody");
        assertThat(requestBody).doesNotContainKey("audio");
        assertThat(requestBody).containsEntry("fileName", "speech.webm");
        assertThat(requestBody).containsEntry("sourceAudioSize", 12);
    }
}
