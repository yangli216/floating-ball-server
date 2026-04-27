package com.regionalai.floatingball.server.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regionalai.floatingball.server.common.api.ApiResponse;
import com.regionalai.floatingball.server.modules.device.entity.AiDevice;
import com.regionalai.floatingball.server.modules.device.service.DeviceService;
import com.regionalai.floatingball.server.modules.release.dto.ReleasePolicyView;
import com.regionalai.floatingball.server.modules.release.service.ReleaseService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class DeviceAuthFilter extends OncePerRequestFilter {

    private final DeviceService deviceService;
    private final ReleaseService releaseService;
    private final ObjectMapper objectMapper;

    public DeviceAuthFilter(DeviceService deviceService, ReleaseService releaseService, ObjectMapper objectMapper) {
        this.deviceService = deviceService;
        this.releaseService = releaseService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return !uri.startsWith("/v1/")
            || "/v1/client/register".equals(uri)
            || uri.startsWith("/v1/client/releases/")
            || uri.startsWith("/admin/")
            || "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            writeUnauthorized(response, request, "缺少设备令牌");
            return;
        }

        String token = authHeader.substring("Bearer ".length()).trim();
        AiDevice device = deviceService.findActiveByToken(token);
        if (device == null) {
            writeUnauthorized(response, request, "设备令牌无效或已停用");
            return;
        }

        String updateChannel = request.getHeader("X-Update-Channel");
        String clientVersion = request.getHeader("X-Client-Version");
        if (!StringUtils.hasText(clientVersion)) {
            clientVersion = device.getClientVersion();
        }
        if (releaseService.isUpdateRequired(updateChannel, clientVersion)) {
            ReleasePolicyView policy = releaseService.getRequiredPolicy(updateChannel);
            writeUpdateRequired(response, request, policy.getMinSupportedVersion());
            return;
        }

        try {
            DeviceContextHolder.set(device);
            filterChain.doFilter(request, response);
        } finally {
            DeviceContextHolder.clear();
        }
    }

    private void writeUnauthorized(HttpServletResponse response,
                                   HttpServletRequest request,
                                   String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        String requestId = request.getHeader("X-Request-Id");
        response.getWriter().write(objectMapper.writeValueAsString(
            ApiResponse.error("AUTH-401", message, requestId == null ? "N/A" : requestId)
        ));
    }

    private void writeUpdateRequired(HttpServletResponse response,
                                     HttpServletRequest request,
                                     String minSupportedVersion) throws IOException {
        response.setStatus(426);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        String requestId = request.getHeader("X-Request-Id");
        String targetVersion = StringUtils.hasText(minSupportedVersion) ? minSupportedVersion : "最新版本";
        response.getWriter().write(objectMapper.writeValueAsString(
            ApiResponse.error(
                "UPDATE-REQUIRED",
                "当前客户端版本过低，请升级到 " + targetVersion + " 或更高版本后继续使用",
                requestId == null ? "N/A" : requestId
            )
        ));
    }
}
