package com.regionalai.floatingball.server.modules.patientmemory.controller;

import com.regionalai.floatingball.server.common.api.ApiResponse;
import com.regionalai.floatingball.server.common.util.RequestIdUtils;
import com.regionalai.floatingball.server.modules.device.entity.AiDevice;
import com.regionalai.floatingball.server.modules.patientmemory.dto.PatientMemoryResolveRequest;
import com.regionalai.floatingball.server.modules.patientmemory.dto.PatientMemoryResolveResponse;
import com.regionalai.floatingball.server.modules.patientmemory.dto.PatientMemorySyncRequest;
import com.regionalai.floatingball.server.modules.patientmemory.dto.PatientMemorySyncResponse;
import com.regionalai.floatingball.server.modules.patientmemory.service.PatientMemoryService;
import com.regionalai.floatingball.server.security.DeviceContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/v1/client/patient-memory")
public class ClientPatientMemoryController {

    private final PatientMemoryService patientMemoryService;

    public ClientPatientMemoryController(PatientMemoryService patientMemoryService) {
        this.patientMemoryService = patientMemoryService;
    }

    @PostMapping("/sync")
    public ApiResponse<PatientMemorySyncResponse> sync(@RequestBody PatientMemorySyncRequest request,
                                                       HttpServletRequest httpServletRequest) {
        AiDevice device = DeviceContextHolder.get();
        return ApiResponse.success(patientMemoryService.sync(device, request), RequestIdUtils.resolve(httpServletRequest));
    }

    @PostMapping("/resolve")
    public ApiResponse<PatientMemoryResolveResponse> resolve(@RequestBody PatientMemoryResolveRequest request,
                                                             HttpServletRequest httpServletRequest) {
        AiDevice device = DeviceContextHolder.get();
        return ApiResponse.success(patientMemoryService.resolve(device, request), RequestIdUtils.resolve(httpServletRequest));
    }
}
