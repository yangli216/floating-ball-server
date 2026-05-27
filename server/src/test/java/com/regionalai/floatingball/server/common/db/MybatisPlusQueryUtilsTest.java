package com.regionalai.floatingball.server.common.db;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MybatisPlusQueryUtilsTest {

    @Test
    void selectLimitShouldUseMybatisPlusPage() {
        BaseMapper<String> mapper = mock(BaseMapper.class);
        when(mapper.selectPage(any(IPage.class), any())).thenAnswer(invocation -> {
            IPage<String> page = invocation.getArgument(0);
            page.setRecords(Arrays.asList("a", "b"));
            return page;
        });

        List<String> records = MybatisPlusQueryUtils.selectLimit(mapper, new QueryWrapper<String>(), 2);

        assertEquals(Arrays.asList("a", "b"), records);
        verify(mapper).selectPage(any(IPage.class), any());
    }

    @Test
    void selectMapsLimitShouldUseMybatisPlusMapsPage() {
        BaseMapper<Object> mapper = mock(BaseMapper.class);
        Map<String, Object> row = new HashMap<String, Object>();
        row.put("name", "default");
        when(mapper.selectMapsPage(any(IPage.class), any())).thenAnswer(invocation -> {
            IPage<Map<String, Object>> page = invocation.getArgument(0);
            page.setRecords(Collections.singletonList(row));
            return page;
        });

        List<Map<String, Object>> records = MybatisPlusQueryUtils.selectMapsLimit(mapper, new QueryWrapper<Object>(), 1);

        assertEquals("default", records.get(0).get("name"));
        verify(mapper).selectMapsPage(any(IPage.class), any());
    }

    @Test
    void limitShouldIgnoreNonPositiveSizes() {
        BaseMapper<String> mapper = mock(BaseMapper.class);

        assertEquals(Collections.emptyList(), MybatisPlusQueryUtils.selectLimit(mapper, new QueryWrapper<String>(), 0));

        verify(mapper, never()).selectPage(any(IPage.class), any());
    }

    @Test
    void selectFirstShouldFallbackForLegacyMapperMocks() {
        BaseMapper<String> mapper = mock(BaseMapper.class);
        when(mapper.selectPage(any(IPage.class), any())).thenReturn(null);
        when(mapper.selectOne(any())).thenReturn("legacy");

        assertEquals("legacy", MybatisPlusQueryUtils.selectFirst(mapper, new QueryWrapper<String>()));
    }
}
