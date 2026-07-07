package com.regionalai.floatingball.server.modules.patientmemory.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.regionalai.floatingball.server.common.exception.BusinessException;
import com.regionalai.floatingball.server.common.util.ObjectIdUtils;
import com.regionalai.floatingball.server.modules.device.entity.AiDevice;
import com.regionalai.floatingball.server.modules.patientmemory.dto.PatientMemoryBriefVO;
import com.regionalai.floatingball.server.modules.patientmemory.dto.PatientMemoryResolveRequest;
import com.regionalai.floatingball.server.modules.patientmemory.dto.PatientMemoryResolveResponse;
import com.regionalai.floatingball.server.modules.patientmemory.dto.PatientMemorySyncRequest;
import com.regionalai.floatingball.server.modules.patientmemory.dto.PatientMemorySyncResponse;
import com.regionalai.floatingball.server.modules.patientmemory.entity.AiPatientMemory;
import com.regionalai.floatingball.server.modules.patientmemory.entity.AiPatientMemoryFact;
import com.regionalai.floatingball.server.modules.patientmemory.entity.AiPatientMemoryObservation;
import com.regionalai.floatingball.server.modules.patientmemory.mapper.AiPatientMemoryFactMapper;
import com.regionalai.floatingball.server.modules.patientmemory.mapper.AiPatientMemoryMapper;
import com.regionalai.floatingball.server.modules.patientmemory.mapper.AiPatientMemoryObservationMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

@Service
public class PatientMemoryService {

    private static final Logger log = LoggerFactory.getLogger(PatientMemoryService.class);

    private static final int MAX_OBSERVATIONS = 100;
    private static final int MAX_FACTS_PER_OBSERVATION = 100;
    private static final int MAX_OBSERVATION_JSON_BYTES = 256 * 1024;
    private static final int MAX_BRIEF_ITEMS_PER_SECTION = 8;

