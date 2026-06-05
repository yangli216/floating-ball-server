package com.regionalai.floatingball.server.modules.analytics.mapper;

import com.regionalai.floatingball.server.modules.analytics.dto.AnalyticsQueryDTO;
import com.regionalai.floatingball.server.modules.analytics.dto.FunctionUsageQueryDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalyticsMapperTest {

    @Test
    void mapperShouldKeepMyBatisAnnotations() throws Exception {
        assertTrue(AnalyticsMapper.class.isAnnotationPresent(Mapper.class));

        Method countAiService = AnalyticsMapper.class.getMethod("countAiService", AnalyticsQueryDTO.class);
        assertHasQueryParam(countAiService);
        assertSelectContains(countAiService, "c_ai_feature_event", "event_status", "id_region");

        Method functionRanking = AnalyticsMapper.class.getMethod("queryFunctionUsageRanking", FunctionUsageQueryDTO.class);
        assertHasQueryParam(functionRanking);
        assertSelectContains(functionRanking, "CASE", "treatment_plan_recommendation", "ORDER BY callCount DESC");

        Method functionTrend = AnalyticsMapper.class.getMethod("queryFunctionUsageTrend", FunctionUsageQueryDTO.class);
        assertHasQueryParam(functionTrend);
        assertSelectContains(functionTrend, "TO_CHAR(TRUNC(e.event_time)", "AS moduleName", "dayStr");
    }

    @Test
    void distinctModuleSqlShouldExposeCanonicalFeatureCatalog() throws Exception {
        Method method = AnalyticsMapper.class.getMethod("queryDistinctModules");
        String sql = joinedSql(method);

        assertTrue(sql.contains("语音问诊"));
        assertTrue(sql.contains("智能问诊"));
        assertTrue(sql.contains("AI推荐处置"));
        assertTrue(sql.contains("AI推荐治疗方案"));
        assertTrue(sql.contains("知识库使用"));
    }

    @Test
    void diagnosisMatchedSqlShouldUseChangeSummary() throws Exception {
        Method method = AnalyticsMapper.class.getMethod("countDiagnosisMatchedConsultations", AnalyticsQueryDTO.class);
        String sql = joinedSql(method);

        assertTrue(sql.contains("c_ai_user_consultation_log"));
        assertTrue(sql.contains("change_summary_json"));
        assertTrue(sql.contains("$.diagnosisChanges"));
    }

    @Test
    void analyticsSqlShouldJoinEnabledRegionAndOrgForScopeFilters() throws Exception {
        Method summary = AnalyticsMapper.class.getMethod("countAiService", AnalyticsQueryDTO.class);
        Method consultation = AnalyticsMapper.class.getMethod("countConsultation", AnalyticsQueryDTO.class);
        Method functionUsage = AnalyticsMapper.class.getMethod("queryFunctionUsageRanking", FunctionUsageQueryDTO.class);

        assertEnabledScopeJoin(summary);
        assertEnabledScopeJoin(consultation);
        assertEnabledScopeJoin(functionUsage);
    }

    private void assertHasQueryParam(Method method) {
        Parameter parameter = method.getParameters()[0];
        Param annotation = parameter.getAnnotation(Param.class);
        assertEquals("query", annotation.value());
    }

    private void assertSelectContains(Method method, String first, String second, String third) {
        String sql = joinedSql(method);
        assertTrue(sql.contains(first));
        assertTrue(sql.contains(second));
        assertTrue(sql.contains(third));
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
