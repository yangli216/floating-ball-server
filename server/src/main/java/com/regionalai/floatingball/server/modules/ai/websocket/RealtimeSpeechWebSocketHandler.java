package com.regionalai.floatingball.server.modules.ai.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.regionalai.floatingball.server.modules.config.dto.ResolvedAiConfig;
import com.regionalai.floatingball.server.modules.config.service.ConfigService;
import com.regionalai.floatingball.server.modules.device.entity.AiDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class RealtimeSpeechWebSocketHandler extends AbstractWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(RealtimeSpeechWebSocketHandler.class);

    private static final String ALIYUN_SPEECH_PROVIDER = "aliyun-dashscope";
    private static final String DEFAULT_REALTIME_MODEL = "paraformer-realtime-v2";
    private static final String DEFAULT_DASHSCOPE_WS_URL = "wss://dashscope.aliyuncs.com/api-ws/v1/inference";

    private final ConfigService configService;
    private final ObjectMapper objectMapper;
    private final StandardWebSocketClient webSocketClient;

    public RealtimeSpeechWebSocketHandler(ConfigService configService, ObjectMapper objectMapper) {
        this.configService = configService;
        this.objectMapper = objectMapper;
        this.webSocketClient = new StandardWebSocketClient();
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        AiDevice device = (AiDevice) session.getAttributes().get(RealtimeSpeechHandshakeInterceptor.DEVICE_ATTRIBUTE);
        if (device == null) {
            log.warn("realtime speech ws: connection rejected, no device attribute. sessionId={}", session.getId());
            sendErrorAndClose(session, "设备令牌无效或已停用");
            return;
        }

        ResolvedAiConfig config = configService.resolveByDevice(device);
        if (!ALIYUN_SPEECH_PROVIDER.equalsIgnoreCase(config.getSpeechProvider())) {
            log.warn("realtime speech ws: connection rejected, speech provider not dashscope. deviceId={}, provider={}", device.getIdDevice(), config.getSpeechProvider());
            sendErrorAndClose(session, "当前语音提供方未启用 DashScope 实时识别");
            return;
        }

        String apiKey = resolveAudioApiKey(config);
        if (!StringUtils.hasText(apiKey)) {
            log.warn("realtime speech ws: connection rejected, no audio api key. deviceId={}", device.getIdDevice());
            sendErrorAndClose(session, "未配置语音服务密钥");
            return;
        }

        log.info("realtime speech ws: connection established. deviceId={}, model={}", device.getIdDevice(), resolveRealtimeModel(config));
        RealtimeProxySession proxySession = new RealtimeProxySession(session, config);
        session.getAttributes().put(RealtimeProxySession.ATTRIBUTE, proxySession);
        proxySession.connect(apiKey);
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        RealtimeProxySession proxySession = resolveProxySession(session);
        if (proxySession == null) {
            return;
        }
        ByteBuffer payload = message.getPayload();
        byte[] bytes = new byte[payload.remaining()];
        payload.get(bytes);
        proxySession.forwardAudio(bytes);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        RealtimeProxySession proxySession = resolveProxySession(session);
        if (proxySession == null) {
            return;
        }
        try {
            JsonNode root = objectMapper.readTree(message.getPayload());
            String type = root.path("type").asText();
            if ("finish".equals(type)) {
                proxySession.finish();
            }
        } catch (Exception ex) {
            proxySession.sendError("实时语音控制消息解析失败：" + ex.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("realtime speech ws: connection closed. sessionId={}, status={}", session.getId(), status);
        RealtimeProxySession proxySession = resolveProxySession(session);
        if (proxySession != null) {
            proxySession.closeDashScope();
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("realtime speech ws: transport error. sessionId={}, error={}", session.getId(), exception.getMessage());
        RealtimeProxySession proxySession = resolveProxySession(session);
        if (proxySession != null) {
            proxySession.sendError("实时语音连接异常：" + exception.getMessage());
            proxySession.closeDashScope();
        }
    }

    private RealtimeProxySession resolveProxySession(WebSocketSession session) {
        return (RealtimeProxySession) session.getAttributes().get(RealtimeProxySession.ATTRIBUTE);
    }

    private String resolveAudioApiKey(ResolvedAiConfig config) {
        return StringUtils.hasText(config.getAudioApiKey()) ? config.getAudioApiKey() : config.getApiKey();
    }

    private String resolveRealtimeModel(ResolvedAiConfig config) {
        String model = StringUtils.hasText(config.getSpeechModel()) ? config.getSpeechModel().trim() : DEFAULT_REALTIME_MODEL;
        String lowerModel = model.toLowerCase();
        if ((lowerModel.startsWith("fun-asr") && lowerModel.contains("realtime"))
            || lowerModel.startsWith("paraformer-realtime")
            || "gummy-realtime-v1".equals(lowerModel)
            || "gummy-chat-v1".equals(lowerModel)) {
            return model;
        }
        return DEFAULT_REALTIME_MODEL;
    }

    private String resolveDashScopeWsUrl(ResolvedAiConfig config) {
        String baseUrl = config.getAudioBaseUrl();
        if (StringUtils.hasText(baseUrl) && baseUrl.trim().toLowerCase().startsWith("wss://")) {
            return baseUrl.trim().replaceAll("/+$", "");
        }
        return DEFAULT_DASHSCOPE_WS_URL;
    }

    private void sendErrorAndClose(WebSocketSession session, String message) {
        try {
            sendJson(session, errorPayload(message));
            session.close(CloseStatus.SERVER_ERROR);
        } catch (IOException ex) {
            log.debug("realtime speech ws: failed to send error and close session. sessionId={}, error={}", session.getId(), ex.getMessage());
        }
    }

    private Map<String, Object> errorPayload(String message) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("type", "error");
        payload.put("message", message);
        return payload;
    }

    private void sendJson(WebSocketSession session, Map<String, Object> payload) throws IOException {
        if (session.isOpen()) {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
        }
    }

    private final class RealtimeProxySession {

        private static final String ATTRIBUTE = "realtimeSpeechProxySession";

        private final WebSocketSession clientSession;
        private final ResolvedAiConfig config;
        private final String taskId;
        private final List<byte[]> audioBuffer = new ArrayList<byte[]>();
        private final StringBuilder fullText = new StringBuilder();
        private WebSocketSession dashScopeSession;
        private boolean taskStarted;
        private boolean finishRequested;
        private boolean closed;

        private RealtimeProxySession(WebSocketSession clientSession, ResolvedAiConfig config) {
            this.clientSession = clientSession;
            this.config = config;
            this.taskId = UUID.randomUUID().toString().replace("-", "");
        }

        private void connect(String apiKey) {
            try {
                WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
                headers.setBearerAuth(apiKey);
                String endpoint = resolveDashScopeWsUrl(config);
                WebSocketHandler dashScopeHandler = new DashScopeWebSocketHandler(this);
                webSocketClient.doHandshake(dashScopeHandler, headers, URI.create(endpoint))
                    .addCallback(this::onDashScopeConnected, this::onDashScopeConnectFailed);
            } catch (RuntimeException ex) {
                sendError("DashScope 实时语音连接创建失败：" + ex.getMessage());
            }
        }

        private synchronized void onDashScopeConnected(WebSocketSession session) {
            this.dashScopeSession = session;
            try {
                sendRunTask();
            } catch (IOException ex) {
                sendError("DashScope run-task 发送失败：" + ex.getMessage());
            }
        }

        private void onDashScopeConnectFailed(Throwable error) {
            sendError("DashScope 实时语音连接失败：" + error.getMessage());
        }

        private synchronized void forwardAudio(byte[] bytes) {
            if (closed) {
                return;
            }
            if (!taskStarted || dashScopeSession == null || !dashScopeSession.isOpen()) {
                audioBuffer.add(bytes);
                return;
            }
            sendDashScopeBinary(bytes);
        }

        private synchronized void finish() {
            finishRequested = true;
            if (taskStarted && dashScopeSession != null && dashScopeSession.isOpen()) {
                sendFinishTask();
            }
        }

        private synchronized void closeDashScope() {
            closed = true;
            if (dashScopeSession != null && dashScopeSession.isOpen()) {
                try {
                    dashScopeSession.close();
                } catch (IOException ex) {
                    log.debug("realtime speech ws: failed to close dashscope session. error={}", ex.getMessage());
                }
            }
        }

        private synchronized void onDashScopeMessage(String payload) {
            try {
                JsonNode root = objectMapper.readTree(payload);
                JsonNode header = root.path("header");
                String event = header.path("event").asText();
                if ("task-started".equals(event)) {
                    taskStarted = true;
                    flushAudioBuffer();
                    if (finishRequested) {
                        sendFinishTask();
                    }
                    return;
                }
                if ("result-generated".equals(event)) {
                    JsonNode sentence = root.path("payload").path("output").path("sentence");
                    String text = sentence.path("text").asText("");
                    boolean sentenceEnd = sentence.path("sentence_end").asBoolean(false);
                    if (sentenceEnd) {
                        fullText.append(text);
                    }
                    sendText(text, sentenceEnd);
                    return;
                }
                if ("task-finished".equals(event)) {
                    sendFinal();
                    closeDashScope();
                    return;
                }
                if ("task-failed".equals(event)) {
                    String message = header.path("error_message").asText(header.path("message").asText("DashScope 实时语音识别失败"));
                    sendError(message);
                    closeDashScope();
                }
            } catch (Exception ex) {
                sendError("DashScope 实时语音响应解析失败：" + ex.getMessage());
            }
        }

        private void onDashScopeClosed() {
            if (!closed && clientSession.isOpen()) {
                sendFinal();
            }
        }

        private void sendRunTask() throws IOException {
            Map<String, Object> header = new LinkedHashMap<String, Object>();
            header.put("action", "run-task");
            header.put("task_id", taskId);
            header.put("streaming", "duplex");

            Map<String, Object> parameters = new LinkedHashMap<String, Object>();
            parameters.put("format", "pcm");
            parameters.put("sample_rate", 16000);

            Map<String, Object> body = new LinkedHashMap<String, Object>();
            body.put("task_group", "audio");
            body.put("task", "asr");
            body.put("function", "recognition");
            body.put("model", resolveRealtimeModel(config));
            body.put("parameters", parameters);
            body.put("input", Collections.emptyMap());

            Map<String, Object> message = new LinkedHashMap<String, Object>();
            message.put("header", header);
            message.put("payload", body);
            dashScopeSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
        }

        private void sendFinishTask() {
            try {
                Map<String, Object> header = new LinkedHashMap<String, Object>();
                header.put("action", "finish-task");
                header.put("task_id", taskId);
                header.put("streaming", "duplex");

                Map<String, Object> body = new LinkedHashMap<String, Object>();
                body.put("input", Collections.emptyMap());

                Map<String, Object> message = new LinkedHashMap<String, Object>();
                message.put("header", header);
                message.put("payload", body);
                dashScopeSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
            } catch (IOException ex) {
                sendError("DashScope finish-task 发送失败：" + ex.getMessage());
            }
        }

        private void flushAudioBuffer() {
            for (byte[] bytes : audioBuffer) {
                sendDashScopeBinary(bytes);
            }
            audioBuffer.clear();
        }

        private void sendDashScopeBinary(byte[] bytes) {
            try {
                dashScopeSession.sendMessage(new BinaryMessage(bytes));
            } catch (IOException ex) {
                sendError("DashScope 音频帧发送失败：" + ex.getMessage());
            }
        }

        private void sendText(String text, boolean sentenceEnd) {
            Map<String, Object> payload = new LinkedHashMap<String, Object>();
            payload.put("type", "text");
            payload.put("text", text);
            payload.put("isSentenceEnd", sentenceEnd);
            sendClient(payload);
        }

        private void sendFinal() {
            Map<String, Object> payload = new LinkedHashMap<String, Object>();
            payload.put("type", "final");
            payload.put("text", fullText.toString());
            sendClient(payload);
        }

        private void sendError(String message) {
            sendClient(errorPayload(message));
        }

        private void sendClient(Map<String, Object> payload) {
            try {
                sendJson(clientSession, payload);
            } catch (IOException ex) {
                log.debug("realtime speech ws: failed to send client payload. error={}", ex.getMessage());
            }
        }
    }

    private final class DashScopeWebSocketHandler extends AbstractWebSocketHandler {

        private final RealtimeProxySession proxySession;

        private DashScopeWebSocketHandler(RealtimeProxySession proxySession) {
            this.proxySession = proxySession;
        }

        @Override
        public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {
            if (message instanceof TextMessage) {
                proxySession.onDashScopeMessage(((TextMessage) message).getPayload());
            }
        }

        @Override
        public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
            proxySession.onDashScopeClosed();
        }

        @Override
        public void handleTransportError(WebSocketSession session, Throwable exception) {
            proxySession.sendError("DashScope 实时语音连接异常：" + exception.getMessage());
        }
    }
}
