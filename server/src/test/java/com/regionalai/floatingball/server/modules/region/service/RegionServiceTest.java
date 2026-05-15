package com.regionalai.floatingball.server.modules.region.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.regionalai.floatingball.server.common.api.PageResponse;
import com.regionalai.floatingball.server.common.exception.BusinessException;
import com.regionalai.floatingball.server.modules.region.entity.AiRegion;
import com.regionalai.floatingball.server.modules.region.mapper.AiRegionMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class RegionServiceTest {

    @Mock
    private AiRegionMapper aiRegionMapper;

    private RegionService regionService;

    @BeforeEach
    void setUp() {
        Configuration configuration = new Configuration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        assistant.setCurrentNamespace("test");
        TableInfoHelper.initTableInfo(assistant, AiRegion.class);
        regionService = new RegionService(aiRegionMapper);
    }

    @Test
    void listShouldReturnPagedRecordsAndApplyKeywordFilter() {
        AiRegion record = buildRegion("REG001", "REG-CODE", "华东区域");
        Page<AiRegion> mapperResult = new Page<>(1, 10, 1);
        mapperResult.setRecords(Collections.singletonList(record));

        when(aiRegionMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mapperResult);

        PageResponse<AiRegion> response = regionService.list(1, 10, "华东");

        assertEquals(1L, response.getCurrent());
        assertEquals(10L, response.getSize());
        assertEquals(1L, response.getTotal());
        assertEquals(Collections.singletonList(record), response.getRecords());

        ArgumentCaptor<Page<AiRegion>> pageCaptor = ArgumentCaptor.forClass(Page.class);
        ArgumentCaptor<LambdaQueryWrapper<AiRegion>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(aiRegionMapper).selectPage(pageCaptor.capture(), wrapperCaptor.capture());
        assertEquals(1L, pageCaptor.getValue().getCurrent());
        assertEquals(10L, pageCaptor.getValue().getSize());
        String sqlSegment = wrapperCaptor.getValue().getSqlSegment();
        long likeCount = sqlSegment.split("LIKE", -1).length - 1;
        assertTrue(sqlSegment.contains("LIKE"));
        assertEquals(1L, likeCount);
    }

    @Test
    void saveShouldRejectBlankRegionName() {
        AiRegion region = new AiRegion();
        region.setNaRegion(" ");

        BusinessException ex = assertThrows(BusinessException.class, () -> regionService.save(region));

        assertEquals("区域名称不能为空", ex.getMessage());
        verify(aiRegionMapper, never()).insert(any(AiRegion.class));
    }

    @Test
    void saveShouldPopulateDefaultFlagsWhenMissing() {
        AiRegion region = new AiRegion();
        region.setNaRegion("华北区域");

        AiRegion saved = regionService.save(region);

        assertSame(region, saved);
        assertEquals("1", region.getFgActive());
        assertEquals("1", region.getSdStatus());
        verify(aiRegionMapper).insert(region);
    }

    @Test
    void updateShouldSetIdAndReturnSelectedRegion() {
        AiRegion request = new AiRegion();
        request.setNaRegion("更新后区域");

        AiRegion persisted = buildRegion("REG001", "REG-CODE", "更新后区域");
        when(aiRegionMapper.selectById("REG001")).thenReturn(persisted);

        AiRegion result = regionService.update("REG001", request);

        assertEquals("REG001", request.getIdRegion());
        assertSame(persisted, result);
        verify(aiRegionMapper).updateById(request);
        verify(aiRegionMapper).selectById("REG001");
    }

    @Test
    void invalidateShouldThrowWhenRegionDoesNotExist() {
        when(aiRegionMapper.selectById("REG404")).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> regionService.invalidate("REG404"));

        assertEquals("区域不存在", ex.getMessage());
        verify(aiRegionMapper, never()).updateById(any(AiRegion.class));
    }

    @Test
    void invalidateShouldMarkRegionInactive() {
        AiRegion region = buildRegion("REG001", "REG-CODE", "华北区域");
        region.setFgActive("1");
        when(aiRegionMapper.selectById("REG001")).thenReturn(region);

        regionService.invalidate("REG001");

        assertEquals("0", region.getFgActive());
        verify(aiRegionMapper).updateById(region);
    }

    private AiRegion buildRegion(String idRegion, String cdRegion, String naRegion) {
        AiRegion region = new AiRegion();
        region.setIdRegion(idRegion);
        region.setCdRegion(cdRegion);
        region.setNaRegion(naRegion);
        region.setFgActive("1");
        region.setSdStatus("1");
        return region;
    }
}
