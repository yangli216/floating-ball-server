package com.regionalai.floatingball.server.modules.stats.controller;

import com.regionalai.floatingball.server.common.api.ApiResponse;
import com.regionalai.floatingball.server.common.util.RequestIdUtils;
import com.regionalai.floatingball.server.modules.stats.dto.OverviewStatsVO;
import com.regionalai.floatingball.server.modules.stats.service.OverviewStatsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/admin/api/stats")
public class AdminStatsController {

    private final OverviewStatsService overviewStatsService;

    public AdminStatsController(OverviewStatsService overviewStatsService) {
        this.overviewStatsService = overviewStatsService;
    }

    @GetMapping("/overview")
    public ApiResponse<OverviewStatsVO> overview(HttpServletRequest request) {
        return ApiResponse.success(overviewStatsService.getOverview(), RequestIdUtils.resolve(request));
    }
}
