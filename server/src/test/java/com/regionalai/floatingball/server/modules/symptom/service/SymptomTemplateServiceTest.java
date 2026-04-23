package com.regionalai.floatingball.server.modules.symptom.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regionalai.floatingball.server.common.exception.BusinessException;
import com.regionalai.floatingball.server.modules.datapackage.dto.TemplateDeltaVO;
import com.regionalai.floatingball.server.modules.datapackage.service.BuiltinTemplateSeedService;
import com.regionalai.floatingball.server.modules.datapackage.service.DataPackageService;
import com.regionalai.floatingball.server.modules.symptom.dto.BuiltinSymptomImportResultVO;
import com.regionalai.floatingball.server.modules.symptom.dto.JsonSymptomImportRequest;
import com.regionalai.floatingball.server.modules.symptom.dto.SymptomTemplateVO;
import com.regionalai.floatingball.server.modules.symptom.entity.AiSymptomTemplate;
import com.regionalai.floatingball.server.modules.symptom.mapper.AiSymptomTemplateMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class SymptomTemplateServiceTest {

    @Mock
    private AiSymptomTemplateMapper aiSymptomTemplateMapper;

    @Mock
    private DataPackageService dataPackageService;

    private SymptomTemplateService symptomTemplateService;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        symptomTemplateService = new SymptomTemplateService(
            aiSymptomTemplateMapper,
            objectMapper,
            new BuiltinTemplateSeedService(objectMapper),
            dataPackageService
        );
    }

    @Test
    void getClientDeltaShouldPreferOrgScopeAndMergeBySymptomKey() {
        AiSymptomTemplate globalFever = buildTemplate("T1", "fever", "全局发热", "western", null, null, 2, LocalDateTime.of(2026, 4, 20, 10, 0));
        AiSymptomTemplate orgFever = buildTemplate("T2", "fever", "机构发热", "western", "ORG001", null, 1, LocalDateTime.of(2026, 4, 21, 10, 0));
        AiSymptomTemplate globalCough = buildTemplate("T3", "cough", "全局咳嗽", "western", null, null, 3, LocalDateTime.of(2026, 4, 19, 10, 0));

        when(aiSymptomTemplateMapper.selectList(any()))
            .thenReturn(Arrays.asList(globalFever, orgFever, globalCough))
            .thenReturn(Collections.<AiSymptomTemplate>emptyList());

        TemplateDeltaVO delta = symptomTemplateService.getClientDelta("ORG001", "REG001", null);

        assertTrue(delta.getVersion().startsWith("symptom-"));
        assertEquals(2, delta.getWestern().size());
        LinkedHashMap<?, ?> fever = (LinkedHashMap<?, ?>) delta.getWestern().get(0);
        LinkedHashMap<?, ?> cough = (LinkedHashMap<?, ?>) delta.getWestern().get(1);
        assertEquals("机构发热", fever.get("name"));
        assertEquals("fever", fever.get("key"));
        assertEquals("全局咳嗽", cough.get("name"));
        assertTrue(delta.getTcm().isEmpty());
    }

    @Test
    void getClientDeltaShouldFallbackToLegacyTemplatePackageWhenSymptomTableEmpty() {
        TemplateDeltaVO legacy = new TemplateDeltaVO();
        legacy.setVersion("legacy-1");
        legacy.setWestern(Collections.<Object>singletonList(Collections.singletonMap("key", "legacy")));

        when(aiSymptomTemplateMapper.selectList(any()))
            .thenReturn(Collections.<AiSymptomTemplate>emptyList())
            .thenReturn(Collections.<AiSymptomTemplate>emptyList());
        when(dataPackageService.getTemplateDelta("ORG001", "REG001", "0")).thenReturn(legacy);

        TemplateDeltaVO delta = symptomTemplateService.getClientDelta("ORG001", "REG001", "0");

        assertEquals("legacy-1", delta.getVersion());
        assertEquals(1, delta.getWestern().size());
        verify(dataPackageService).getTemplateDelta("ORG001", "REG001", "0");
    }

    @Test
    void saveShouldRejectDuplicateSymptomKeyInSameScope() {
        SymptomTemplateVO request = new SymptomTemplateVO();
        request.setMedicalMode("western");
        request.setKey("fever");
        request.setName("发热");
        request.setConfig(Collections.<String, Object>singletonMap("sections", Collections.emptyList()));

        AiSymptomTemplate existing = buildTemplate("T1", "fever", "已存在", "western", null, null, 1, LocalDateTime.now());
        when(aiSymptomTemplateMapper.selectOne(any())).thenReturn(existing);

        BusinessException ex = assertThrows(BusinessException.class, () -> symptomTemplateService.save(request));

        assertEquals("同作用域下已存在相同症状 Key", ex.getMessage());
        verify(aiSymptomTemplateMapper, never()).insert(any(AiSymptomTemplate.class));
    }

    @Test
    void importJsonShouldSupportArrayTemplates() {
        JsonSymptomImportRequest request = new JsonSymptomImportRequest();
        request.setMedicalMode("western");
        request.setOverwriteExisting(Boolean.TRUE);
        request.setContentJson("[{\"key\":\"fever\",\"name\":\"发热\",\"config\":{\"sections\":[]}},{\"key\":\"cough\",\"name\":\"咳嗽\",\"config\":{\"sections\":[]}}]");

        when(aiSymptomTemplateMapper.selectOne(any())).thenReturn(null);

        BuiltinSymptomImportResultVO result = symptomTemplateService.importJson(request);

        assertEquals("western", result.getMedicalMode());
        assertEquals(2, result.getCreatedCount());
        assertEquals(0, result.getUpdatedCount());
        verify(aiSymptomTemplateMapper, times(2)).insert(any(AiSymptomTemplate.class));
    }

    @Test
    void importJsonShouldSupportSymptomsWrapperObject() {
        JsonSymptomImportRequest request = new JsonSymptomImportRequest();
        request.setMedicalMode("tcm");
        request.setOverwriteExisting(Boolean.TRUE);
        request.setContentJson("{\"version\":\"1.0.0\",\"symptoms\":[{\"key\":\"tcm_headache\",\"name\":\"头痛\",\"config\":{\"sections\":[]}}]}");

        AiSymptomTemplate existing = buildTemplate("T1", "tcm_headache", "旧头痛", "tcm", null, null, 1, LocalDateTime.now());
        when(aiSymptomTemplateMapper.selectOne(any())).thenReturn(existing);

        BuiltinSymptomImportResultVO result = symptomTemplateService.importJson(request);

        assertEquals("tcm", result.getMedicalMode());
        assertEquals(0, result.getCreatedCount());
        assertEquals(1, result.getUpdatedCount());
        verify(aiSymptomTemplateMapper, times(1)).updateById(any(AiSymptomTemplate.class));
    }

    private AiSymptomTemplate buildTemplate(String id,
                                            String key,
                                            String name,
                                            String medicalMode,
                                            String idOrg,
                                            String idRegion,
                                            int sortOrder,
                                            LocalDateTime updateTime) {
        AiSymptomTemplate item = new AiSymptomTemplate();
        item.setIdTemplate(id);
        item.setCdSymptom(key);
        item.setNaSymptom(name);
        item.setSdMedicalMode(medicalMode);
        item.setDesSymptom(name + "-desc");
        item.setFgCommon("1");
        item.setSortOrder(Integer.valueOf(sortOrder));
        item.setSystemCategoryJson("[\"nervous\"]");
        item.setBodyPartsJson("[]");
        item.setCustomScript("");
        item.setApplicablePopulationJson("{\"genders\":[],\"ageGroups\":[]}");
        item.setConfigJson("{\"title\":\"智能问诊\",\"sections\":[]}");
        item.setIdOrg(idOrg);
        item.setIdRegion(idRegion);
        item.setSdStatus("1");
        item.setFgActive("1");
        item.setInsertTime(updateTime.minusHours(1));
        item.setUpdateTime(updateTime);
        return item;
    }
}
