package com.regionalai.floatingball.server.modules.lisresult.service;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
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
    void submitReportWritesValidItemsAndMarksApplyReported() {
        HiOdsApply apply = new HiOdsApply();
        apply.setIdApply("APPLY001");
        apply.setSdDisp("1");
        apply.setSdApply("1");
        apply.setIdOrg("ORG001");
        apply.setIdTet("TENANT001");
        apply.setRevision(2);
        when(applyMapper.selectById("APPLY001")).thenReturn(apply);

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

        verify(applyMapper).update(org.mockito.Mockito.<HiOdsApply>isNull(), any(UpdateWrapper.class));
    }

    @Test
    void submitReportRejectsAlreadyReportedApply() {
        HiOdsApply apply = new HiOdsApply();
        apply.setIdApply("APPLY001");
        apply.setSdDisp("1");
        apply.setSdApply("3");
        when(applyMapper.selectById("APPLY001")).thenReturn(apply);

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
        when(applyMapper.selectById("PACS001")).thenReturn(apply);

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
}
