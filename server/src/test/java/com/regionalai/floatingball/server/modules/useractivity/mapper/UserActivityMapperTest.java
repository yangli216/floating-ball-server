package com.regionalai.floatingball.server.modules.useractivity.mapper;

import com.regionalai.floatingball.server.common.db.DatabaseDialect;
import com.regionalai.floatingball.server.common.db.DatabaseDialectHolder;
import com.regionalai.floatingball.server.modules.useractivity.dto.UserActivityQueryDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserActivityMapperTest {

    @AfterEach
    void tearDown() {
        DatabaseDialectHolder.set(new DatabaseDialect(DatabaseDialect.Kind.ORACLE));
    }

    @Test
    void mapperShouldKeepMyBatisProviderAnnotations() throws Exception {
        assertTrue(UserActivityMapper.class.isAnnotationPresent(Mapper.class));

        Method countActiveUsers = UserActivityMapper.class.getMethod("countActiveUsers", UserActivityQueryDTO.class);
        assertHasQueryParam(countActiveUsers);
        assertProvider(countActiveUsers, "countActiveUsers");

        Method queryUserActivityList = UserActivityMapper.class.getMethod("queryUserActivityList", UserActivityQueryDTO.class);
        assertHasQueryParam(queryUserActivityList);
        assertProvider(queryUserActivityList, "queryUserActivityList");
    }

    @Test
    void oracleProviderShouldUseOracleTopOneSyntax() {
        DatabaseDialectHolder.set(new DatabaseDialect(DatabaseDialect.Kind.ORACLE));
        String sql = new UserActivitySqlProvider().queryUserActivityList();

        assertEnabledScopeJoin(sql);
        assertTrue(sql.contains("o.id_region AS idRegion"));
        assertTrue(sql.contains("r.na_region AS naRegion"));
        assertTrue(sql.contains("ROWNUM = 1"));
        assertFalse(sql.contains("LIMIT 1"));
    }

    @Test
    void gaussdbProviderShouldUseLimitSyntaxAndBoundTimes() {
        DatabaseDialectHolder.set(new DatabaseDialect(DatabaseDialect.Kind.OPENGAUSS));
        String sql = new UserActivitySqlProvider().queryUserActivityList();

        assertEnabledScopeJoin(sql);
        assertTrue(sql.contains("LIMIT 1"));
        assertTrue(sql.contains("query.dateFromTime"));
        assertTrue(sql.contains("query.dateToExclusiveTime"));
        assertFalse(sql.contains("ROWNUM"));
        assertFalse(sql.contains("TO_DATE"));
    }

    @Test
    void hisOrganizationFilterShouldScopeFactsAndDeviceDenominator() {
        DatabaseDialectHolder.set(new DatabaseDialect(DatabaseDialect.Kind.ORACLE));
        UserActivitySqlProvider provider = new UserActivitySqlProvider();

        assertTrue(provider.countActiveUsers().contains("ucl.id_his_org = #{query.hisOrgId}"));
        assertTrue(provider.countConsultations().contains("ucl.id_his_org = #{query.hisOrgId}"));
        assertTrue(provider.countTotalDevices().contains("ucl_scope.id_his_org = #{query.hisOrgId}"));

        String list = provider.queryUserActivityList();
        assertTrue(list.contains("id_his_org"));
        assertTrue(list.contains("AS hisOrgId"));
        assertTrue(list.contains("AS hisOrgName"));
    }

    @Test
    void userActivityListShouldOrderMostActiveDevicesFirst() {
        DatabaseDialectHolder.set(new DatabaseDialect(DatabaseDialect.Kind.ORACLE));

        String sql = new UserActivitySqlProvider().queryUserActivityList();

        assertTrue(sql.contains(
            "ORDER BY consultationCount DESC, effectiveConsultationCount DESC, "
                + "lastActiveTime DESC NULLS LAST, idDevice ASC"
        ));
    }

    private void assertHasQueryParam(Method method) {
        Parameter parameter = method.getParameters()[0];
        Param annotation = parameter.getAnnotation(Param.class);
        assertEquals("query", annotation.value());
    }

    private void assertProvider(Method method, String providerMethod) {
        SelectProvider provider = method.getAnnotation(SelectProvider.class);
        assertEquals(UserActivitySqlProvider.class, provider.type());
        assertEquals(providerMethod, provider.method());
    }

    private void assertEnabledScopeJoin(String sql) {
        assertTrue(sql.contains("JOIN c_ai_org"));
        assertTrue(sql.contains("JOIN c_ai_region"));
        assertTrue(sql.contains("o.sd_status = '1'"));
        assertTrue(sql.contains("r.sd_status = '1'"));
        assertTrue(sql.contains("o.id_region = #{query.idRegion}"));
    }
}
