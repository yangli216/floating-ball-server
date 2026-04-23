package com.regionalai.floatingball.server.modules.datapackage.controller;

import com.regionalai.floatingball.server.common.api.ApiResponse;
import com.regionalai.floatingball.server.common.api.PageResponse;
import com.regionalai.floatingball.server.common.util.RequestIdUtils;
import com.regionalai.floatingball.server.modules.datapackage.dto.TemplateDeltaVO;
import com.regionalai.floatingball.server.modules.datapackage.entity.AiDataPackage;
import com.regionalai.floatingball.server.modules.datapackage.service.DataPackageService;
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
@RequestMapping("/admin/api/data-packages")
public class AdminDataPackageController {

    private final DataPackageService dataPackageService;

    public AdminDataPackageController(DataPackageService dataPackageService) {
        this.dataPackageService = dataPackageService;
    }

    @GetMapping
    public ApiResponse<PageResponse<AiDataPackage>> list(@RequestParam(defaultValue = "1") long current,
                                                         @RequestParam(defaultValue = "10") long size,
                                                         @RequestParam(required = false) String keyword,
                                                         @RequestParam(required = false) String sdPackageType,
                                                         @RequestParam(required = false) String sdStatus,
                                                         @RequestParam(required = false) String idRegion,
                                                         @RequestParam(required = false) String idOrg,
                                                         HttpServletRequest request) {
        return ApiResponse.success(
            dataPackageService.list(current, size, keyword, sdPackageType, sdStatus, idRegion, idOrg),
            RequestIdUtils.resolve(request)
        );
    }

    @PostMapping
    public ApiResponse<AiDataPackage> save(@RequestBody AiDataPackage dataPackage, HttpServletRequest request) {
        return ApiResponse.success(dataPackageService.save(dataPackage), RequestIdUtils.resolve(request));
    }

    @PutMapping("/{idPackage}")
    public ApiResponse<AiDataPackage> update(@PathVariable String idPackage,
                                             @RequestBody AiDataPackage dataPackage,
                                             HttpServletRequest request) {
        return ApiResponse.success(dataPackageService.update(idPackage, dataPackage), RequestIdUtils.resolve(request));
    }

    @PostMapping("/{idPackage}/publish")
    public ApiResponse<Void> publish(@PathVariable String idPackage, HttpServletRequest request) {
        dataPackageService.publish(idPackage);
        return ApiResponse.success(null, RequestIdUtils.resolve(request));
    }

    @PostMapping("/{idPackage}/archive")
    public ApiResponse<Void> archive(@PathVariable String idPackage, HttpServletRequest request) {
        dataPackageService.archive(idPackage);
        return ApiResponse.success(null, RequestIdUtils.resolve(request));
    }

    @GetMapping("/template-default")
    public ApiResponse<TemplateDeltaVO> builtinTemplate(HttpServletRequest request) {
        return ApiResponse.success(dataPackageService.getBuiltinTemplateSnapshot(), RequestIdUtils.resolve(request));
    }

    @DeleteMapping("/{idPackage}")
    public ApiResponse<Void> invalidate(@PathVariable String idPackage, HttpServletRequest request) {
        dataPackageService.invalidate(idPackage);
        return ApiResponse.success(null, RequestIdUtils.resolve(request));
    }
}
