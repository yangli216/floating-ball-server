package com.regionalai.floatingball.server.modules.emrtemplate.controller;

import com.regionalai.floatingball.server.common.api.ApiResponse;
import com.regionalai.floatingball.server.common.util.RequestIdUtils;
import com.regionalai.floatingball.server.modules.emrtemplate.dto.InpatientEmrTemplateCacheVO;
import com.regionalai.floatingball.server.modules.emrtemplate.dto.InpatientEmrTemplateResolveRequest;
import com.regionalai.floatingball.server.modules.emrtemplate.service.InpatientEmrTemplateCacheService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/v1/client/inpatient-emr/templates")
public class ClientInpatientEmrTemplateController {

    private final InpatientEmrTemplateCacheService templateCacheService;

    public ClientInpatientEmrTemplateController(InpatientEmrTemplateCacheService templateCacheService) {
        this.templateCacheService = templateCacheService;
    }

    @PostMapping("/resolve")
    public ApiResponse<InpatientEmrTemplateCacheVO> resolve(@RequestBody InpatientEmrTemplateResolveRequest request,
                                                            HttpServletRequest httpServletRequest) {
        return ApiResponse.success(templateCacheService.resolve(request), RequestIdUtils.resolve(httpServletRequest));
    }
}
