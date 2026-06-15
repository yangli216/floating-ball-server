package com.regionalai.floatingball.server.modules.org.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.regionalai.floatingball.server.common.api.PageResponse;
import com.regionalai.floatingball.server.common.db.DatabaseDialect;
import com.regionalai.floatingball.server.common.exception.BusinessException;
import com.regionalai.floatingball.server.modules.org.entity.AiOrg;
import com.regionalai.floatingball.server.modules.org.mapper.AiOrgMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrgServiceTest {

    @Mock
    private AiOrgMapper aiOrgMapper;

    private OrgService orgService;

    @BeforeEach
    void setUp() {
        Configuration configuration = new Configuration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        assistant.setCurrentNamespace("test");
        TableInfoHelper.initTableInfo(assistant, AiOrg.class);
        orgService = new OrgService(aiOrgMapper, new DatabaseDialect(DatabaseDialect.Kind.ORACLE));
    }

    @Test
    void listShouldReturnPagedRecordsAndApplyKeywordFilter() {
        AiOrg record = buildOrg("ORG001", "ORG-CODE", "人民医院");
        Page<AiOrg> mapperResult = new Page<>(2, 5, 1);
        mapperResult.setRecords(Collections.singletonList(record));

        when(aiOrgMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mapperResult);

        PageResponse<AiOrg> response = orgService.list(2, 5, "人民", "REG001", "1");

        assertEquals(2L, response.getCurrent());
        assertEquals(5L, response.getSize());
        assertEquals(1L, response.getTotal());
        assertEquals(Collections.singletonList(record), response.getRecords());

        ArgumentCaptor<Page<AiOrg>> pageCaptor = ArgumentCaptor.forClass(Page.class);
        ArgumentCaptor<LambdaQueryWrapper<AiOrg>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(aiOrgMapper).selectPage(pageCaptor.capture(), wrapperCaptor.capture());
        assertEquals(2L, pageCaptor.getValue().getCurrent());
        assertEquals(5L, pageCaptor.getValue().getSize());
        String sqlSegment = wrapperCaptor.getValue().getSqlSegment();
        long likeCount = sqlSegment.split("LIKE", -1).length - 1;
        assertTrue(sqlSegment.contains("LIKE"));
        assertTrue(sqlSegment.contains("id_region"));
        assertTrue(sqlSegment.contains("sd_status"));
        assertEquals(2L, likeCount);
    }

    @Test
    void saveShouldRejectBlankOrgName() {
        AiOrg org = new AiOrg();
        org.setCdOrg("ORG-CODE");
        org.setNaOrg(" ");

        BusinessException ex = assertThrows(BusinessException.class, () -> orgService.save(org));

        assertEquals("机构名称不能为空", ex.getMessage());
        verify(aiOrgMapper, never()).insert(any(AiOrg.class));
    }

    @Test
    void saveShouldRejectBlankOrgCode() {
        AiOrg org = new AiOrg();
        org.setCdOrg(" ");
        org.setNaOrg("区域总院");

        BusinessException ex = assertThrows(BusinessException.class, () -> orgService.save(org));

        assertEquals("机构编码不能为空", ex.getMessage());
        verify(aiOrgMapper, never()).insert(any(AiOrg.class));
    }

    @Test
    void saveShouldPopulateDefaultFlagsWhenMissing() {
        AiOrg org = new AiOrg();
        org.setCdOrg(" ORG-CODE ");
        org.setNaOrg("区域总院");

        AiOrg saved = orgService.save(org);

        assertSame(org, saved);
        assertEquals("ORG-CODE", org.getCdOrg());
        assertEquals("1", org.getFgActive());
        assertEquals("1", org.getSdStatus());
        verify(aiOrgMapper).insert(org);
    }

    @Test
    void saveShouldRejectDuplicateOrgCodeBeforeInsert() {
        AiOrg existing = buildOrg("ORG001", "ORG-CODE", "已存在机构");
        when(aiOrgMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        AiOrg org = new AiOrg();
        org.setCdOrg("ORG-CODE");
        org.setNaOrg("区域总院");

        BusinessException ex = assertThrows(BusinessException.class, () -> orgService.save(org));

        assertEquals("机构编码已存在", ex.getMessage());
        verify(aiOrgMapper, never()).insert(any(AiOrg.class));
    }

    @Test
    void saveShouldTranslateDatabaseUniqueConflictToBusinessError() {
        AiOrg org = new AiOrg();
        org.setCdOrg("ORG-CODE");
        org.setNaOrg("区域总院");
        when(aiOrgMapper.insert(any(AiOrg.class))).thenThrow(new DuplicateKeyException("uk_c_ai_org_code_active"));

        BusinessException ex = assertThrows(BusinessException.class, () -> orgService.save(org));

        assertEquals("机构编码已存在", ex.getMessage());
    }

    @Test
    void updateShouldSetIdAndReturnSelectedOrg() {
        AiOrg request = new AiOrg();
        request.setCdOrg(" ORG-CODE ");
        request.setNaOrg("更新后机构");

        AiOrg persisted = buildOrg("ORG001", "ORG-CODE", "更新后机构");
        when(aiOrgMapper.selectById("ORG001")).thenReturn(persisted);

        AiOrg result = orgService.update("ORG001", request);

        assertEquals("ORG001", request.getIdOrg());
        assertEquals("ORG-CODE", request.getCdOrg());
        assertSame(persisted, result);
        verify(aiOrgMapper).updateById(request);
        verify(aiOrgMapper).selectById("ORG001");
    }

    @Test
    void updateShouldRejectDuplicateOrgCode() {
        AiOrg request = new AiOrg();
        request.setCdOrg("ORG-CODE");
        request.setNaOrg("更新后机构");
        when(aiOrgMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(buildOrg("ORG002", "ORG-CODE", "重复机构"));

        BusinessException ex = assertThrows(BusinessException.class, () -> orgService.update("ORG001", request));

        assertEquals("机构编码已存在", ex.getMessage());
        verify(aiOrgMapper, never()).updateById(any(AiOrg.class));
    }

    @Test
    void invalidateShouldThrowWhenOrgDoesNotExist() {
        when(aiOrgMapper.selectById("ORG404")).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> orgService.invalidate("ORG404"));

        assertEquals("机构不存在", ex.getMessage());
        verify(aiOrgMapper, never()).updateById(any(AiOrg.class));
    }

    @Test
    void invalidateShouldDisableOrgStatus() {
        AiOrg org = buildOrg("ORG001", "ORG-CODE", "区域总院");
        org.setFgActive("1");
        org.setSdStatus("1");
        when(aiOrgMapper.selectById("ORG001")).thenReturn(org);

        orgService.invalidate("ORG001");

        assertEquals("1", org.getFgActive());
        assertEquals("0", org.getSdStatus());
        verify(aiOrgMapper).updateById(org);
    }

    @Test
    void enableShouldEnableOrgStatus() {
        AiOrg org = buildOrg("ORG001", "ORG-CODE", "区域总院");
        org.setFgActive("1");
        org.setSdStatus("0");
        when(aiOrgMapper.selectById("ORG001")).thenReturn(org);

        orgService.enable("ORG001");

        assertEquals("1", org.getFgActive());
        assertEquals("1", org.getSdStatus());
        verify(aiOrgMapper).updateById(org);
    }

    private AiOrg buildOrg(String idOrg, String cdOrg, String naOrg) {
        AiOrg org = new AiOrg();
        org.setIdOrg(idOrg);
        org.setCdOrg(cdOrg);
        org.setNaOrg(naOrg);
        org.setFgActive("1");
        org.setSdStatus("1");
        return org;
    }
}
