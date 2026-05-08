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

    @Select({
        "<script>",
        "SELECT COUNT(1) AS cnt FROM c_ai_op_log WHERE fg_active = '1' AND sd_log_type = 'ai_proxy'",
        "<if test='query.dateFrom != null and query.dateFrom != \"\"'>",
        "  AND operation_time &gt;= TO_DATE(#{query.dateFrom}, 'yyyy-MM-dd')",
        "</if>",
        "<if test='query.dateTo != null and query.dateTo != \"\"'>",
        "  AND operation_time &lt; TO_DATE(#{query.dateTo}, 'yyyy-MM-dd') + 1",
        "</if>",
        "<if test='query.idOrg != null and query.idOrg != \"\"'>",
        "  AND id_org = #{query.idOrg}",
        "</if>",
        "</script>"
    })
    long countAiService(@Param("query") AnalyticsQueryDTO query);

    @Select({
        "<script>",
        "SELECT COUNT(1) AS cnt FROM c_ai_user_consultation_log WHERE fg_active = '1'",
        "<if test='query.dateFrom != null and query.dateFrom != \"\"'>",
        "  AND consultation_time &gt;= TO_DATE(#{query.dateFrom}, 'yyyy-MM-dd')",
        "</if>",
        "<if test='query.dateTo != null and query.dateTo != \"\"'>",
        "  AND consultation_time &lt; TO_DATE(#{query.dateTo}, 'yyyy-MM-dd') + 1",
        "</if>",
        "<if test='query.idOrg != null and query.idOrg != \"\"'>",
        "  AND id_org = #{query.idOrg}",
        "</if>",
        "</script>"
    })
    long countConsultation(@Param("query") AnalyticsQueryDTO query);

    @Select({
        "<script>",
        "SELECT COUNT(DISTINCT id_doctor) AS cnt FROM c_ai_user_consultation_log WHERE fg_active = '1'",
        "<if test='query.dateFrom != null and query.dateFrom != \"\"'>",
        "  AND consultation_time &gt;= TO_DATE(#{query.dateFrom}, 'yyyy-MM-dd')",
        "</if>",
        "<if test='query.dateTo != null and query.dateTo != \"\"'>",
        "  AND consultation_time &lt; TO_DATE(#{query.dateTo}, 'yyyy-MM-dd') + 1",
        "</if>",
        "<if test='query.idOrg != null and query.idOrg != \"\"'>",
        "  AND id_org = #{query.idOrg}",
        "</if>",
        "</script>"
    })
    long countActiveDoctors(@Param("query") AnalyticsQueryDTO query);

    @Select({
        "<script>",
        "SELECT COUNT(1) AS cnt FROM c_ai_user_consultation_log",
        "WHERE fg_active = '1' AND status = 'completed'",
        "<if test='query.dateFrom != null and query.dateFrom != \"\"'>",
        "  AND consultation_time &gt;= TO_DATE(#{query.dateFrom}, 'yyyy-MM-dd')",
        "</if>",
        "<if test='query.dateTo != null and query.dateTo != \"\"'>",
        "  AND consultation_time &lt; TO_DATE(#{query.dateTo}, 'yyyy-MM-dd') + 1",
        "</if>",
        "<if test='query.idOrg != null and query.idOrg != \"\"'>",
        "  AND id_org = #{query.idOrg}",
        "</if>",
        "</script>"
    })
    long countAdoptedConsultations(@Param("query") AnalyticsQueryDTO query);

    @Select({
        "<script>",
        "SELECT COUNT(1) AS cnt FROM c_ai_user_consultation_log",
        "WHERE fg_active = '1' AND status IN ('completed','abandoned')",
        "<if test='query.dateFrom != null and query.dateFrom != \"\"'>",
        "  AND consultation_time &gt;= TO_DATE(#{query.dateFrom}, 'yyyy-MM-dd')",
        "</if>",
        "<if test='query.dateTo != null and query.dateTo != \"\"'>",
        "  AND consultation_time &lt; TO_DATE(#{query.dateTo}, 'yyyy-MM-dd') + 1",
        "</if>",
        "<if test='query.idOrg != null and query.idOrg != \"\"'>",
        "  AND id_org = #{query.idOrg}",
        "</if>",
        "</script>"
    })
    long countFinalizedConsultations(@Param("query") AnalyticsQueryDTO query);

    @Select({
        "<script>",
        "SELECT COUNT(1) AS cnt FROM c_ai_user_consultation_log",
        "WHERE fg_active = '1' AND status IN ('completed','abandoned')",
        "  AND JSON_VALUE(change_summary_json, '$.diagnosisChanges' RETURNING NUMBER) = 0",
        "<if test='query.dateFrom != null and query.dateFrom != \"\"'>",
        "  AND consultation_time &gt;= TO_DATE(#{query.dateFrom}, 'yyyy-MM-dd')",
        "</if>",
        "<if test='query.dateTo != null and query.dateTo != \"\"'>",
        "  AND consultation_time &lt; TO_DATE(#{query.dateTo}, 'yyyy-MM-dd') + 1",
        "</if>",
        "<if test='query.idOrg != null and query.idOrg != \"\"'>",
        "  AND id_org = #{query.idOrg}",
        "</if>",
        "</script>"
    })
    long countDiagnosisMatchedConsultations(@Param("query") AnalyticsQueryDTO query);

    @Select({
        "<script>",
        "SELECT TO_CHAR(TRUNC(operation_time), 'yyyy-MM-dd') AS day_str, COUNT(1) AS cnt",
        "FROM c_ai_op_log WHERE fg_active = '1' AND sd_log_type = 'ai_proxy'",
        "<if test='query.dateFrom != null and query.dateFrom != \"\"'>",
        "  AND operation_time &gt;= TO_DATE(#{query.dateFrom}, 'yyyy-MM-dd')",
        "</if>",
        "<if test='query.dateTo != null and query.dateTo != \"\"'>",
        "  AND operation_time &lt; TO_DATE(#{query.dateTo}, 'yyyy-MM-dd') + 1",
        "</if>",
        "<if test='query.idOrg != null and query.idOrg != \"\"'>",
        "  AND id_org = #{query.idOrg}",
        "</if>",
        "GROUP BY TRUNC(operation_time)",
        "ORDER BY TRUNC(operation_time)",
        "</script>"
    })
    List<Map<String, Object>> queryAiServiceTrend(@Param("query") AnalyticsQueryDTO query);

    @Select({
        "<script>",
        "SELECT TO_CHAR(TRUNC(consultation_time), 'yyyy-MM-dd') AS day_str, COUNT(1) AS cnt",
        "FROM c_ai_user_consultation_log WHERE fg_active = '1'",
        "<if test='query.dateFrom != null and query.dateFrom != \"\"'>",
        "  AND consultation_time &gt;= TO_DATE(#{query.dateFrom}, 'yyyy-MM-dd')",
        "</if>",
        "<if test='query.dateTo != null and query.dateTo != \"\"'>",
        "  AND consultation_time &lt; TO_DATE(#{query.dateTo}, 'yyyy-MM-dd') + 1",
        "</if>",
        "<if test='query.idOrg != null and query.idOrg != \"\"'>",
        "  AND id_org = #{query.idOrg}",
        "</if>",
        "GROUP BY TRUNC(consultation_time)",
        "ORDER BY TRUNC(consultation_time)",
        "</script>"
    })
    List<Map<String, Object>> queryConsultationTrend(@Param("query") AnalyticsQueryDTO query);

    @Select({
        "<script>",
        "SELECT o.na_org AS name, NVL(l.cnt, 0) AS value",
        "FROM c_ai_org o",
        "LEFT JOIN (",
        "  SELECT id_org, COUNT(1) AS cnt FROM c_ai_op_log WHERE fg_active = '1' AND sd_log_type = 'ai_proxy'",
        "  <if test='query.dateFrom != null and query.dateFrom != \"\"'>",
        "    AND operation_time &gt;= TO_DATE(#{query.dateFrom}, 'yyyy-MM-dd')",
        "  </if>",
        "  <if test='query.dateTo != null and query.dateTo != \"\"'>",
        "    AND operation_time &lt; TO_DATE(#{query.dateTo}, 'yyyy-MM-dd') + 1",
        "  </if>",
        "  <if test='query.idRegion != null and query.idRegion != \"\"'>",
        "    AND id_org IN (SELECT id_org FROM c_ai_org WHERE id_region = #{query.idRegion} AND fg_active = '1')",
        "  </if>",
        "  GROUP BY id_org",
        ") l ON o.id_org = l.id_org",
        "WHERE o.fg_active = '1'",
        "ORDER BY value DESC",
        "</script>"
    })
    List<DistributionItemVO> queryOrgDistribution(@Param("query") AnalyticsQueryDTO query);

    @Select({
        "<script>",
        "SELECT r.na_region AS name, SUM(NVL(l.cnt, 0)) AS value",
        "FROM c_ai_region r",
        "LEFT JOIN c_ai_org o ON o.id_region = r.id_region AND o.fg_active = '1'",
        "LEFT JOIN (",
        "  SELECT id_org, COUNT(1) AS cnt FROM c_ai_op_log WHERE fg_active = '1' AND sd_log_type = 'ai_proxy'",
        "  <if test='query.dateFrom != null and query.dateFrom != \"\"'>",
        "    AND operation_time &gt;= TO_DATE(#{query.dateFrom}, 'yyyy-MM-dd')",
        "  </if>",
        "  <if test='query.dateTo != null and query.dateTo != \"\"'>",
        "    AND operation_time &lt; TO_DATE(#{query.dateTo}, 'yyyy-MM-dd') + 1",
        "  </if>",
        "  GROUP BY id_org",
        ") l ON o.id_org = l.id_org",
        "WHERE r.fg_active = '1'",
        "GROUP BY r.na_region",
        "ORDER BY value DESC",
        "</script>"
    })
    List<DistributionItemVO> queryRegionDistributionRaw(@Param("query") AnalyticsQueryDTO query);

    @Select({
        "SELECT DISTINCT source_module AS module_name FROM c_ai_op_log",
        "WHERE fg_active = '1' AND source_module IS NOT NULL",
        "ORDER BY source_module"
    })
    List<String> queryDistinctModules();

    @Select({
        "<script>",
        "SELECT source_module AS moduleName,",
        "  COUNT(1) AS callCount,",
        "  COUNT(DISTINCT id_device) AS doctorCount,",
        "  ROUND(COUNT(1) / NULLIF(COUNT(DISTINCT id_device), 0)) AS avgPerDoctor",
        "FROM c_ai_op_log WHERE fg_active = '1' AND source_module IS NOT NULL",
        "<if test='query.dateFrom != null and query.dateFrom != \"\"'>",
        "  AND operation_time &gt;= TO_DATE(#{query.dateFrom}, 'yyyy-MM-dd')",
        "</if>",
        "<if test='query.dateTo != null and query.dateTo != \"\"'>",
        "  AND operation_time &lt; TO_DATE(#{query.dateTo}, 'yyyy-MM-dd') + 1",
        "</if>",
        "<if test='query.idOrg != null and query.idOrg != \"\"'>",
        "  AND id_org = #{query.idOrg}",
        "</if>",
        "<if test='query.functionModules != null and query.functionModules.size() > 0'>",
        "  AND source_module IN",
        "  <foreach collection='query.functionModules' item='m' open='(' separator=',' close=')'>#{m}</foreach>",
        "</if>",
        "GROUP BY source_module",
        "ORDER BY callCount DESC",
        "</script>"
    })
    List<FunctionUsageItemVO> queryFunctionUsageRanking(@Param("query") FunctionUsageQueryDTO query);

    @Select({
        "<script>",
        "SELECT source_module AS moduleName,",
        "  COUNT(1) AS callCount,",
        "  COUNT(DISTINCT id_device) AS doctorCount,",
        "  ROUND(COUNT(1) / NULLIF(COUNT(DISTINCT id_device), 0)) AS avgPerDoctor",
        "FROM c_ai_op_log WHERE fg_active = '1' AND source_module IS NOT NULL",
        "<if test='query.dateFrom != null and query.dateFrom != \"\"'>",
        "  AND operation_time &gt;= TO_DATE(#{query.dateFrom}, 'yyyy-MM-dd')",
        "</if>",
        "<if test='query.dateTo != null and query.dateTo != \"\"'>",
        "  AND operation_time &lt; TO_DATE(#{query.dateTo}, 'yyyy-MM-dd') + 1",
        "</if>",
        "<if test='query.idOrg != null and query.idOrg != \"\"'>",
        "  AND id_org = #{query.idOrg}",
        "</if>",
        "<if test='query.functionModules != null and query.functionModules.size() > 0'>",
        "  AND source_module IN",
        "  <foreach collection='query.functionModules' item='m' open='(' separator=',' close=')'>#{m}</foreach>",
        "</if>",
        "GROUP BY source_module",
        "ORDER BY callCount DESC",
        "</script>"
    })
    List<FunctionUsageItemVO> queryFunctionUsagePreviousRanking(@Param("query") FunctionUsageQueryDTO query);

    @Select({
        "<script>",
        "SELECT TO_CHAR(TRUNC(operation_time), 'yyyy-MM-dd') AS dayStr,",
        "  source_module AS moduleName, COUNT(1) AS cnt",
        "FROM c_ai_op_log WHERE fg_active = '1' AND source_module IS NOT NULL",
        "<if test='query.dateFrom != null and query.dateFrom != \"\"'>",
        "  AND operation_time &gt;= TO_DATE(#{query.dateFrom}, 'yyyy-MM-dd')",
        "</if>",
        "<if test='query.dateTo != null and query.dateTo != \"\"'>",
        "  AND operation_time &lt; TO_DATE(#{query.dateTo}, 'yyyy-MM-dd') + 1",
        "</if>",
        "<if test='query.idOrg != null and query.idOrg != \"\"'>",
        "  AND id_org = #{query.idOrg}",
        "</if>",
        "<if test='query.functionModules != null and query.functionModules.size() > 0'>",
        "  AND source_module IN",
        "  <foreach collection='query.functionModules' item='m' open='(' separator=',' close=')'>#{m}</foreach>",
        "</if>",
        "GROUP BY TRUNC(operation_time), source_module",
        "ORDER BY TRUNC(operation_time), source_module",
        "</script>"
    })
    List<Map<String, Object>> queryFunctionUsageTrend(@Param("query") FunctionUsageQueryDTO query);
}
