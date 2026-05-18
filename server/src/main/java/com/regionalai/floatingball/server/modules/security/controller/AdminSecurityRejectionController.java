package com.regionalai.floatingball.server.modules.security.controller;

import com.regionalai.floatingball.server.common.api.ApiResponse;
import com.regionalai.floatingball.server.common.api.PageResponse;
import com.regionalai.floatingball.server.common.util.RequestIdUtils;
import com.regionalai.floatingball.server.modules.security.dto.SecurityDistributionVO;
import com.regionalai.floatingball.server.modules.security.dto.SecurityQueryDTO;
import com.regionalai.floatingball.server.modules.security.dto.SecuritySummaryVO;
import com.regionalai.floatingball.server.modules.security.dto.SecurityTrendVO;
import com.regionalai.floatingball.server.modules.security.entity.SecurityRejectionLog;
import com.regionalai.floatingball.server.modules.security.service.SecurityRejectionLogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/admin/api/security/rejections")
public class AdminSecurityRejectionController {

    private final SecurityRejectionLogService securityRejectionLogService;

    public AdminSecurityRejectionController(SecurityRejectionLogService securityRejectionLogService) {
        this.securityRejectionLogService = securityRejectionLogService;
    }

    @GetMapping
    public ApiResponse<PageResponse<SecurityRejectionLog>> list(
        @RequestParam(defaultValue = "1") long current,
        @RequestParam(defaultValue = "10") long size,
        @RequestParam(required = false) String rejectionType,
        @RequestParam(required = false) String requestPath,
        @RequestParam(required = false) String clientIp,
        @RequestParam(required = false) String idDevice,
        @RequestParam(required = false) String rejectReason,
        @RequestParam(required = false) String dateFrom,
        @RequestParam(required = false) String dateTo,
        HttpServletRequest request) {
        return ApiResponse.success(
            securityRejectionLogService.list(current, size, rejectionType, requestPath, clientIp, idDevice, rejectReason, dateFrom, dateTo),
            RequestIdUtils.resolve(request)
        );
    }

    @GetMapping("/summary")
    public ApiResponse<SecuritySummaryVO> summary(SecurityQueryDTO query, HttpServletRequest request) {
        return ApiResponse.success(securityRejectionLogService.getSummary(query), RequestIdUtils.resolve(request));
    }

    @GetMapping("/trend")
    public ApiResponse<SecurityTrendVO> trend(SecurityQueryDTO query, HttpServletRequest request) {
        return ApiResponse.success(securityRejectionLogService.getTrend(query), RequestIdUtils.resolve(request));
    }

    @GetMapping("/distribution")
    public ApiResponse<SecurityDistributionVO> distribution(SecurityQueryDTO query, HttpServletRequest request) {
        return ApiResponse.success(securityRejectionLogService.getDistribution(query), RequestIdUtils.resolve(request));
    }
}
