package com.regionalai.floatingball.server.modules.patientmemory.controller;

import com.regionalai.floatingball.server.common.api.ApiResponse;
import com.regionalai.floatingball.server.common.api.PageResponse;
import com.regionalai.floatingball.server.common.util.RequestIdUtils;
import com.regionalai.floatingball.server.modules.patientmemory.dto.AdminPatientMemoryDetailVO;
import com.regionalai.floatingball.server.modules.patientmemory.dto.AdminPatientMemoryFactActionRequest;
import com.regionalai.floatingball.server.modules.patientmemory.dto.AdminPatientMemoryFactUpdateRequest;
import com.regionalai.floatingball.server.modules.patientmemory.dto.AdminPatientMemoryListItem;
import com.regionalai.floatingball.server.modules.patientmemory.dto.AdminPatientMemoryQuery;
import com.regionalai.floatingball.server.modules.patientmemory.service.AdminPatientMemoryService;
import com.regionalai.floatingball.server.security.AdminContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/admin/api/patient-memories")
public class AdminPatientMemoryController {

    private final AdminPatientMemoryService patientMemoryService;

    public AdminPatientMemoryController(AdminPatientMemoryService patientMemoryService) {
        this.patientMemoryService = patientMemoryService;
    }

    @GetMapping
    public ApiResponse<PageResponse<AdminPatientMemoryListItem>> list(AdminPatientMemoryQuery query,
                                                                      HttpServletRequest request) {
        return ApiResponse.success(
            patientMemoryService.list(AdminContextHolder.get(), query),
            RequestIdUtils.resolve(request)
        );
    }

    @GetMapping("/{memoryId}")
    public ApiResponse<AdminPatientMemoryDetailVO> detail(@PathVariable String memoryId,
                                                          HttpServletRequest request) {
        return ApiResponse.success(
            patientMemoryService.detail(AdminContextHolder.get(), memoryId),
            RequestIdUtils.resolve(request)
        );
    }

    @PutMapping("/{memoryId}/facts/{factId}")
    public ApiResponse<AdminPatientMemoryDetailVO> updateFact(@PathVariable String memoryId,
                                                              @PathVariable String factId,
                                                              @RequestBody AdminPatientMemoryFactUpdateRequest body,
                                                              HttpServletRequest request) {
        return ApiResponse.success(
            patientMemoryService.updateFact(AdminContextHolder.get(), memoryId, factId, body),
            RequestIdUtils.resolve(request)
        );
    }

    @PostMapping("/{memoryId}/facts/{factId}/suppress")
    public ApiResponse<AdminPatientMemoryDetailVO> suppressFact(@PathVariable String memoryId,
                                                                @PathVariable String factId,
                                                                @RequestBody AdminPatientMemoryFactActionRequest body,
                                                                HttpServletRequest request) {
        return ApiResponse.success(
            patientMemoryService.suppressFact(AdminContextHolder.get(), memoryId, factId, body),
            RequestIdUtils.resolve(request)
        );
    }

    @PostMapping("/{memoryId}/facts/{factId}/restore")
    public ApiResponse<AdminPatientMemoryDetailVO> restoreFact(@PathVariable String memoryId,
                                                               @PathVariable String factId,
                                                               @RequestBody AdminPatientMemoryFactActionRequest body,
                                                               HttpServletRequest request) {
        return ApiResponse.success(
            patientMemoryService.restoreFact(AdminContextHolder.get(), memoryId, factId, body),
            RequestIdUtils.resolve(request)
        );
    }
}
