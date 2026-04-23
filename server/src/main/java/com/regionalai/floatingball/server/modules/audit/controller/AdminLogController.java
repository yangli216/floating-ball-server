package com.regionalai.floatingball.server.modules.audit.controller;

import com.regionalai.floatingball.server.common.api.ApiResponse;
import com.regionalai.floatingball.server.common.api.PageResponse;
import com.regionalai.floatingball.server.common.util.RequestIdUtils;
import com.regionalai.floatingball.server.modules.audit.entity.AiOpLog;
import com.regionalai.floatingball.server.modules.audit.service.AuditService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/admin/api/logs")
public class AdminLogController {

    private final AuditService auditService;

    public AdminLogController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public ApiResponse<PageResponse<AiOpLog>> list(@RequestParam(defaultValue = "1") long current,
                                                   @RequestParam(defaultValue = "10") long size,
                                                   @RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false) String logType,
                                                   @RequestParam(required = false) String module,
                                                   @RequestParam(required = false) String result,
                                                   @RequestParam(required = false) String dateFrom,
                                                   @RequestParam(required = false) String dateTo,
                                                   HttpServletRequest request) {
        return ApiResponse.success(
            auditService.list(current, size, keyword, logType, module, result, dateFrom, dateTo),
            RequestIdUtils.resolve(request)
        );
    }
}
