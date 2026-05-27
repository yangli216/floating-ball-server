package com.regionalai.floatingball.server.common.db;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class MybatisPlusQueryUtils {

    private MybatisPlusQueryUtils() {
    }

    public static <T> T selectFirst(BaseMapper<T> mapper, Wrapper<T> wrapper) {
        Page<T> page = new Page<T>(1, 1, false);
        Page<T> result = mapper.selectPage(page, wrapper);
        if (result == null) {
            return mapper.selectOne(wrapper);
        }
        List<T> records = result.getRecords();
        return records.isEmpty() ? null : records.get(0);
    }

    public static <T> List<T> selectLimit(BaseMapper<T> mapper, Wrapper<T> wrapper, long limit) {
        if (limit <= 0) {
            return Collections.emptyList();
        }
        Page<T> page = new Page<T>(1, limit, false);
        Page<T> result = mapper.selectPage(page, wrapper);
        if (result == null) {
            List<T> records = mapper.selectList(wrapper);
            if (records == null) {
                return Collections.emptyList();
            }
            int max = (int) Math.min(records.size(), limit);
            return records.subList(0, max);
        }
        return result.getRecords();
    }

    public static <T> List<Map<String, Object>> selectMapsLimit(BaseMapper<T> mapper, Wrapper<T> wrapper, long limit) {
        if (limit <= 0) {
            return Collections.emptyList();
        }
        IPage<Map<String, Object>> page = new Page<Map<String, Object>>(1, limit, false);
        IPage<Map<String, Object>> result = mapper.selectMapsPage(page, wrapper);
        if (result == null) {
            List<Map<String, Object>> records = mapper.selectMaps(wrapper);
            if (records == null) {
                return Collections.emptyList();
            }
            int max = (int) Math.min(records.size(), limit);
            return records.subList(0, max);
        }
        return result.getRecords();
    }
}
