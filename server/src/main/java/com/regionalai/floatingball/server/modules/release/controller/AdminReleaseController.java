package com.regionalai.floatingball.server.modules.release.controller;

import com.regionalai.floatingball.server.common.api.ApiResponse;
import com.regionalai.floatingball.server.common.util.RequestIdUtils;
import com.regionalai.floatingball.server.modules.release.dto.ReleaseUploadRequest;
import com.regionalai.floatingball.server.modules.release.dto.ReleaseView;
import com.regionalai.floatingball.server.modules.release.service.ReleaseService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/admin/api/releases")
public class AdminReleaseController {

    private final ReleaseService releaseService;

    public AdminReleaseController(ReleaseService releaseService) {
        this.releaseService = releaseService;
    }

    @GetMapping
    public ApiResponse<List<ReleaseView>> list(@RequestParam(required = false) String channel,
                                               HttpServletRequest request) {
        return ApiResponse.success(releaseService.list(channel), RequestIdUtils.resolve(request));
    }

    @PostMapping("/upload")
    public ApiResponse<ReleaseView> upload(@ModelAttribute ReleaseUploadRequest uploadRequest,
                                           HttpServletRequest request) {
        return ApiResponse.success(releaseService.upload(uploadRequest), RequestIdUtils.resolve(request));
    }
}
