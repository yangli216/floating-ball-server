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

    String FUNCTION_FEATURE_CASE =
        "CASE "
            + "WHEN op_action IN ('generate_diagnosis_checklist','confirm_differential_checklist') "
            + "  OR op_title LIKE '%鉴别%' OR scene_code LIKE '%checklist%' THEN 'AI诊断鉴别' "
            + "WHEN op_action IN ('generate_diagnosis_recommendation','generate_tcm_diagnosis_recommendation') "
            + "  OR op_title LIKE '%诊断推荐%' OR scene_code LIKE '%diagnosis%' THEN 'AI推荐诊断' "
            + "WHEN op_action IN ('generate_treatment_recommendation','generate_tcm_treatment_recommendation') "
            + "  OR op_title LIKE '%用药推荐%' OR op_title LIKE '%治疗推荐%' OR scene_code LIKE '%medication%' THEN 'AI推荐用药' "
            + "WHEN op_action = 'generate_examination_recommendation' "
            + "  OR op_title LIKE '%检查推荐%' OR scene_code LIKE '%examination%' THEN 'AI推荐检查' "
            + "WHEN op_action = 'generate_lab_test_recommendation' "
            + "  OR op_title LIKE '%检验推荐%' OR scene_code LIKE '%lab-test%' OR scene_code LIKE '%lab_test%' THEN 'AI推荐检验' "
            + "WHEN op_action = 'generate_procedure_recommendation' "
            + "  OR op_title LIKE '%处置推荐%' OR scene_code LIKE '%procedure%' THEN 'AI推荐处置' "
            + "WHEN op_action IN ('build_report_interpretation','his_start_report_interpretation') "
            + "  OR source_module = 'report_interpretation' OR scene_code = 'report-interpretation' THEN '报告单解读' "
            + "WHEN op_action IN ('extract_voice_record','repair_voice_extraction','start_voice_consultation','start_voice_capture','open_voice_consultation','discard_voice_result','speech_transcribe','speech_realtime','transcribe','realtime') "
            + "  OR na_module IN ('voice_consultation','voice_capture','speech','speech_proxy','aliyunSpeech') "
            + "  OR source_module IN ('voice_consultation_ai','voice_intent','voice_consultation_result','voice_capsule','aliyunSpeech','voice_safety_reviewer') "
            + "  OR scene_code LIKE 'voice-%' OR scene_code IN ('voice-consultation','voice-interaction','chat-input') THEN '语音问诊' "
            + "WHEN op_action IN ('open_consultation','start_consultation','start_consultation_assist','generate_medical_record','submit_to_his','complete_consultation','generate_final_report','request_phis_reference','request_reference:diagnosis','request_reference:medicine','request_reference:medication','request_reference:examination','request_reference:lab_test','request_reference:procedure','reference_feedback:diagnosis','reference_feedback:medicine','reference_feedback:medication','reference_feedback:examination','reference_feedback:lab_test','reference_feedback:procedure') "
            + "  OR na_module = 'consultation' "
            + "  OR source_module IN ('consultation_page','consultation_record','consultation_reference','his_bridge') "
            + "  OR scene_code IN ('consultation','consultation-assist','consultation-reference','consultation-record') THEN '智能问诊' "
            + "WHEN op_action IN ('chat','chat_stream','stream_reply','send_message') "
            + "  OR na_module = 'chat' OR source_module IN ('chat_panel','llm') OR scene_code IN ('chat','chat-stream') THEN '聊天' "
            + "WHEN op_action LIKE 'knowledge_%' OR source_module IN ('knowledge_base','pmphai','knowledge_panel') OR scene_code = 'knowledge-base' THEN '知识库使用' "
            + "ELSE NULL END";

    String FUNCTION_USAGE_CONSULTATION_EVENT_SELECT =
        "  SELECT CASE LOWER(consultation_type) WHEN 'voice' THEN '语音问诊' WHEN 'smart' THEN '智能问诊' END AS feature_name,"
            + "    'consultation:' || consultation_id || ':' || LOWER(consultation_type) || ':' || NVL(id_device, '-') AS event_key,"
            + "    NVL(TRIM(id_doctor), id_device) AS doctor_key,"
            + "    consultation_time AS event_time,"
            + "    id_org"
            + "  FROM c_ai_user_consultation_log"
            + "  WHERE fg_active = '1' AND LOWER(consultation_type) IN ('voice','smart')";

    String FUNCTION_USAGE_OP_EVENT_SELECT =
        "  SELECT feature_name,"
            + "    'op:' || COALESCE(TRIM(trace_id), id_log) AS event_key,"
            + "    id_device AS doctor_key,"
            + "    operation_time AS event_time,"
            + "    id_org"
            + "  FROM ("
            + "    SELECT id_log, id_device, id_org, op_action, op_title, source_module, scene_code, na_module, trace_id, operation_time,"
            + "      " + FUNCTION_FEATURE_CASE + " AS feature_name"
            + "    FROM c_ai_op_log"
            + "    WHERE fg_active = '1'";

    String FUNCTION_USAGE_OP_EVENT_TAIL =
        "  )"
            + "  WHERE feature_name IS NOT NULL AND feature_name NOT IN ('语音问诊','智能问诊')";

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
        "SELECT DISTINCT feature_name AS module_name",
        "FROM (",
        "  SELECT CASE LOWER(consultation_type) WHEN 'voice' THEN '语音问诊' WHEN 'smart' THEN '智能问诊' END AS feature_name",
        "  FROM c_ai_user_consultation_log",
        "  WHERE fg_active = '1' AND LOWER(consultation_type) IN ('voice','smart')",
        "  UNION ALL",
        "  SELECT feature_name",
        "  FROM (",
        "    SELECT " + FUNCTION_FEATURE_CASE + " AS feature_name",
        "    FROM c_ai_op_log WHERE fg_active = '1'",
        "  )",
        "  WHERE feature_name IS NOT NULL AND feature_name NOT IN ('语音问诊','智能问诊')",
        ")",
        "WHERE feature_name IS NOT NULL",
        "ORDER BY feature_name"
    })
    List<String> queryDistinctModules();

    @Select({
        "<script>",
        "SELECT feature_name AS moduleName, COUNT(1) AS callCount, COUNT(DISTINCT doctor_key) AS doctorCount,",
        "  NVL(ROUND(COUNT(1) / NULLIF(COUNT(DISTINCT doctor_key), 0)), 0) AS avgPerDoctor",
        "FROM (",
        "  SELECT feature_name, event_key, MAX(doctor_key) AS doctor_key, MIN(event_time) AS event_time",
        "  FROM (",
        FUNCTION_USAGE_CONSULTATION_EVENT_SELECT,
        "  <if test='query.dateFrom != null and query.dateFrom != \"\"'>",
        "    AND consultation_time &gt;= TO_DATE(#{query.dateFrom}, 'yyyy-MM-dd')",
        "  </if>",
        "  <if test='query.dateTo != null and query.dateTo != \"\"'>",
        "    AND consultation_time &lt; TO_DATE(#{query.dateTo}, 'yyyy-MM-dd') + 1",
        "  </if>",
        "  <if test='query.idOrg != null and query.idOrg != \"\"'>",
        "    AND id_org = #{query.idOrg}",
        "  </if>",
        "  <if test='query.idRegion != null and query.idRegion != \"\"'>",
        "    AND id_org IN (SELECT id_org FROM c_ai_org WHERE id_region = #{query.idRegion} AND fg_active = '1')",
        "  </if>",
        "  UNION ALL",
        FUNCTION_USAGE_OP_EVENT_SELECT,
        "    <if test='query.dateFrom != null and query.dateFrom != \"\"'>",
        "      AND operation_time &gt;= TO_DATE(#{query.dateFrom}, 'yyyy-MM-dd')",
        "    </if>",
        "    <if test='query.dateTo != null and query.dateTo != \"\"'>",
        "      AND operation_time &lt; TO_DATE(#{query.dateTo}, 'yyyy-MM-dd') + 1",
        "    </if>",
        "    <if test='query.idOrg != null and query.idOrg != \"\"'>",
        "      AND id_org = #{query.idOrg}",
        "    </if>",
        "    <if test='query.idRegion != null and query.idRegion != \"\"'>",
        "      AND id_org IN (SELECT id_org FROM c_ai_org WHERE id_region = #{query.idRegion} AND fg_active = '1')",
        "    </if>",
        FUNCTION_USAGE_OP_EVENT_TAIL,
        "  )",
        "  WHERE feature_name IS NOT NULL",
        "<if test='query.functionModules != null and query.functionModules.size() > 0'>",
        "  AND feature_name IN",
        "  <foreach collection='query.functionModules' item='m' open='(' separator=',' close=')'>#{m}</foreach>",
        "</if>",
        "  GROUP BY feature_name, event_key",
        ")",
        "GROUP BY feature_name",
        "ORDER BY callCount DESC",
        "</script>"
    })
    List<FunctionUsageItemVO> queryFunctionUsageRanking(@Param("query") FunctionUsageQueryDTO query);

    @Select({
        "<script>",
        "SELECT feature_name AS moduleName, COUNT(1) AS callCount, COUNT(DISTINCT doctor_key) AS doctorCount,",
        "  NVL(ROUND(COUNT(1) / NULLIF(COUNT(DISTINCT doctor_key), 0)), 0) AS avgPerDoctor",
        "FROM (",
        "  SELECT feature_name, event_key, MAX(doctor_key) AS doctor_key, MIN(event_time) AS event_time",
        "  FROM (",
        FUNCTION_USAGE_CONSULTATION_EVENT_SELECT,
        "  <if test='query.dateFrom != null and query.dateFrom != \"\"'>",
        "    AND consultation_time &gt;= TO_DATE(#{query.dateFrom}, 'yyyy-MM-dd')",
        "  </if>",
        "  <if test='query.dateTo != null and query.dateTo != \"\"'>",
        "    AND consultation_time &lt; TO_DATE(#{query.dateTo}, 'yyyy-MM-dd') + 1",
        "  </if>",
        "  <if test='query.idOrg != null and query.idOrg != \"\"'>",
        "    AND id_org = #{query.idOrg}",
        "  </if>",
        "  <if test='query.idRegion != null and query.idRegion != \"\"'>",
        "    AND id_org IN (SELECT id_org FROM c_ai_org WHERE id_region = #{query.idRegion} AND fg_active = '1')",
        "  </if>",
        "  UNION ALL",
        FUNCTION_USAGE_OP_EVENT_SELECT,
        "    <if test='query.dateFrom != null and query.dateFrom != \"\"'>",
        "      AND operation_time &gt;= TO_DATE(#{query.dateFrom}, 'yyyy-MM-dd')",
        "    </if>",
        "    <if test='query.dateTo != null and query.dateTo != \"\"'>",
        "      AND operation_time &lt; TO_DATE(#{query.dateTo}, 'yyyy-MM-dd') + 1",
        "    </if>",
        "    <if test='query.idOrg != null and query.idOrg != \"\"'>",
        "      AND id_org = #{query.idOrg}",
        "    </if>",
        "    <if test='query.idRegion != null and query.idRegion != \"\"'>",
        "      AND id_org IN (SELECT id_org FROM c_ai_org WHERE id_region = #{query.idRegion} AND fg_active = '1')",
        "    </if>",
        FUNCTION_USAGE_OP_EVENT_TAIL,
        "  )",
        "  WHERE feature_name IS NOT NULL",
        "<if test='query.functionModules != null and query.functionModules.size() > 0'>",
        "  AND feature_name IN",
        "  <foreach collection='query.functionModules' item='m' open='(' separator=',' close=')'>#{m}</foreach>",
        "</if>",
        "  GROUP BY feature_name, event_key",
        ")",
        "GROUP BY feature_name",
        "ORDER BY callCount DESC",
        "</script>"
    })
    List<FunctionUsageItemVO> queryFunctionUsagePreviousRanking(@Param("query") FunctionUsageQueryDTO query);

    @Select({
        "<script>",
        "SELECT TO_CHAR(TRUNC(event_time), 'yyyy-MM-dd') AS dayStr, feature_name AS moduleName, COUNT(1) AS cnt",
        "FROM (",
        "  SELECT feature_name, event_key, MIN(event_time) AS event_time",
        "  FROM (",
        FUNCTION_USAGE_CONSULTATION_EVENT_SELECT,
        "  <if test='query.dateFrom != null and query.dateFrom != \"\"'>",
        "    AND consultation_time &gt;= TO_DATE(#{query.dateFrom}, 'yyyy-MM-dd')",
        "  </if>",
        "  <if test='query.dateTo != null and query.dateTo != \"\"'>",
        "    AND consultation_time &lt; TO_DATE(#{query.dateTo}, 'yyyy-MM-dd') + 1",
        "  </if>",
        "  <if test='query.idOrg != null and query.idOrg != \"\"'>",
        "    AND id_org = #{query.idOrg}",
        "  </if>",
        "  <if test='query.idRegion != null and query.idRegion != \"\"'>",
        "    AND id_org IN (SELECT id_org FROM c_ai_org WHERE id_region = #{query.idRegion} AND fg_active = '1')",
        "  </if>",
        "  UNION ALL",
        FUNCTION_USAGE_OP_EVENT_SELECT,
        "    <if test='query.dateFrom != null and query.dateFrom != \"\"'>",
        "      AND operation_time &gt;= TO_DATE(#{query.dateFrom}, 'yyyy-MM-dd')",
        "    </if>",
        "    <if test='query.dateTo != null and query.dateTo != \"\"'>",
        "      AND operation_time &lt; TO_DATE(#{query.dateTo}, 'yyyy-MM-dd') + 1",
        "    </if>",
        "    <if test='query.idOrg != null and query.idOrg != \"\"'>",
        "      AND id_org = #{query.idOrg}",
        "    </if>",
        "    <if test='query.idRegion != null and query.idRegion != \"\"'>",
        "      AND id_org IN (SELECT id_org FROM c_ai_org WHERE id_region = #{query.idRegion} AND fg_active = '1')",
        "    </if>",
        FUNCTION_USAGE_OP_EVENT_TAIL,
        "  )",
        "  WHERE feature_name IS NOT NULL",
        "<if test='query.functionModules != null and query.functionModules.size() > 0'>",
        "  AND feature_name IN",
        "  <foreach collection='query.functionModules' item='m' open='(' separator=',' close=')'>#{m}</foreach>",
        "</if>",
        "  GROUP BY feature_name, event_key",
        ")",
        "GROUP BY TRUNC(event_time), feature_name",
        "ORDER BY TRUNC(event_time), feature_name",
        "</script>"
    })
    List<Map<String, Object>> queryFunctionUsageTrend(@Param("query") FunctionUsageQueryDTO query);
}
