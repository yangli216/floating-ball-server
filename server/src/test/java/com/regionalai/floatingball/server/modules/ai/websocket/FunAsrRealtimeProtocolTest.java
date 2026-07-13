package com.regionalai.floatingball.server.modules.ai.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FunAsrRealtimeProtocolTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void startAndFinishPayloadsShouldMatchFunAsrWebSocketContract() {
        FunAsrRealtimeProtocol protocol = new FunAsrRealtimeProtocol();

        Map<String, Object> start = protocol.startPayload();
        Map<String, Object> finish = protocol.finishPayload();

        assertEquals("2pass", start.get("mode"));
        assertEquals(Arrays.asList(5, 10, 5), start.get("chunk_size"));
        assertEquals(10, start.get("chunk_interval"));
        assertEquals("pcm", start.get("wav_format"));
        assertEquals(Boolean.TRUE, start.get("is_speaking"));
        assertEquals(Boolean.TRUE, start.get("itn"));
        assertEquals(Boolean.FALSE, finish.get("is_speaking"));
    }

    @Test
    void onlineChunksShouldAccumulateUntilOfflineCorrectionArrives() throws Exception {
        FunAsrRealtimeProtocol protocol = new FunAsrRealtimeProtocol();

        FunAsrRealtimeProtocol.Result first = protocol.accept(objectMapper.readTree(
            "{\"mode\":\"2pass-online\",\"text\":\"患者咳\",\"is_final\":false}"
        ));
        FunAsrRealtimeProtocol.Result second = protocol.accept(objectMapper.readTree(
            "{\"mode\":\"2pass-online\",\"text\":\"嗽\",\"is_final\":false}"
        ));
        FunAsrRealtimeProtocol.Result corrected = protocol.accept(objectMapper.readTree(
            "{\"mode\":\"2pass-offline\",\"text\":\"患者咳嗽。\",\"is_final\":true}"
        ));

        assertEquals("患者咳", first.getText());
        assertFalse(first.isSentenceEnd());
        assertEquals("患者咳嗽", second.getText());
        assertFalse(second.isSentenceEnd());
        assertEquals("患者咳嗽。", corrected.getText());
        assertTrue(corrected.isSentenceEnd());
        assertTrue(corrected.isFinalSignal());
        assertEquals("患者咳嗽。", protocol.getFullText());
    }

    @Test
    void finalOnlineSignalShouldCommitBufferedText() throws Exception {
        FunAsrRealtimeProtocol protocol = new FunAsrRealtimeProtocol();
        protocol.accept(objectMapper.readTree("{\"mode\":\"online\",\"text\":\"发热\"}"));

        FunAsrRealtimeProtocol.Result result = protocol.accept(objectMapper.readTree(
            "{\"mode\":\"online\",\"text\":\"一天\",\"is_final\":true}"
        ));

        assertEquals("发热一天", result.getText());
        assertTrue(result.isSentenceEnd());
        assertTrue(result.isFinalSignal());
        assertEquals("发热一天", protocol.getFullText());
    }

    @Test
    void offlineResponseAfterFinishShouldCloseEvenWhenUpstreamFinalFlagIsFalse() throws Exception {
        FunAsrRealtimeProtocol protocol = new FunAsrRealtimeProtocol();
        protocol.finishPayload();

        FunAsrRealtimeProtocol.Result result = protocol.accept(objectMapper.readTree(
            "{\"is_final\":false,\"mode\":\"offline\",\"text\":\"患者发热一天\",\"wav_name\":\"microphone\"}"
        ));

        assertEquals("患者发热一天", result.getText());
        assertTrue(result.isSentenceEnd());
        assertTrue(result.isFinalSignal());
        assertEquals("患者发热一天", protocol.getFullText());
    }

    @Test
    void upstreamErrorShouldBeReturnedWithoutText() throws Exception {
        FunAsrRealtimeProtocol protocol = new FunAsrRealtimeProtocol();

        FunAsrRealtimeProtocol.Result result = protocol.accept(objectMapper.readTree(
            "{\"code\":500,\"message\":\"模型加载失败\"}"
        ));

        assertEquals("模型加载失败", result.getErrorMessage());
        assertNull(result.getText());
        assertFalse(result.isFinalSignal());
    }
}
