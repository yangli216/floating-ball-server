package com.regionalai.floatingball.server.modules.feedback.controller;

import com.regionalai.floatingball.server.common.api.ApiResponse;
import com.regionalai.floatingball.server.common.api.PageResponse;
import com.regionalai.floatingball.server.common.util.RequestIdUtils;
import com.regionalai.floatingball.server.modules.feedback.dto.AdminFeedbackDetailResponse;
import com.regionalai.floatingball.server.modules.feedback.dto.AdminFeedbackListItem;
import com.regionalai.floatingball.server.modules.feedback.dto.FeedbackListQuery;
import com.regionalai.floatingball.server.modules.feedback.service.FeedbackService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/admin/api/feedbacks")
public class AdminFeedbackController {

    private final FeedbackService feedbackService;

    public AdminFeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @GetMapping
    public ApiResponse<PageResponse<AdminFeedbackListItem>> list(FeedbackListQuery query,
                                                                 HttpServletRequest request) {
        if (query.getCurrent() <= 0) {
            query.setCurrent(1);
        }
        if (query.getSize() <= 0) {
            query.setSize(10);
        }
        return ApiResponse.success(
            feedbackService.list(query),
            RequestIdUtils.resolve(request)
        );
    }

    @GetMapping("/{feedbackId}")
    public ApiResponse<AdminFeedbackDetailResponse> detail(@PathVariable String feedbackId,
                                                           HttpServletRequest request) {
        return ApiResponse.success(
            feedbackService.detail(feedbackId),
            RequestIdUtils.resolve(request)
        );
    }
}
