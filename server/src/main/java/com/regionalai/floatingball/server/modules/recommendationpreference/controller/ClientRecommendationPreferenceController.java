package com.regionalai.floatingball.server.modules.recommendationpreference.controller;

import com.regionalai.floatingball.server.common.api.ApiResponse;
import com.regionalai.floatingball.server.common.util.RequestIdUtils;
import com.regionalai.floatingball.server.modules.device.entity.AiDevice;
import com.regionalai.floatingball.server.modules.recommendationpreference.dto.RecommendationPreferenceBatchRequest;
import com.regionalai.floatingball.server.modules.recommendationpreference.dto.RecommendationPreferenceBatchResponse;
import com.regionalai.floatingball.server.modules.recommendationpreference.dto.RecommendationPreferenceRankRequest;
import com.regionalai.floatingball.server.modules.recommendationpreference.dto.RecommendationPreferenceRankResponse;
import com.regionalai.floatingball.server.modules.recommendationpreference.service.RecommendationPreferenceService;
import com.regionalai.floatingball.server.security.DeviceContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/v1/client/recommendation-preferences")
public class ClientRecommendationPreferenceController {

    private final RecommendationPreferenceService recommendationPreferenceService;

    public ClientRecommendationPreferenceController(RecommendationPreferenceService recommendationPreferenceService) {
        this.recommendationPreferenceService = recommendationPreferenceService;
    }

    @PostMapping("/events/batch")
    public ApiResponse<RecommendationPreferenceBatchResponse> saveBatch(@RequestBody RecommendationPreferenceBatchRequest request,
                                                                        HttpServletRequest httpServletRequest) {
        AiDevice device = DeviceContextHolder.get();
        return ApiResponse.success(recommendationPreferenceService.saveBatch(device, request), RequestIdUtils.resolve(httpServletRequest));
    }

    @PostMapping("/rank")
    public ApiResponse<RecommendationPreferenceRankResponse> rank(@RequestBody RecommendationPreferenceRankRequest request,
                                                                  HttpServletRequest httpServletRequest) {
        AiDevice device = DeviceContextHolder.get();
        return ApiResponse.success(recommendationPreferenceService.rank(device, request), RequestIdUtils.resolve(httpServletRequest));
    }
}
