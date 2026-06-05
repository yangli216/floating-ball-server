package com.regionalai.floatingball.server.modules.analytics.mapper;

import com.regionalai.floatingball.server.modules.analytics.dto.AnalyticsQueryDTO;
import com.regionalai.floatingball.server.modules.analytics.dto.DistributionItemVO;
import com.regionalai.floatingball.server.modules.analytics.dto.FunctionUsageItemVO;
import com.regionalai.floatingball.server.modules.analytics.dto.FunctionUsageQueryDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface AnalyticsMapper {

    String FUNCTION_USAGE_MODULE_EXPR = "CASE "
        + "WHEN e.feature_code = 'voice_consultation' THEN '语音问诊' "
        + "WHEN e.feature_code = 'smart_consultation' THEN '智能问诊' "
        + "WHEN e.feature_code = 'report_interpretation' THEN '报告单解读' "
        + "WHEN e.feature_code = 'chat' THEN '聊天' "
        + "WHEN e.feature_code = 'diagnosis_checklist' THEN 'AI诊断鉴别' "
        + "WHEN e.feature_code = 'diagnosis_recommendation' THEN 'AI推荐诊断' "
        + "WHEN e.feature_code = 'medication_recommendation' THEN 'AI推荐用药' "
        + "WHEN e.feature_code = 'examination_recommendation' THEN 'AI推荐检查' "
        + "WHEN e.feature_code = 'lab_test_recommendation' THEN 'AI推荐检验' "
        + "WHEN e.feature_code = 'procedure_recommendation' THEN 'AI推荐处置' "
        + "WHEN e.feature_code = 'treatment_plan_recommendation' THEN 'AI推荐治疗方案' "
        + "WHEN e.feature_code = 'knowledge_usage' THEN '知识库使用' "
        + "WHEN e.feature_name = 'AI诊疗方案推荐' THEN 'AI推荐治疗方案' "
        + "ELSE e.feature_name END";

    String FEATURE_SCOPE_JOIN = "JOIN c_ai_org o ON o.id_org = e.id_org AND o.fg_active = '1' AND o.sd_status = '1'";
    String FEATURE_REGION_JOIN = "JOIN c_ai_region r ON r.id_region = o.id_region AND r.fg_active = '1' AND r.sd_status = '1'";
    String CONSULTATION_SCOPE_JOIN = "JOIN c_ai_org o ON o.id_org = ucl.id_org AND o.fg_active = '1' AND o.sd_status = '1'";
    String CONSULTATION_REGION_JOIN = "JOIN c_ai_region r ON r.id_region = o.id_region AND r.fg_active = '1' AND r.sd_status = '1'";

    @Select({
        "<script>",
        "SELECT COUNT(1) AS cnt FROM c_ai_feature_event e",
        FEATURE_SCOPE_JOIN,
        FEATURE_REGION_JOIN,
        "WHERE e.fg_active = '1' AND LOWER(e.event_status) = 'success'",
        "<if test='query.dateFrom != null and query.dateFrom != \"\"'>",
        "  AND e.event_time &gt;= TO_DATE(#{query.dateFrom}, 'yyyy-MM-dd')",
        "</if>",
        "<if test='query.dateTo != null and query.dateTo != \"\"'>",
        "  AND e.event_time &lt; TO_DATE(#{query.dateTo}, 'yyyy-MM-dd') + 1",
        "</if>",
        "<if test='query.idOrg != null and query.idOrg != \"\"'>",
        "  AND o.id_org = #{query.idOrg}",
        "</if>",
        "<if test='query.idRegion != null and query.idRegion != \"\"'>",
        "  AND o.id_region = #{query.idRegion}",
        "</if>",
        "</script>"
    })
    long countAiService(@Param("query") AnalyticsQueryDTO query);

    @Select({
        "<script>",
        "SELECT COUNT(1) AS cnt FROM c_ai_user_consultation_log ucl",
        CONSULTATION_SCOPE_JOIN,
        CONSULTATION_REGION_JOIN,
        "WHERE ucl.fg_active = '1'",
        "<if test='query.dateFrom != null and query.dateFrom != \"\"'>",
        "  AND ucl.consultation_time &gt;= TO_DATE(#{query.dateFrom}, 'yyyy-MM-dd')",
        "</if>",
        "<if test='query.dateTo != null and query.dateTo != \"\"'>",
        "  AND ucl.consultation_time &lt; TO_DATE(#{query.dateTo}, 'yyyy-MM-dd') + 1",
        "</if>",
        "<if test='query.idOrg != null and query.idOrg != \"\"'>",
        "  AND o.id_org = #{query.idOrg}",
        "</if>",
        "<if test='query.idRegion != null and query.idRegion != \"\"'>",
        "  AND o.id_region = #{query.idRegion}",
        "</if>",
        "</script>"
    })
    long countConsultation(@Param("query") AnalyticsQueryDTO query);

    @Select({
        "<script>",
        "SELECT COUNT(DISTINCT ucl.id_doctor) AS cnt FROM c_ai_user_consultation_log ucl",
        CONSULTATION_SCOPE_JOIN,
        CONSULTATION_REGION_JOIN,
        "WHERE ucl.fg_active = '1'",
        "<if test='query.dateFrom != null and query.dateFrom != \"\"'>",
        "  AND ucl.consultation_time &gt;= TO_DATE(#{query.dateFrom}, 'yyyy-MM-dd')",
        "</if>",
        "<if test='query.dateTo != null and query.dateTo != \"\"'>",
        "  AND ucl.consultation_time &lt; TO_DATE(#{query.dateTo}, 'yyyy-MM-dd') + 1",
        "</if>",
        "<if test='query.idOrg != null and query.idOrg != \"\"'>",
        "  AND o.id_org = #{query.idOrg}",
        "</if>",
        "<if test='query.idRegion != null and query.idRegion != \"\"'>",
        "  AND o.id_region = #{query.idRegion}",
        "</if>",
        "</script>"
    })
    long countActiveDoctors(@Param("query") AnalyticsQueryDTO query);

    @Select({
        "<script>",
        "SELECT COUNT(1) AS cnt FROM c_ai_user_consultation_log ucl",
        CONSULTATION_SCOPE_JOIN,
        CONSULTATION_REGION_JOIN,
        "WHERE ucl.fg_active = '1' AND ucl.status = 'completed'",
        "<if test='query.dateFrom != null and query.dateFrom != \"\"'>",
        "  AND ucl.consultation_time &gt;= TO_DATE(#{query.dateFrom}, 'yyyy-MM-dd')",
        "</if>",
        "<if test='query.dateTo != null and query.dateTo != \"\"'>",
        "  AND ucl.consultation_time &lt; TO_DATE(#{query.dateTo}, 'yyyy-MM-dd') + 1",
        "</if>",
        "<if test='query.idOrg != null and query.idOrg != \"\"'>",
        "  AND o.id_org = #{query.idOrg}",
        "</if>",
        "<if test='query.idRegion != null and query.idRegion != \"\"'>",
        "  AND o.id_region = #{query.idRegion}",
        "</if>",
        "</script>"
    })
    long countAdoptedConsultations(@Param("query") AnalyticsQueryDTO query);

    @Select({
        "<script>",
        "SELECT COUNT(1) AS cnt FROM c_ai_user_consultation_log ucl",
        CONSULTATION_SCOPE_JOIN,
        CONSULTATION_REGION_JOIN,
        "WHERE ucl.fg_active = '1' AND ucl.status IN ('completed','abandoned')",
        "<if test='query.dateFrom != null and query.dateFrom != \"\"'>",
        "  AND ucl.consultation_time &gt;= TO_DATE(#{query.dateFrom}, 'yyyy-MM-dd')",
        "</if>",
        "<if test='query.dateTo != null and query.dateTo != \"\"'>",
        "  AND ucl.consultation_time &lt; TO_DATE(#{query.dateTo}, 'yyyy-MM-dd') + 1",
        "</if>",
        "<if test='query.idOrg != null and query.idOrg != \"\"'>",
        "  AND o.id_org = #{query.idOrg}",
        "</if>",
        "<if test='query.idRegion != null and query.idRegion != \"\"'>",
        "  AND o.id_region = #{query.idRegion}",
        "</if>",
        "</script>"
    })
    long countFinalizedConsultations(@Param("query") AnalyticsQueryDTO query);

    @Select({
        "<script>",
        "SELECT COUNT(1) AS cnt FROM c_ai_user_consultation_log ucl",
        CONSULTATION_SCOPE_JOIN,
        CONSULTATION_REGION_JOIN,
        "WHERE ucl.fg_active = '1' AND ucl.status IN ('completed','abandoned')",
        "  AND JSON_VALUE(ucl.change_summary_json, '$.diagnosisChanges' RETURNING NUMBER) = 0",
        "<if test='query.dateFrom != null and query.dateFrom != \"\"'>",
        "  AND ucl.consultation_time &gt;= TO_DATE(#{query.dateFrom}, 'yyyy-MM-dd')",
        "</if>",
        "<if test='query.dateTo != null and query.dateTo != \"\"'>",
        "  AND ucl.consultation_time &lt; TO_DATE(#{query.dateTo}, 'yyyy-MM-dd') + 1",
        "</if>",
        "<if test='query.idOrg != null and query.idOrg != \"\"'>",
        "  AND o.id_org = #{query.idOrg}",
        "</if>",
        "<if test='query.idRegion != null and query.idRegion != \"\"'>",
        "  AND o.id_region = #{query.idRegion}",
        "</if>",
        "</script>"
    })
    long countDiagnosisMatchedConsultations(@Param("query") AnalyticsQueryDTO query);

    @Select({
        "<script>",
        "SELECT TO_CHAR(TRUNC(e.event_time), 'yyyy-MM-dd') AS day_str, COUNT(1) AS cnt",
        "FROM c_ai_feature_event e",
        FEATURE_SCOPE_JOIN,
        FEATURE_REGION_JOIN,
        "WHERE e.fg_active = '1' AND LOWER(e.event_status) = 'success'",
        "<if test='query.dateFrom != null and query.dateFrom != \"\"'>",
        "  AND e.event_time &gt;= TO_DATE(#{query.dateFrom}, 'yyyy-MM-dd')",
        "</if>",
        "<if test='query.dateTo != null and query.dateTo != \"\"'>",
        "  AND e.event_time &lt; TO_DATE(#{query.dateTo}, 'yyyy-MM-dd') + 1",
        "</if>",
        "<if test='query.idOrg != null and query.idOrg != \"\"'>",
        "  AND o.id_org = #{query.idOrg}",
        "</if>",
        "<if test='query.idRegion != null and query.idRegion != \"\"'>",
        "  AND o.id_region = #{query.idRegion}",
        "</if>",
        "GROUP BY TRUNC(e.event_time)",
        "ORDER BY TRUNC(e.event_time)",
        "</script>"
    })
    List<Map<String, Object>> queryAiServiceTrend(@Param("query") AnalyticsQueryDTO query);

    @Select({
        "<script>",
        "SELECT TO_CHAR(TRUNC(ucl.consultation_time), 'yyyy-MM-dd') AS day_str, COUNT(1) AS cnt",
        "FROM c_ai_user_consultation_log ucl",
        CONSULTATION_SCOPE_JOIN,
        CONSULTATION_REGION_JOIN,
        "WHERE ucl.fg_active = '1'",
        "<if test='query.dateFrom != null and query.dateFrom != \"\"'>",
        "  AND ucl.consultation_time &gt;= TO_DATE(#{query.dateFrom}, 'yyyy-MM-dd')",
        "</if>",
        "<if test='query.dateTo != null and query.dateTo != \"\"'>",
        "  AND ucl.consultation_time &lt; TO_DATE(#{query.dateTo}, 'yyyy-MM-dd') + 1",
        "</if>",
        "<if test='query.idOrg != null and query.idOrg != \"\"'>",
        "  AND o.id_org = #{query.idOrg}",
        "</if>",
        "<if test='query.idRegion != null and query.idRegion != \"\"'>",
        "  AND o.id_region = #{query.idRegion}",
        "</if>",
        "GROUP BY TRUNC(ucl.consultation_time)",
        "ORDER BY TRUNC(ucl.consultation_time)",
        "</script>"
    })
    List<Map<String, Object>> queryConsultationTrend(@Param("query") AnalyticsQueryDTO query);

    @Select({
        "<script>",
        "SELECT o.na_org AS name, NVL(l.cnt, 0) AS value",
        "FROM c_ai_org o",
        "JOIN c_ai_region r ON r.id_region = o.id_region AND r.fg_active = '1' AND r.sd_status = '1'",
        "LEFT JOIN (",
        "  SELECT e.id_org, COUNT(1) AS cnt FROM c_ai_feature_event e",
        "  JOIN c_ai_org o2 ON o2.id_org = e.id_org AND o2.fg_active = '1' AND o2.sd_status = '1'",
        "  JOIN c_ai_region r2 ON r2.id_region = o2.id_region AND r2.fg_active = '1' AND r2.sd_status = '1'",
        "  WHERE e.fg_active = '1' AND LOWER(e.event_status) = 'success'",
        "  <if test='query.dateFrom != null and query.dateFrom != \"\"'>",
        "    AND e.event_time &gt;= TO_DATE(#{query.dateFrom}, 'yyyy-MM-dd')",
        "  </if>",
        "  <if test='query.dateTo != null and query.dateTo != \"\"'>",
        "    AND e.event_time &lt; TO_DATE(#{query.dateTo}, 'yyyy-MM-dd') + 1",
        "  </if>",
        "  <if test='query.idRegion != null and query.idRegion != \"\"'>",
        "    AND o2.id_region = #{query.idRegion}",
        "  </if>",
        "  <if test='query.idOrg != null and query.idOrg != \"\"'>",
        "    AND o2.id_org = #{query.idOrg}",
        "  </if>",
        "  GROUP BY e.id_org",
        ") l ON o.id_org = l.id_org",
        "WHERE o.fg_active = '1' AND o.sd_status = '1'",
        "<if test='query.idRegion != null and query.idRegion != \"\"'>",
        "  AND o.id_region = #{query.idRegion}",
        "</if>",
        "<if test='query.idOrg != null and query.idOrg != \"\"'>",
        "  AND o.id_org = #{query.idOrg}",
        "</if>",
        "ORDER BY value DESC",
        "</script>"
    })
    List<DistributionItemVO> queryOrgDistribution(@Param("query") AnalyticsQueryDTO query);

    @Select({
        "<script>",
        "SELECT r.na_region AS name, SUM(NVL(l.cnt, 0)) AS value",
        "FROM c_ai_region r",
        "LEFT JOIN c_ai_org o ON o.id_region = r.id_region AND o.fg_active = '1' AND o.sd_status = '1'",
        "LEFT JOIN (",
        "  SELECT e.id_org, COUNT(1) AS cnt FROM c_ai_feature_event e",
        "  JOIN c_ai_org o2 ON o2.id_org = e.id_org AND o2.fg_active = '1' AND o2.sd_status = '1'",
        "  JOIN c_ai_region r2 ON r2.id_region = o2.id_region AND r2.fg_active = '1' AND r2.sd_status = '1'",
        "  WHERE e.fg_active = '1' AND LOWER(e.event_status) = 'success'",
        "  <if test='query.dateFrom != null and query.dateFrom != \"\"'>",
        "    AND e.event_time &gt;= TO_DATE(#{query.dateFrom}, 'yyyy-MM-dd')",
        "  </if>",
        "  <if test='query.dateTo != null and query.dateTo != \"\"'>",
        "    AND e.event_time &lt; TO_DATE(#{query.dateTo}, 'yyyy-MM-dd') + 1",
        "  </if>",
        "  <if test='query.idRegion != null and query.idRegion != \"\"'>",
        "    AND o2.id_region = #{query.idRegion}",
        "  </if>",
        "  <if test='query.idOrg != null and query.idOrg != \"\"'>",
        "    AND o2.id_org = #{query.idOrg}",
        "  </if>",
        "  GROUP BY e.id_org",
        ") l ON o.id_org = l.id_org",
        "WHERE r.fg_active = '1' AND r.sd_status = '1'",
        "<if test='query.idRegion != null and query.idRegion != \"\"'>",
        "  AND r.id_region = #{query.idRegion}",
        "</if>",
        "<if test='query.idOrg != null and query.idOrg != \"\"'>",
        "  AND o.id_org = #{query.idOrg}",
        "</if>",
        "GROUP BY r.id_region, r.na_region",
        "ORDER BY value DESC",
        "</script>"
    })
    List<DistributionItemVO> queryRegionDistributionRaw(@Param("query") AnalyticsQueryDTO query);

    @Select({
        "SELECT '语音问诊' AS module_name FROM dual UNION ALL",
        "SELECT '智能问诊' FROM dual UNION ALL",
        "SELECT '报告单解读' FROM dual UNION ALL",
        "SELECT '聊天' FROM dual UNION ALL",
        "SELECT 'AI诊断鉴别' FROM dual UNION ALL",
        "SELECT 'AI推荐诊断' FROM dual UNION ALL",
        "SELECT 'AI推荐用药' FROM dual UNION ALL",
        "SELECT 'AI推荐检查' FROM dual UNION ALL",
        "SELECT 'AI推荐检验' FROM dual UNION ALL",
        "SELECT 'AI推荐处置' FROM dual UNION ALL",
        "SELECT 'AI推荐治疗方案' FROM dual UNION ALL",
        "SELECT '知识库使用' FROM dual"
    })
    List<String> queryDistinctModules();

    @Select({
        "<script>",
        "SELECT " + FUNCTION_USAGE_MODULE_EXPR + " AS moduleName, COUNT(1) AS callCount, COUNT(DISTINCT NVL(TRIM(e.id_doctor), e.id_device)) AS doctorCount,",
        "  NVL(ROUND(COUNT(1) / NULLIF(COUNT(DISTINCT NVL(TRIM(e.id_doctor), e.id_device)), 0)), 0) AS avgPerDoctor",
        "FROM c_ai_feature_event e",
        FEATURE_SCOPE_JOIN,
        FEATURE_REGION_JOIN,
        "WHERE e.fg_active = '1' AND LOWER(e.event_status) = 'success'",
        "<if test='query.dateFrom != null and query.dateFrom != \"\"'>",
        "  AND e.event_time &gt;= TO_DATE(#{query.dateFrom}, 'yyyy-MM-dd')",
        "</if>",
        "<if test='query.dateTo != null and query.dateTo != \"\"'>",
        "  AND e.event_time &lt; TO_DATE(#{query.dateTo}, 'yyyy-MM-dd') + 1",
        "</if>",
        "<if test='query.idOrg != null and query.idOrg != \"\"'>",
        "  AND o.id_org = #{query.idOrg}",
        "</if>",
        "<if test='query.idRegion != null and query.idRegion != \"\"'>",
        "  AND o.id_region = #{query.idRegion}",
        "</if>",
        "<if test='query.functionModules != null and query.functionModules.size() > 0'>",
        "  AND " + FUNCTION_USAGE_MODULE_EXPR + " IN",
        "  <foreach collection='query.functionModules' item='m' open='(' separator=',' close=')'>#{m}</foreach>",
        "</if>",
        "GROUP BY " + FUNCTION_USAGE_MODULE_EXPR,
        "ORDER BY callCount DESC",
        "</script>"
    })
    List<FunctionUsageItemVO> queryFunctionUsageRanking(@Param("query") FunctionUsageQueryDTO query);

    @Select({
        "<script>",
        "SELECT " + FUNCTION_USAGE_MODULE_EXPR + " AS moduleName, COUNT(1) AS callCount, COUNT(DISTINCT NVL(TRIM(e.id_doctor), e.id_device)) AS doctorCount,",
        "  NVL(ROUND(COUNT(1) / NULLIF(COUNT(DISTINCT NVL(TRIM(e.id_doctor), e.id_device)), 0)), 0) AS avgPerDoctor",
        "FROM c_ai_feature_event e",
        FEATURE_SCOPE_JOIN,
        FEATURE_REGION_JOIN,
        "WHERE e.fg_active = '1' AND LOWER(e.event_status) = 'success'",
        "<if test='query.dateFrom != null and query.dateFrom != \"\"'>",
        "  AND e.event_time &gt;= TO_DATE(#{query.dateFrom}, 'yyyy-MM-dd')",
        "</if>",
        "<if test='query.dateTo != null and query.dateTo != \"\"'>",
        "  AND e.event_time &lt; TO_DATE(#{query.dateTo}, 'yyyy-MM-dd') + 1",
        "</if>",
        "<if test='query.idOrg != null and query.idOrg != \"\"'>",
        "  AND o.id_org = #{query.idOrg}",
        "</if>",
        "<if test='query.idRegion != null and query.idRegion != \"\"'>",
        "  AND o.id_region = #{query.idRegion}",
        "</if>",
        "<if test='query.functionModules != null and query.functionModules.size() > 0'>",
        "  AND " + FUNCTION_USAGE_MODULE_EXPR + " IN",
        "  <foreach collection='query.functionModules' item='m' open='(' separator=',' close=')'>#{m}</foreach>",
        "</if>",
        "GROUP BY " + FUNCTION_USAGE_MODULE_EXPR,
        "ORDER BY callCount DESC",
        "</script>"
    })
    List<FunctionUsageItemVO> queryFunctionUsagePreviousRanking(@Param("query") FunctionUsageQueryDTO query);

    @Select({
        "<script>",
        "SELECT TO_CHAR(TRUNC(e.event_time), 'yyyy-MM-dd') AS dayStr, " + FUNCTION_USAGE_MODULE_EXPR + " AS moduleName, COUNT(1) AS cnt",
        "FROM c_ai_feature_event e",
        FEATURE_SCOPE_JOIN,
        FEATURE_REGION_JOIN,
        "WHERE e.fg_active = '1' AND LOWER(e.event_status) = 'success'",
        "<if test='query.dateFrom != null and query.dateFrom != \"\"'>",
        "  AND e.event_time &gt;= TO_DATE(#{query.dateFrom}, 'yyyy-MM-dd')",
        "</if>",
        "<if test='query.dateTo != null and query.dateTo != \"\"'>",
        "  AND e.event_time &lt; TO_DATE(#{query.dateTo}, 'yyyy-MM-dd') + 1",
        "</if>",
        "<if test='query.idOrg != null and query.idOrg != \"\"'>",
        "  AND o.id_org = #{query.idOrg}",
        "</if>",
        "<if test='query.idRegion != null and query.idRegion != \"\"'>",
        "  AND o.id_region = #{query.idRegion}",
        "</if>",
        "<if test='query.functionModules != null and query.functionModules.size() > 0'>",
        "  AND " + FUNCTION_USAGE_MODULE_EXPR + " IN",
        "  <foreach collection='query.functionModules' item='m' open='(' separator=',' close=')'>#{m}</foreach>",
        "</if>",
        "GROUP BY TRUNC(e.event_time), " + FUNCTION_USAGE_MODULE_EXPR,
        "ORDER BY TRUNC(e.event_time), " + FUNCTION_USAGE_MODULE_EXPR,
        "</script>"
    })
    List<Map<String, Object>> queryFunctionUsageTrend(@Param("query") FunctionUsageQueryDTO query);
}
