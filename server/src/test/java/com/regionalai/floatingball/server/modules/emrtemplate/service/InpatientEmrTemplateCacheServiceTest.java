package com.regionalai.floatingball.server.modules.emrtemplate.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regionalai.floatingball.server.modules.emrtemplate.dto.InpatientEmrTemplateCacheVO;
import com.regionalai.floatingball.server.modules.emrtemplate.dto.InpatientEmrTemplateFieldGenerationRequest;
import com.regionalai.floatingball.server.modules.emrtemplate.dto.InpatientEmrTemplatePromptGenerateRequest;
import com.regionalai.floatingball.server.modules.emrtemplate.dto.InpatientEmrTemplatePromptGenerateVO;
import com.regionalai.floatingball.server.modules.emrtemplate.dto.InpatientEmrTemplatePromptRequest;
import com.regionalai.floatingball.server.modules.emrtemplate.dto.InpatientEmrTemplateResolveRequest;
import com.regionalai.floatingball.server.modules.emrtemplate.entity.AiInpatientEmrTemplateCache;
import com.regionalai.floatingball.server.modules.emrtemplate.mapper.AiInpatientEmrTemplateCacheMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InpatientEmrTemplateCacheServiceTest {

    @Mock
    private AiInpatientEmrTemplateCacheMapper cacheMapper;

    private InpatientEmrTemplateCacheService service;

    @BeforeEach
    void setUp() {
        service = new InpatientEmrTemplateCacheService(cacheMapper, new ObjectMapper());
    }

    @Test
    void resolveStoresAllFields() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        AtomicReference<AiInpatientEmrTemplateCache> inserted = new AtomicReference<AiInpatientEmrTemplateCache>();
        when(cacheMapper.selectList(any())).thenReturn(Collections.emptyList());
        doAnswer(invocation -> {
            AiInpatientEmrTemplateCache entity = invocation.getArgument(0);
            entity.setIdCache("cache-1");
            inserted.set(entity);
            return Integer.valueOf(1);
        }).when(cacheMapper).insert(any(AiInpatientEmrTemplateCache.class));
        when(cacheMapper.selectById("cache-1")).thenAnswer(invocation -> inserted.get());

        InpatientEmrTemplateResolveRequest request = new InpatientEmrTemplateResolveRequest();
        request.setTemplateId("emr_tpl_daily_course");
        request.setTemplateHash("tpl_hash");
        request.setTemplateName("日常病程记录");
        request.setHtmlContent("<span data-id=\"病程记录文本\"></span>");
        request.setFields(sampleFields());

        InpatientEmrTemplateCacheVO result = service.resolve(request);

        assertEquals("tpl_hash", result.getTemplateHash());
        assertEquals(Integer.valueOf(2), result.getFieldCount());
        assertEquals(Boolean.FALSE, result.getCacheHit());
        assertEquals(2, result.getFields().size());
        assertEquals("病程记录文本", result.getFields().get(0).get("id"));
        List<Map<String, Object>> storedFields = objectMapper.readValue(inserted.get().getFieldsJson(), List.class);
        assertEquals(2, storedFields.size());
        assertEquals("病程记录文本", storedFields.get(0).get("id"));
        assertEquals("页眉姓名", storedFields.get(1).get("id"));
        verify(cacheMapper).insert(any(AiInpatientEmrTemplateCache.class));
    }

    @Test
    void resolveReturnsEnabledCacheBeforeParsingPayload() throws Exception {
        AiInpatientEmrTemplateCache cached = new AiInpatientEmrTemplateCache();
        cached.setIdCache("cache-2");
        cached.setTemplateId("emr_tpl_daily_course");
        cached.setTemplateHash("tpl_hash");
        cached.setTemplateName("日常病程记录");
        cached.setFieldsJson(new ObjectMapper().writeValueAsString(aiFieldsWithPrompt(null)));
        cached.setFieldCount(Integer.valueOf(1));
        cached.setFgActive("1");
        cached.setSdStatus("1");
        when(cacheMapper.selectList(any())).thenReturn(Collections.singletonList(cached));

        InpatientEmrTemplateResolveRequest request = new InpatientEmrTemplateResolveRequest();
        request.setTemplateId("emr_tpl_daily_course");
        request.setTemplateHash("tpl_hash");

        InpatientEmrTemplateCacheVO result = service.resolve(request);

        assertEquals(Boolean.TRUE, result.getCacheHit());
        assertEquals("病程记录文本", result.getFields().get(0).get("id"));
        verify(cacheMapper, never()).insert(any(AiInpatientEmrTemplateCache.class));
    }

    @Test
    void resolveUpdatesTemplateNameWhenCacheHit() throws Exception {
        AiInpatientEmrTemplateCache cached = new AiInpatientEmrTemplateCache();
        cached.setIdCache("cache-4");
        cached.setTemplateId("emr_tpl_daily_course");
        cached.setTemplateHash("tpl_hash");
        cached.setTemplateName("旧模板名");
        cached.setFieldsJson(new ObjectMapper().writeValueAsString(aiFieldsWithPrompt(null)));
        cached.setFieldCount(Integer.valueOf(1));
        cached.setFgActive("1");
        cached.setSdStatus("1");
        when(cacheMapper.selectList(any())).thenReturn(Collections.singletonList(cached));

        InpatientEmrTemplateResolveRequest request = new InpatientEmrTemplateResolveRequest();
        request.setTemplateId("emr_tpl_daily_course");
        request.setTemplateHash("tpl_hash");
        request.setTemplateName("日常病程记录");

        InpatientEmrTemplateCacheVO result = service.resolve(request);

        assertEquals("日常病程记录", result.getTemplateName());
        verify(cacheMapper).updateById(cached);
    }

    @Test
    void resolveMergesCachedPromptIntoCurrentClientFields() throws Exception {
        AiInpatientEmrTemplateCache cached = new AiInpatientEmrTemplateCache();
        cached.setIdCache("cache-5");
        cached.setTemplateId("emr_tpl_daily_course");
        cached.setTemplateHash("tpl_hash");
        cached.setTemplateName("日常病程记录");
        cached.setFieldsJson(new ObjectMapper().writeValueAsString(aiFieldsWithPrompt("请按今日住院资料生成病程记录。")));
        cached.setFieldCount(Integer.valueOf(1));
        cached.setFgActive("1");
        cached.setSdStatus("1");
        when(cacheMapper.selectList(any())).thenReturn(Collections.singletonList(cached));

        InpatientEmrTemplateResolveRequest request = new InpatientEmrTemplateResolveRequest();
        request.setTemplateId("emr_tpl_daily_course");
        request.setTemplateHash("tpl_hash");
        request.setFields(sampleFields());

        InpatientEmrTemplateCacheVO result = service.resolve(request);

        assertEquals(2, result.getFields().size());
        Map<?, ?> rule = (Map<?, ?>) result.getFields().get(0).get("rule");
        assertEquals("请按今日住院资料生成病程记录。", rule.get("prompt"));
        assertEquals("请按今日住院资料生成病程记录。", rule.get("resolvedPrompt"));
        assertEquals("custom", rule.get("promptSource"));
        assertEquals("页眉姓名", result.getFields().get(1).get("id"));
    }

    @Test
    void resolveRefreshesStoredFieldsToCurrentFullFieldsAndKeepsPrompt() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        List<Map<String, Object>> legacyFields = aiFieldsWithPrompt("沿用已维护提示词。");

        AiInpatientEmrTemplateCache cached = new AiInpatientEmrTemplateCache();
        cached.setIdCache("cache-7");
        cached.setTemplateId("emr_tpl_daily_course");
        cached.setTemplateHash("tpl_hash");
        cached.setTemplateName("日常病程记录");
        cached.setFieldsJson(objectMapper.writeValueAsString(legacyFields));
        cached.setFieldCount(Integer.valueOf(2));
        cached.setFgActive("1");
        cached.setSdStatus("1");
        when(cacheMapper.selectList(any())).thenReturn(Collections.singletonList(cached));

        InpatientEmrTemplateResolveRequest request = new InpatientEmrTemplateResolveRequest();
        request.setTemplateId("emr_tpl_daily_course");
        request.setTemplateHash("tpl_hash");
        request.setFields(sampleFields());

        InpatientEmrTemplateCacheVO result = service.resolve(request);

        assertEquals(2, result.getFields().size());
        Map<?, ?> responseRule = (Map<?, ?>) result.getFields().get(0).get("rule");
        assertEquals("沿用已维护提示词。", responseRule.get("prompt"));

        List<Map<String, Object>> storedFields = objectMapper.readValue(cached.getFieldsJson(), List.class);
        assertEquals(2, storedFields.size());
        assertEquals("病程记录文本", storedFields.get(0).get("id"));
        assertEquals("页眉姓名", storedFields.get(1).get("id"));
        assertEquals("沿用已维护提示词。", ((Map<?, ?>) storedFields.get(0).get("rule")).get("prompt"));
        verify(cacheMapper).updateById(cached);
    }

    @Test
    void updateFieldPromptWritesPromptIntoRule() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        AiInpatientEmrTemplateCache cache = new AiInpatientEmrTemplateCache();
        cache.setIdCache("cache-3");
        cache.setTemplateId("emr_tpl_daily_course");
        cache.setTemplateHash("tpl_hash");
        cache.setFieldsJson(objectMapper.writeValueAsString(sampleFields()));
        cache.setFieldCount(Integer.valueOf(1));
        cache.setFgActive("1");
        cache.setSdStatus("1");
        when(cacheMapper.selectById("cache-3")).thenReturn(cache);

        InpatientEmrTemplatePromptRequest request = new InpatientEmrTemplatePromptRequest();
        request.setPrompt("仅依据住院登记、医嘱和体温单生成日常病程记录。");

        InpatientEmrTemplateCacheVO result = service.updateFieldPrompt("cache-3", "病程记录文本", request);

        Map<?, ?> rule = (Map<?, ?>) result.getFields().get(0).get("rule");
        assertEquals("仅依据住院登记、医嘱和体温单生成日常病程记录。", rule.get("prompt"));
        assertEquals("仅依据住院登记、医嘱和体温单生成日常病程记录。", rule.get("resolvedPrompt"));
        assertEquals("custom", rule.get("promptSource"));
        assertEquals(2, result.getFields().size());
        verify(cacheMapper).updateById(cache);
    }

    @Test
    void updateFieldGenerationCanPromoteNonAiField() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        AiInpatientEmrTemplateCache cache = new AiInpatientEmrTemplateCache();
        cache.setIdCache("cache-8");
        cache.setTemplateId("emr_tpl_daily_course");
        cache.setTemplateHash("tpl_hash");
        cache.setFieldsJson(objectMapper.writeValueAsString(sampleFields()));
        cache.setFieldCount(Integer.valueOf(2));
        cache.setFgActive("1");
        cache.setSdStatus("1");
        when(cacheMapper.selectById("cache-8")).thenReturn(cache);

        InpatientEmrTemplateFieldGenerationRequest request = new InpatientEmrTemplateFieldGenerationRequest();
        request.setAiSuitable(Boolean.TRUE);

        InpatientEmrTemplateCacheVO result = service.updateFieldGeneration("cache-8", "页眉姓名", request);

        Map<String, Object> promoted = result.getFields().get(1);
        assertEquals(Boolean.TRUE, promoted.get("aiSuitable"));
        assertEquals("ai", ((Map<?, ?>) promoted.get("rule")).get("source"));
        assertEquals("default", ((Map<?, ?>) promoted.get("rule")).get("promptSource"));
        assertTrue(String.valueOf(((Map<?, ?>) promoted.get("rule")).get("resolvedPrompt")).contains("字段 data-id：页眉姓名"));
        verify(cacheMapper).updateById(cache);
    }

    @Test
    void generateFieldPromptUsesEditableGeneratorInstruction() throws Exception {
        AiInpatientEmrTemplateCache cache = new AiInpatientEmrTemplateCache();
        cache.setIdCache("cache-9");
        cache.setTemplateId("emr_tpl_daily_course");
        cache.setTemplateHash("tpl_hash");
        cache.setFieldsJson(new ObjectMapper().writeValueAsString(sampleFields()));
        cache.setFieldCount(Integer.valueOf(2));
        cache.setFgActive("1");
        cache.setSdStatus("1");
        when(cacheMapper.selectById("cache-9")).thenReturn(cache);

        InpatientEmrTemplatePromptGenerateRequest request = new InpatientEmrTemplatePromptGenerateRequest();
        request.setGeneratorInstruction("请生成适合日常病程记录正文的字段提示词。");

        InpatientEmrTemplatePromptGenerateVO result = service.generateFieldPrompt("cache-9", "病程记录文本", request);

        assertEquals("请生成适合日常病程记录正文的字段提示词。", result.getGeneratorInstruction());
        assertTrue(result.getPrompt().contains("病程记录文本"));
        assertTrue(result.getPrompt().contains("只输出该字段应回填的正文"));
    }

    @Test
    void getReturnsDefaultPromptInfoForUnconfiguredAiField() throws Exception {
        AiInpatientEmrTemplateCache cache = new AiInpatientEmrTemplateCache();
        cache.setIdCache("cache-6");
        cache.setTemplateId("emr_tpl_daily_course");
        cache.setTemplateHash("tpl_hash");
        cache.setFieldsJson(new ObjectMapper().writeValueAsString(aiFieldsWithPrompt(null)));
        cache.setFieldCount(Integer.valueOf(1));
        cache.setFgActive("1");
        cache.setSdStatus("1");
        when(cacheMapper.selectById("cache-6")).thenReturn(cache);

        InpatientEmrTemplateCacheVO result = service.get("cache-6");

        Map<?, ?> rule = (Map<?, ?>) result.getFields().get(0).get("rule");
        assertEquals("default", rule.get("promptSource"));
        assertTrue(String.valueOf(rule.get("resolvedPrompt")).contains("字段 data-id：病程记录文本"));
        assertFalse(((Map<?, ?>) result.getFields().get(0).get("rule")).containsKey("prompt"));
    }

    @Test
    void invalidateMarksCacheInactive() throws Exception {
        AiInpatientEmrTemplateCache cache = new AiInpatientEmrTemplateCache();
        cache.setIdCache("cache-delete");
        cache.setTemplateId("emr_tpl_daily_course");
        cache.setTemplateHash("tpl_hash");
        cache.setFieldsJson(new ObjectMapper().writeValueAsString(sampleFields()));
        cache.setFieldCount(Integer.valueOf(2));
        cache.setFgActive("1");
        cache.setSdStatus("1");
        when(cacheMapper.selectById("cache-delete")).thenReturn(cache);

        service.invalidate("cache-delete");

        assertEquals("0", cache.getFgActive());
        verify(cacheMapper).updateById(cache);
    }

    private List<Map<String, Object>> sampleFields() {
        List<Map<String, Object>> fields = aiFieldsWithPrompt(null);

        Map<String, Object> nonAiRule = new LinkedHashMap<String, Object>();
        nonAiRule.put("source", "his_or_system");
        nonAiRule.put("constraints", Collections.singletonList("按 HIS 事实填充"));

        Map<String, Object> nonAiField = new LinkedHashMap<String, Object>();
        nonAiField.put("id", "页眉姓名");
        nonAiField.put("name", "页眉姓名");
        nonAiField.put("meaning", "患者姓名");
        nonAiField.put("type", "text");
        nonAiField.put("readonly", Boolean.FALSE);
        nonAiField.put("key", Boolean.FALSE);
        nonAiField.put("aiSuitable", Boolean.FALSE);
        nonAiField.put("rule", nonAiRule);
        fields.add(nonAiField);

        return fields;
    }

    private List<Map<String, Object>> aiFieldsWithPrompt(String prompt) {
        Map<String, Object> rule = new LinkedHashMap<String, Object>();
        rule.put("source", "ai");
        rule.put("constraints", Collections.singletonList("仅依据 HIS 数据生成"));
        rule.put("dependencies", Collections.singletonList("registration"));
        rule.put("promptIntent", "inpatientRecordSection");
        if (prompt != null) {
            rule.put("prompt", prompt);
        }

        Map<String, Object> field = new LinkedHashMap<String, Object>();
        field.put("id", "病程记录文本");
        field.put("name", "病程记录文本");
        field.put("meaning", "病程记录正文");
        field.put("type", "text");
        field.put("readonly", Boolean.FALSE);
        field.put("key", Boolean.FALSE);
        field.put("aiSuitable", Boolean.TRUE);
        field.put("rule", rule);

        List<Map<String, Object>> fields = new ArrayList<Map<String, Object>>();
        fields.add(field);
        return fields;
    }
}
