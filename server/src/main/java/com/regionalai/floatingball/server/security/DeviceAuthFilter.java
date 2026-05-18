package com.regionalai.floatingball.server.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regionalai.floatingball.server.common.api.ApiResponse;
import com.regionalai.floatingball.server.modules.device.entity.AiDevice;
import com.regionalai.floatingball.server.modules.device.service.DeviceService;
import com.regionalai.floatingball.server.modules.release.dto.ReleasePolicyView;
import com.regionalai.floatingball.server.modules.release.service.ReleaseService;
import com.regionalai.floatingball.server.modules.security.service.SecurityRejectionLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(DeviceAuthFilter.class);
    private static final String EMPTY_STRING_SHA256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

    private final DeviceService deviceService;
    private final ReleaseService releaseService;
    private final RequestSignatureVerifier signatureVerifier;
    private final SecurityRejectionLogService rejectionLogService;
    private final ObjectMapper objectMapper;

    public DeviceAuthFilter(DeviceService deviceService,
                            ReleaseService releaseService,
                            RequestSignatureVerifier signatureVerifier,
                            SecurityRejectionLogService rejectionLogService,
                            ObjectMapper objectMapper) {
        this.deviceService = deviceService;
        this.releaseService = releaseService;
        this.signatureVerifier = signatureVerifier;
        this.rejectionLogService = rejectionLogService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return !uri.startsWith("/v1/")
            || "/v1/client/register".equals(uri)
            || "/v1/ai/speech/realtime/ws".equals(uri)
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
            log.warn("device auth failed: missing or invalid authorization header. uri={}", request.getRequestURI());
            recordRejection("AUTH_MISSING_TOKEN", request, null, "缺少设备令牌", "Authorization header missing or malformed", false, null, null);
            writeUnauthorized(response, request, "缺少设备令牌");
            return;
        }

        String token = authHeader.substring("Bearer ".length()).trim();
        AiDevice device = deviceService.findActiveByToken(token);
        if (device == null) {
            log.warn("device auth failed: invalid or inactive token. uri={}", request.getRequestURI());
            recordRejection("AUTH_INVALID_TOKEN", request, null, "设备令牌无效或已停用", "Token does not match any active device", false, null, null);
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
            log.warn("device version too old, update required. uri={}, clientVersion={}, minSupportedVersion={}",
                request.getRequestURI(), clientVersion, policy.getMinSupportedVersion());
            recordRejection("VERSION_OUTDATED", request, device, "客户端版本过低", "minSupported=" + policy.getMinSupportedVersion() + " current=" + clientVersion, false, null, null);
            writeUpdateRequired(response, request, policy.getMinSupportedVersion());
            return;
        }

        if (StringUtils.hasText(device.getDevicePublicKey())) {
            String timestamp = request.getHeader("X-Timestamp");
            String nonce = request.getHeader("X-Nonce");
            String signature = request.getHeader("X-Signature");
            String bodySha256 = request.getHeader("X-Body-SHA256");

            boolean hasSigHeaders = timestamp != null && nonce != null && signature != null;

            if (!hasSigHeaders) {
                log.warn("device signature missing. uri={}", request.getRequestURI());
                recordRejection("SIG_MISSING", request, device, "缺少请求签名", "Expected X-Timestamp/X-Nonce/X-Signature headers", false, timestamp, nonce);
                writeSignatureRejected(response, request, "缺少请求签名");
                return;
            }

            String method = request.getMethod().toUpperCase();
            String path = request.getRequestURI();
            String bodyHash = StringUtils.hasText(bodySha256) ? bodySha256 : EMPTY_STRING_SHA256;

            RequestSignatureVerifier.VerificationResult result = signatureVerifier.verify(
                device.getDevicePublicKey(), method, path, timestamp, nonce, bodyHash, signature);

            if (!result.isValid()) {
                log.warn("device signature invalid. uri={}, deviceId={}, reason={}", request.getRequestURI(), device.getIdDevice(), result.getErrorMessage());
                recordRejection("SIG_INVALID", request, device, "签名验证失败", result.getErrorMessage(), true, timestamp, nonce);
                writeSignatureRejected(response, request, result.getErrorMessage());
                return;
            }
        } else {
            log.warn("device has no public key registered. uri={}, deviceId={}", request.getRequestURI(), device.getIdDevice());
            recordRejection("SIG_NO_PUBLIC_KEY", request, device, "设备未注册公钥", "Device registered without ECDSA public key", false, null, null);
            writeUnauthorized(response, request, "设备未注册公钥，请重新注册");
            return;
        }

        try {
            DeviceContextHolder.set(device);
            filterChain.doFilter(request, response);
        } finally {
            DeviceContextHolder.clear();
        }
    }

    private void recordRejection(String type, HttpServletRequest request, AiDevice device,
                                  String reason, String detail, boolean hasSignature,
                                  String timestamp, String nonce) {
        String clientIp = resolveClientIp(request);
        SecurityRejectionLogService.RejectionRecord record = SecurityRejectionLogService.RejectionRecord
            .of(type, request.getMethod(), request.getRequestURI(), clientIp)
            .device(device)
            .requestId(request.getHeader("X-Request-Id"))
            .reason(reason)
            .detail(detail)
            .signature(hasSignature, timestamp, nonce)
            .clientInfo(request.getHeader("X-Client-Version"), request.getHeader("X-Update-Channel"));

        rejectionLogService.logRejection(record);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(ip)) {
            int commaIdx = ip.indexOf(',');
            return commaIdx > 0 ? ip.substring(0, commaIdx).trim() : ip.trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(ip)) {
            return ip.trim();
        }
        return request.getRemoteAddr();
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

    private void writeSignatureRejected(HttpServletResponse response,
                                         HttpServletRequest request,
                                         String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        String requestId = request.getHeader("X-Request-Id");
        response.getWriter().write(objectMapper.writeValueAsString(
            ApiResponse.error("SIG-401", "请求签名验证失败: " + message, requestId == null ? "N/A" : requestId)
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
