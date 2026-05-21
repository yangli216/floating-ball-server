package com.regionalai.floatingball.server.modules.feedback.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.regionalai.floatingball.server.modules.feedback.entity.AiFeedback;
import org.apache.ibatis.annotations.Mapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.ParameterizedType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiFeedbackMapperTest {

    @Test
    void mapperShouldKeepMyBatisPlusEntityContract() {
        assertTrue(AiFeedbackMapper.class.isAnnotationPresent(Mapper.class));

        ParameterizedType generic = (ParameterizedType) AiFeedbackMapper.class.getGenericInterfaces()[0];
        assertEquals(BaseMapper.class, generic.getRawType());
        assertEquals(AiFeedback.class, generic.getActualTypeArguments()[0]);
    }
}
