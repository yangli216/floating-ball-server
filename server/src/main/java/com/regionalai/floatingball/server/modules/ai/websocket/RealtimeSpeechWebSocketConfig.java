package com.regionalai.floatingball.server.modules.ai.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class RealtimeSpeechWebSocketConfig implements WebSocketConfigurer {

    private final RealtimeSpeechWebSocketHandler realtimeSpeechWebSocketHandler;
    private final RealtimeSpeechHandshakeInterceptor realtimeSpeechHandshakeInterceptor;

    public RealtimeSpeechWebSocketConfig(RealtimeSpeechWebSocketHandler realtimeSpeechWebSocketHandler,
                                         RealtimeSpeechHandshakeInterceptor realtimeSpeechHandshakeInterceptor) {
        this.realtimeSpeechWebSocketHandler = realtimeSpeechWebSocketHandler;
        this.realtimeSpeechHandshakeInterceptor = realtimeSpeechHandshakeInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(realtimeSpeechWebSocketHandler, "/v1/ai/speech/realtime/ws")
            .addInterceptors(realtimeSpeechHandshakeInterceptor)
            .setAllowedOriginPatterns("*");
    }
}
