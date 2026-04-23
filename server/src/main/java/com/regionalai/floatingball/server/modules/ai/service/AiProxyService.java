package com.regionalai.floatingball.server.modules.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.regionalai.floatingball.server.common.exception.BusinessException;
import com.regionalai.floatingball.server.modules.ai.dto.ChatRequest;
import com.regionalai.floatingball.server.modules.ai.dto.SpeechRequest;
import com.regionalai.floatingball.server.modules.audit.service.AuditService;
import com.regionalai.floatingball.server.modules.config.dto.ResolvedAiConfig;
import com.regionalai.floatingball.server.modules.config.service.ConfigService;
import com.regionalai.floatingball.server.modules.device.entity.AiDevice;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiProxyService {

    private final ConfigService configService;
    private final AuditService auditService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public AiProxyService(ConfigService configService,
                          AuditService auditService,
                          RestTemplate restTemplate,
                          ObjectMapper objectMapper) {
        this.configService = configService;
        this.auditService = auditService;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public String chat(AiDevice device, ChatRequest request) {
        UpstreamChatConfig upstreamConfig = resolveChatConfig(device, request);
        return chat(device, request, upstreamConfig);
    }

    public void validateChatConfig(AiDevice device, ChatRequest request) {
        resolveChatConfig(device, request);
    }

    public String testChatConnection(String baseUrl, String apiKey, String model) {
        UpstreamChatConfig upstreamConfig = new UpstreamChatConfig(trimRightSlash(baseUrl), apiKey, model);
        Map<String, Object> userMessage = new LinkedHashMap<String, Object>();
        userMessage.put("role", "user");
        userMessage.put("content", "您好，这是一条连通性测试消息，请只回复“测试成功”。");
        return chatOnce(upstreamConfig, Collections.<Map<String, Object>>singletonList(userMessage));
    }

    private String chat(AiDevice device, ChatRequest request, UpstreamChatConfig upstreamConfig) {
        return chatOnce(device, request, upstreamConfig, request.getMessages(), request.getTemperature());
    }

    private String chatOnce(UpstreamChatConfig upstreamConfig,
                            List<Map<String, Object>> messages) {
        return chatOnce(upstreamConfig, messages, null);
    }

    private String chatOnce(UpstreamChatConfig upstreamConfig,
                            List<Map<String, Object>> messages,
                            Double temperature) {
        return chatOnce(null, null, upstreamConfig, messages, temperature);
    }

    private String chatOnce(AiDevice device,
                            ChatRequest request,
                            UpstreamChatConfig upstreamConfig,
                            List<Map<String, Object>> messages,
                            Double temperature) {
        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("model", upstreamConfig.getModel());
        payload.put("messages", messages);
        payload.put("stream", Boolean.FALSE);
        if (temperature != null) {
            payload.put("temperature", temperature);
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(upstreamConfig.getApiKey());
            ResponseEntity<JsonNode> responseEntity = restTemplate.postForEntity(
                upstreamConfig.getBaseUrl() + "/chat/completions",
                new HttpEntity<Map<String, Object>>(payload, headers),
                JsonNode.class
            );
            JsonNode response = responseEntity.getBody();

            if (response == null) {
                throw new BusinessException("AI 响应为空");
            }
            JsonNode contentNode = response.path("choices").path(0).path("message").path("content");
            String responseText = contentNode.isMissingNode() ? response.toString() : contentNode.asText();
            auditService.saveSystemLog(
                device,
                "ai_proxy",
                "ai",
                "chat",
                buildChatLogPayload(request, upstreamConfig, payload, responseText, null, response.toString()),
                true
            );
            return responseText;
        } catch (HttpStatusCodeException ex) {
            auditService.saveSystemLog(
                device,
                "ai_proxy",
                "ai",
                "chat",
                buildChatLogPayload(request, upstreamConfig, payload, null, buildUpstreamErrorMessage("AI", ex), ex.getResponseBodyAsString()),
                false
            );
            throw new BusinessException(buildUpstreamErrorMessage("AI", ex));
        } catch (ResourceAccessException ex) {
            auditService.saveSystemLog(
                device,
                "ai_proxy",
                "ai",
                "chat",
                buildChatLogPayload(request, upstreamConfig, payload, null, buildUpstreamRequestErrorMessage("AI", ex), null),
                false
            );
            throw new BusinessException(buildUpstreamRequestErrorMessage("AI", ex));
        } catch (RuntimeException ex) {
            auditService.saveSystemLog(
                device,
                "ai_proxy",
                "ai",
                "chat",
                buildChatLogPayload(request, upstreamConfig, payload, null, ex.getMessage(), null),
                false
            );
            throw new BusinessException("AI调用失败：" + (StringUtils.hasText(ex.getMessage()) ? ex.getMessage() : "未知异常"));
        }
    }

    public SseEmitter chatStream(AiDevice device, ChatRequest request) {
        UpstreamChatConfig upstreamConfig = resolveChatConfig(device, request);
        SseEmitter emitter = new SseEmitter(120000L);
        new Thread(() -> streamChat(device, request, upstreamConfig, emitter), "ai-chat-stream").start();
        return emitter;
    }

    private void streamChat(AiDevice device, ChatRequest request, UpstreamChatConfig upstreamConfig, SseEmitter emitter) {
        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("model", upstreamConfig.getModel());
        payload.put("messages", request.getMessages());
        payload.put("stream", Boolean.TRUE);
        if (request.getTemperature() != null) {
            payload.put("temperature", request.getTemperature());
        }

        StringBuilder responseTextBuilder = new StringBuilder();
        try {
            restTemplate.execute(
                upstreamConfig.getBaseUrl() + "/chat/completions",
                HttpMethod.POST,
                requestCallback -> {
                    requestCallback.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                    requestCallback.getHeaders().setAccept(Collections.singletonList(MediaType.TEXT_EVENT_STREAM));
                    requestCallback.getHeaders().setBearerAuth(upstreamConfig.getApiKey());
                    objectMapper.writeValue(requestCallback.getBody(), payload);
                },
                response -> {
                    StreamForwardResult result = forwardSseBody(response.getBody(), emitter, responseTextBuilder);
                    auditService.saveSystemLog(
                        device,
                        "ai_proxy",
                        "ai",
                        "chat_stream",
                        buildChatLogPayload(request, upstreamConfig, payload, result.responseText, result.errorMessage, null),
                        !StringUtils.hasText(result.errorMessage)
                    );
                    return null;
                }
            );
        } catch (HttpStatusCodeException ex) {
            String errorMessage = buildUpstreamErrorMessage("AI", ex);
            auditService.saveSystemLog(
                device,
                "ai_proxy",
                "ai",
                "chat_stream",
                buildChatLogPayload(request, upstreamConfig, payload, responseTextBuilder.toString(), errorMessage, ex.getResponseBodyAsString()),
                false
            );
            sendErrorFrame(emitter, errorMessage);
        } catch (ResourceAccessException ex) {
            String errorMessage = buildUpstreamRequestErrorMessage("AI", ex);
            auditService.saveSystemLog(
                device,
                "ai_proxy",
                "ai",
                "chat_stream",
                buildChatLogPayload(request, upstreamConfig, payload, responseTextBuilder.toString(), errorMessage, null),
                false
            );
            sendErrorFrame(emitter, errorMessage);
        } catch (Exception ex) {
            String errorMessage = "AI流式响应封装失败：" + (StringUtils.hasText(ex.getMessage()) ? ex.getMessage() : "未知异常");
            auditService.saveSystemLog(
                device,
                "ai_proxy",
                "ai",
                "chat_stream",
                buildChatLogPayload(request, upstreamConfig, payload, responseTextBuilder.toString(), errorMessage, null),
                false
            );
            sendErrorFrame(emitter, errorMessage);
        }
    }

    private StreamForwardResult forwardSseBody(InputStream bodyStream,
                                               SseEmitter emitter,
                                               StringBuilder responseTextBuilder) throws IOException {
        if (bodyStream == null) {
            throw new BusinessException("AI 流式响应为空");
        }

        boolean receivedDone = false;
        String errorMessage = null;
        BufferedReader reader = new BufferedReader(new InputStreamReader(bodyStream, StandardCharsets.UTF_8));
        String line;
        while ((line = reader.readLine()) != null) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (!trimmed.startsWith("data:")) {
                continue;
            }
            String data = trimmed.substring(5).trim();
            emitter.send(SseEmitter.event().data(data));
            String content = extractSseContent(data);
            if (content != null) {
                responseTextBuilder.append(content);
            }
            String currentErrorMessage = extractSseErrorMessage(data);
            if (StringUtils.hasText(currentErrorMessage)) {
                errorMessage = currentErrorMessage;
            }
            if ("[DONE]".equals(data)) {
                receivedDone = true;
                break;
            }
        }

        if (!receivedDone) {
            throw new BusinessException("AI 流式响应未正常结束");
        }
        emitter.complete();
        return new StreamForwardResult(responseTextBuilder.toString(), errorMessage);
    }

    private void sendErrorFrame(SseEmitter emitter, String message) {
        try {
            Map<String, Object> errorPayload = new HashMap<String, Object>();
            Map<String, String> error = new HashMap<String, String>();
            error.put("message", message);
            errorPayload.put("error", error);
            emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(errorPayload)));
            emitter.send(SseEmitter.event().data("[DONE]"));
            emitter.complete();
        } catch (Exception sendError) {
            emitter.completeWithError(sendError);
        }
    }

    public String transcribe(AiDevice device, SpeechRequest request) {
        return proxySpeech(device, request, "transcribe");
    }

    public String realtime(AiDevice device, SpeechRequest request) {
        return proxySpeech(device, request, "realtime");
    }

    private String proxySpeech(AiDevice device, SpeechRequest request, String action) {
        ResolvedAiConfig config = configService.resolveByDevice(device);
        if (!StringUtils.hasText(config.getAudioBaseUrl())) {
            throw new BusinessException("未配置语音服务地址");
        }
        PreparedSpeechFile preparedFile = prepareSpeechFile(request);
        String audioModel = resolveAudioModel(config);

        try {
            MultiValueMap<String, Object> formData = new LinkedMultiValueMap<String, Object>();
            formData.add("file", new ByteArrayResource(preparedFile.audioBytes) {
                @Override
                public String getFilename() {
                    return preparedFile.fileName;
                }
            });
            formData.add("model", audioModel);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.setBearerAuth(config.getApiKey());

            ResponseEntity<JsonNode> responseEntity = restTemplate.postForEntity(
                config.getAudioBaseUrl() + "/audio/transcriptions",
                new HttpEntity<MultiValueMap<String, Object>>(formData, headers),
                JsonNode.class
            );
            JsonNode response = responseEntity.getBody();

            if (response == null) {
                throw new BusinessException("语音转写响应为空");
            }

            JsonNode textNode = response.path("text");
            String text = textNode.isMissingNode() ? response.toString() : textNode.asText();
            auditService.saveSystemLogWithAudioFile(
                device,
                "speech_proxy",
                "speech",
                action,
                buildSpeechLogPayload(request, preparedFile, config.getAudioBaseUrl(), audioModel, text, true, null, response.toString()),
                true,
                preparedFile.audioBytes,
                preparedFile.fileName
            );
            return text;
        } catch (HttpStatusCodeException ex) {
            String errorMessage = buildUpstreamErrorMessage("语音服务", ex);
            auditService.saveSystemLogWithAudioFile(
                device,
                "speech_proxy",
                "speech",
                action,
                buildSpeechLogPayload(request, preparedFile, config.getAudioBaseUrl(), audioModel, null, false, errorMessage, ex.getResponseBodyAsString()),
                false,
                preparedFile.audioBytes,
                preparedFile.fileName
            );
            throw new BusinessException(errorMessage);
        } catch (ResourceAccessException ex) {
            String errorMessage = buildUpstreamRequestErrorMessage("语音服务", ex);
            auditService.saveSystemLogWithAudioFile(
                device,
                "speech_proxy",
                "speech",
                action,
                buildSpeechLogPayload(request, preparedFile, config.getAudioBaseUrl(), audioModel, null, false, errorMessage, null),
                false,
                preparedFile.audioBytes,
                preparedFile.fileName
            );
            throw new BusinessException(errorMessage);
        } catch (RuntimeException ex) {
            auditService.saveSystemLogWithAudioFile(
                device,
                "speech_proxy",
                "speech",
                action,
                buildSpeechLogPayload(request, preparedFile, config.getAudioBaseUrl(), audioModel, null, false, ex.getMessage(), null),
                false,
                preparedFile.audioBytes,
                preparedFile.fileName
            );
            throw ex;
        }
    }

    private String buildUpstreamErrorMessage(String serviceName, HttpStatusCodeException ex) {
        String upstreamMessage = extractUpstreamErrorMessage(ex.getResponseBodyAsString());
        String statusPart = ex.getRawStatusCode() > 0 ? "（" + ex.getRawStatusCode() + "）" : "";
        if (StringUtils.hasText(upstreamMessage)) {
            return serviceName + "调用失败" + statusPart + "：" + upstreamMessage;
        }
        return serviceName + "调用失败" + statusPart + "：" + ex.getStatusText();
    }

    private String buildUpstreamRequestErrorMessage(String serviceName, ResourceAccessException ex) {
        String reason = StringUtils.hasText(ex.getMessage()) ? ex.getMessage() : "网络连接异常";
        return serviceName + "调用失败：网络连接异常，" + reason;
    }

    private String extractUpstreamErrorMessage(String responseBody) {
        if (!StringUtils.hasText(responseBody)) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode errorNode = root.path("error");
            if (errorNode.isObject()) {
                JsonNode messageNode = errorNode.path("message");
                if (messageNode.isTextual() && StringUtils.hasText(messageNode.asText())) {
                    return messageNode.asText();
                }
            }
            JsonNode messageNode = root.path("message");
            if (messageNode.isTextual() && StringUtils.hasText(messageNode.asText())) {
                return messageNode.asText();
            }
            return responseBody;
        } catch (Exception ignore) {
            return responseBody;
        }
    }

    private String trimRightSlash(String value) {
        return value == null ? null : value.replaceAll("/+$", "");
    }

    private PreparedSpeechFile prepareSpeechFile(SpeechRequest request) {
        byte[] sourceAudioBytes = decodeAudio(request.getAudio());
        String sourceMimeType = resolveMimeType(request);
        String sourceFileName = resolveFileName(request);

        if (isPcmRequest(request, sourceMimeType)) {
            return new PreparedSpeechFile(
                sourceAudioBytes,
                wrapPcmAsWav(sourceAudioBytes, 16000, (short) 1, (short) 16),
                sourceMimeType,
                "audio/wav",
                sourceFileName,
                replaceExtension(sourceFileName, ".wav"),
                true
            );
        }

        return new PreparedSpeechFile(
            sourceAudioBytes,
            sourceAudioBytes,
            sourceMimeType,
            sourceMimeType,
            sourceFileName,
            sourceFileName,
            false
        );
    }

    private byte[] decodeAudio(String base64Audio) {
        try {
            return Base64.getDecoder().decode(base64Audio);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("音频内容不是合法的 Base64");
        }
    }

    private String resolveMimeType(SpeechRequest request) {
        if (StringUtils.hasText(request.getMimeType())) {
            return request.getMimeType().trim();
        }
        if ("pcm".equalsIgnoreCase(request.getFormat())) {
            return "audio/pcm";
        }
        return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }

    private MediaType resolveMediaType(String mimeType) {
        try {
            return MediaType.parseMediaType(mimeType);
        } catch (IllegalArgumentException ex) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    private String resolveAudioModel(ResolvedAiConfig config) {
        return StringUtils.hasText(config.getAudioModel()) ? config.getAudioModel().trim() : "whisper-1";
    }

    private String resolveFileName(SpeechRequest request) {
        if (StringUtils.hasText(request.getFileName())) {
            return request.getFileName().trim();
        }
        String scene = StringUtils.hasText(request.getScene()) ? request.getScene().trim() : "speech";
        return scene + "-" + System.currentTimeMillis() + resolveExtension(request);
    }

    private String resolveExtension(SpeechRequest request) {
        String mimeType = resolveMimeType(request);
        if ("audio/webm".equalsIgnoreCase(mimeType)) {
            return ".webm";
        }
        if ("audio/wav".equalsIgnoreCase(mimeType) || "audio/wave".equalsIgnoreCase(mimeType)) {
            return ".wav";
        }
        if ("audio/pcm".equalsIgnoreCase(mimeType) || "pcm".equalsIgnoreCase(request.getFormat())) {
            return ".pcm";
        }
        if ("audio/ogg".equalsIgnoreCase(mimeType)) {
            return ".ogg";
        }
        if ("audio/mpeg".equalsIgnoreCase(mimeType)) {
            return ".mp3";
        }
        if ("audio/mp4".equalsIgnoreCase(mimeType)) {
            return ".m4a";
        }
        return ".bin";
    }

    private boolean isPcmRequest(SpeechRequest request, String mimeType) {
        return "pcm".equalsIgnoreCase(request.getFormat()) || "audio/pcm".equalsIgnoreCase(mimeType);
    }

    private byte[] wrapPcmAsWav(byte[] pcmBytes, int sampleRate, short channels, short bitsPerSample) {
        int byteRate = sampleRate * channels * bitsPerSample / 8;
        short blockAlign = (short) (channels * bitsPerSample / 8);
        ByteBuffer buffer = ByteBuffer.allocate(44 + pcmBytes.length).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put("RIFF".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(36 + pcmBytes.length);
        buffer.put("WAVE".getBytes(StandardCharsets.UTF_8));
        buffer.put("fmt ".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(16);
        buffer.putShort((short) 1);
        buffer.putShort(channels);
        buffer.putInt(sampleRate);
        buffer.putInt(byteRate);
        buffer.putShort(blockAlign);
        buffer.putShort(bitsPerSample);
        buffer.put("data".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(pcmBytes.length);
        buffer.put(pcmBytes);
        return buffer.array();
    }

    private String replaceExtension(String fileName, String extension) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex <= 0) {
            return fileName + extension;
        }
        return fileName.substring(0, dotIndex) + extension;
    }

    private Map<String, Object> buildChatLogPayload(ChatRequest request,
                                                    UpstreamChatConfig upstreamConfig,
                                                    Map<String, Object> requestBody,
                                                    String responseText,
                                                    String errorMessage,
                                                    String upstreamBody) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("traceId", request == null ? null : request.getTraceId());
        payload.put("scene", request == null ? null : request.getScene());
        payload.put("sourceModule", request == null ? null : request.getSourceModule());
        payload.put("sessionId", request == null ? null : request.getSessionId());
        payload.put("configProfile", request == null ? null : request.getConfigProfile());
        payload.put("baseUrl", upstreamConfig.getBaseUrl());
        payload.put("model", upstreamConfig.getModel());
        payload.put("requestBody", requestBody);
        payload.put("responseText", responseText);
        payload.put("upstreamBody", upstreamBody);
        payload.put("errorMessage", errorMessage);
        return payload;
    }

    private Map<String, Object> buildSpeechLogPayload(SpeechRequest request,
                                                      PreparedSpeechFile preparedFile,
                                                      String audioBaseUrl,
                                                      String audioModel,
                                                      String responseText,
                                                      boolean success,
                                                      String errorMessage,
                                                      String upstreamBody) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        Map<String, Object> requestBody = new LinkedHashMap<String, Object>();
        requestBody.put("traceId", request.getTraceId());
        requestBody.put("sourceModule", request.getSourceModule());
        requestBody.put("sessionId", request.getSessionId());
        requestBody.put("mimeType", request.getMimeType());
        requestBody.put("format", request.getFormat());
        requestBody.put("fileName", request.getFileName());
        requestBody.put("scene", request.getScene());
        requestBody.put("sourceAudioSize", preparedFile.sourceAudioBytes.length);
        payload.put("traceId", request.getTraceId());
        payload.put("sourceModule", request.getSourceModule());
        payload.put("sessionId", request.getSessionId());
        payload.put("scene", request.getScene());
        payload.put("baseUrl", audioBaseUrl);
        payload.put("requestBody", requestBody);
        payload.put("preparedFile", buildPreparedFilePayload(preparedFile));
        payload.put("audioModel", audioModel);
        payload.put("responseText", responseText);
        payload.put("upstreamBody", upstreamBody);
        payload.put("success", success);
        payload.put("errorMessage", errorMessage);
        return payload;
    }

    private Map<String, Object> buildPreparedFilePayload(PreparedSpeechFile preparedFile) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("sourceMimeType", preparedFile.sourceMimeType);
        payload.put("uploadMimeType", preparedFile.mimeType);
        payload.put("sourceFileName", preparedFile.sourceFileName);
        payload.put("uploadFileName", preparedFile.fileName);
        payload.put("sourceAudioSize", preparedFile.sourceAudioBytes.length);
        payload.put("uploadAudioSize", preparedFile.audioBytes.length);
        payload.put("normalizedToWav", preparedFile.normalizedToWav);
        return payload;
    }

    private String extractSseContent(String data) {
        if (!StringUtils.hasText(data) || "[DONE]".equals(data)) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(data);
            JsonNode contentNode = root.path("choices").path(0).path("delta").path("content");
            if (contentNode.isTextual()) {
                return contentNode.asText();
            }
        } catch (Exception ignore) {
        }
        return null;
    }

    private String extractSseErrorMessage(String data) {
        if (!StringUtils.hasText(data) || "[DONE]".equals(data)) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(data);
            JsonNode errorNode = root.path("error");
            if (errorNode.isObject()) {
                JsonNode messageNode = errorNode.path("message");
                if (messageNode.isTextual() && StringUtils.hasText(messageNode.asText())) {
                    return messageNode.asText();
                }
            }
        } catch (Exception ignore) {
        }
        return null;
    }

    private UpstreamChatConfig resolveChatConfig(AiDevice device, ChatRequest request) {
        ResolvedAiConfig resolved = configService.resolveByDevice(device);
        String profile = request == null ? null : request.getConfigProfile();
        if ("reviewer".equalsIgnoreCase(profile)) {
            if (!Boolean.TRUE.equals(resolved.getReviewerEnabled())) {
                throw new BusinessException("当前设备未开启独立审查 AI");
            }
            return new UpstreamChatConfig(
                StringUtils.hasText(resolved.getReviewerBaseUrl()) ? resolved.getReviewerBaseUrl() : resolved.getBaseUrl(),
                StringUtils.hasText(resolved.getReviewerApiKey()) ? resolved.getReviewerApiKey() : resolved.getApiKey(),
                StringUtils.hasText(resolved.getReviewerModel()) ? resolved.getReviewerModel() : resolved.getModel()
            );
        }
        return new UpstreamChatConfig(
            resolved.getBaseUrl(),
            resolved.getApiKey(),
            resolved.getModel()
        );
    }

    private static class UpstreamChatConfig {

        private final String baseUrl;
        private final String apiKey;
        private final String model;

        private UpstreamChatConfig(String baseUrl, String apiKey, String model) {
            if (!StringUtils.hasText(baseUrl)) {
                throw new BusinessException("未配置 AI 服务地址");
            }
            if (!StringUtils.hasText(apiKey)) {
                throw new BusinessException("未配置 AI 服务密钥");
            }
            if (!StringUtils.hasText(model)) {
                throw new BusinessException("未配置 AI 模型");
            }
            this.baseUrl = baseUrl;
            this.apiKey = apiKey;
            this.model = model;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public String getModel() {
            return model;
        }
    }

    private static final class StreamForwardResult {

        private final String responseText;
        private final String errorMessage;

        private StreamForwardResult(String responseText, String errorMessage) {
            this.responseText = responseText;
            this.errorMessage = errorMessage;
        }
    }

    private static final class PreparedSpeechFile {

        private final byte[] sourceAudioBytes;
        private final byte[] audioBytes;
        private final String sourceMimeType;
        private final String mimeType;
        private final String sourceFileName;
        private final String fileName;
        private final boolean normalizedToWav;

        private PreparedSpeechFile(byte[] sourceAudioBytes,
                                   byte[] audioBytes,
                                   String sourceMimeType,
                                   String mimeType,
                                   String sourceFileName,
                                   String fileName,
                                   boolean normalizedToWav) {
            this.sourceAudioBytes = sourceAudioBytes;
            this.audioBytes = audioBytes;
            this.sourceMimeType = sourceMimeType;
            this.mimeType = mimeType;
            this.sourceFileName = sourceFileName;
            this.fileName = fileName;
            this.normalizedToWav = normalizedToWav;
        }
    }
}
