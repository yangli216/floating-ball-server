package com.regionalai.floatingball.server.modules.recommendationpreference.controller;

import com.regionalai.floatingball.server.common.api.ApiResponse;
import com.regionalai.floatingball.server.common.api.PageResponse;
import com.regionalai.floatingball.server.common.util.RequestIdUtils;
import com.regionalai.floatingball.server.modules.recommendationpreference.dto.AdminRecommendationPreferenceAggregateVO;
import com.regionalai.floatingball.server.modules.recommendationpreference.dto.AdminRecommendationPreferenceEventVO;
import com.regionalai.floatingball.server.modules.recommendationpreference.dto.AdminRecommendationPreferenceQuery;
import com.regionalai.floatingball.server.modules.recommendationpreference.dto.AdminRecommendationPreferenceSummaryVO;
import com.regionalai.floatingball.server.modules.recommendationpreference.service.AdminRecommendationPreferenceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/admin/api/recommendation-preferences")
public class AdminRecommendationPreferenceController {

    private final AdminRecommendationPreferenceService recommendationPreferenceService;

    public AdminRecommendationPreferenceController(AdminRecommendationPreferenceService recommendationPreferenceService) {
        this.recommendationPreferenceService = recommendationPreferenceService;
    }

    @GetMapping("/summary")
    public ApiResponse<AdminRecommendationPreferenceSummaryVO> summary(AdminRecommendationPreferenceQuery query,
                                                                       HttpServletRequest request) {
        return ApiResponse.success(
            recommendationPreferenceService.summary(query),
            RequestIdUtils.resolve(request)
        );
    }

    @GetMapping("/aggregates")
    public ApiResponse<PageResponse<AdminRecommendationPreferenceAggregateVO>> aggregates(AdminRecommendationPreferenceQuery query,
                                                                                         HttpServletRequest request) {
        return ApiResponse.success(
            recommendationPreferenceService.aggregates(query),
            RequestIdUtils.resolve(request)
        );
    }

    @GetMapping("/events")
    public ApiResponse<PageResponse<AdminRecommendationPreferenceEventVO>> events(AdminRecommendationPreferenceQuery query,
                                                                                 HttpServletRequest request) {
        return ApiResponse.success(
            recommendationPreferenceService.events(query),
            RequestIdUtils.resolve(request)
        );
    }
}
