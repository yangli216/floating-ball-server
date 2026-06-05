package com.regionalai.floatingball.server.modules.region.controller;

import com.regionalai.floatingball.server.common.api.ApiResponse;
import com.regionalai.floatingball.server.common.api.PageResponse;
import com.regionalai.floatingball.server.common.util.RequestIdUtils;
import com.regionalai.floatingball.server.modules.region.entity.AiRegion;
import com.regionalai.floatingball.server.modules.region.service.RegionService;
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
@RequestMapping("/admin/api/regions")
public class AdminRegionController {

    private final RegionService regionService;

    public AdminRegionController(RegionService regionService) {
        this.regionService = regionService;
    }

    @GetMapping
    public ApiResponse<PageResponse<AiRegion>> list(@RequestParam(defaultValue = "1") long current,
                                                    @RequestParam(defaultValue = "10") long size,
                                                    @RequestParam(required = false) String keyword,
                                                    @RequestParam(required = false) String sdStatus,
                                                    HttpServletRequest request) {
        return ApiResponse.success(regionService.list(current, size, keyword, sdStatus), RequestIdUtils.resolve(request));
    }

    @PostMapping
    public ApiResponse<AiRegion> save(@RequestBody AiRegion region, HttpServletRequest request) {
        return ApiResponse.success(regionService.save(region), RequestIdUtils.resolve(request));
    }

    @PutMapping("/{idRegion}")
    public ApiResponse<AiRegion> update(@PathVariable String idRegion,
                                        @RequestBody AiRegion region,
                                        HttpServletRequest request) {
        return ApiResponse.success(regionService.update(idRegion, region), RequestIdUtils.resolve(request));
    }

    @DeleteMapping("/{idRegion}")
    public ApiResponse<Void> invalidate(@PathVariable String idRegion, HttpServletRequest request) {
        regionService.invalidate(idRegion);
        return ApiResponse.success(null, RequestIdUtils.resolve(request));
    }

    @PostMapping("/{idRegion}/enable")
    public ApiResponse<Void> enable(@PathVariable String idRegion, HttpServletRequest request) {
        regionService.enable(idRegion);
        return ApiResponse.success(null, RequestIdUtils.resolve(request));
    }
}
