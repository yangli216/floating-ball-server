package com.regionalai.floatingball.server.modules.useractivity.controller;

import com.regionalai.floatingball.server.common.api.ApiResponse;
import com.regionalai.floatingball.server.common.api.PageResponse;
import com.regionalai.floatingball.server.common.util.RequestIdUtils;
import com.regionalai.floatingball.server.modules.useractivity.dto.RegionTreeNodeVO;
import com.regionalai.floatingball.server.modules.useractivity.dto.UserActivityItemVO;
import com.regionalai.floatingball.server.modules.useractivity.dto.UserActivityQueryDTO;
import com.regionalai.floatingball.server.modules.useractivity.dto.UserActivitySummaryVO;
import com.regionalai.floatingball.server.modules.useractivity.service.UserActivityService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/admin/api/user-activity")
public class AdminUserActivityController {

    private final UserActivityService userActivityService;

    public AdminUserActivityController(UserActivityService userActivityService) {
        this.userActivityService = userActivityService;
    }

    @GetMapping("/summary")
    public ApiResponse<UserActivitySummaryVO> summary(UserActivityQueryDTO query, HttpServletRequest request) {
        return ApiResponse.success(userActivityService.getSummary(query), RequestIdUtils.resolve(request));
    }

    @GetMapping("/region-tree")
    public ApiResponse<List<RegionTreeNodeVO>> regionTree(UserActivityQueryDTO query, HttpServletRequest request) {
        return ApiResponse.success(userActivityService.getRegionTree(query), RequestIdUtils.resolve(request));
    }

    @GetMapping("/users")
    public ApiResponse<PageResponse<UserActivityItemVO>> users(UserActivityQueryDTO query,
                                                               @RequestParam(defaultValue = "1") long current,
                                                               @RequestParam(defaultValue = "10") long size,
                                                               HttpServletRequest request) {
        return ApiResponse.success(userActivityService.getUserList(query, current, size), RequestIdUtils.resolve(request));
    }
}
