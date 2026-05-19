package com.regionalai.floatingball.server.modules.featureevent.controller;

import com.regionalai.floatingball.server.common.api.ApiResponse;
import com.regionalai.floatingball.server.common.util.RequestIdUtils;
import com.regionalai.floatingball.server.modules.device.entity.AiDevice;
import com.regionalai.floatingball.server.modules.featureevent.dto.FeatureEventBatchRequest;
import com.regionalai.floatingball.server.modules.featureevent.dto.FeatureEventBatchResponse;
import com.regionalai.floatingball.server.modules.featureevent.service.FeatureEventService;
import com.regionalai.floatingball.server.security.DeviceContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/v1/client/feature-events")
public class ClientFeatureEventController {

    private final FeatureEventService featureEventService;

    public ClientFeatureEventController(FeatureEventService featureEventService) {
        this.featureEventService = featureEventService;
    }

    @PostMapping("/batch")
    public ApiResponse<FeatureEventBatchResponse> saveBatch(@RequestBody FeatureEventBatchRequest request,
                                                            HttpServletRequest httpServletRequest) {
        AiDevice device = DeviceContextHolder.get();
        return ApiResponse.success(featureEventService.saveBatch(device, request), RequestIdUtils.resolve(httpServletRequest));
    }
}
