package com.regionalai.floatingball.server.modules.analytics.controller;

import com.regionalai.floatingball.server.common.api.ApiResponse;
import com.regionalai.floatingball.server.common.util.RequestIdUtils;
import com.regionalai.floatingball.server.modules.analytics.dto.AnalyticsQueryDTO;
import com.regionalai.floatingball.server.modules.analytics.dto.AnalyticsSummaryVO;
import com.regionalai.floatingball.server.modules.analytics.dto.DistributionDataVO;
import com.regionalai.floatingball.server.modules.analytics.dto.FunctionUsageQueryDTO;
import com.regionalai.floatingball.server.modules.analytics.dto.FunctionUsageResponseVO;
import com.regionalai.floatingball.server.modules.analytics.dto.TrendDataVO;
import com.regionalai.floatingball.server.modules.analytics.service.AnalyticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/admin/api/analytics")
public class AdminAnalyticsController {

    private final AnalyticsService analyticsService;

    public AdminAnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/summary")
    public ApiResponse<AnalyticsSummaryVO> summary(AnalyticsQueryDTO query, HttpServletRequest request) {
        return ApiResponse.success(analyticsService.getSummary(query), RequestIdUtils.resolve(request));
    }

    @GetMapping("/trend")
    public ApiResponse<TrendDataVO> trend(AnalyticsQueryDTO query, HttpServletRequest request) {
        return ApiResponse.success(analyticsService.getTrend(query), RequestIdUtils.resolve(request));
    }

    @GetMapping("/distribution")
    public ApiResponse<DistributionDataVO> distribution(AnalyticsQueryDTO query, HttpServletRequest request) {
        return ApiResponse.success(analyticsService.getDistribution(query), RequestIdUtils.resolve(request));
    }

    @GetMapping("/function-modules")
    public ApiResponse<List<String>> functionModules(HttpServletRequest request) {
        return ApiResponse.success(analyticsService.getFunctionModuleOptions(), RequestIdUtils.resolve(request));
    }

    @GetMapping("/function-usage")
    public ApiResponse<FunctionUsageResponseVO> functionUsage(FunctionUsageQueryDTO query, HttpServletRequest request) {
        return ApiResponse.success(analyticsService.getFunctionUsage(query), RequestIdUtils.resolve(request));
    }
}
