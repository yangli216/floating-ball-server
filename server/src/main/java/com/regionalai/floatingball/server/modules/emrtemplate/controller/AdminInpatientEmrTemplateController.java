package com.regionalai.floatingball.server.modules.emrtemplate.controller;

import com.regionalai.floatingball.server.common.api.ApiResponse;
import com.regionalai.floatingball.server.common.api.PageResponse;
import com.regionalai.floatingball.server.common.util.RequestIdUtils;
import com.regionalai.floatingball.server.modules.emrtemplate.dto.InpatientEmrTemplateCacheVO;
import com.regionalai.floatingball.server.modules.emrtemplate.dto.InpatientEmrTemplatePromptRequest;
import com.regionalai.floatingball.server.modules.emrtemplate.service.InpatientEmrTemplateCacheService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/admin/api/inpatient-emr/templates")
public class AdminInpatientEmrTemplateController {

    private final InpatientEmrTemplateCacheService templateCacheService;

    public AdminInpatientEmrTemplateController(InpatientEmrTemplateCacheService templateCacheService) {
        this.templateCacheService = templateCacheService;
    }

    @GetMapping
    public ApiResponse<PageResponse<InpatientEmrTemplateCacheVO>> list(@RequestParam(defaultValue = "1") long current,
                                                                       @RequestParam(defaultValue = "10") long size,
                                                                       @RequestParam(required = false) String keyword,
                                                                       @RequestParam(required = false) String sdStatus,
                                                                       HttpServletRequest request) {
        return ApiResponse.success(
            templateCacheService.list(current, size, keyword, sdStatus),
            RequestIdUtils.resolve(request)
        );
    }

    @GetMapping("/{idCache}")
    public ApiResponse<InpatientEmrTemplateCacheVO> get(@PathVariable String idCache,
                                                        HttpServletRequest request) {
        return ApiResponse.success(templateCacheService.get(idCache), RequestIdUtils.resolve(request));
    }

    @PutMapping("/{idCache}/fields/{fieldId}/prompt")
    public ApiResponse<InpatientEmrTemplateCacheVO> updateFieldPrompt(@PathVariable String idCache,
                                                                      @PathVariable String fieldId,
                                                                      @RequestBody InpatientEmrTemplatePromptRequest request,
                                                                      HttpServletRequest httpServletRequest) {
        return ApiResponse.success(
            templateCacheService.updateFieldPrompt(idCache, fieldId, request),
            RequestIdUtils.resolve(httpServletRequest)
        );
    }

    @PostMapping("/{idCache}/enable")
    public ApiResponse<InpatientEmrTemplateCacheVO> enable(@PathVariable String idCache,
                                                           HttpServletRequest request) {
        return ApiResponse.success(templateCacheService.enable(idCache), RequestIdUtils.resolve(request));
    }

    @PostMapping("/{idCache}/disable")
    public ApiResponse<InpatientEmrTemplateCacheVO> disable(@PathVariable String idCache,
                                                            HttpServletRequest request) {
        return ApiResponse.success(templateCacheService.disable(idCache), RequestIdUtils.resolve(request));
    }

    @DeleteMapping("/{idCache}")
    public ApiResponse<Void> invalidate(@PathVariable String idCache,
                                        HttpServletRequest request) {
        templateCacheService.invalidate(idCache);
        return ApiResponse.success(null, RequestIdUtils.resolve(request));
    }
}
