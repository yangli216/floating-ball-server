package com.regionalai.floatingball.server.modules.feedback.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.regionalai.floatingball.server.common.api.PageResponse;
import com.regionalai.floatingball.server.common.exception.BusinessException;
import com.regionalai.floatingball.server.modules.audit.entity.AiOpLog;
import com.regionalai.floatingball.server.modules.audit.mapper.AiOpLogMapper;
import com.regionalai.floatingball.server.modules.device.entity.AiDevice;
import com.regionalai.floatingball.server.modules.feedback.dto.AdminFeedbackDetailResponse;
import com.regionalai.floatingball.server.modules.feedback.dto.AdminFeedbackListItem;
import com.regionalai.floatingball.server.modules.feedback.dto.ClientFeedbackSubmitRequest;
import com.regionalai.floatingball.server.modules.feedback.dto.ClientFeedbackSubmitResponse;
import com.regionalai.floatingball.server.modules.feedback.dto.FeedbackTimelineItem;
import com.regionalai.floatingball.server.modules.feedback.entity.AiFeedback;
import com.regionalai.floatingball.server.modules.feedback.mapper.AiFeedbackMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FeedbackService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter ISO_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final AiFeedbackMapper aiFeedbackMapper;
    private final AiOpLogMapper aiOpLogMapper;
    private final ObjectMapper objectMapper;

    public FeedbackService(AiFeedbackMapper aiFeedbackMapper,
                           AiOpLogMapper aiOpLogMapper,
                           ObjectMapper objectMapper) {
        this.aiFeedbackMapper = aiFeedbackMapper;
        this.aiOpLogMapper = aiOpLogMapper;
        this.objectMapper = objectMapper;
    }

    public ClientFeedbackSubmitResponse submit(AiDevice device, ClientFeedbackSubmitRequest request) {
        validateRequest(request);

        AiFeedback feedback = new AiFeedback();
        feedback.setIdDevice(device == null ? null : device.getIdDevice());
        feedback.setIdOrg(device == null ? null : device.getIdOrg());
        feedback.setSessionId(trimToNull(request.getSessionId()));
        feedback.setTraceId(trimToNull(request.getTraceId()));
        feedback.setSourceModule(firstNonBlank(request.getSourceModule(), "settings_feedback"));
        feedback.setScore(request.getScore());
        feedback.setCommentText(request.getComment().trim());
        feedback.setFeedbackTime(LocalDateTime.now());
        feedback.setFgActive("1");

        ClientFeedbackSubmitRequest.ScreenshotPayload screenshot = request.getScreenshot();
        if (screenshot != null) {
            feedback.setScreenshotFileName(trimToNull(screenshot.getFileName()));
            feedback.setScreenshotMimeType(trimToNull(screenshot.getMimeType()));
            feedback.setScreenshotDataUrl(trimToNull(screenshot.getDataUrl()));
        }

        try {
            feedback.setChainContextJson(objectMapper.writeValueAsString(
                request.getChainContext() == null ? Collections.emptyMap() : request.getChainContext()
            ));
        } catch (Exception ex) {
            throw new BusinessException("反馈链路上下文序列化失败");
        }

        aiFeedbackMapper.insert(feedback);
        return new ClientFeedbackSubmitResponse(feedback.getIdFeedback(), "accepted");
    }

    public PageResponse<AdminFeedbackListItem> list(long current,
                                                    long size,
                                                    String keyword,
                                                    Integer score,
                                                    String sourceModule,
                                                    String dateFrom,
                                                    String dateTo) {
        Page<AiFeedback> page = new Page<AiFeedback>(current, size);
        QueryWrapper<AiFeedback> wrapper = new QueryWrapper<AiFeedback>()
            .eq("fg_active", "1")
            .orderByDesc("feedback_time");

        if (StringUtils.hasText(keyword)) {
            String trimmed = keyword.trim();
            wrapper.and(query -> query
                .like("comment_text", trimmed)
                .or().like("trace_id", trimmed)
                .or().like("session_id", trimmed)
                .or().like("id_device", trimmed)
                .or().like("id_org", trimmed));
        }
        if (score != null) {
            wrapper.eq("score", score);
        }
        if (StringUtils.hasText(sourceModule)) {
            wrapper.like("source_module", sourceModule.trim());
        }

        LocalDateTime startTime = parseDateTime(dateFrom, false);
        LocalDateTime endTime = parseDateTime(dateTo, true);
        if (startTime != null) {
            wrapper.ge("feedback_time", startTime);
        }
        if (endTime != null) {
            wrapper.le("feedback_time", endTime);
        }

        Page<AiFeedback> result = aiFeedbackMapper.selectPage(page, wrapper);
        List<AdminFeedbackListItem> records = result.getRecords().stream()
            .map(this::toListItem)
            .collect(Collectors.toList());
        return new PageResponse<AdminFeedbackListItem>(
            result.getCurrent(),
            result.getSize(),
            result.getTotal(),
            records
        );
    }

    public AdminFeedbackDetailResponse detail(String feedbackId) {
        AiFeedback feedback = aiFeedbackMapper.selectOne(new QueryWrapper<AiFeedback>()
            .eq("id_feedback", feedbackId)
            .eq("fg_active", "1")
            .last("FETCH FIRST 1 ROWS ONLY"));
        if (feedback == null) {
            throw new BusinessException("反馈记录不存在");
        }

        AdminFeedbackDetailResponse response = new AdminFeedbackDetailResponse();
        response.setFeedback(toDetail(feedback));
        response.setTimeline(buildTimeline(feedback));
        return response;
    }

    private void validateRequest(ClientFeedbackSubmitRequest request) {
        if (request.getScore() == null || request.getScore() < 1 || request.getScore() > 5) {
            throw new BusinessException("评分必须在 1 到 5 分之间");
        }
        if (!StringUtils.hasText(request.getComment())) {
            throw new BusinessException("反馈说明不能为空");
        }
        ClientFeedbackSubmitRequest.ScreenshotPayload screenshot = request.getScreenshot();
        if (screenshot != null && StringUtils.hasText(screenshot.getDataUrl())) {
            String dataUrl = screenshot.getDataUrl().trim();
            if (!dataUrl.startsWith("data:image/")) {
                throw new BusinessException("截图必须为图片 dataUrl");
            }
        }
    }

    private AdminFeedbackListItem toListItem(AiFeedback feedback) {
        AdminFeedbackListItem item = new AdminFeedbackListItem();
        item.setFeedbackId(feedback.getIdFeedback());
        item.setScore(feedback.getScore());
        item.setComment(feedback.getCommentText());
        item.setSourceModule(feedback.getSourceModule());
        item.setTraceId(feedback.getTraceId());
        item.setSessionId(feedback.getSessionId());
        item.setHasScreenshot(StringUtils.hasText(feedback.getScreenshotDataUrl()));
        item.setIdDevice(feedback.getIdDevice());
        item.setIdOrg(feedback.getIdOrg());
        item.setCreatedAt(feedback.getFeedbackTime());
        return item;
    }

    private AdminFeedbackDetailResponse.FeedbackDetail toDetail(AiFeedback feedback) {
        AdminFeedbackDetailResponse.FeedbackDetail detail = new AdminFeedbackDetailResponse.FeedbackDetail();
        detail.setFeedbackId(feedback.getIdFeedback());
        detail.setScore(feedback.getScore());
        detail.setComment(feedback.getCommentText());
        detail.setSourceModule(feedback.getSourceModule());
        detail.setTraceId(feedback.getTraceId());
        detail.setSessionId(feedback.getSessionId());
        detail.setIdDevice(feedback.getIdDevice());
        detail.setIdOrg(feedback.getIdOrg());
        detail.setScreenshotFileName(feedback.getScreenshotFileName());
        detail.setScreenshotMimeType(feedback.getScreenshotMimeType());
        detail.setScreenshotDataUrl(feedback.getScreenshotDataUrl());
        detail.setChainContext(parseJsonMap(feedback.getChainContextJson()));
        detail.setCreatedAt(feedback.getFeedbackTime());
        return detail;
    }

    private List<FeedbackTimelineItem> buildTimeline(AiFeedback feedback) {
        List<FeedbackTimelineItem> timeline = new ArrayList<FeedbackTimelineItem>();
        timeline.add(buildFeedbackTimelineItem(feedback));

        QueryWrapper<AiOpLog> wrapper = new QueryWrapper<AiOpLog>()
            .eq("fg_active", "1")
            .orderByAsc("operation_time");
        if (StringUtils.hasText(feedback.getIdDevice())) {
            wrapper.eq("id_device", feedback.getIdDevice());
        }
        if (StringUtils.hasText(feedback.getTraceId())) {
            wrapper.apply("dbms_lob.instr(payload_json, {0}) > 0", feedback.getTraceId().trim());
        } else if (StringUtils.hasText(feedback.getSessionId())) {
            wrapper.apply("dbms_lob.instr(payload_json, {0}) > 0", feedback.getSessionId().trim());
            if (feedback.getFeedbackTime() != null) {
                wrapper.ge("operation_time", feedback.getFeedbackTime().minusMinutes(30));
                wrapper.le("operation_time", feedback.getFeedbackTime().plusMinutes(5));
            }
        } else {
            if (feedback.getFeedbackTime() != null) {
                wrapper.ge("operation_time", feedback.getFeedbackTime().minusMinutes(10));
                wrapper.le("operation_time", feedback.getFeedbackTime().plusMinutes(1));
            }
        }

        List<AiOpLog> logs = aiOpLogMapper.selectList(wrapper);
        for (AiOpLog log : logs) {
            timeline.add(buildLogTimelineItem(log));
        }

        timeline.sort(Comparator.comparing(FeedbackTimelineItem::getTime, Comparator.nullsLast(Comparator.naturalOrder())));
        return timeline;
    }

    private FeedbackTimelineItem buildFeedbackTimelineItem(AiFeedback feedback) {
        FeedbackTimelineItem item = new FeedbackTimelineItem();
        item.setType("feedback");
        item.setTime(feedback.getFeedbackTime());
        item.setTitle("用户提交反馈");
        item.setResult("success");
        item.setPayload(parseJsonMap(feedback.getChainContextJson()));
        return item;
    }

    private FeedbackTimelineItem buildLogTimelineItem(AiOpLog log) {
        FeedbackTimelineItem item = new FeedbackTimelineItem();
        item.setType(log.getSdLogType());
        item.setTime(log.getOperationTime());
        item.setTitle(firstNonBlank(log.getDesOp(), log.getNaModule(), log.getSdLogType()));
        item.setResult(resolveResult(log.getOpResult()));
        Map<String, Object> payload = new LinkedHashMap<String, Object>(parseJsonMap(log.getPayloadJson()));
        if (StringUtils.hasText(log.getAudioFilePath())) {
            payload.put("audioFilePath", log.getAudioFilePath());
        }
        item.setPayload(payload);
        return item;
    }

    private Map<String, Object> parseJsonMap(String value) {
        if (!StringUtils.hasText(value)) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(value, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception ex) {
            return Collections.singletonMap("raw", value);
        }
    }

    private String resolveResult(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return "unknown";
        }
        String lower = normalized.toLowerCase();
        if ("1".equals(lower) || "true".equals(lower) || "success".equals(lower) || "ok".equals(lower)) {
            return "success";
        }
        if ("0".equals(lower) || "false".equals(lower) || "fail".equals(lower) || "failure".equals(lower) || "error".equals(lower)) {
            return "failure";
        }
        return lower;
    }

    private LocalDateTime parseDateTime(String value, boolean endOfDay) {
        String text = trimToNull(value);
        if (text == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(text, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDateTime.parse(text, ISO_DATE_TIME_FORMATTER);
        } catch (DateTimeParseException ignored) {
        }
        try {
            LocalDate date = LocalDate.parse(text);
            return endOfDay ? date.atTime(LocalTime.MAX) : date.atStartOfDay();
        } catch (DateTimeParseException ignored) {
        }
        throw new BusinessException("时间格式不正确，应为 yyyy-MM-dd HH:mm:ss");
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            String normalized = trimToNull(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
