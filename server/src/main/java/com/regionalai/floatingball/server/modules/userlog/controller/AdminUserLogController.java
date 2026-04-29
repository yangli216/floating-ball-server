package com.regionalai.floatingball.server.modules.userlog.controller;

import com.regionalai.floatingball.server.common.api.ApiResponse;
import com.regionalai.floatingball.server.common.api.PageResponse;
import com.regionalai.floatingball.server.common.util.RequestIdUtils;
import com.regionalai.floatingball.server.modules.userlog.dto.ConsultationTimelineItem;
import com.regionalai.floatingball.server.modules.userlog.dto.UserConsultationLogListItem;
import com.regionalai.floatingball.server.modules.userlog.entity.AiUserConsultationLog;
import com.regionalai.floatingball.server.modules.userlog.service.UserConsultationLogService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/admin/api/user-logs/consultations")
public class AdminUserLogController {

    private final UserConsultationLogService userConsultationLogService;

    public AdminUserLogController(UserConsultationLogService userConsultationLogService) {
        this.userConsultationLogService = userConsultationLogService;
    }

    @GetMapping
    public ApiResponse<PageResponse<UserConsultationLogListItem>> list(@RequestParam(defaultValue = "1") long current,
                                                                       @RequestParam(defaultValue = "10") long size,
                                                                       @RequestParam(required = false) String keyword,
                                                                       @RequestParam(required = false) String consultationType,
                                                                       @RequestParam(required = false) String status,
                                                                       @RequestParam(required = false) Integer minChanges,
                                                                       @RequestParam(required = false) Integer maxChanges,
                                                                       @RequestParam(required = false) String dateFrom,
                                                                       @RequestParam(required = false) String dateTo,
                                                                       HttpServletRequest request) {
        return ApiResponse.success(
            userConsultationLogService.list(current, size, keyword, consultationType, status, minChanges, maxChanges, dateFrom, dateTo),
            RequestIdUtils.resolve(request)
        );
    }

    @GetMapping("/{idLog}")
    public ApiResponse<AiUserConsultationLog> detail(@PathVariable String idLog,
                                                     HttpServletRequest request) {
        return ApiResponse.success(
            userConsultationLogService.detail(idLog),
            RequestIdUtils.resolve(request)
        );
    }

    @GetMapping("/{idLog}/audio")
    public ResponseEntity<Resource> audio(@PathVariable String idLog) {
        UserConsultationLogService.AudioFile audioFile = userConsultationLogService.resolveAudioFile(idLog);
        MediaType mediaType = resolveMediaType(audioFile.getMimeType());
        String fileName = StringUtils.hasText(audioFile.getFileName()) ? audioFile.getFileName() : "voice-consultation-audio";
        Resource resource = new FileSystemResource(audioFile.getPath().toFile());
        return ResponseEntity.ok()
            .contentType(mediaType)
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                .filename(fileName, StandardCharsets.UTF_8)
                .build()
                .toString())
            .body(resource);
    }

    @GetMapping("/{idLog}/timeline")
    public ApiResponse<List<ConsultationTimelineItem>> timeline(@PathVariable String idLog,
                                                                 HttpServletRequest request) {
        return ApiResponse.success(
            userConsultationLogService.getTimeline(idLog),
            RequestIdUtils.resolve(request)
        );
    }

    @GetMapping("/export")
    public ResponseEntity<Resource> export(@RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) String consultationType,
                                            @RequestParam(required = false) String status,
                                            @RequestParam(required = false) Integer minChanges,
                                            @RequestParam(required = false) Integer maxChanges,
                                            @RequestParam(required = false) String dateFrom,
                                            @RequestParam(required = false) String dateTo) {
        byte[] data = userConsultationLogService.exportExcel(keyword, consultationType, status, minChanges, maxChanges, dateFrom, dateTo);
        String fileName = "user-logs-" + System.currentTimeMillis() + ".xlsx";
        ByteArrayResource resource = new ByteArrayResource(data);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                .filename(fileName, StandardCharsets.UTF_8)
                .build()
                .toString())
            .contentLength(data.length)
            .body(resource);
    }

    private MediaType resolveMediaType(String mimeType) {
        if (!StringUtils.hasText(mimeType)) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(mimeType);
        } catch (IllegalArgumentException ex) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
