package com.regionalai.floatingball.server.modules.patientmemory.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.regionalai.floatingball.server.common.exception.BusinessException;
import com.regionalai.floatingball.server.modules.auth.dto.AdminCurrentUser;
import com.regionalai.floatingball.server.modules.patientmemory.dto.AdminPatientMemoryDetailVO;
import com.regionalai.floatingball.server.modules.patientmemory.dto.AdminPatientMemoryFactUpdateRequest;
import com.regionalai.floatingball.server.modules.patientmemory.entity.AiPatientMemory;
import com.regionalai.floatingball.server.modules.patientmemory.entity.AiPatientMemoryAudit;
import com.regionalai.floatingball.server.modules.patientmemory.entity.AiPatientMemoryFact;
import com.regionalai.floatingball.server.modules.patientmemory.mapper.AiPatientMemoryAuditMapper;
import com.regionalai.floatingball.server.modules.patientmemory.mapper.AiPatientMemoryFactMapper;
import com.regionalai.floatingball.server.modules.patientmemory.mapper.AiPatientMemoryMapper;
import com.regionalai.floatingball.server.modules.patientmemory.mapper.AiPatientMemoryObservationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminPatientMemoryServiceTest {

    @Mock private AiPatientMemoryMapper memoryMapper;
    @Mock private AiPatientMemoryFactMapper factMapper;
    @Mock private AiPatientMemoryObservationMapper observationMapper;
    @Mock private AiPatientMemoryAuditMapper auditMapper;

    private AdminPatientMemoryService service;
    private AiPatientMemory memory;
    private AiPatientMemoryFact fact;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        PatientMemoryService patientMemoryService = new PatientMemoryService(
            memoryMapper, observationMapper, factMapper, objectMapper
        );
        service = new AdminPatientMemoryService(
            memoryMapper, factMapper, observationMapper, auditMapper, patientMemoryService, objectMapper
        );
        memory = new AiPatientMemory();
        memory.setIdMemory("MEM001");
        memory.setIdOrg("ORG001");
        memory.setIdHisOrg("HIS001");
        memory.setPatientId("PAT001");
        memory.setPatientName("王某");
        memory.setMemoryVersion(4L);
        memory.setConflictCount(0);
        memory.setQualityStatus("fresh");
        memory.setFgActive("1");

        fact = new AiPatientMemoryFact();
        fact.setIdFact("FACT001");
        fact.setIdMemory("MEM001");
        fact.setFactKey("diagnosis:e119");
        fact.setFactKeyHash("HASH001");
        fact.setFactType("diagnosis");
        fact.setFactCode("E11.9");
        fact.setFactName("糖尿病");
        fact.setFactStatus("historical");
        fact.setConfidenceLevel("structured");
        fact.setOriginCode("his");
        fact.setFgSuppressed("0");
        fact.setFgActive("1");
        fact.setRevisionNo(1);
    }

    @Test
    void updateFactShouldCreateAuditedAdminOverrideAndBumpVersion() {
        when(memoryMapper.selectOne(any(Wrapper.class))).thenReturn(memory);
        when(factMapper.selectOne(any(Wrapper.class))).thenReturn(fact);
        when(factMapper.selectList(any(Wrapper.class))).thenReturn(Collections.singletonList(fact));
        when(factMapper.selectCount(any(Wrapper.class))).thenReturn(1L);
        when(observationMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(new Page<>(1, 100));
        when(auditMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(new Page<>(1, 100));

        AdminPatientMemoryFactUpdateRequest request = new AdminPatientMemoryFactUpdateRequest();
        request.setName("2型糖尿病");
        request.setStatus("active");
        request.setConfidence("confirmed");
        request.setCorrectionNote("根据医生复核和原始病历纠正具体分型");

        AdminPatientMemoryDetailVO detail = service.updateFact(systemAdmin(), "MEM001", "FACT001", request);

        assertEquals("2型糖尿病", fact.getFactName());
        assertEquals("admin", fact.getOriginCode());
        assertEquals("confirmed", fact.getConfidenceLevel());
        assertEquals(2, fact.getRevisionNo());
        assertEquals(5L, memory.getMemoryVersion());
        assertEquals("2型糖尿病", detail.getFacts().get(0).getName());
        ArgumentCaptor<AiPatientMemoryAudit> auditCaptor = ArgumentCaptor.forClass(AiPatientMemoryAudit.class);
        verify(auditMapper).insert(auditCaptor.capture());
        assertEquals("correct", auditCaptor.getValue().getActionCode());
        assertTrue(auditCaptor.getValue().getNoteText().contains("原始病历"));
    }

    @Test
    void detailShouldRejectCrossOrganizationAccessForNonSystemAdmin() {
        when(memoryMapper.selectOne(any(Wrapper.class))).thenReturn(memory);
        AdminCurrentUser user = new AdminCurrentUser();
        user.setIdUser("USER002");
        user.setIdOrg("ORG002");
        user.setRoles(Collections.singletonList("ORG_ADMIN"));

        BusinessException error = assertThrows(BusinessException.class, () -> service.detail(user, "MEM001"));

        assertTrue(error.getMessage().contains("无权访问"));
    }

    private AdminCurrentUser systemAdmin() {
        AdminCurrentUser user = new AdminCurrentUser();
        user.setIdUser("USER001");
        user.setNaUser("系统管理员");
        user.setIdOrg("ORG001");
        user.setRoles(Collections.singletonList("SYSTEM_ADMIN"));
        return user;
    }
}
