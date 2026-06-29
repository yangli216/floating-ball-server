package com.regionalai.floatingball.server.modules.recommendationpreference.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.regionalai.floatingball.server.common.exception.BusinessException;
import com.regionalai.floatingball.server.modules.config.dto.ResolvedAiConfig;
import com.regionalai.floatingball.server.modules.config.service.ConfigService;
import com.regionalai.floatingball.server.modules.device.entity.AiDevice;
import com.regionalai.floatingball.server.modules.recommendationpreference.dto.RecommendationPreferenceBatchRequest;
import com.regionalai.floatingball.server.modules.recommendationpreference.dto.RecommendationPreferenceBatchResponse;
import com.regionalai.floatingball.server.modules.recommendationpreference.dto.RecommendationPreferenceRankRequest;
import com.regionalai.floatingball.server.modules.recommendationpreference.dto.RecommendationPreferenceRankResponse;
import com.regionalai.floatingball.server.modules.recommendationpreference.entity.AiRecommendationPreferenceAggregate;
import com.regionalai.floatingball.server.modules.recommendationpreference.entity.AiRecommendationPreferenceEvent;
import com.regionalai.floatingball.server.modules.recommendationpreference.mapper.AiRecommendationPreferenceAggregateMapper;
import com.regionalai.floatingball.server.modules.recommendationpreference.mapper.AiRecommendationPreferenceEventMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class RecommendationPreferenceService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationPreferenceService.class);

    private static final String FEATURE_COLLECTION = "recommendationPreferenceCollection";
    private static final String FEATURE_RERANK = "recommendationPreferenceRerank";
    private static final int DEFAULT_RERANK_MIN_COUNT = 3;
    private static final int DEFAULT_CONFIDENCE_FULL_SAMPLE_COUNT = 20;
    private static final double DEFAULT_MAX_RANK_BOOST = 1.2D;
    private static final double DOCTOR_SCOPE_WEIGHT = 1D;
    private static final double DEPT_SCOPE_WEIGHT = 0.7D;
    private static final double ORG_SCOPE_WEIGHT = 0.45D;
    private static final int MAX_CANDIDATES = 50;

    private static final Set<String> SUPPORTED_TYPES = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
        "diagnosis", "medicine", "exam", "lab_test", "procedure"
    )));
    private static final Set<String> SUPPORTED_ACTIONS = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
        "final_select", "manual_match", "confirm_match"
    )));

    private final AiRecommendationPreferenceEventMapper eventMapper;
    private final AiRecommendationPreferenceAggregateMapper aggregateMapper;
    private final ConfigService configService;
    private final ObjectMapper objectMapper;

    public RecommendationPreferenceService(AiRecommendationPreferenceEventMapper eventMapper,
                                           AiRecommendationPreferenceAggregateMapper aggregateMapper,
                                           ConfigService configService,
                                           ObjectMapper objectMapper) {
        this.eventMapper = eventMapper;
        this.aggregateMapper = aggregateMapper;
        this.configService = configService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public RecommendationPreferenceBatchResponse saveBatch(AiDevice device, RecommendationPreferenceBatchRequest request) {
        if (!isCollectionEnabled(device)) {
            return new RecommendationPreferenceBatchResponse();
        }
        if (request == null || request.getEvents() == null || request.getEvents().isEmpty()) {
            return new RecommendationPreferenceBatchResponse();
        }

        RecommendationPreferenceBatchResponse response = new RecommendationPreferenceBatchResponse();
        int index = 0;
        for (RecommendationPreferenceBatchRequest.RecommendationPreferenceEventRequest event : request.getEvents()) {
            SaveResult result = saveOne(device, event);
            if (result.state == State.ACCEPTED) {
                response.setAccepted(response.getAccepted() + 1);
            } else if (result.state == State.SKIPPED) {
                response.setSkipped(response.getSkipped() + 1);
            } else {
                response.addRejection(index, resolveRequestEventId(event), resolveRequestRecommendationType(event), result.reason);
            }
            index++;
        }
        log.info("recommendation preference batch saved. deviceId={}, accepted={}, skipped={}, rejected={}",
            device == null ? null : device.getIdDevice(), response.getAccepted(), response.getSkipped(), response.getRejected());
        return response;
    }

    public RecommendationPreferenceRankResponse rank(AiDevice device, RecommendationPreferenceRankRequest request) {
        String type = trimToNull(request == null ? null : request.getRecommendationType());
        RecommendationPreferenceRankResponse response = new RecommendationPreferenceRankResponse(false, type);
        if (!isRerankEnabled(device) || type == null || !SUPPORTED_TYPES.contains(type)) {
            return response;
        }
        if (request.getCandidates() == null || request.getCandidates().isEmpty()) {
            response.setEnabled(true);
            return response;
        }

        response.setEnabled(true);
        String doctorId = trimToNull(request.getDoctorId());
        String deptId = trimToNull(request.getDeptId());
        String orgId = device == null ? null : trimToNull(device.getIdOrg());

        int count = 0;
        for (RecommendationPreferenceRankRequest.Candidate candidate : request.getCandidates()) {
            if (count >= MAX_CANDIDATES) {
                break;
            }
            count++;

            String itemKey = trimToNull(candidate == null ? null : candidate.getItemKey());
            if (itemKey == null) {
                continue;
            }

            ScopedAggregate scoped = findBestAggregate(orgId, deptId, doctorId, type, itemKey);
            int samples = scoped == null ? 0 : sampleCount(scoped.aggregate);
            if (scoped == null || samples < DEFAULT_RERANK_MIN_COUNT) {
                response.getItems().add(new RecommendationPreferenceRankResponse.Item(itemKey, 0D, 0D, samples, "none", "insufficient_samples"));
                continue;
            }

            double score = scoped.aggregate.getPreferenceScore() == null ? 0D : scoped.aggregate.getPreferenceScore().doubleValue();
            double boost = rankBoost(score, samples, scoped.scope);
            response.getItems().add(new RecommendationPreferenceRankResponse.Item(
                itemKey,
                round(score),
                round(boost),
                samples,
                scoped.scope,
                scoped.scope + "_preference"
            ));
        }
        return response;
    }

    private SaveResult saveOne(AiDevice device, RecommendationPreferenceBatchRequest.RecommendationPreferenceEventRequest request) {
        if (request == null) {
            return SaveResult.rejected("事件不能为空");
        }
        String type = trimToNull(request.getRecommendationType());
        if (type == null || !SUPPORTED_TYPES.contains(type)) {
            return SaveResult.rejected("recommendationType 不支持");
        }
        String action = trimToNull(request.getAction());
        if (action == null || !SUPPORTED_ACTIONS.contains(action)) {
            return SaveResult.rejected("action 不支持");
        }
        String itemKey = trimToNull(request.getItemKey());
        if (itemKey == null) {
            return SaveResult.rejected("itemKey 不能为空");
        }
        if (!isValidItemKey(type, itemKey)) {
            return SaveResult.rejected("itemKey 与 recommendationType 不匹配");
        }
        String idempotencyKey = resolveIdempotencyKey(request);
        if (idempotencyKey == null) {
            return SaveResult.rejected("eventId 或 idempotencyKey 不能为空");
        }

        String idDevice = device == null ? null : trimToNull(device.getIdDevice());
        if (idDevice != null && existsEvent(idDevice, idempotencyKey)) {
            return SaveResult.skipped();
        }

        String payloadJson;
        try {
            payloadJson = writePayload(request);
        } catch (BusinessException ex) {
            return SaveResult.rejected(ex.getMessage());
        }

        AiRecommendationPreferenceEvent entity = toEventEntity(device, request, type, action, itemKey, idempotencyKey, payloadJson);

        try {
            eventMapper.insert(entity);
            if (shouldAggregate(request, action)) {
                updateAggregates(entity);
            }
            return SaveResult.accepted();
        } catch (DuplicateKeyException ex) {
            return SaveResult.skipped();
        }
    }

    private AiRecommendationPreferenceEvent toEventEntity(AiDevice device,
                                                          RecommendationPreferenceBatchRequest.RecommendationPreferenceEventRequest request,
                                                          String type,
                                                          String action,
                                                          String itemKey,
                                                          String idempotencyKey,
                                                          String payloadJson) {
        AiRecommendationPreferenceEvent entity = new AiRecommendationPreferenceEvent();
        entity.setIdEvent(resolveEventId(request.getEventId()));
        entity.setIdDevice(device == null ? null : trimToNull(device.getIdDevice()));
        entity.setIdOrg(device == null ? null : trimToNull(device.getIdOrg()));
        entity.setIdRegion(device == null ? null : trimToNull(device.getIdRegion()));
        entity.setRecommendationType(type);
        entity.setActionCode(action);
        entity.setIdempotencyKey(idempotencyKey);
        entity.setItemKey(itemKey);
        entity.setItemId(trimToNull(request.getItemId()));
        entity.setItemCode(trimToNull(request.getItemCode()));
        entity.setItemName(trimToNull(request.getItemName()));
        entity.setFgSelected(Boolean.FALSE.equals(request.getSelected()) ? "0" : "1");
        entity.setFgPrimary(Boolean.TRUE.equals(request.getPrimary()) ? "1" : "0");
        entity.setTraceId(trimToNull(request.getTraceId()));
        entity.setConsultationId(trimToNull(request.getConsultationId()));
        entity.setSessionId(trimToNull(request.getSessionId()));
        entity.setSourceModule(trimToNull(request.getSourceModule()));
        entity.setSceneCode(trimToNull(request.getScene()));
        entity.setIdDoctor(trimToNull(request.getDoctorId()));
        entity.setNaDoctor(trimToNull(request.getDoctorName()));
        entity.setIdDept(trimToNull(request.getDeptId()));
        entity.setNaDept(trimToNull(request.getDeptName()));
        entity.setPromptVersion(trimToNull(request.getPromptVersion()));
        entity.setTemplateVersion(trimToNull(request.getTemplateVersion()));
        entity.setModelVersion(trimToNull(request.getModelVersion()));
        entity.setPayloadJson(payloadJson);
        entity.setEventTime(resolveEventTime(request.getTimestamp()));
        entity.setFgActive("1");
        return entity;
    }

    private boolean shouldAggregate(RecommendationPreferenceBatchRequest.RecommendationPreferenceEventRequest request, String action) {
        return Boolean.TRUE.equals(request.getSelected()) || "manual_match".equals(action) || "confirm_match".equals(action);
    }

    private void updateAggregates(AiRecommendationPreferenceEvent event) {
        upsertAggregate(event, null, null);
        if (StringUtils.hasText(event.getIdDept())) {
            upsertAggregate(event, event.getIdDept(), null);
        }
        if (StringUtils.hasText(event.getIdDoctor())) {
            upsertAggregate(event, event.getIdDept(), event.getIdDoctor());
        }
    }

    private void upsertAggregate(AiRecommendationPreferenceEvent event, String deptId, String doctorId) {
        AiRecommendationPreferenceAggregate aggregate = findAggregate(
            event.getIdOrg(), deptId, doctorId, event.getRecommendationType(), event.getItemKey());
        boolean created = aggregate == null;
        if (aggregate == null) {
            aggregate = new AiRecommendationPreferenceAggregate();
            aggregate.setIdAgg(UUID.randomUUID().toString().replace("-", ""));
            aggregate.setIdOrg(event.getIdOrg());
            aggregate.setIdRegion(event.getIdRegion());
            aggregate.setIdDept(deptId);
            aggregate.setIdDoctor(doctorId);
            aggregate.setRecommendationType(event.getRecommendationType());
            aggregate.setItemKey(event.getItemKey());
            aggregate.setItemId(event.getItemId());
            aggregate.setItemCode(event.getItemCode());
            aggregate.setItemName(event.getItemName());
            aggregate.setSelectedCount(0);
            aggregate.setConfirmCount(0);
            aggregate.setManualMatchCount(0);
            aggregate.setPreferenceScore(BigDecimal.ZERO);
            aggregate.setFgActive("1");
        }

        aggregate.setSelectedCount(safeInt(aggregate.getSelectedCount()) + ("1".equals(event.getFgSelected()) ? 1 : 0));
        aggregate.setConfirmCount(safeInt(aggregate.getConfirmCount()) + ("confirm_match".equals(event.getActionCode()) ? 1 : 0));
        aggregate.setManualMatchCount(safeInt(aggregate.getManualMatchCount()) + ("manual_match".equals(event.getActionCode()) ? 1 : 0));
        aggregate.setLastEventTime(event.getEventTime());
        aggregate.setPreferenceScore(calculatePreferenceScore(aggregate));
        if (StringUtils.hasText(event.getItemName())) {
            aggregate.setItemName(event.getItemName());
        }
        if (StringUtils.hasText(event.getItemCode())) {
            aggregate.setItemCode(event.getItemCode());
        }
        if (StringUtils.hasText(event.getItemId())) {
            aggregate.setItemId(event.getItemId());
        }

        if (created) {
            aggregateMapper.insert(aggregate);
        } else {
            aggregateMapper.updateById(aggregate);
        }
    }

    private BigDecimal calculatePreferenceScore(AiRecommendationPreferenceAggregate aggregate) {
        int selected = safeInt(aggregate.getSelectedCount());
        int confirmed = safeInt(aggregate.getConfirmCount());
        int manual = safeInt(aggregate.getManualMatchCount());
        int weighted = selected + confirmed + (manual * 2);
        double score = Math.min(1D, weighted / 10D);
        return BigDecimal.valueOf(score).setScale(4, RoundingMode.HALF_UP);
    }

    private AiRecommendationPreferenceAggregate findAggregate(String orgId,
                                                              String deptId,
                                                              String doctorId,
                                                              String type,
                                                              String itemKey) {
        LambdaQueryWrapper<AiRecommendationPreferenceAggregate> wrapper = new LambdaQueryWrapper<AiRecommendationPreferenceAggregate>()
            .eq(AiRecommendationPreferenceAggregate::getFgActive, "1")
            .eq(AiRecommendationPreferenceAggregate::getRecommendationType, type)
            .eq(AiRecommendationPreferenceAggregate::getItemKey, itemKey);
        eqOrIsNull(wrapper, AiRecommendationPreferenceAggregate::getIdOrg, orgId);
        eqOrIsNull(wrapper, AiRecommendationPreferenceAggregate::getIdDept, deptId);
        eqOrIsNull(wrapper, AiRecommendationPreferenceAggregate::getIdDoctor, doctorId);
        return aggregateMapper.selectOne(wrapper);
    }

    private ScopedAggregate findBestAggregate(String orgId, String deptId, String doctorId, String type, String itemKey) {
        if (StringUtils.hasText(doctorId)) {
            AiRecommendationPreferenceAggregate aggregate = findAggregate(orgId, deptId, doctorId, type, itemKey);
            if (aggregate != null) {
                return new ScopedAggregate("doctor", aggregate);
            }
        }
        if (StringUtils.hasText(deptId)) {
            AiRecommendationPreferenceAggregate aggregate = findAggregate(orgId, deptId, null, type, itemKey);
            if (aggregate != null) {
                return new ScopedAggregate("dept", aggregate);
            }
        }
        AiRecommendationPreferenceAggregate aggregate = findAggregate(orgId, null, null, type, itemKey);
        return aggregate == null ? null : new ScopedAggregate("org", aggregate);
    }

    private void eqOrIsNull(LambdaQueryWrapper<AiRecommendationPreferenceAggregate> wrapper,
                            com.baomidou.mybatisplus.core.toolkit.support.SFunction<AiRecommendationPreferenceAggregate, ?> column,
                            String value) {
        if (StringUtils.hasText(value)) {
            wrapper.eq(column, value);
        } else {
            wrapper.isNull(column);
        }
    }

    private boolean existsEvent(String idDevice, String idempotencyKey) {
        Long count = eventMapper.selectCount(new LambdaQueryWrapper<AiRecommendationPreferenceEvent>()
            .eq(AiRecommendationPreferenceEvent::getFgActive, "1")
            .eq(AiRecommendationPreferenceEvent::getIdDevice, idDevice)
            .eq(AiRecommendationPreferenceEvent::getIdempotencyKey, idempotencyKey));
        return count != null && count > 0;
    }

    private boolean isCollectionEnabled(AiDevice device) {
        return !Boolean.FALSE.equals(resolveFeatures(device).get(FEATURE_COLLECTION));
    }

    private boolean isRerankEnabled(AiDevice device) {
        return Boolean.TRUE.equals(resolveFeatures(device).get(FEATURE_RERANK));
    }

    private Map<String, Boolean> resolveFeatures(AiDevice device) {
        if (device == null) {
            return Collections.emptyMap();
        }
        try {
            ResolvedAiConfig config = configService.resolveByDevice(device);
            return config.getFeatures() == null ? Collections.<String, Boolean>emptyMap() : config.getFeatures();
        } catch (RuntimeException ex) {
            log.warn("recommendation preference feature resolve failed. deviceId={}, error={}",
                device.getIdDevice(), ex.getMessage());
            return Collections.emptyMap();
        }
    }

    private String writePayload(RecommendationPreferenceBatchRequest.RecommendationPreferenceEventRequest request) {
        Map<String, Object> payload = request.getPayload() == null
            ? Collections.<String, Object>emptyMap()
            : new HashMap<String, Object>(request.getPayload());
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new BusinessException("payload 序列化失败");
        } catch (StackOverflowError | RuntimeException ex) {
            throw new BusinessException("payload 序列化失败");
        }
    }

    private String resolveEventId(String eventId) {
        String normalized = trimToNull(eventId);
        if (normalized != null && normalized.length() <= 64) {
            return normalized;
        }
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String resolveIdempotencyKey(RecommendationPreferenceBatchRequest.RecommendationPreferenceEventRequest request) {
        String idempotencyKey = trimToNull(request.getIdempotencyKey());
        if (idempotencyKey != null) {
            return idempotencyKey;
        }
        String eventId = trimToNull(request.getEventId());
        return eventId == null ? null : "event:" + eventId;
    }

    private boolean isValidItemKey(String type, String itemKey) {
        if ("diagnosis".equals(type)) {
            return itemKey.startsWith("diagnosis:") || itemKey.startsWith("diagnosis-code:");
        }
        return itemKey.startsWith("order:" + type + ":");
    }

    private LocalDateTime resolveEventTime(Long timestamp) {
        if (timestamp == null || timestamp <= 0) {
            return LocalDateTime.now();
        }
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
    }

    private int sampleCount(AiRecommendationPreferenceAggregate aggregate) {
        return safeInt(aggregate.getSelectedCount()) + safeInt(aggregate.getConfirmCount()) + safeInt(aggregate.getManualMatchCount());
    }

    private double rankBoost(double preferenceScore, int sampleCount, String scope) {
        double score = Math.min(1D, Math.max(0D, preferenceScore));
        double boost = DEFAULT_MAX_RANK_BOOST * score * sampleConfidence(sampleCount) * scopeWeight(scope);
        return Math.min(DEFAULT_MAX_RANK_BOOST, Math.max(0D, boost));
    }

    private double sampleConfidence(int sampleCount) {
        if (sampleCount < DEFAULT_RERANK_MIN_COUNT) {
            return 0D;
        }
        return Math.min(1D, Math.log(1D + sampleCount) / Math.log(1D + DEFAULT_CONFIDENCE_FULL_SAMPLE_COUNT));
    }

    private double scopeWeight(String scope) {
        if ("doctor".equals(scope)) {
            return DOCTOR_SCOPE_WEIGHT;
        }
        if ("dept".equals(scope)) {
            return DEPT_SCOPE_WEIGHT;
        }
        if ("org".equals(scope)) {
            return ORG_SCOPE_WEIGHT;
        }
        return 0D;
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP).doubleValue();
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String resolveRequestEventId(RecommendationPreferenceBatchRequest.RecommendationPreferenceEventRequest request) {
        return request == null ? null : trimToNull(request.getEventId());
    }

    private String resolveRequestRecommendationType(RecommendationPreferenceBatchRequest.RecommendationPreferenceEventRequest request) {
        return request == null ? null : trimToNull(request.getRecommendationType());
    }

    private static final class ScopedAggregate {
        private final String scope;
        private final AiRecommendationPreferenceAggregate aggregate;

        private ScopedAggregate(String scope, AiRecommendationPreferenceAggregate aggregate) {
            this.scope = scope;
            this.aggregate = aggregate;
        }
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
