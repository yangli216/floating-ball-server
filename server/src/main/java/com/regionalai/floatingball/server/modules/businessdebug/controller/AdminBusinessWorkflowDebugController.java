package com.regionalai.floatingball.server.modules.businessdebug.controller;

import com.regionalai.floatingball.server.common.api.ApiResponse;
import com.regionalai.floatingball.server.common.api.PageResponse;
import com.regionalai.floatingball.server.common.util.RequestIdUtils;
import com.regionalai.floatingball.server.modules.businessdebug.dto.BusinessDebugConsultationItem;
import com.regionalai.floatingball.server.modules.businessdebug.dto.BusinessDebugContextVO;
import com.regionalai.floatingball.server.modules.businessdebug.dto.BusinessDebugExecuteRequest;
import com.regionalai.floatingball.server.modules.businessdebug.dto.BusinessDebugExecuteResponse;
import com.regionalai.floatingball.server.modules.businessdebug.service.BusinessWorkflowDebugService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/admin/api/business-workflow-debug")
public class AdminBusinessWorkflowDebugController {

    private final BusinessWorkflowDebugService businessWorkflowDebugService;

    public AdminBusinessWorkflowDebugController(BusinessWorkflowDebugService businessWorkflowDebugService) {
        this.businessWorkflowDebugService = businessWorkflowDebugService;
    }

    @GetMapping("/consultations")
    public ApiResponse<PageResponse<BusinessDebugConsultationItem>> listConsultations(@RequestParam(defaultValue = "1") long current,
                                                                                      @RequestParam(defaultValue = "10") long size,
                                                                                      @RequestParam(required = false) String keyword,
                                                                                      @RequestParam(required = false) String status,
                                                                                      HttpServletRequest request) {
        return ApiResponse.success(
            businessWorkflowDebugService.listConsultations(current, size, keyword, status),
            RequestIdUtils.resolve(request)
        );
    }

    @GetMapping("/consultations/{idRun}/context")
    public ApiResponse<BusinessDebugContextVO> context(@PathVariable String idRun,
                                                       HttpServletRequest request) {
        return ApiResponse.success(
            businessWorkflowDebugService.context(idRun),
            RequestIdUtils.resolve(request)
        );
    }

    @PostMapping("/execute")
    public ApiResponse<BusinessDebugExecuteResponse> execute(@RequestBody BusinessDebugExecuteRequest body,
                                                             HttpServletRequest request) {
        return ApiResponse.success(
            businessWorkflowDebugService.execute(body),
            RequestIdUtils.resolve(request)
        );
    }
}
