package com.regionalai.floatingball.server.modules.lisresult.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.regionalai.floatingball.server.common.db.DatabaseDialect;
import com.regionalai.floatingball.server.common.exception.BusinessException;
import com.regionalai.floatingball.server.modules.auth.dto.AdminCurrentUser;
import com.regionalai.floatingball.server.modules.lisresult.dto.LisReportItemRequest;
import com.regionalai.floatingball.server.modules.lisresult.dto.LisReportSubmitRequest;
import com.regionalai.floatingball.server.modules.lisresult.dto.LisReportSubmitResponse;
import com.regionalai.floatingball.server.modules.lisresult.dto.PacsReportSubmitRequest;
import com.regionalai.floatingball.server.modules.lisresult.entity.HiOdsApply;
import com.regionalai.floatingball.server.modules.lisresult.entity.HiOdsApplyLisReport;
import com.regionalai.floatingball.server.modules.lisresult.entity.HiOdsApplyPacsReport;
import com.regionalai.floatingball.server.modules.lisresult.mapper.HiOdsApplyLisReportMapper;
import com.regionalai.floatingball.server.modules.lisresult.mapper.HiOdsApplyMapper;
import com.regionalai.floatingball.server.modules.lisresult.mapper.HiOdsApplyPacsReportMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LisResultEntryServiceTest {

    @Mock
    private HiOdsApplyMapper applyMapper;

    @Mock
    private HiOdsApplyLisReportMapper reportMapper;

    @Mock
    private HiOdsApplyPacsReportMapper pacsReportMapper;

    private LisResultEntryService service;

    @BeforeEach
    void setUp() {
        service = new LisResultEntryService(
            applyMapper,
            reportMapper,
            pacsReportMapper,
            new DatabaseDialect(DatabaseDialect.Kind.ORACLE)
        );
    }

    @Test
    void listAppliesRejectsUnsupportedBusinessType() {
        assertThrows(BusinessException.class, () -> service.listApplies(1, 10, null, null, "9", null, null, null, null));
    }

    @Test
    void listAppliesSelectsOnlyDisplayedColumns() {
        Page<HiOdsApply> pageResult = new Page<HiOdsApply>(1, 10);
        pageResult.setRecords(Collections.emptyList());
        when(applyMapper.selectPage(any(Page.class), any())).thenReturn(pageResult);

        service.listApplies(1, 10, "上呼吸道感染", null, null, null, "ORG001", "2026-07-01", "2026-07-02");

        ArgumentCaptor<Wrapper<HiOdsApply>> queryCaptor = wrapperCaptor();
        verify(applyMapper).selectPage(any(Page.class), queryCaptor.capture());
        assertSelectColumns(queryCaptor.getValue(),
            "id_apply",
            "cd_apply",
            "na_apply",
            "sd_disp",
            "sd_business",
            "sd_apply",
            "fg_urgent",
            "id_pi",
            "id_vis",
            "id_reg",
            "nas_diag",
            "na_dept_exec",
            "na_doc_exec",
            "id_org",
            "insert_time"
        );
    }

    @Test
    void listReportsSelectsOnlyDisplayedColumns() {
        when(reportMapper.selectList(any())).thenReturn(Collections.emptyList());

        service.listReports("APPLY001");

        ArgumentCaptor<Wrapper<HiOdsApplyLisReport>> queryCaptor = wrapperCaptor();
        verify(reportMapper).selectList(queryCaptor.capture());
        assertSelectColumns(queryCaptor.getValue(),
            "id_report",
            "id_apply",
            "resultid",
            "id_report_group",
            "cd_result",
            "na_result",
            "test_result",
            "result_qualitative",
            "reference_range",
            "reference_low",
            "reference_high",
            "result_unit",
            "result_hint",
            "instrument_code",
            "instrument_name",
            "insert_user",
            "insert_time",
            "update_user",
            "update_time"
        );
    }

    @Test
    void getPacsReportSelectsOnlyDisplayedColumns() {
        when(pacsReportMapper.selectOne(any())).thenReturn(null);

        service.getPacsReport("PACS001");

        ArgumentCaptor<Wrapper<HiOdsApplyPacsReport>> queryCaptor = wrapperCaptor();
        verify(pacsReportMapper).selectOne(queryCaptor.capture());
        assertSelectColumns(queryCaptor.getValue(),
            "id_report",
            "id_apply",
            "\"RESULT\"",
            "remark",
            "clinical_impression",
            "negative_positive",
            "diagnostic_imaging",
            "na_insert_user",
            "na_update_user",
            "cd_study",
            "id_dept",
            "na_dept",
            "insert_user",
            "insert_time",
            "update_user",
            "update_time"
        );
    }

    @Test
    void submitReportWritesValidItemsAndMarksApplyReported() {
        HiOdsApply apply = new HiOdsApply();
        apply.setIdApply("APPLY001");
        apply.setSdDisp("1");
        apply.setSdApply("1");
        apply.setIdOrg("ORG001");
        apply.setIdTet("TENANT001");
        apply.setRevision(2);
        when(applyMapper.selectOne(any())).thenReturn(apply);

        LisReportItemRequest valid = new LisReportItemRequest();
        valid.setCdResult("WBC");
        valid.setNaResult("白细胞计数");
        valid.setTestResult("6.2");
        valid.setReferenceRange("3.5-9.5");
        valid.setReferenceLow("3.5");
        valid.setReferenceHigh("9.5");
        valid.setResultUnit("10^9/L");

        LisReportItemRequest empty = new LisReportItemRequest();
        empty.setNaResult("空指标");

        LisReportSubmitRequest request = new LisReportSubmitRequest();
        request.setInstrumentCode("MANUAL");
        request.setInstrumentName("手工录入");
        request.setItems(Arrays.asList(valid, empty));

        AdminCurrentUser user = new AdminCurrentUser();
        user.setNaUser("系统管理员");

        LisReportSubmitResponse response = service.submitReport("APPLY001", request, user);

        assertEquals("APPLY001", response.getIdApply());
        assertEquals(1, response.getItemCount());
        assertNotNull(response.getReportGroupId());
        assertEquals(24, response.getReportGroupId().length());

        ArgumentCaptor<HiOdsApplyLisReport> reportCaptor = ArgumentCaptor.forClass(HiOdsApplyLisReport.class);
        verify(reportMapper).insert(reportCaptor.capture());
        HiOdsApplyLisReport report = reportCaptor.getValue();
        assertEquals(24, report.getIdReport().length());
        assertEquals("APPLY001", report.getIdApply());
        assertEquals("WBC", report.getCdResult());
        assertEquals("白细胞计数", report.getNaResult());
        assertEquals("6.2", report.getTestResult());
        assertEquals("ORG001", report.getIdOrg());
        assertEquals("TENANT001", report.getIdTet());
        assertEquals("系统管理员", report.getInsertUser());
        assertEquals(response.getReportGroupId(), report.getResultid());
        assertEquals(response.getReportGroupId(), report.getIdReportGroup());

        ArgumentCaptor<Wrapper<HiOdsApply>> queryCaptor = wrapperCaptor();
        verify(applyMapper).selectOne(queryCaptor.capture());
        assertSelectColumns(queryCaptor.getValue(),
            "id_apply",
            "sd_disp",
            "sd_apply",
            "id_result",
            "id_org",
            "id_tet",
            "revision",
            "dt_exec"
        );
        verify(applyMapper).update(org.mockito.Mockito.<HiOdsApply>isNull(), any(UpdateWrapper.class));
    }

    @Test
    void submitReportRejectsAlreadyReportedApply() {
        HiOdsApply apply = new HiOdsApply();
        apply.setIdApply("APPLY001");
        apply.setSdDisp("1");
        apply.setSdApply("3");
        when(applyMapper.selectOne(any())).thenReturn(apply);

        LisReportItemRequest item = new LisReportItemRequest();
        item.setNaResult("白细胞计数");
        item.setTestResult("6.2");
        LisReportSubmitRequest request = new LisReportSubmitRequest();
        request.setItems(Arrays.asList(item));

        assertThrows(BusinessException.class, () -> service.submitReport("APPLY001", request, null));

        verify(reportMapper, never()).insert(any(HiOdsApplyLisReport.class));
        verify(applyMapper, never()).update(org.mockito.Mockito.<HiOdsApply>isNull(), any(UpdateWrapper.class));
    }

    @Test
    void submitPacsReportWritesReportAndMarksApplyReported() {
        HiOdsApply apply = new HiOdsApply();
        apply.setIdApply("PACS001");
        apply.setSdDisp("2");
        apply.setSdApply("2");
        apply.setIdOrg("ORG001");
        apply.setIdTet("TENANT001");
        apply.setRevision(4);
        when(applyMapper.selectOne(any())).thenReturn(apply);

        PacsReportSubmitRequest request = new PacsReportSubmitRequest();
        request.setResult("双肺纹理增多，未见明确实变影。");
        request.setClinicalImpression("咳嗽待查");
        request.setDiagnosticImaging("双肺纹理增多，请结合临床。");
        request.setNegativePositive("阴性");
        request.setCdStudy("PACS202606170001");
        request.setIdDept("A1");
        request.setNaDept("放射科");

        AdminCurrentUser user = new AdminCurrentUser();
        user.setNaUser("系统管理员");

        LisReportSubmitResponse response = service.submitPacsReport("PACS001", request, user);

        assertEquals("PACS001", response.getIdApply());
        assertEquals(1, response.getItemCount());
        assertNotNull(response.getReportGroupId());
        assertEquals(24, response.getReportGroupId().length());

        ArgumentCaptor<HiOdsApplyPacsReport> reportCaptor = ArgumentCaptor.forClass(HiOdsApplyPacsReport.class);
        verify(pacsReportMapper).insert(reportCaptor.capture());
        HiOdsApplyPacsReport report = reportCaptor.getValue();
        assertEquals(24, report.getIdReport().length());
        assertEquals(response.getReportGroupId(), report.getIdReport());
        assertEquals("PACS001", report.getIdApply());
        assertEquals("双肺纹理增多，未见明确实变影。", report.getResult());
        assertEquals("双肺纹理增多，请结合临床。", report.getDiagnosticImaging());
        assertEquals("阴性", report.getNegativePositive());
        assertEquals("PACS202606170001", report.getCdStudy());
        assertEquals("ORG001", report.getIdOrg());
        assertEquals("系统管理员", report.getInsertUser());

        verify(applyMapper).update(org.mockito.Mockito.<HiOdsApply>isNull(), any(UpdateWrapper.class));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> ArgumentCaptor<Wrapper<T>> wrapperCaptor() {
        return ArgumentCaptor.forClass((Class) Wrapper.class);
    }

    private void assertSelectColumns(Wrapper<?> wrapper, String... columns) {
        assertEquals(Arrays.asList(columns), selectedColumns(wrapper));
    }

    private List<String> selectedColumns(Wrapper<?> wrapper) {
        String sqlSelect = ((QueryWrapper<?>) wrapper).getSqlSelect();
        return Arrays.stream(sqlSelect.split(","))
            .map(String::trim)
            .collect(Collectors.toList());
    }
}
