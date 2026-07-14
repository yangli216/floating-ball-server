package com.regionalai.floatingball.server.modules.analytics.controller;

import com.regionalai.floatingball.server.common.api.ApiResponse;
import com.regionalai.floatingball.server.common.util.RequestIdUtils;
import com.regionalai.floatingball.server.modules.analytics.dto.AnalyticsQueryDTO;
import com.regionalai.floatingball.server.modules.analytics.dto.AnalyticsSummaryVO;
import com.regionalai.floatingball.server.modules.analytics.dto.DistributionDataVO;
import com.regionalai.floatingball.server.modules.analytics.dto.FunctionUsageQueryDTO;
import com.regionalai.floatingball.server.modules.analytics.dto.FunctionUsageResponseVO;
import com.regionalai.floatingball.server.modules.analytics.dto.HisOrgOptionVO;
import com.regionalai.floatingball.server.modules.analytics.dto.TrendDataVO;
import com.regionalai.floatingball.server.modules.analytics.service.AnalyticsService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
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

    @GetMapping("/his-org-options")
    public ApiResponse<List<HisOrgOptionVO>> hisOrgOptions(HttpServletRequest request) {
        return ApiResponse.success(analyticsService.getHisOrgOptions(), RequestIdUtils.resolve(request));
    }

    @GetMapping("/function-usage")
    public ApiResponse<FunctionUsageResponseVO> functionUsage(FunctionUsageQueryDTO query, HttpServletRequest request) {
        return ApiResponse.success(analyticsService.getFunctionUsage(query), RequestIdUtils.resolve(request));
    }

    @GetMapping("/export")
    public ResponseEntity<Resource> exportAnalytics(AnalyticsQueryDTO query) {
        return excelResponse(analyticsService.exportAnalyticsExcel(query), "analytics-" + System.currentTimeMillis() + ".xlsx");
    }

    @GetMapping("/function-usage/export")
    public ResponseEntity<Resource> exportFunctionUsage(FunctionUsageQueryDTO query) {
        return excelResponse(analyticsService.exportFunctionUsageExcel(query), "function-usage-" + System.currentTimeMillis() + ".xlsx");
    }

    private ResponseEntity<Resource> excelResponse(byte[] data, String fileName) {
        ByteArrayResource resource = new ByteArrayResource(data);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                .filename(fileName, StandardCharsets.UTF_8)
                .build()
                .toString())
            .contentLength(data.length)
            .body(resource);
    }
}
