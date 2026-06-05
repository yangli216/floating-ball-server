package com.regionalai.floatingball.server.modules.useractivity.mapper;

import com.regionalai.floatingball.server.modules.useractivity.dto.UserActivityQueryDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserActivityMapperTest {

    @Test
    void mapperShouldKeepMyBatisAnnotations() throws Exception {
        assertTrue(UserActivityMapper.class.isAnnotationPresent(Mapper.class));

        Method countActiveUsers = UserActivityMapper.class.getMethod("countActiveUsers", UserActivityQueryDTO.class);
        assertHasQueryParam(countActiveUsers);
        assertEnabledScopeJoin(countActiveUsers);

        Method queryUserActivityList = UserActivityMapper.class.getMethod("queryUserActivityList", UserActivityQueryDTO.class);
        assertHasQueryParam(queryUserActivityList);
        assertEnabledScopeJoin(queryUserActivityList);
        String sql = joinedSql(queryUserActivityList);
        assertTrue(sql.contains("o.id_region AS idRegion"));
        assertTrue(sql.contains("r.na_region AS naRegion"));
    }

    private void assertHasQueryParam(Method method) {
        Parameter parameter = method.getParameters()[0];
        Param annotation = parameter.getAnnotation(Param.class);
        assertEquals("query", annotation.value());
    }

    private void assertEnabledScopeJoin(Method method) {
        String sql = joinedSql(method);
        assertTrue(sql.contains("JOIN c_ai_org"));
        assertTrue(sql.contains("JOIN c_ai_region"));
        assertTrue(sql.contains("o.sd_status = '1'"));
        assertTrue(sql.contains("r.sd_status = '1'"));
        assertTrue(sql.contains("o.id_region = #{query.idRegion}"));
    }

    private String joinedSql(Method method) {
        Select select = method.getAnnotation(Select.class);
        return String.join("\n", select.value());
    }
}
