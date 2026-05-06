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
            String module = resolveModule(event.getEventType(), payload);
            String action = resolveAction(event.getEventType(), payload);
            String title = resolveTitle(payload, action);
            log.setNaModule(module);
            log.setOpAction(action);
            log.setOpTitle(title);
            log.setSourceModule(resolveSourceModule(payload));
            log.setSceneCode(resolveScene(payload));
            log.setTraceId(resolveTraceId(payload));
            log.setDesOp(title);
            log.setOpResult(resolveResult(payload));
            log.setOperationTime(event.getTimestamp() == null
                ? LocalDateTime.now()
                : LocalDateTime.ofInstant(Instant.ofEpochMilli(event.getTimestamp()), ZoneId.systemDefault()));
            log.setConsultationId(resolveConsultationId(payload));
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

    private String resolveTitle(Map<String, Object> payload, String fallbackAction) {
        return firstNonBlank(
            extractText(payload, "title"),
            extractText(payload, "operationTitle"),
            extractText(payload, "description"),
            fallbackAction
        );
    }

    private String resolveSourceModule(Map<String, Object> payload) {
        return firstNonBlank(
            extractText(payload, "sourceModule"),
            extractNestedText(payload, "details", "sourceModule")
        );
    }

    private String resolveScene(Map<String, Object> payload) {
        return firstNonBlank(
            extractText(payload, "scene"),
            extractNestedText(payload, "details", "scene")
        );
    }

    private String resolveTraceId(Map<String, Object> payload) {
        return firstNonBlank(
            extractText(payload, "traceId"),
            extractNestedText(payload, "details", "traceId")
        );
    }

    private String resolveConsultationId(Map<String, Object> payload) {
        return firstNonBlank(
            extractText(payload, "consultationId"),
            extractNestedText(payload, "details", "consultationId")
        );
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

    @SuppressWarnings("unchecked")
    private String extractNestedText(Map<String, Object> payload, String parentKey, String key) {
        if (payload == null) {
            return null;
        }
        Object parent = payload.get(parentKey);
        if (!(parent instanceof Map)) {
            return null;
        }
        Object value = ((Map<String, Object>) parent).get(key);
        return value == null ? null : trimToNull(String.valueOf(value));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asPayloadMap(Object payload) {
        if (!(payload instanceof Map)) {
            return null;
        }
        return (Map<String, Object>) payload;
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
                                      String action,
                                      String title,
                                      String sourceModule,
                                      String scene,
                                      String traceId,
                                      String consultationId,
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
                .like(AiOpLog::getOpAction, keyword)
                .or()
                .like(AiOpLog::getOpTitle, keyword)
                .or()
                .like(AiOpLog::getSourceModule, keyword)
                .or()
                .like(AiOpLog::getSceneCode, keyword)
                .or()
                .like(AiOpLog::getTraceId, keyword)
                .or()
                .like(AiOpLog::getConsultationId, keyword)
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
        if (StringUtils.hasText(action)) {
            wrapper.like(AiOpLog::getOpAction, action.trim());
        }
        if (StringUtils.hasText(title)) {
            wrapper.like(AiOpLog::getOpTitle, title.trim());
        }
        if (StringUtils.hasText(sourceModule)) {
            wrapper.like(AiOpLog::getSourceModule, sourceModule.trim());
        }
        if (StringUtils.hasText(scene)) {
            wrapper.like(AiOpLog::getSceneCode, scene.trim());
        }
        if (StringUtils.hasText(traceId)) {
            wrapper.eq(AiOpLog::getTraceId, traceId.trim());
        }
        if (StringUtils.hasText(consultationId)) {
            wrapper.eq(AiOpLog::getConsultationId, consultationId.trim());
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
        Map<String, Object> payloadMap = asPayloadMap(payload);
        log.setOpAction(action);
        log.setOpTitle(resolveTitle(payloadMap, action));
        log.setSourceModule(resolveSourceModule(payloadMap));
        log.setSceneCode(resolveScene(payloadMap));
        log.setTraceId(resolveTraceId(payloadMap));
        log.setConsultationId(resolveConsultationId(payloadMap));
        log.setDesOp(firstNonBlank(log.getOpTitle(), action));
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
