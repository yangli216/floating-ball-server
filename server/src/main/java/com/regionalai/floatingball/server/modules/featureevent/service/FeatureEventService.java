package com.regionalai.floatingball.server.modules.featureevent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.regionalai.floatingball.server.common.exception.BusinessException;
import com.regionalai.floatingball.server.modules.device.entity.AiDevice;
import com.regionalai.floatingball.server.modules.featureevent.dto.FeatureEventBatchRequest;
import com.regionalai.floatingball.server.modules.featureevent.dto.FeatureEventBatchResponse;
import com.regionalai.floatingball.server.modules.featureevent.entity.AiFeatureEvent;
import com.regionalai.floatingball.server.modules.featureevent.mapper.AiFeatureEventMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.UUID;

@Service
public class FeatureEventService {

    private static final Logger log = LoggerFactory.getLogger(FeatureEventService.class);

    private final AiFeatureEventMapper featureEventMapper;
    private final ObjectMapper objectMapper;

    public FeatureEventService(AiFeatureEventMapper featureEventMapper,
                               ObjectMapper objectMapper) {
        this.featureEventMapper = featureEventMapper;
        this.objectMapper = objectMapper;
    }

    public FeatureEventBatchResponse saveBatch(AiDevice device, FeatureEventBatchRequest request) {
        if (request == null || request.getEvents() == null || request.getEvents().isEmpty()) {
            return new FeatureEventBatchResponse(0, 0);
        }

        FeatureEventBatchResponse response = new FeatureEventBatchResponse(0, 0);
        int index = 0;
        for (FeatureEventBatchRequest.FeatureEventRequest event : request.getEvents()) {
            SaveResult result = saveOne(device, event);
            if (result.state == State.ACCEPTED) {
                response.setAccepted(response.getAccepted() + 1);
            } else if (result.state == State.SKIPPED) {
                response.setSkipped(response.getSkipped() + 1);
            } else if (result.state == State.REJECTED) {
                response.addRejection(index, resolveRequestEventId(event), resolveRequestFeatureCode(event), result.reason);
            }
            index++;
        }
        log.info("feature event batch saved. deviceId={}, accepted={}, skipped={}, rejected={}",
            device == null ? null : device.getIdDevice(), response.getAccepted(), response.getSkipped(), response.getRejected());
        return response;
    }

    private SaveResult saveOne(AiDevice device, FeatureEventBatchRequest.FeatureEventRequest request) {
        if (request == null) {
            return SaveResult.rejected("事件不能为空");
        }
        String featureCode = trimToNull(request.getFeatureCode());
        if (featureCode == null) {
            return SaveResult.rejected("featureCode 不能为空");
        }
        String featureName = FeatureEventCatalog.resolveName(featureCode);
        if (featureName == null) {
            return SaveResult.rejected("featureCode 不支持");
        }

        String idDevice = device == null ? null : trimToNull(device.getIdDevice());
        String idempotencyKey = trimToNull(request.getIdempotencyKey());
        if (idempotencyKey == null) {
            return SaveResult.rejected("idempotencyKey 不能为空");
        }
        if (idDevice != null && exists(idDevice, idempotencyKey)) {
            return SaveResult.skipped();
        }

        String payloadJson;
        try {
            payloadJson = writePayload(request);
        } catch (BusinessException ex) {
            return SaveResult.rejected(ex.getMessage());
        }

        AiFeatureEvent entity = new AiFeatureEvent();
        entity.setIdEvent(resolveEventId(request));
        entity.setIdDevice(idDevice);
        entity.setIdOrg(device == null ? null : trimToNull(device.getIdOrg()));
        entity.setIdRegion(device == null ? null : trimToNull(device.getIdRegion()));
        entity.setHisOrgId(trimToNull(request.getHisOrgId()));
        entity.setHisOrgName(trimToNull(request.getHisOrgName()));
        entity.setFeatureCode(featureCode);
        entity.setFeatureName(featureName);
        entity.setEventAction(trimToNull(request.getEventAction()));
        entity.setIdempotencyKey(idempotencyKey);
        entity.setTraceId(trimToNull(request.getTraceId()));
        entity.setConsultationId(trimToNull(request.getConsultationId()));
        entity.setSessionId(trimToNull(request.getSessionId()));
        entity.setSourceModule(trimToNull(request.getSourceModule()));
        entity.setSceneCode(trimToNull(request.getScene()));
        entity.setIdDoctor(trimToNull(request.getDoctorId()));
        entity.setNaDoctor(trimToNull(request.getDoctorName()));
        entity.setIdDept(trimToNull(request.getDeptId()));
        entity.setNaDept(trimToNull(request.getDeptName()));
        entity.setEventStatus(resolveStatus(request.getStatus()));
        entity.setPayloadJson(payloadJson);
        entity.setEventTime(resolveEventTime(request.getTimestamp()));
        entity.setFgActive("1");

        try {
            featureEventMapper.insert(entity);
            return SaveResult.accepted();
        } catch (DuplicateKeyException ex) {
            return SaveResult.skipped();
        }
    }

    private boolean exists(String idDevice, String idempotencyKey) {
        if (!StringUtils.hasText(idDevice) || !StringUtils.hasText(idempotencyKey)) {
            return false;
        }
        Long count = featureEventMapper.selectCount(new LambdaQueryWrapper<AiFeatureEvent>()
            .eq(AiFeatureEvent::getFgActive, "1")
            .eq(AiFeatureEvent::getIdDevice, idDevice)
            .eq(AiFeatureEvent::getIdempotencyKey, idempotencyKey));
        return count != null && count > 0;
    }

    private String resolveEventId(FeatureEventBatchRequest.FeatureEventRequest request) {
        String eventId = trimToNull(request.getEventId());
        if (eventId != null && eventId.length() <= 64) {
            return eventId;
        }
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String resolveStatus(String status) {
        String text = trimToNull(status);
        return text == null ? "success" : text;
    }

    private LocalDateTime resolveEventTime(Long timestamp) {
        if (timestamp == null || timestamp <= 0) {
            return LocalDateTime.now();
        }
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
    }

    private String writePayload(FeatureEventBatchRequest.FeatureEventRequest request) {
        Object payload = request.getPayload() == null ? Collections.emptyMap() : request.getPayload();
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new BusinessException("payload 序列化失败");
        } catch (StackOverflowError | RuntimeException ex) {
            throw new BusinessException("payload 序列化失败");
        }
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String resolveRequestEventId(FeatureEventBatchRequest.FeatureEventRequest request) {
        return request == null ? null : trimToNull(request.getEventId());
    }

    private String resolveRequestFeatureCode(FeatureEventBatchRequest.FeatureEventRequest request) {
        return request == null ? null : trimToNull(request.getFeatureCode());
    }

    private static final class SaveResult {
        private static final SaveResult ACCEPTED = new SaveResult(State.ACCEPTED, null);
        private static final SaveResult SKIPPED = new SaveResult(State.SKIPPED, null);

        private final State state;
        private final String reason;

        private SaveResult(State state, String reason) {
            this.state = state;
            this.reason = reason;
        }

        private static SaveResult accepted() {
            return ACCEPTED;
        }

        private static SaveResult skipped() {
            return SKIPPED;
        }

        private static SaveResult rejected(String reason) {
            return new SaveResult(State.REJECTED, reason);
        }
    }

    private enum State {
        ACCEPTED,
        SKIPPED,
        REJECTED
    }
}
