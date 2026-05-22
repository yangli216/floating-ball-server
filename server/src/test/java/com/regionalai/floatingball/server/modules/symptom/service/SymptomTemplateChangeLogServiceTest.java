package com.regionalai.floatingball.server.modules.symptom.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.regionalai.floatingball.server.common.api.PageResponse;
import com.regionalai.floatingball.server.modules.auth.dto.AdminCurrentUser;
import com.regionalai.floatingball.server.modules.symptom.dto.SymptomTemplateChangeLogVO;
import com.regionalai.floatingball.server.modules.symptom.dto.SymptomTemplateVO;
import com.regionalai.floatingball.server.modules.symptom.entity.AiSymptomTemplateChangeLog;
import com.regionalai.floatingball.server.modules.symptom.mapper.AiSymptomTemplateChangeLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SymptomTemplateChangeLogServiceTest {

    @Mock
    private AiSymptomTemplateChangeLogMapper changeLogMapper;

    private ObjectMapper objectMapper;
    private SymptomTemplateChangeLogService service;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new SymptomTemplateChangeLogService(changeLogMapper, objectMapper);
    }

    @Test
    void recordShouldPersistOperatorAndFieldDiff() throws Exception {
        SymptomTemplateVO before = buildTemplate("T1", "fever", "发热", "1");
        SymptomTemplateVO after = buildTemplate("T1", "fever", "高热", "0");
        AdminCurrentUser operator = new AdminCurrentUser();
        operator.setIdUser("USER001");
        operator.setCdUser("admin");
        operator.setNaUser("系统管理员");

        service.record(SymptomTemplateChangeLogService.OPERATION_UPDATE, before, after, operator);

        ArgumentCaptor<AiSymptomTemplateChangeLog> captor = ArgumentCaptor.forClass(AiSymptomTemplateChangeLog.class);
        verify(changeLogMapper).insert(captor.capture());
        AiSymptomTemplateChangeLog saved = captor.getValue();
        assertEquals("T1", saved.getIdTemplate());
        assertEquals("fever", saved.getCdSymptom());
        assertEquals("高热", saved.getNaSymptom());
        assertEquals("USER001", saved.getIdOperator());
        assertEquals("admin", saved.getCdOperator());
        assertEquals("系统管理员", saved.getNaOperator());
        assertTrue(saved.getChangeSummary().contains("name"));

        Map<?, ?> diff = objectMapper.readValue(saved.getDiffJson(), Map.class);
        assertTrue(diff.containsKey("name"));
        assertTrue(diff.containsKey("sdStatus"));
    }

    @Test
    void listShouldParseSnapshotsAndDiff() {
        AiSymptomTemplateChangeLog row = new AiSymptomTemplateChangeLog();
        row.setIdLog("LOG001");
        row.setIdTemplate("T1");
        row.setCdSymptom("fever");
        row.setNaSymptom("发热");
        row.setSdMedicalMode("western");
        row.setOperationType(SymptomTemplateChangeLogService.OPERATION_CREATE);
        row.setIdOperator("USER001");
        row.setCdOperator("admin");
        row.setNaOperator("系统管理员");
        row.setChangeSummary("新增症状模板：发热");
        row.setBeforeJson("{}");
        row.setAfterJson("{\"name\":\"发热\"}");
        row.setDiffJson("{\"name\":{\"before\":null,\"after\":\"发热\"}}");
        row.setOperationTime(LocalDateTime.of(2026, 5, 22, 10, 30));
        row.setInsertTime(LocalDateTime.of(2026, 5, 22, 10, 30));
        row.setUpdateTime(LocalDateTime.of(2026, 5, 22, 10, 30));

        Page<AiSymptomTemplateChangeLog> page = new Page<AiSymptomTemplateChangeLog>(1, 10);
        page.setTotal(1);
        page.setRecords(Collections.singletonList(row));
        when(changeLogMapper.selectPage(any(), any())).thenReturn(page);

        PageResponse<SymptomTemplateChangeLogVO> result = service.list(1, 10, "T1", null, null, null, null, null, null);

        assertEquals(1, result.getTotal());
        SymptomTemplateChangeLogVO view = result.getRecords().get(0);
        assertEquals("LOG001", view.getIdLog());
        assertEquals("发热", view.getAfterSnapshot().get("name"));
        assertTrue(view.getDiff().containsKey("name"));
    }

    private SymptomTemplateVO buildTemplate(String id, String key, String name, String status) {
        SymptomTemplateVO vo = new SymptomTemplateVO();
        vo.setId(id);
        vo.setMedicalMode("western");
        vo.setKey(key);
        vo.setName(name);
        vo.setDescription("");
        vo.setCommonSymptom(Boolean.TRUE);
        vo.setSystemCategory(Collections.singletonList("nervous"));
        vo.setBodyParts(Collections.<String>emptyList());
        Map<String, Object> config = new LinkedHashMap<String, Object>();
        config.put("sections", Collections.emptyList());
        vo.setConfig(config);
        vo.setApplicablePopulation(Collections.<String, Object>emptyMap());
        vo.setSortOrder(Integer.valueOf(1));
        vo.setSdStatus(status);
        return vo;
    }
}
