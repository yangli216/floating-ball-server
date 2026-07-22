package com.regionalai.floatingball.server.modules.userlog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.regionalai.floatingball.server.modules.userlog.entity.AiUserConsultationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiUserConsultationLogMapperTest {

    @Test
    void mapperShouldKeepLatestUserNameQueryContract() throws Exception {
        assertTrue(AiUserConsultationLogMapper.class.isAnnotationPresent(Mapper.class));

        Method method = AiUserConsultationLogMapper.class.getMethod("selectLatestUserNames", List.class);
        SelectProvider provider = method.getAnnotation(SelectProvider.class);
        assertEquals(UserConsultationLogSqlProvider.class, provider.type());
        assertEquals("selectLatestUserNames", provider.method());

        Parameter parameter = method.getParameters()[0];
        assertEquals("deviceIds", parameter.getAnnotation(Param.class).value());
    }

    @Test
    void providerShouldSelectLatestNonBlankNameForEachDevice() {
        String sql = new UserConsultationLogSqlProvider().selectLatestUserNames();

        assertTrue(sql.contains("ROW_NUMBER() OVER (PARTITION BY ucl.id_device"));
        assertTrue(sql.contains("ORDER BY ucl.consultation_time DESC, ucl.id_log DESC"));
        assertTrue(sql.contains("LENGTH(TRIM(ucl.na_doctor)) &gt; 0"));
        assertTrue(sql.contains("<foreach collection='deviceIds'"));
        assertTrue(sql.contains("<otherwise>AND 1 = 0</otherwise>"));
        assertTrue(sql.contains(") latest WHERE rowNumber = 1"));
    }

    @Test
    void mapperShouldStillExtendUserLogBaseMapper() {
        ParameterizedType generic = (ParameterizedType) AiUserConsultationLogMapper.class.getGenericInterfaces()[0];

        assertEquals(BaseMapper.class, generic.getRawType());
        assertEquals(AiUserConsultationLog.class, generic.getActualTypeArguments()[0]);
    }
}
