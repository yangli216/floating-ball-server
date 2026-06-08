package com.regionalai.floatingball.server.modules.emrtemplate.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.regionalai.floatingball.server.common.api.PageResponse;
import com.regionalai.floatingball.server.common.exception.BusinessException;
import com.regionalai.floatingball.server.modules.emrtemplate.dto.InpatientEmrTemplateCacheVO;
import com.regionalai.floatingball.server.modules.emrtemplate.dto.InpatientEmrTemplatePromptRequest;
import com.regionalai.floatingball.server.modules.emrtemplate.dto.InpatientEmrTemplateResolveRequest;
import com.regionalai.floatingball.server.modules.emrtemplate.entity.AiInpatientEmrTemplateCache;
import com.regionalai.floatingball.server.modules.emrtemplate.mapper.AiInpatientEmrTemplateCacheMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class InpatientEmrTemplateCacheService {

    private static final Logger log = LoggerFactory.getLogger(InpatientEmrTemplateCacheService.class);

    private static final String ACTIVE_ENABLED = "1";
    private static final String ACTIVE_DISABLED = "0";
    private static final String STATUS_ENABLED = "1";
    private static final String STATUS_DISABLED = "0";

    private static final TypeReference<List<Map<String, Object>>> FIELD_LIST_TYPE =
        new TypeReference<List<Map<String, Object>>>() {
        };

    private final AiInpatientEmrTemplateCacheMapper cacheMapper;
    private final ObjectMapper objectMapper;

    public InpatientEmrTemplateCacheService(AiInpatientEmrTemplateCacheMapper cacheMapper,
                                            ObjectMapper objectMapper) {
        this.cacheMapper = cacheMapper;
        this.objectMapper = objectMapper;
    }

    public PageResponse<InpatientEmrTemplateCacheVO> list(long current,
                                                          long size,
                                                          String keyword,
                                                          String sdStatus) {
        Page<AiInpatientEmrTemplateCache> page = new Page<AiInpatientEmrTemplateCache>(current, size);
        LambdaQueryWrapper<AiInpatientEmrTemplateCache> wrapper = new LambdaQueryWrapper<AiInpatientEmrTemplateCache>()
            .eq(AiInpatientEmrTemplateCache::getFgActive, ACTIVE_ENABLED)
            .orderByDesc(AiInpatientEmrTemplateCache::getUpdateTime);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(q -> q.like(AiInpatientEmrTemplateCache::getTemplateId, keyword.trim())
                .or()
                .like(AiInpatientEmrTemplateCache::getTemplateHash, keyword.trim())
                .or()
                .like(AiInpatientEmrTemplateCache::getTemplateName, keyword.trim()));
        }
        if (StringUtils.hasText(sdStatus)) {
            wrapper.eq(AiInpatientEmrTemplateCache::getSdStatus, sdStatus.trim());
        }
        Page<AiInpatientEmrTemplateCache> result = cacheMapper.selectPage(page, wrapper);
        List<InpatientEmrTemplateCacheVO> records = new ArrayList<InpatientEmrTemplateCacheVO>();
        for (AiInpatientEmrTemplateCache item : result.getRecords()) {
            records.add(toView(item, false));
        }
        return new PageResponse<InpatientEmrTemplateCacheVO>(
            result.getCurrent(),
            result.getSize(),
            result.getTotal(),
            records
        );
    }

    public InpatientEmrTemplateCacheVO get(String idCache) {
        return toView(requireCache(idCache), false);
    }

    @Transactional
    public InpatientEmrTemplateCacheVO resolve(InpatientEmrTemplateResolveRequest request) {
        if (request == null) {
            throw new BusinessException("请求体不能为空");
        }
        String templateId = resolveTemplateId(request);
        String templateHash = resolveTemplateHash(request);
        AiInpatientEmrTemplateCache cached = findEnabledByTemplateId(templateId);
        if (cached != null) {
            updateCachedTemplateMetadata(cached, request, templateHash);
            return toView(cached, true);
        }

        if (!StringUtils.hasText(request.getHtmlContent())) {
            throw new BusinessException("htmlContent 不能为空");
        }
        if (request.getFields() == null || request.getFields().isEmpty()) {
            throw new BusinessException("fields 不能为空");
        }

        AiInpatientEmrTemplateCache entity = new AiInpatientEmrTemplateCache();
        entity.setTemplateId(templateId);
        entity.setTemplateHash(templateHash);
        entity.setTemplateName(trimToNull(request.getTemplateName()));
        entity.setHtmlContent(request.getHtmlContent());
        entity.setFieldsJson(writeFields(request.getFields()));
        entity.setFieldCount(Integer.valueOf(request.getFields().size()));
        entity.setSdStatus(STATUS_ENABLED);
        entity.setFgActive(ACTIVE_ENABLED);
        cacheMapper.insert(entity);
        log.info("inpatient emr template cache saved. idCache={}, templateId={}", entity.getIdCache(), templateId);
        return toView(cacheMapper.selectById(entity.getIdCache()), false);
    }

    private void updateCachedTemplateMetadata(AiInpatientEmrTemplateCache cached,
                                              InpatientEmrTemplateResolveRequest request,
                                              String templateHash) {
        boolean changed = false;
        String templateName = trimToNull(request.getTemplateName());
        if (templateName != null && !templateName.equals(cached.getTemplateName())) {
            cached.setTemplateName(templateName);
            changed = true;
        }
        if (StringUtils.hasText(templateHash) && !templateHash.equals(cached.getTemplateHash())) {
            cached.setTemplateHash(templateHash);
            changed = true;
        }
        if (StringUtils.hasText(request.getHtmlContent()) && !request.getHtmlContent().equals(cached.getHtmlContent())) {
            cached.setHtmlContent(request.getHtmlContent());
            changed = true;
        }
        if (changed) {
            cacheMapper.updateById(cached);
        }
    }

    @Transactional
    public InpatientEmrTemplateCacheVO updateFieldPrompt(String idCache,
                                                        String fieldId,
                                                        InpatientEmrTemplatePromptRequest request) {
        AiInpatientEmrTemplateCache cache = requireCache(idCache);
        if (!StringUtils.hasText(fieldId)) {
            throw new BusinessException("fieldId 不能为空");
        }
        List<Map<String, Object>> fields = parseFields(cache.getFieldsJson());
        boolean updated = false;
        for (Map<String, Object> field : fields) {
            String id = stringValue(field.get("id"));
            if (!fieldId.equals(id)) {
                continue;
            }
            Map<String, Object> rule = ensureRule(field);
            String prompt = request == null ? null : trimToNull(request.getPrompt());
            if (prompt == null) {
                rule.remove("prompt");
            } else {
                rule.put("prompt", prompt);
            }
            updated = true;
            break;
        }
        if (!updated) {
            throw new BusinessException("未找到指定模板字段");
        }
        cache.setFieldsJson(writeFields(fields));
        cache.setFieldCount(Integer.valueOf(fields.size()));
        cacheMapper.updateById(cache);
        log.info("inpatient emr template prompt updated. idCache={}, fieldId={}", idCache, fieldId);
        return toView(cacheMapper.selectById(idCache), false);
    }

    @Transactional
    public InpatientEmrTemplateCacheVO enable(String idCache) {
        AiInpatientEmrTemplateCache cache = requireCache(idCache);
        cache.setSdStatus(STATUS_ENABLED);
        cacheMapper.updateById(cache);
        return toView(cacheMapper.selectById(idCache), false);
    }

    @Transactional
    public InpatientEmrTemplateCacheVO disable(String idCache) {
        AiInpatientEmrTemplateCache cache = requireCache(idCache);
        cache.setSdStatus(STATUS_DISABLED);
        cacheMapper.updateById(cache);
        return toView(cacheMapper.selectById(idCache), false);
    }

    @Transactional
    public void invalidate(String idCache) {
        AiInpatientEmrTemplateCache cache = requireCache(idCache);
        cache.setFgActive(ACTIVE_DISABLED);
        cacheMapper.updateById(cache);
        log.info("inpatient emr template cache invalidated. idCache={}", idCache);
    }

    private AiInpatientEmrTemplateCache findEnabledByTemplateId(String templateId) {
        List<AiInpatientEmrTemplateCache> records = cacheMapper.selectList(
            new LambdaQueryWrapper<AiInpatientEmrTemplateCache>()
                .eq(AiInpatientEmrTemplateCache::getTemplateId, templateId)
                .eq(AiInpatientEmrTemplateCache::getFgActive, ACTIVE_ENABLED)
                .eq(AiInpatientEmrTemplateCache::getSdStatus, STATUS_ENABLED)
                .orderByDesc(AiInpatientEmrTemplateCache::getUpdateTime)
        );
        return records.isEmpty() ? null : records.get(0);
    }

    private AiInpatientEmrTemplateCache requireCache(String idCache) {
        if (!StringUtils.hasText(idCache)) {
            throw new BusinessException("idCache 不能为空");
        }
        AiInpatientEmrTemplateCache cache = cacheMapper.selectById(idCache);
        if (cache == null || !ACTIVE_ENABLED.equals(cache.getFgActive())) {
            throw new BusinessException("模板缓存不存在或已删除");
        }
        return cache;
    }

    private InpatientEmrTemplateCacheVO toView(AiInpatientEmrTemplateCache entity, boolean cacheHit) {
        InpatientEmrTemplateCacheVO vo = new InpatientEmrTemplateCacheVO();
        vo.setId(entity.getIdCache());
        vo.setTemplateId(entity.getTemplateId());
        vo.setTemplateHash(entity.getTemplateHash());
        vo.setTemplateName(entity.getTemplateName());
        vo.setHtmlContent(entity.getHtmlContent());
        vo.setFields(parseFields(entity.getFieldsJson()));
        vo.setFieldCount(entity.getFieldCount());
        vo.setSdStatus(entity.getSdStatus());
        vo.setCacheHit(Boolean.valueOf(cacheHit));
        vo.setCreatedAt(toMillis(entity.getInsertTime()));
        vo.setUpdatedAt(toMillis(entity.getUpdateTime()));
        return vo;
    }

    private String resolveTemplateId(InpatientEmrTemplateResolveRequest request) {
        if (!StringUtils.hasText(request.getTemplateId())) {
            throw new BusinessException("templateId 不能为空");
        }
        return request.getTemplateId().trim();
    }

    private String resolveTemplateHash(InpatientEmrTemplateResolveRequest request) {
        if (StringUtils.hasText(request.getTemplateHash())) {
            return request.getTemplateHash().trim();
        }
        if (!StringUtils.hasText(request.getHtmlContent())) {
            throw new BusinessException("templateHash 或 htmlContent 不能为空");
        }
        return "sha256_" + sha256(request.getHtmlContent());
    }

    private String writeFields(List<Map<String, Object>> fields) {
        try {
            return objectMapper.writeValueAsString(fields == null ? Collections.emptyList() : fields);
        } catch (JsonProcessingException ex) {
            throw new BusinessException("字段结构无法序列化");
        }
    }

    private List<Map<String, Object>> parseFields(String fieldsJson) {
        if (!StringUtils.hasText(fieldsJson)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(fieldsJson, FIELD_LIST_TYPE);
        } catch (Exception ex) {
            log.warn("invalid inpatient emr template fields json", ex);
            return Collections.emptyList();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> ensureRule(Map<String, Object> field) {
        Object ruleValue = field.get("rule");
        if (ruleValue instanceof Map) {
            return (Map<String, Object>) ruleValue;
        }
        Map<String, Object> rule = new LinkedHashMap<String, Object>();
        field.put("rule", rule);
        return rule;
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte item : bytes) {
                builder.append(String.format("%02x", item & 0xff));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new BusinessException("当前运行环境不支持 SHA-256");
        }
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private Long toMillis(LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return Long.valueOf(value.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
    }
}
