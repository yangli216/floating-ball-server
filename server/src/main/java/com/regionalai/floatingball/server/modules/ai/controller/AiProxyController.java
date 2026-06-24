package com.regionalai.floatingball.server.modules.ai.controller;

import com.regionalai.floatingball.server.common.api.ApiResponse;
import com.regionalai.floatingball.server.common.util.RequestIdUtils;
import com.regionalai.floatingball.server.modules.ai.dto.ChatRequest;
import com.regionalai.floatingball.server.modules.ai.dto.ChatResponse;
import com.regionalai.floatingball.server.modules.ai.dto.SpeechRequest;
import com.regionalai.floatingball.server.modules.ai.dto.SpeechResponse;
import com.regionalai.floatingball.server.modules.ai.service.AiProxyService;
import com.regionalai.floatingball.server.modules.device.entity.AiDevice;
import com.regionalai.floatingball.server.security.DeviceContextHolder;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/v1/ai")
public class AiProxyController {

    private final AiProxyService aiProxyService;

    public AiProxyController(AiProxyService aiProxyService) {
        this.aiProxyService = aiProxyService;
    }

    @PostMapping(value = "/chat", produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_EVENT_STREAM_VALUE })
    public Object chat(@Validated @RequestBody ChatRequest request, HttpServletRequest httpServletRequest) {
        AiDevice device = DeviceContextHolder.get();
        if (Boolean.TRUE.equals(request.getStream())) {
            aiProxyService.validateChatConfig(device, request);
            return aiProxyService.chatStream(device, request);
        }
        return ApiResponse.success(new ChatResponse(aiProxyService.chat(device, request)), RequestIdUtils.resolve(httpServletRequest));
    }

    @PostMapping(value = "/speech/transcribe", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<SpeechResponse> transcribe(@Validated @RequestBody SpeechRequest request,
                                                  HttpServletRequest httpServletRequest) {
        AiDevice device = DeviceContextHolder.get();
        return ApiResponse.success(new SpeechResponse(aiProxyService.transcribe(device, request)), RequestIdUtils.resolve(httpServletRequest));
    }

    @PostMapping(value = "/speech/realtime", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<SpeechResponse> realtime(@Validated @RequestBody SpeechRequest request,
                                                HttpServletRequest httpServletRequest) {
        AiDevice device = DeviceContextHolder.get();
        return ApiResponse.success(new SpeechResponse(aiProxyService.realtime(device, request)), RequestIdUtils.resolve(httpServletRequest));
    }
}
