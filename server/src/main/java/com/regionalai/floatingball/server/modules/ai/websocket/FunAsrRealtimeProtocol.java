package com.regionalai.floatingball.server.modules.ai.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

final class FunAsrRealtimeProtocol {

    private final StringBuilder finalizedText = new StringBuilder();
    private final StringBuilder onlineText = new StringBuilder();
    private boolean finishRequested;

    Map<String, Object> startPayload() {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("mode", "2pass");
        payload.put("chunk_size", Arrays.asList(5, 10, 5));
        payload.put("chunk_interval", 10);
        payload.put("wav_name", "microphone");
        payload.put("wav_format", "pcm");
        payload.put("is_speaking", true);
        payload.put("hotwords", "");
        payload.put("itn", true);
        return payload;
    }

    Map<String, Object> finishPayload() {
        finishRequested = true;
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("is_speaking", false);
        return payload;
    }

    Result accept(JsonNode root) {
        String errorMessage = resolveError(root);
        if (StringUtils.hasText(errorMessage)) {
            return Result.error(errorMessage);
        }

        String mode = root.path("mode").asText("").trim().toLowerCase();
        String text = root.path("text").asText("");
        boolean finalSignal = root.path("is_final").asBoolean(false);
        boolean offlineResult = "2pass-offline".equals(mode) || "offline".equals(mode);

        if (offlineResult) {
            onlineText.setLength(0);
            finalizedText.append(text);
            return new Result(text, true, finalSignal || finishRequested, null);
        }

        if (StringUtils.hasText(text)) {
            onlineText.append(text);
        }
        if (finalSignal) {
            String completed = onlineText.toString();
            finalizedText.append(completed);
            onlineText.setLength(0);
            return new Result(completed, true, true, null);
        }
        return new Result(onlineText.toString(), false, false, null);
    }

    String getFullText() {
        return finalizedText.toString() + onlineText.toString();
    }

    private String resolveError(JsonNode root) {
        JsonNode code = root.path("code");
        if (!code.isMissingNode() && !code.isNull()) {
            String value = code.asText("");
            if (StringUtils.hasText(value) && !"0".equals(value) && !"200".equals(value)) {
                return root.path("message").asText(root.path("error").asText("FunASR 实时语音识别失败"));
            }
        }
        String error = root.path("error").asText("");
        return StringUtils.hasText(error) ? error : null;
    }

    static final class Result {

        private final String text;
        private final boolean sentenceEnd;
        private final boolean finalSignal;
        private final String errorMessage;

        private Result(String text, boolean sentenceEnd, boolean finalSignal, String errorMessage) {
            this.text = text;
            this.sentenceEnd = sentenceEnd;
            this.finalSignal = finalSignal;
            this.errorMessage = errorMessage;
        }

        static Result error(String message) {
            return new Result(null, false, false, message);
        }

        String getText() {
            return text;
        }

        boolean isSentenceEnd() {
            return sentenceEnd;
        }

        boolean isFinalSignal() {
            return finalSignal;
        }

        String getErrorMessage() {
            return errorMessage;
        }
    }
}
