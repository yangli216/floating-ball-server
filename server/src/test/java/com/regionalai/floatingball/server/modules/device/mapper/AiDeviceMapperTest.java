package com.regionalai.floatingball.server.modules.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.regionalai.floatingball.server.modules.device.entity.AiDevice;
import org.junit.jupiter.api.Test;
import org.apache.ibatis.annotations.Mapper;

import java.lang.reflect.ParameterizedType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiDeviceMapperTest {

    @Test
    void mapperShouldKeepMyBatisPlusEntityContract() {
        assertTrue(AiDeviceMapper.class.isAnnotationPresent(Mapper.class));

        ParameterizedType generic = (ParameterizedType) AiDeviceMapper.class.getGenericInterfaces()[0];
        assertEquals(BaseMapper.class, generic.getRawType());
        assertEquals(AiDevice.class, generic.getActualTypeArguments()[0]);
    }
}
