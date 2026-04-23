package com.regionalai.floatingball.server.modules.knowledge.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.regionalai.floatingball.server.common.api.ApiResponse;
import com.regionalai.floatingball.server.common.util.RequestIdUtils;
import com.regionalai.floatingball.server.modules.device.entity.AiDevice;
import com.regionalai.floatingball.server.modules.knowledge.dto.PmphaiClipRequest;
import com.regionalai.floatingball.server.modules.knowledge.dto.PmphaiListRequest;
import com.regionalai.floatingball.server.modules.knowledge.dto.PmphaiPageUrlRequest;
import com.regionalai.floatingball.server.modules.knowledge.dto.PmphaiPageUrlResponse;
import com.regionalai.floatingball.server.modules.knowledge.dto.PmphaiSearchRequest;
import com.regionalai.floatingball.server.modules.knowledge.service.PmphaiProxyService;
import com.regionalai.floatingball.server.security.DeviceContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/v1/knowledge/pmphai")
public class PmphaiProxyController {

    private final PmphaiProxyService pmphaiProxyService;

    public PmphaiProxyController(PmphaiProxyService pmphaiProxyService) {
        this.pmphaiProxyService = pmphaiProxyService;
    }

    @PostMapping("/search")
    public ApiResponse<JsonNode> search(@Validated @RequestBody PmphaiSearchRequest request,
                                        HttpServletRequest httpServletRequest) {
        AiDevice device = DeviceContextHolder.get();
        return ApiResponse.success(pmphaiProxyService.search(device, request), RequestIdUtils.resolve(httpServletRequest));
    }

    @PostMapping("/clip")
    public ApiResponse<JsonNode> clip(@Validated @RequestBody PmphaiClipRequest request,
                                      HttpServletRequest httpServletRequest) {
        AiDevice device = DeviceContextHolder.get();
        return ApiResponse.success(pmphaiProxyService.clip(device, request), RequestIdUtils.resolve(httpServletRequest));
    }

    @PostMapping("/list")
    public ApiResponse<JsonNode> list(@RequestBody PmphaiListRequest request,
                                      HttpServletRequest httpServletRequest) {
        AiDevice device = DeviceContextHolder.get();
        return ApiResponse.success(pmphaiProxyService.list(device, request), RequestIdUtils.resolve(httpServletRequest));
    }

    @PostMapping("/page-url")
    public ApiResponse<PmphaiPageUrlResponse> pageUrl(@Validated @RequestBody PmphaiPageUrlRequest request,
                                                      HttpServletRequest httpServletRequest) {
        AiDevice device = DeviceContextHolder.get();
        return ApiResponse.success(new PmphaiPageUrlResponse(pmphaiProxyService.generatePageUrl(device, request)), RequestIdUtils.resolve(httpServletRequest));
    }

    @GetMapping("/kgbases")
    public ApiResponse<JsonNode> knowledgeBases(@RequestParam(value = "kgBaseId", required = false) String kgBaseId,
                                                HttpServletRequest httpServletRequest) {
        AiDevice device = DeviceContextHolder.get();
        return ApiResponse.success(pmphaiProxyService.knowledgeBases(device, kgBaseId), RequestIdUtils.resolve(httpServletRequest));
    }

    @GetMapping("/categories")
    public ApiResponse<JsonNode> categories(@RequestParam("kgBaseId") String kgBaseId,
                                            HttpServletRequest httpServletRequest) {
        AiDevice device = DeviceContextHolder.get();
        return ApiResponse.success(pmphaiProxyService.categories(device, kgBaseId), RequestIdUtils.resolve(httpServletRequest));
    }
}
