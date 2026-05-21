package com.regionalai.floatingball.server.modules.config.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.regionalai.floatingball.server.modules.config.entity.AiConfig;
import org.apache.ibatis.annotations.Mapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.ParameterizedType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiConfigMapperTest {

    @Test
    void mapperShouldKeepMyBatisPlusEntityContract() {
        assertTrue(AiConfigMapper.class.isAnnotationPresent(Mapper.class));

        ParameterizedType generic = (ParameterizedType) AiConfigMapper.class.getGenericInterfaces()[0];
        assertEquals(BaseMapper.class, generic.getRawType());
        assertEquals(AiConfig.class, generic.getActualTypeArguments()[0]);
    }
}
