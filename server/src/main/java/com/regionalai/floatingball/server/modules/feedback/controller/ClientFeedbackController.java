package com.regionalai.floatingball.server.modules.feedback.controller;

import com.regionalai.floatingball.server.common.api.ApiResponse;
import com.regionalai.floatingball.server.common.util.RequestIdUtils;
import com.regionalai.floatingball.server.modules.device.entity.AiDevice;
import com.regionalai.floatingball.server.modules.feedback.dto.ClientFeedbackSubmitRequest;
import com.regionalai.floatingball.server.modules.feedback.dto.ClientFeedbackSubmitResponse;
import com.regionalai.floatingball.server.modules.feedback.service.FeedbackService;
import com.regionalai.floatingball.server.security.DeviceContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/v1/client/feedbacks")
public class ClientFeedbackController {

    private final FeedbackService feedbackService;

    public ClientFeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping
    public ApiResponse<ClientFeedbackSubmitResponse> submit(@Validated @RequestBody ClientFeedbackSubmitRequest request,
                                                            HttpServletRequest httpServletRequest) {
        AiDevice device = DeviceContextHolder.get();
        return ApiResponse.success(
            feedbackService.submit(device, request),
            RequestIdUtils.resolve(httpServletRequest)
        );
    }
}
