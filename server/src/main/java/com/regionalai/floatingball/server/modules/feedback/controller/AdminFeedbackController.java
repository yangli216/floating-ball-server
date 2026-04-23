package com.regionalai.floatingball.server.modules.feedback.controller;

import com.regionalai.floatingball.server.common.api.ApiResponse;
import com.regionalai.floatingball.server.common.api.PageResponse;
import com.regionalai.floatingball.server.common.util.RequestIdUtils;
import com.regionalai.floatingball.server.modules.feedback.dto.AdminFeedbackDetailResponse;
import com.regionalai.floatingball.server.modules.feedback.dto.AdminFeedbackListItem;
import com.regionalai.floatingball.server.modules.feedback.service.FeedbackService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    public ApiResponse<PageResponse<AdminFeedbackListItem>> list(@RequestParam(defaultValue = "1") long current,
                                                                 @RequestParam(defaultValue = "10") long size,
                                                                 @RequestParam(required = false) String keyword,
                                                                 @RequestParam(required = false) Integer score,
                                                                 @RequestParam(required = false) String sourceModule,
                                                                 @RequestParam(required = false) String dateFrom,
                                                                 @RequestParam(required = false) String dateTo,
                                                                 HttpServletRequest request) {
        return ApiResponse.success(
            feedbackService.list(current, size, keyword, score, sourceModule, dateFrom, dateTo),
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
