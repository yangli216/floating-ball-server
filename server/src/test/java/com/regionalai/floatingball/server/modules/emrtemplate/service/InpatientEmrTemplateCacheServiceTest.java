package com.regionalai.floatingball.server.modules.emrtemplate.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regionalai.floatingball.server.modules.emrtemplate.dto.InpatientEmrTemplateCacheVO;
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
    void resolveStoresFirstParsedTemplate() {
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
        request.setTemplateHash("tpl_hash");
        request.setTemplateName("日常病程记录");
        request.setHtmlContent("<span data-id=\"病程记录文本\"></span>");
        request.setFields(sampleFields());

        InpatientEmrTemplateCacheVO result = service.resolve(request);

        assertEquals("tpl_hash", result.getTemplateHash());
        assertEquals(Integer.valueOf(1), result.getFieldCount());
        assertEquals(Boolean.FALSE, result.getCacheHit());
        assertEquals("病程记录文本", result.getFields().get(0).get("id"));
        verify(cacheMapper).insert(any(AiInpatientEmrTemplateCache.class));
    }

    @Test
    void resolveReturnsEnabledCacheBeforeParsingPayload() throws Exception {
        AiInpatientEmrTemplateCache cached = new AiInpatientEmrTemplateCache();
        cached.setIdCache("cache-2");
        cached.setTemplateHash("tpl_hash");
        cached.setTemplateName("日常病程记录");
        cached.setFieldsJson(new ObjectMapper().writeValueAsString(sampleFields()));
        cached.setFieldCount(Integer.valueOf(1));
        cached.setFgActive("1");
        cached.setSdStatus("1");
        when(cacheMapper.selectList(any())).thenReturn(Collections.singletonList(cached));

        InpatientEmrTemplateResolveRequest request = new InpatientEmrTemplateResolveRequest();
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
        cached.setTemplateHash("tpl_hash");
        cached.setTemplateName("旧模板名");
        cached.setFieldsJson(new ObjectMapper().writeValueAsString(sampleFields()));
        cached.setFieldCount(Integer.valueOf(1));
        cached.setFgActive("1");
        cached.setSdStatus("1");
        when(cacheMapper.selectList(any())).thenReturn(Collections.singletonList(cached));

        InpatientEmrTemplateResolveRequest request = new InpatientEmrTemplateResolveRequest();
        request.setTemplateHash("tpl_hash");
        request.setTemplateName("日常病程记录");

        InpatientEmrTemplateCacheVO result = service.resolve(request);

        assertEquals("日常病程记录", result.getTemplateName());
        verify(cacheMapper).updateById(cached);
    }

    @Test
    void updateFieldPromptWritesPromptIntoRule() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        AiInpatientEmrTemplateCache cache = new AiInpatientEmrTemplateCache();
        cache.setIdCache("cache-3");
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
        verify(cacheMapper).updateById(cache);
    }

    private List<Map<String, Object>> sampleFields() {
        Map<String, Object> rule = new LinkedHashMap<String, Object>();
        rule.put("source", "ai");
        rule.put("constraints", Collections.singletonList("仅依据 HIS 数据生成"));

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