    private static final Set<String> SUPPORTED_SOURCE_TYPES = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
        "patient_profile", "allergy_snapshot", "visit_summary", "outpatient_record",
        "lab_report", "exam_report", "doctor_confirmation"
    )));
    private static final Set<String> SUPPORTED_FACT_TYPES = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
        "allergy", "chronic_condition", "diagnosis", "medication", "procedure",
        "lab_result", "exam_result", "vital", "history", "reminder"
    )));
    private static final Set<String> SUPPORTED_STATUSES = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
        "active", "historical", "inactive", "unknown", "disputed"
    )));
    private static final Set<String> SUPPORTED_CONFIDENCE = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
        "confirmed", "structured", "extracted", "low"
    )));

    private final AiPatientMemoryMapper memoryMapper;
    private final AiPatientMemoryObservationMapper observationMapper;
    private final AiPatientMemoryFactMapper factMapper;
    private final ObjectMapper objectMapper;

    public PatientMemoryService(AiPatientMemoryMapper memoryMapper,
                                AiPatientMemoryObservationMapper observationMapper,
                                AiPatientMemoryFactMapper factMapper,
                                ObjectMapper objectMapper) {
        this.memoryMapper = memoryMapper;
        this.observationMapper = observationMapper;
        this.factMapper = factMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public synchronized PatientMemorySyncResponse sync(AiDevice device, PatientMemorySyncRequest request) {
        validateDevice(device);
        PatientMemorySyncRequest.PatientIdentity patient = validatePatient(request);
        String patientId = required(patient.getPatientId(), "patient.patientId 不能为空", 128);
        String hisOrgId = resolveHisOrgId(device, patient.getHisOrgId());

        AiPatientMemory memory = findOrCreateMemory(device, patient, patientId, hisOrgId);
        boolean profileChanged = mergePatientProfile(memory, patient);
        PatientMemorySyncResponse response = new PatientMemorySyncResponse();
        Set<String> changedFactTypes = new LinkedHashSet<String>();

        List<PatientMemorySyncRequest.Observation> observations = request.getObservations() == null
            ? Collections.<PatientMemorySyncRequest.Observation>emptyList()
            : request.getObservations();
        if (observations.size() > MAX_OBSERVATIONS) {
            throw new BusinessException("单次最多同步 " + MAX_OBSERVATIONS + " 条患者记忆观察");
        }

        LocalDateTime latestSourceTime = memory.getLastSourceTime();
        int index = 0;
        for (PatientMemorySyncRequest.Observation observation : observations) {
            ObservationValidation validation = validateObservation(observation);
            if (!validation.valid) {
                response.addRejection(index, observation == null ? null : trimToNull(observation.getSourceKey()), validation.reason);
                index++;
                continue;
            }

            String payloadHash = hashObservation(observation, validation.operation);
            String sourceKeyHash = sha256Hex(validation.sourceKey);
            if (observationExists(memory.getIdMemory(), sourceKeyHash, payloadHash)) {
                response.setSkipped(response.getSkipped() + 1);
                index++;
                continue;
            }

            AiPatientMemoryObservation saved = saveObservation(device, memory, observation, validation, sourceKeyHash, payloadHash);
            if ("tombstone".equals(validation.operation)) {
                changedFactTypes.addAll(tombstoneFacts(memory.getIdMemory(), validation.sourceKey, saved));
            } else {
                changedFactTypes.addAll(projectFacts(memory.getIdMemory(), observation, validation, saved));
            }
            response.setAccepted(response.getAccepted() + 1);
            if (saved.getOccurredTime() != null && (latestSourceTime == null || saved.getOccurredTime().isAfter(latestSourceTime))) {
                latestSourceTime = saved.getOccurredTime();
            }
            index++;
        }

        boolean memoryChanged = profileChanged || response.getAccepted() > 0;
        memory.setLastSyncTime(LocalDateTime.now());
        memory.setLastSourceTime(latestSourceTime);
        if (memoryChanged) {
            memory.setMemoryVersion(valueOrZero(memory.getMemoryVersion()) + 1L);
        }

        PatientMemoryBriefVO brief = refreshSummary(memory);

        response.setMemoryId(memory.getIdMemory());
        response.setMemoryVersion(memory.getMemoryVersion());
        response.setNextCursor("v:" + valueOrZero(memory.getMemoryVersion()));
        response.setChangedFactTypes(changedFactTypes);
        response.setBrief(brief);

        log.info("patient memory sync completed. deviceId={}, orgId={}, memoryId={}, accepted={}, skipped={}, rejected={}, version={}",
            device.getIdDevice(), device.getIdOrg(), memory.getIdMemory(), response.getAccepted(), response.getSkipped(),
            response.getRejected().size(), response.getMemoryVersion());
        return response;
    }

    PatientMemoryBriefVO refreshSummary(AiPatientMemory memory) {
        List<AiPatientMemoryFact> facts = loadActiveFacts(memory.getIdMemory());
        PatientMemoryBriefVO brief = buildBrief(memory, facts);
        memory.setSummaryJson(writeJson(brief, "患者记忆摘要序列化失败"));
        memory.setQualityStatus(brief.getQualityStatus());
        memoryMapper.updateById(memory);
        return brief;
    }

    public PatientMemoryResolveResponse resolve(AiDevice device, PatientMemoryResolveRequest request) {
        validateDevice(device);
        String patientId = required(request == null ? null : request.getPatientId(), "patientId 不能为空", 128);
        String hisOrgId = resolveHisOrgId(device, request == null ? null : request.getHisOrgId());
        AiPatientMemory memory = findMemory(device.getIdOrg(), hisOrgId, patientId);

        PatientMemoryResolveResponse response = new PatientMemoryResolveResponse();
        if (memory == null) {
            response.setFound(false);
            response.setNotModified(false);
            return response;
        }

        response.setFound(true);
        response.setMemoryVersion(memory.getMemoryVersion());
        Long knownVersion = request == null ? null : request.getKnownMemoryVersion();
        if (knownVersion != null && knownVersion.equals(memory.getMemoryVersion())) {
            response.setNotModified(true);
            return response;
        }
        response.setBrief(buildBrief(memory, loadActiveFacts(memory.getIdMemory())));
        return response;
    }

    private void validateDevice(AiDevice device) {
        if (device == null || !StringUtils.hasText(device.getIdOrg())) {
            throw new BusinessException("设备机构上下文缺失");
        }
    }

    private PatientMemorySyncRequest.PatientIdentity validatePatient(PatientMemorySyncRequest request) {
        if (request == null || request.getPatient() == null) {
            throw new BusinessException("patient 不能为空");
        }
        return request.getPatient();
    }

    private AiPatientMemory findOrCreateMemory(AiDevice device,
                                               PatientMemorySyncRequest.PatientIdentity patient,
                                               String patientId,
                                               String hisOrgId) {
        AiPatientMemory existing = findMemory(device.getIdOrg(), hisOrgId, patientId);
        if (existing != null) {
            return existing;
        }

        AiPatientMemory created = new AiPatientMemory();
        created.setIdMemory(ObjectIdUtils.next());
        created.setIdOrg(device.getIdOrg());
        created.setIdRegion(trimToNull(device.getIdRegion()));
        created.setIdHisOrg(hisOrgId);
        created.setPatientId(patientId);
        created.setPatientName(truncate(patient.getName(), 128));
        created.setPatientGender(truncate(patient.getGender(), 16));
        created.setPatientAge(truncate(patient.getAgeText(), 32));
        created.setPatientBirthDate(truncate(patient.getBirthDate(), 32));
        created.setMemoryVersion(0L);
        created.setConflictCount(0);
        created.setQualityStatus("partial");
        created.setSdStatus("active");
        created.setFgActive("1");
        try {
            memoryMapper.insert(created);
            return created;
        } catch (DuplicateKeyException ex) {
            AiPatientMemory concurrent = findMemory(device.getIdOrg(), hisOrgId, patientId);
            if (concurrent != null) {
                return concurrent;
            }
            throw ex;
        }
    }

    private AiPatientMemory findMemory(String orgId, String hisOrgId, String patientId) {
        return memoryMapper.selectOne(new LambdaQueryWrapper<AiPatientMemory>()
            .eq(AiPatientMemory::getFgActive, "1")
            .eq(AiPatientMemory::getIdOrg, orgId)
            .eq(AiPatientMemory::getIdHisOrg, hisOrgId)
            .eq(AiPatientMemory::getPatientId, patientId));
    }

    private boolean mergePatientProfile(AiPatientMemory memory, PatientMemorySyncRequest.PatientIdentity patient) {
        boolean changed = false;
        String incomingName = truncate(patient.getName(), 128);
        String incomingGender = truncate(patient.getGender(), 16);
        String incomingAge = truncate(patient.getAgeText(), 32);
        String incomingBirthDate = truncate(patient.getBirthDate(), 32);

        changed |= assignIfPresentAndDifferent(memory.getPatientName(), incomingName, memory::setPatientName);
        changed |= assignIfPresentAndDifferent(memory.getPatientGender(), incomingGender, memory::setPatientGender);
        changed |= assignIfPresentAndDifferent(memory.getPatientAge(), incomingAge, memory::setPatientAge);
        changed |= assignIfPresentAndDifferent(memory.getPatientBirthDate(), incomingBirthDate, memory::setPatientBirthDate);
        return changed;
    }

    private boolean assignIfPresentAndDifferent(String current, String incoming, StringSetter setter) {
        if (!StringUtils.hasText(incoming) || incoming.equals(current)) {
            return false;
        }
        setter.set(incoming);
        return true;
    }

    private ObservationValidation validateObservation(PatientMemorySyncRequest.Observation observation) {
        if (observation == null) {
            return ObservationValidation.rejected("观察不能为空");
        }
        String sourceKey = trimToNull(observation.getSourceKey());
        if (sourceKey == null) {
            return ObservationValidation.rejected("sourceKey 不能为空");
        }
        if (sourceKey.length() > 256) {
            return ObservationValidation.rejected("sourceKey 不能超过 256 个字符");
        }
        String sourceType = normalizeLower(observation.getSourceType());
        if (sourceType == null || !SUPPORTED_SOURCE_TYPES.contains(sourceType)) {
            return ObservationValidation.rejected("sourceType 不支持");
        }
        String operation = normalizeLower(observation.getOperation());
        operation = operation == null ? "upsert" : operation;
        if (!"upsert".equals(operation) && !"tombstone".equals(operation)) {
            return ObservationValidation.rejected("operation 仅支持 upsert 或 tombstone");
        }
        List<PatientMemorySyncRequest.ClinicalFact> facts = observation.getFacts();
        if (facts != null && facts.size() > MAX_FACTS_PER_OBSERVATION) {
            return ObservationValidation.rejected("单条观察最多包含 " + MAX_FACTS_PER_OBSERVATION + " 个事实");
        }
        if ("upsert".equals(operation) && facts != null) {
            for (PatientMemorySyncRequest.ClinicalFact fact : facts) {
                String reason = validateFact(fact);
                if (reason != null) {
                    return ObservationValidation.rejected(reason);
                }
            }
        }
        return ObservationValidation.accepted(sourceKey, sourceType, operation);
    }

    private String validateFact(PatientMemorySyncRequest.ClinicalFact fact) {
        if (fact == null) {
            return "临床事实不能为空";
        }
        String factType = normalizeLower(fact.getFactType());
        if (factType == null || !SUPPORTED_FACT_TYPES.contains(factType)) {
            return "factType 不支持";
        }
        if (!StringUtils.hasText(fact.getCode()) && !StringUtils.hasText(fact.getName()) && !StringUtils.hasText(fact.getValueText())) {
            return "临床事实至少需要 code、name 或 valueText";
        }
        String status = normalizeLower(fact.getStatus());
        if (status != null && !SUPPORTED_STATUSES.contains(status)) {
            return "事实 status 不支持";
        }
        String confidence = normalizeLower(fact.getConfidence());
        if (confidence != null && !SUPPORTED_CONFIDENCE.contains(confidence)) {
            return "事实 confidence 不支持";
        }
        return null;
    }

    private boolean observationExists(String memoryId, String sourceKeyHash, String payloadHash) {
        Long count = observationMapper.selectCount(new LambdaQueryWrapper<AiPatientMemoryObservation>()
            .eq(AiPatientMemoryObservation::getFgActive, "1")
            .eq(AiPatientMemoryObservation::getIdMemory, memoryId)
            .eq(AiPatientMemoryObservation::getSourceKeyHash, sourceKeyHash)
            .eq(AiPatientMemoryObservation::getPayloadHash, payloadHash));
        return count != null && count > 0;
    }

    private AiPatientMemoryObservation saveObservation(AiDevice device,
                                                       AiPatientMemory memory,
                                                       PatientMemorySyncRequest.Observation request,
                                                       ObservationValidation validation,
                                                       String sourceKeyHash,
                                                       String payloadHash) {
        String payloadJson = writeJson(request.getPayload() == null ? Collections.emptyMap() : request.getPayload(), "观察 payload 序列化失败");
        String factsJson = writeJson(request.getFacts() == null ? Collections.emptyList() : request.getFacts(), "观察 facts 序列化失败");
        if (payloadJson.getBytes(StandardCharsets.UTF_8).length + factsJson.getBytes(StandardCharsets.UTF_8).length > MAX_OBSERVATION_JSON_BYTES) {
            throw new BusinessException("单条患者记忆观察不能超过 256KB");
        }

        AiPatientMemoryObservation entity = new AiPatientMemoryObservation();
        entity.setIdObservation(ObjectIdUtils.next());
        entity.setIdMemory(memory.getIdMemory());
        entity.setIdDevice(trimToNull(device.getIdDevice()));
        entity.setSourceKey(validation.sourceKey);
        entity.setSourceKeyHash(sourceKeyHash);
        entity.setSourceType(validation.sourceType);
        entity.setSourceVersion(truncate(request.getSourceVersion(), 128));
        entity.setOperationCode(validation.operation);
        entity.setPayloadHash(payloadHash);
        entity.setVisitId(truncate(request.getVisitId(), 128));
        entity.setOccurredTime(resolveTime(request.getOccurredAt()));
        entity.setPayloadJson(payloadJson);
        entity.setFactsJson(factsJson);
        entity.setFgLatest("1");
        entity.setFgActive("1");
        observationMapper.insert(entity);

        observationMapper.update(null, new UpdateWrapper<AiPatientMemoryObservation>()
            .eq("fg_active", "1")
            .eq("id_memory", memory.getIdMemory())
            .eq("source_key_hash", sourceKeyHash)
            .ne("id_observation", entity.getIdObservation())
            .set("fg_latest", "0"));
        return entity;
    }

    private Set<String> projectFacts(String memoryId,
                                     PatientMemorySyncRequest.Observation observation,
                                     ObservationValidation validation,
                                     AiPatientMemoryObservation savedObservation) {
        Set<String> changedTypes = new LinkedHashSet<String>();
        List<PatientMemorySyncRequest.ClinicalFact> facts = observation.getFacts() == null
            ? Collections.<PatientMemorySyncRequest.ClinicalFact>emptyList()
            : observation.getFacts();
        Set<String> incomingFactKeyHashes = new HashSet<String>();
        for (PatientMemorySyncRequest.ClinicalFact factRequest : facts) {
            String factType = normalizeLower(factRequest.getFactType());
            incomingFactKeyHashes.add(sha256Hex(resolveFactKey(factRequest, factType)));
        }
        changedTypes.addAll(retireMissingSourceFacts(
            memoryId,
            validation.sourceKey,
            incomingFactKeyHashes,
            savedObservation
        ));
        for (PatientMemorySyncRequest.ClinicalFact factRequest : facts) {
            String factType = normalizeLower(factRequest.getFactType());
            String factKey = resolveFactKey(factRequest, factType);
            String factKeyHash = sha256Hex(factKey);
            AiPatientMemoryFact fact = factMapper.selectOne(new LambdaQueryWrapper<AiPatientMemoryFact>()
                .eq(AiPatientMemoryFact::getFgActive, "1")
                .eq(AiPatientMemoryFact::getIdMemory, memoryId)
                .eq(AiPatientMemoryFact::getFactKeyHash, factKeyHash));

            LocalDateTime observedAt = savedObservation.getOccurredTime() == null ? LocalDateTime.now() : savedObservation.getOccurredTime();
            if (fact == null) {
                fact = new AiPatientMemoryFact();
                fact.setIdFact(ObjectIdUtils.next());
                fact.setIdMemory(memoryId);
                fact.setFactKey(factKey);
                fact.setFactKeyHash(factKeyHash);
                fact.setFirstObservedTime(observedAt);
                fact.setRevisionNo(1);
                fact.setOriginCode("doctor_confirmation".equals(validation.sourceType) ? "doctor" : "his");
                fact.setFgSuppressed("0");
                fact.setFgActive("1");
                applyFactValues(fact, factRequest, validation, savedObservation, observedAt);
                factMapper.insert(fact);
            } else if (!"admin".equals(fact.getOriginCode()) && !"doctor".equals(fact.getOriginCode())) {
                fact.setRevisionNo(valueOrZero(fact.getRevisionNo()) + 1);
                applyFactValues(fact, factRequest, validation, savedObservation, observedAt);
                factMapper.updateById(fact);
            } else {
                fact.setLastObservedTime(observedAt);
                fact.setLatestObservationId(savedObservation.getIdObservation());
                factMapper.updateById(fact);
            }
            changedTypes.add(factType);
        }
        return changedTypes;
    }

    private Set<String> retireMissingSourceFacts(String memoryId,
                                                 String sourceKey,
                                                 Set<String> incomingFactKeyHashes,
                                                 AiPatientMemoryObservation observation) {
        Set<String> changed = new LinkedHashSet<String>();
        List<AiPatientMemoryFact> previousFacts = factMapper.selectList(new LambdaQueryWrapper<AiPatientMemoryFact>()
            .eq(AiPatientMemoryFact::getFgActive, "1")
            .eq(AiPatientMemoryFact::getIdMemory, memoryId)
            .eq(AiPatientMemoryFact::getSourceKey, sourceKey)
            .ne(AiPatientMemoryFact::getFactStatus, "inactive"));
        if (previousFacts == null) {
            return changed;
        }
        for (AiPatientMemoryFact fact : previousFacts) {
            if (!sourceKey.equals(fact.getSourceKey())
                || incomingFactKeyHashes.contains(fact.getFactKeyHash())
                || "admin".equals(fact.getOriginCode())
                || "doctor".equals(fact.getOriginCode())) {
                continue;
            }
            fact.setFactStatus("inactive");
            fact.setLatestObservationId(observation.getIdObservation());
            fact.setLastObservedTime(observation.getOccurredTime());
            fact.setRevisionNo(valueOrZero(fact.getRevisionNo()) + 1);
            factMapper.updateById(fact);
            changed.add(fact.getFactType());
        }
        return changed;
    }

    private void applyFactValues(AiPatientMemoryFact fact,
                                 PatientMemorySyncRequest.ClinicalFact request,
                                 ObservationValidation validation,
                                 AiPatientMemoryObservation observation,
                                 LocalDateTime observedAt) {
        fact.setFactType(normalizeLower(request.getFactType()));
        fact.setFactCode(truncate(request.getCode(), 128));
        fact.setFactName(truncate(request.getName(), 256));
        fact.setValueText(truncate(request.getValueText(), 1000));
        fact.setFactStatus(resolveFactStatus(request));
        fact.setConfidenceLevel(resolveConfidence(request, validation.sourceType));
        fact.setEvidenceText(truncate(request.getEvidenceText(), 1000));
        fact.setSourceType(validation.sourceType);
        fact.setSourceKey(validation.sourceKey);
        fact.setLatestObservationId(observation.getIdObservation());
        fact.setLastObservedTime(observedAt);
    }

    private Set<String> tombstoneFacts(String memoryId,
                                       String sourceKey,
                                       AiPatientMemoryObservation observation) {
        Set<String> changed = new LinkedHashSet<String>();
        List<AiPatientMemoryFact> facts = factMapper.selectList(new LambdaQueryWrapper<AiPatientMemoryFact>()
            .eq(AiPatientMemoryFact::getFgActive, "1")
            .eq(AiPatientMemoryFact::getIdMemory, memoryId)
            .eq(AiPatientMemoryFact::getSourceKey, sourceKey));
        if (facts == null) {
            return changed;
        }
        for (AiPatientMemoryFact fact : facts) {
            if ("admin".equals(fact.getOriginCode()) || "doctor".equals(fact.getOriginCode())) {
                continue;
            }
            fact.setFactStatus("inactive");
            fact.setLatestObservationId(observation.getIdObservation());
            fact.setLastObservedTime(observation.getOccurredTime());
            fact.setRevisionNo(valueOrZero(fact.getRevisionNo()) + 1);
            factMapper.updateById(fact);
            changed.add(fact.getFactType());
        }
        return changed;
    }

    private List<AiPatientMemoryFact> loadActiveFacts(String memoryId) {
        List<AiPatientMemoryFact> facts = factMapper.selectList(new LambdaQueryWrapper<AiPatientMemoryFact>()
            .eq(AiPatientMemoryFact::getFgActive, "1")
            .eq(AiPatientMemoryFact::getIdMemory, memoryId)
            .eq(AiPatientMemoryFact::getFgSuppressed, "0")
            .ne(AiPatientMemoryFact::getFactStatus, "inactive")
            .orderByDesc(AiPatientMemoryFact::getLastObservedTime));
        if (facts == null) {
            return Collections.<AiPatientMemoryFact>emptyList();
        }
        return facts.stream()
            .filter(fact -> "1".equals(fact.getFgActive()))
            .filter(fact -> !"1".equals(fact.getFgSuppressed()))
            .filter(fact -> !"inactive".equals(fact.getFactStatus()))
            .collect(java.util.stream.Collectors.toList());
    }

    private PatientMemoryBriefVO buildBrief(AiPatientMemory memory, List<AiPatientMemoryFact> facts) {
        PatientMemoryBriefVO brief = new PatientMemoryBriefVO();
        brief.setMemoryId(memory.getIdMemory());
        brief.setMemoryVersion(valueOrZero(memory.getMemoryVersion()));
        brief.setPatientId(memory.getPatientId());
        brief.setPatientName(memory.getPatientName());
        brief.setPatientGender(memory.getPatientGender());
        brief.setPatientAge(memory.getPatientAge());
        brief.setConflictCount(valueOrZero(memory.getConflictCount()));
        brief.setLastSyncTime(memory.getLastSyncTime());
        brief.setLastSourceTime(memory.getLastSourceTime());
        brief.setQualityStatus(brief.getConflictCount() > 0 ? "conflicted" : (facts.isEmpty() ? "partial" : "fresh"));

        List<AiPatientMemoryFact> sorted = new ArrayList<AiPatientMemoryFact>(facts);
        sorted.sort(Comparator.comparing(AiPatientMemoryFact::getLastObservedTime,
            Comparator.nullsLast(Comparator.reverseOrder())));
        for (AiPatientMemoryFact fact : sorted) {
            PatientMemoryBriefVO.MemoryFactItem item = toFactItem(fact);
            if ("allergy".equals(fact.getFactType())) {
                addLimited(brief.getAllergies(), item, 20);
            } else if ("chronic_condition".equals(fact.getFactType())) {
                addLimited(brief.getChronicConditions(), item, MAX_BRIEF_ITEMS_PER_SECTION);
            } else if ("diagnosis".equals(fact.getFactType())) {
                addLimited(brief.getRecentDiagnoses(), item, MAX_BRIEF_ITEMS_PER_SECTION);
            } else if ("medication".equals(fact.getFactType())) {
                addLimited(brief.getRecentMedications(), item, MAX_BRIEF_ITEMS_PER_SECTION);
            } else {
                addLimited(brief.getOtherFacts(), item, MAX_BRIEF_ITEMS_PER_SECTION);
            }
        }
        return brief;
    }

    private PatientMemoryBriefVO.MemoryFactItem toFactItem(AiPatientMemoryFact fact) {
        PatientMemoryBriefVO.MemoryFactItem item = new PatientMemoryBriefVO.MemoryFactItem();
        item.setFactId(fact.getIdFact());
        item.setFactType(fact.getFactType());
        item.setCode(fact.getFactCode());
        item.setName(fact.getFactName());
        item.setValueText(fact.getValueText());
        item.setStatus(fact.getFactStatus());
        item.setConfidence(fact.getConfidenceLevel());
        item.setEvidenceText(fact.getEvidenceText());
        item.setSourceType(fact.getSourceType());
        item.setOrigin(fact.getOriginCode());
        item.setLastObservedAt(fact.getLastObservedTime());
        return item;
    }

    private <T> void addLimited(List<T> target, T item, int limit) {
        if (target.size() < limit) {
            target.add(item);
        }
    }

    private String hashObservation(PatientMemorySyncRequest.Observation observation, String operation) {
        Map<String, Object> canonical = new TreeMap<String, Object>();
        canonical.put("operation", operation);
        canonical.put("sourceKey", trimToNull(observation.getSourceKey()));
        canonical.put("sourceType", normalizeLower(observation.getSourceType()));
        canonical.put("sourceVersion", trimToNull(observation.getSourceVersion()));
        canonical.put("visitId", trimToNull(observation.getVisitId()));
        canonical.put("occurredAt", observation.getOccurredAt());
        canonical.put("payload", observation.getPayload() == null ? Collections.emptyMap() : observation.getPayload());

        List<Map<String, Object>> canonicalFacts = new ArrayList<Map<String, Object>>();
        if (observation.getFacts() != null) {
            for (PatientMemorySyncRequest.ClinicalFact fact : observation.getFacts()) {
                Map<String, Object> canonicalFact = new TreeMap<String, Object>();
                canonicalFact.put("factKey", trimToNull(fact.getFactKey()));
                canonicalFact.put("factType", normalizeLower(fact.getFactType()));
                canonicalFact.put("code", trimToNull(fact.getCode()));
                canonicalFact.put("name", trimToNull(fact.getName()));
                canonicalFact.put("valueText", trimToNull(fact.getValueText()));
                canonicalFact.put("status", normalizeLower(fact.getStatus()));
                canonicalFact.put("confidence", normalizeLower(fact.getConfidence()));
                canonicalFact.put("evidenceText", trimToNull(fact.getEvidenceText()));
                canonicalFacts.add(canonicalFact);
            }
        }
        canonicalFacts.sort(Comparator.comparing(item -> String.valueOf(item.get("factType")) + ":" + String.valueOf(item.get("factKey")) + ":" + String.valueOf(item.get("code")) + ":" + String.valueOf(item.get("name"))));
        canonical.put("facts", canonicalFacts);
        return sha256Hex(writeJson(canonical, "观察摘要计算失败"));
    }

    private String resolveFactKey(PatientMemorySyncRequest.ClinicalFact fact, String factType) {
        String provided = trimToNull(fact.getFactKey());
        if (provided != null) {
            return truncate(provided, 256);
        }
        String identity = firstNonBlank(fact.getCode(), fact.getName(), fact.getValueText());
        return factType + ":" + sha256Hex(normalizeIdentity(identity)).substring(0, 24);
    }

    private String resolveFactStatus(PatientMemorySyncRequest.ClinicalFact fact) {
        String status = normalizeLower(fact.getStatus());
        if (status != null) {
            return status;
        }
        return "allergy".equals(normalizeLower(fact.getFactType())) ? "active" : "historical";
    }

    private String resolveConfidence(PatientMemorySyncRequest.ClinicalFact fact, String sourceType) {
        String confidence = normalizeLower(fact.getConfidence());
        if (confidence != null) {
            return confidence;
        }
        return "doctor_confirmation".equals(sourceType) ? "confirmed" : "structured";
    }

    private String resolveHisOrgId(AiDevice device, String requestedHisOrgId) {
        String hisOrgId = trimToNull(requestedHisOrgId);
        return hisOrgId == null ? device.getIdOrg() : truncate(hisOrgId, 64);
    }

    private LocalDateTime resolveTime(Long epochMillis) {
        if (epochMillis == null || epochMillis <= 0) {
            return LocalDateTime.now();
        }
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
    }

    private String writeJson(Object value, String errorMessage) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException | StackOverflowError ex) {
            throw new BusinessException(errorMessage);
        } catch (RuntimeException ex) {
            throw new BusinessException(errorMessage);
        }
    }

    private String sha256Hex(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((text == null ? "" : text).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                sb.append(String.format("%02x", value & 0xff));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 digest failed", ex);
        }
    }

    private String required(String value, String message, int maxLength) {
        String text = trimToNull(value);
        if (text == null) {
            throw new BusinessException(message);
        }
        if (text.length() > maxLength) {
            throw new BusinessException(message.replace("不能为空", "不能超过 " + maxLength + " 个字符"));
        }
        return text;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return "unknown";
    }

    private String normalizeIdentity(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replaceAll("[\\s_-]+", "");
    }

    private String normalizeLower(String value) {
        String text = trimToNull(value);
        return text == null ? null : text.toLowerCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String truncate(String value, int maxLength) {
        String text = trimToNull(value);
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength);
    }

    private long valueOrZero(Long value) {
        return value == null ? 0L : value;
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    private interface StringSetter {
        void set(String value);
    }

    private static final class ObservationValidation {
        private final boolean valid;
        private final String reason;
        private final String sourceKey;
        private final String sourceType;
        private final String operation;

        private ObservationValidation(boolean valid, String reason, String sourceKey, String sourceType, String operation) {
            this.valid = valid;
            this.reason = reason;
            this.sourceKey = sourceKey;
            this.sourceType = sourceType;
            this.operation = operation;
        }

        private static ObservationValidation accepted(String sourceKey, String sourceType, String operation) {
            return new ObservationValidation(true, null, sourceKey, sourceType, operation);
        }

        private static ObservationValidation rejected(String reason) {
            return new ObservationValidation(false, reason, null, null, null);
        }
    }
}
