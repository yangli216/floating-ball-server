package com.regionalai.floatingball.server.modules.datapackage.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regionalai.floatingball.server.modules.datapackage.dto.TemplateDeltaVO;
import com.regionalai.floatingball.server.modules.datapackage.mapper.AiDataPackageMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

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
        when(aiDataPackageMapper.selectList(any())).thenReturn(Collections.emptyList());

        TemplateDeltaVO delta = dataPackageService.getTemplateDelta("ORG001", "REG001", "0");

        assertFalse(delta.getWestern().isEmpty());
        assertFalse(delta.getTcm().isEmpty());
    }

    @Test
    void latestVisibleVersionShouldUseBuiltinTemplateVersionWhenNoPublishedPackage() {
        when(aiDataPackageMapper.selectList(any())).thenReturn(Collections.emptyList());

        String version = dataPackageService.latestVisibleVersion("template", "ORG001", "REG001");

        assertEquals(dataPackageService.getTemplateDelta("ORG001", "REG001", null).getVersion(), version);
    }
}
