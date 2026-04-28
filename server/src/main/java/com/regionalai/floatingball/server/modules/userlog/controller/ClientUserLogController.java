package com.regionalai.floatingball.server.modules.userlog.controller;

import com.regionalai.floatingball.server.common.api.ApiResponse;
import com.regionalai.floatingball.server.common.util.RequestIdUtils;
import com.regionalai.floatingball.server.modules.device.entity.AiDevice;
import com.regionalai.floatingball.server.modules.userlog.dto.UserConsultationLogRequest;
import com.regionalai.floatingball.server.modules.userlog.entity.AiUserConsultationLog;
import com.regionalai.floatingball.server.modules.userlog.service.UserConsultationLogService;
import com.regionalai.floatingball.server.security.DeviceContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/v1/client/user-logs")
public class ClientUserLogController {

    private final UserConsultationLogService userConsultationLogService;

    public ClientUserLogController(UserConsultationLogService userConsultationLogService) {
        this.userConsultationLogService = userConsultationLogService;
    }

    @PostMapping("/consultations")
    public ApiResponse<AiUserConsultationLog> saveConsultation(@RequestBody UserConsultationLogRequest request,
                                                               HttpServletRequest httpServletRequest) {
        AiDevice device = DeviceContextHolder.get();
        return ApiResponse.success(
            userConsultationLogService.save(device, request),
            RequestIdUtils.resolve(httpServletRequest)
        );
    }
}
