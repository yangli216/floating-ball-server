package com.regionalai.floatingball.server.modules.datapackage.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regionalai.floatingball.server.common.exception.BusinessException;
import com.regionalai.floatingball.server.modules.datapackage.dto.TemplateDeltaVO;
import com.regionalai.floatingball.server.modules.datapackage.entity.AiDataPackage;
import com.regionalai.floatingball.server.modules.datapackage.mapper.AiDataPackageMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DataPackageServiceTest {

    @Mock
    private AiDataPackageMapper aiDataPackageMapper;

    private DataPackageService dataPackageService;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        dataPackageService = new DataPackageService(aiDataPackageMapper, objectMapper, new BuiltinTemplateSeedService(objectMapper));
    }

    @Test
    void getTemplateDeltaShouldFallbackToBuiltinTemplatesWhenNoPublishedPackage() {
        TemplateDeltaVO delta = dataPackageService.getTemplateDelta("ORG001", "REG001", "0");

        assertFalse(delta.getWestern().isEmpty());
        assertFalse(delta.getTcm().isEmpty());
    }

    @Test
    void latestVisibleVersionShouldUseBuiltinTemplateVersionWhenNoPublishedPackage() {
        String version = dataPackageService.latestVisibleVersion("template", "ORG001", "REG001");

        assertEquals(dataPackageService.getBuiltinTemplateSnapshot().getVersion(), version);
    }

    @Test
    void saveShouldRejectUnsupportedPackageType() {
        AiDataPackage dataPackage = new AiDataPackage();
        dataPackage.setNaPackage("invalid");
        dataPackage.setSdPackageType("unknown");
        dataPackage.setVersionNum("2026.04.20.1");
        dataPackage.setContentJson("{}");

        BusinessException ex = assertThrows(BusinessException.class, () -> dataPackageService.save(dataPackage));

        assertEquals("仅支持 template 或 mapping 类型的数据包", ex.getMessage());
        verify(aiDataPackageMapper, never()).insert(dataPackage);
    }

    @Test
    void saveShouldRejectTemplateContentWithoutWesternOrTcm() {
        AiDataPackage dataPackage = new AiDataPackage();
        dataPackage.setNaPackage("template");
        dataPackage.setSdPackageType("template");
        dataPackage.setVersionNum("2026.04.20.1");
        dataPackage.setContentJson("{\"foo\":[]}");

        BusinessException ex = assertThrows(BusinessException.class, () -> dataPackageService.save(dataPackage));

        assertEquals("template 类型至少需要包含 western 或 tcm 字段", ex.getMessage());
        verify(aiDataPackageMapper, never()).insert(dataPackage);
    }
}
