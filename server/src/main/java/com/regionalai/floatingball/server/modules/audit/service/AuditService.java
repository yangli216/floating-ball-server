package com.regionalai.floatingball.server.modules.audit.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.regionalai.floatingball.server.common.api.PageResponse;
import com.regionalai.floatingball.server.common.exception.BusinessException;
import com.regionalai.floatingball.server.modules.audit.dto.AuditBatchRequest;
import com.regionalai.floatingball.server.modules.audit.entity.AiOpLog;
import com.regionalai.floatingball.server.modules.audit.mapper.AiOpLogMapper;
import com.regionalai.floatingball.server.modules.device.entity.AiDevice;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.UUID;
import java.util.Map;
import java.util.Collections;

@Service
public class AuditService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter ISO_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final AiOpLogMapper aiOpLogMapper;
    private final ObjectMapper objectMapper;
    private final AudioLogStorageService audioLogStorageService;

    public AuditService(AiOpLogMapper aiOpLogMapper,
                        ObjectMapper objectMapper,
                        AudioLogStorageService audioLogStorageService) {
        this.aiOpLogMapper = aiOpLogMapper;
        this.objectMapper = objectMapper;
        this.audioLogStorageService = audioLogStorageService;
    }

    public int saveBatch(AiDevice device, AuditBatchRequest request) {
        if (request.getEvents() == null || request.getEvents().isEmpty()) {
            return 0;
        }
        int accepted = 0;
        for (AuditBatchRequest.AuditEvent event : request.getEvents()) {
            Map<String, Object> payload = event.getPayload();
            AiOpLog log = new AiOpLog();
            log.setIdDevice(device.getIdDevice());
            log.setIdOrg(device.getIdOrg());
            log.setSdLogType(event.getEventType());
            log.setNaModule(resolveModule(event.getEventType(), payload));
            log.setDesOp(resolveAction(event.getEventType(), payload));
            log.setOpResult(resolveResult(payload));
            log.setOperationTime(event.getTimestamp() == null
                ? LocalDateTime.now()
                : LocalDateTime.ofInstant(Instant.ofEpochMilli(event.getTimestamp()), ZoneId.systemDefault()));
            log.setFgActive("1");
            try {
                log.setPayloadJson(objectMapper.writeValueAsString(payload));
            } catch (JsonProcessingException ex) {
                throw new BusinessException("审计事件序列化失败");
            }
            aiOpLogMapper.insert(log);
            accepted++;
        }
        return accepted;
    }

    private String resolveModule(String eventType, Map<String, Object> payload) {
        return firstNonBlank(
            extractText(payload, "module"),
            extractText(payload, "operationType"),
            extractText(payload, "metricType"),
            extractText(payload, "targetType"),
            extractText(payload, "recType"),
            extractText(payload, "sessionType"),
            trimToNull(eventType)
        );
    }

    private String resolveAction(String eventType, Map<String, Object> payload) {
        String action = firstNonBlank(
            extractText(payload, "action"),
            extractText(payload, "operationName"),
            extractText(payload, "feedbackType"),
            extractText(payload, "recType"),
            extractText(payload, "metricType")
        );
        return action != null ? action : trimToNull(eventType);
    }

    private String resolveResult(Map<String, Object> payload) {
        String explicitResult = normalizeResultValue(payload == null ? null : payload.get("result"));
        if (explicitResult != null) {
            return explicitResult;
        }
        String successResult = normalizeResultValue(payload == null ? null : payload.get("success"));
        if (successResult != null) {
            return successResult;
        }
        return "1";
    }

    private String extractText(Map<String, Object> payload, String key) {
        if (payload == null || !payload.containsKey(key)) {
            return null;
        }
        return trimToNull(String.valueOf(payload.get(key)));
    }

    private String firstNonBlank(String... candidates) {
        if (candidates == null) {
            return null;
        }
        for (String candidate : candidates) {
            String text = trimToNull(candidate);
            if (text != null) {
                return text;
            }
        }
        return null;
    }

    private String normalizeResultValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean) {
            return Boolean.TRUE.equals(value) ? "1" : "0";
        }
        String text = trimToNull(String.valueOf(value));
        if (text == null) {
            return null;
        }
        String normalized = text.toLowerCase();
        if ("1".equals(normalized) || "true".equals(normalized) || "success".equals(normalized) || "ok".equals(normalized)) {
            return "1";
        }
        if ("0".equals(normalized) || "false".equals(normalized) || "fail".equals(normalized)
            || "failure".equals(normalized) || "error".equals(normalized)) {
            return "0";
        }
        return text;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    public PageResponse<AiOpLog> list(long current,
                                      long size,
                                      String keyword,
                                      String logType,
                                      String module,
                                      String result,
                                      String dateFrom,
                                      String dateTo) {
        Page<AiOpLog> page = new Page<AiOpLog>(current, size);
        LocalDateTime startTime = parseDateTime(dateFrom, false);
        LocalDateTime endTime = parseDateTime(dateTo, true);
        LambdaQueryWrapper<AiOpLog> wrapper = new LambdaQueryWrapper<AiOpLog>()
            .eq(AiOpLog::getFgActive, "1")
            .orderByDesc(AiOpLog::getOperationTime);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(q -> q
                .like(AiOpLog::getNaModule, keyword)
                .or()
                .like(AiOpLog::getSdLogType, keyword)
                .or()
                .like(AiOpLog::getDesOp, keyword)
                .or()
                .like(AiOpLog::getPayloadJson, keyword)
                .or()
                .like(AiOpLog::getAudioFilePath, keyword)
                .or()
                .like(AiOpLog::getIdDevice, keyword)
                .or()
                .like(AiOpLog::getIdOrg, keyword));
        }
        if (StringUtils.hasText(logType)) {
            wrapper.eq(AiOpLog::getSdLogType, logType.trim());
        }
        if (StringUtils.hasText(module)) {
            wrapper.like(AiOpLog::getNaModule, module.trim());
        }
        if (StringUtils.hasText(result)) {
            applyResultFilter(wrapper, result);
        }
        if (startTime != null) {
            wrapper.ge(AiOpLog::getOperationTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(AiOpLog::getOperationTime, endTime);
        }
        Page<AiOpLog> pageResult = aiOpLogMapper.selectPage(page, wrapper);
        return new PageResponse<AiOpLog>(
            pageResult.getCurrent(),
            pageResult.getSize(),
            pageResult.getTotal(),
            pageResult.getRecords()
        );
    }

    public void saveSystemLog(AiDevice device,
                              String logType,
                              String module,
                              String action,
                              Object payload,
                              boolean success) {
        saveSystemLogInternal(device, logType, module, action, payload, success, null, null);
    }

    public void saveSystemLogWithAudioFile(AiDevice device,
                                           String logType,
                                           String module,
                                           String action,
                                           Object payload,
                                           boolean success,
                                           byte[] audioBytes,
                                           String audioFileName) {
        saveSystemLogInternal(device, logType, module, action, payload, success, audioBytes, audioFileName);
    }

    private void saveSystemLogInternal(AiDevice device,
                                       String logType,
                                       String module,
                                       String action,
                                       Object payload,
                                       boolean success,
                                       byte[] audioBytes,
                                       String audioFileName) {
        AiOpLog log = new AiOpLog();
        log.setIdLog(UUID.randomUUID().toString().replace("-", ""));
        log.setIdDevice(device == null ? null : device.getIdDevice());
        log.setIdOrg(device == null ? null : device.getIdOrg());
        log.setSdLogType(logType);
        log.setNaModule(module);
        log.setDesOp(action);
        log.setOpResult(success ? "1" : "0");
        log.setOperationTime(LocalDateTime.now());
        log.setFgActive("1");
        String storedAudioPath = null;
        try {
            if (audioBytes != null && audioBytes.length > 0) {
                storedAudioPath = audioLogStorageService.store(audioBytes, audioFileName, log.getIdLog());
                log.setAudioFilePath(storedAudioPath);
            }
            log.setPayloadJson(objectMapper.writeValueAsString(payload == null ? Collections.emptyMap() : payload));
            aiOpLogMapper.insert(log);
        } catch (Exception ignored) {
            audioLogStorageService.deleteQuietly(storedAudioPath);
            // 审计日志不影响主链路
        }
    }

    private void applyResultFilter(LambdaQueryWrapper<AiOpLog> wrapper, String result) {
        String normalized = result.trim().toLowerCase();
        if ("success".equals(normalized) || "1".equals(normalized) || "true".equals(normalized)) {
            wrapper.and(q -> q.eq(AiOpLog::getOpResult, "1")
                .or()
                .eq(AiOpLog::getOpResult, "true")
                .or()
                .eq(AiOpLog::getOpResult, "TRUE")
                .or()
                .eq(AiOpLog::getOpResult, "success")
                .or()
                .eq(AiOpLog::getOpResult, "SUCCESS"));
            return;
        }
        if ("failure".equals(normalized) || "0".equals(normalized) || "false".equals(normalized)) {
            wrapper.and(q -> q.eq(AiOpLog::getOpResult, "0")
                .or()
                .eq(AiOpLog::getOpResult, "false")
                .or()
                .eq(AiOpLog::getOpResult, "FALSE")
                .or()
                .eq(AiOpLog::getOpResult, "fail")
                .or()
                .eq(AiOpLog::getOpResult, "FAIL")
                .or()
                .eq(AiOpLog::getOpResult, "failure")
                .or()
                .eq(AiOpLog::getOpResult, "FAILURE"));
            return;
        }
        wrapper.eq(AiOpLog::getOpResult, result.trim());
    }

    private LocalDateTime parseDateTime(String value, boolean endOfDay) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String candidate = value.trim();
        try {
            if (candidate.length() == 10) {
                LocalDate date = LocalDate.parse(candidate, DateTimeFormatter.ISO_LOCAL_DATE);
                return endOfDay ? date.atTime(LocalTime.MAX) : date.atStartOfDay();
            }
            return LocalDateTime.parse(candidate, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDateTime.parse(candidate, ISO_DATE_TIME_FORMATTER);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return OffsetDateTime.parse(candidate).toLocalDateTime();
        } catch (DateTimeParseException ex) {
            throw new BusinessException("日志查询时间格式非法");
        }
    }
}
