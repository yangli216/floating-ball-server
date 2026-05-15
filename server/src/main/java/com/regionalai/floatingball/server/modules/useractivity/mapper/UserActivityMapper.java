package com.regionalai.floatingball.server.modules.useractivity.mapper;

import com.regionalai.floatingball.server.modules.useractivity.dto.UserActivityQueryDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface UserActivityMapper {

    @Select({
        "<script>",
        "SELECT COUNT(DISTINCT d.id_device) AS cnt",
        "FROM c_ai_device d",
        "WHERE d.fg_active = '1'",
        "  AND EXISTS (",
        "    SELECT 1 FROM c_ai_user_consultation_log ucl",
        "    WHERE ucl.fg_active = '1' AND ucl.id_device = d.id_device",
        "    <if test='query.dateFrom != null and query.dateFrom != \"\"'>",
        "      AND ucl.consultation_time &gt;= TO_DATE(#{query.dateFrom}, 'yyyy-MM-dd')",
        "    </if>",
        "    <if test='query.dateTo != null and query.dateTo != \"\"'>",
        "      AND ucl.consultation_time &lt; TO_DATE(#{query.dateTo}, 'yyyy-MM-dd') + 1",
        "    </if>",
        "  )",
        "<if test='query.idRegion != null and query.idRegion != \"\"'>",
        "  AND d.id_region = #{query.idRegion}",
        "</if>",
        "</script>"
    })
    long countActiveUsers(@Param("query") UserActivityQueryDTO query);

    @Select({
        "<script>",
        "SELECT COUNT(1) AS cnt",
        "FROM c_ai_device d",
        "WHERE d.fg_active = '1'",
        "<if test='query.idRegion != null and query.idRegion != \"\"'>",
        "  AND d.id_region = #{query.idRegion}",
        "</if>",
        "</script>"
    })
    long countTotalDevices(@Param("query") UserActivityQueryDTO query);

    @Select({
        "<script>",
        "SELECT AVG(",
        "  (SELECT COUNT(1) FROM c_ai_user_consultation_log ucl",
        "   WHERE ucl.fg_active = '1' AND ucl.id_device = d.id_device",
        "   <if test='query.dateFrom != null and query.dateFrom != \"\"'>",
        "     AND ucl.consultation_time &gt;= TO_DATE(#{query.dateFrom}, 'yyyy-MM-dd')",
        "   </if>",
        "   <if test='query.dateTo != null and query.dateTo != \"\"'>",
        "     AND ucl.consultation_time &lt; TO_DATE(#{query.dateTo}, 'yyyy-MM-dd') + 1",
        "   </if>",
        "  ) * 15",
        ") AS avg_duration",
        "FROM c_ai_device d",
        "WHERE d.fg_active = '1'",
        "  AND EXISTS (",
        "    SELECT 1 FROM c_ai_user_consultation_log ucl2",
        "    WHERE ucl2.fg_active = '1' AND ucl2.id_device = d.id_device",
        "    <if test='query.dateFrom != null and query.dateFrom != \"\"'>",
        "      AND ucl2.consultation_time &gt;= TO_DATE(#{query.dateFrom}, 'yyyy-MM-dd')",
        "    </if>",
        "    <if test='query.dateTo != null and query.dateTo != \"\"'>",
        "      AND ucl2.consultation_time &lt; TO_DATE(#{query.dateTo}, 'yyyy-MM-dd') + 1",
        "    </if>",
        "  )",
        "<if test='query.idRegion != null and query.idRegion != \"\"'>",
        "  AND d.id_region = #{query.idRegion}",
        "</if>",
        "</script>"
    })
    Double queryAvgUsageDuration(@Param("query") UserActivityQueryDTO query);

    @Select({
        "SELECT id_region AS id, na_region AS name, sd_region_type AS type, id_parent AS parentId",
        "FROM c_ai_region WHERE fg_active = '1' AND sd_status = '1'",
        "ORDER BY sort_order, na_region"
    })
    List<Map<String, Object>> queryAllRegions();

    @Select({
        "<script>",
        "SELECT d.id_region, COUNT(DISTINCT d.id_device) AS cnt",
        "FROM c_ai_device d",
        "WHERE d.fg_active = '1'",
        "  AND EXISTS (",
        "    SELECT 1 FROM c_ai_user_consultation_log ucl",
        "    WHERE ucl.fg_active = '1' AND ucl.id_device = d.id_device",
        "    <if test='query.dateFrom != null and query.dateFrom != \"\"'>",
        "      AND ucl.consultation_time &gt;= TO_DATE(#{query.dateFrom}, 'yyyy-MM-dd')",
        "    </if>",
        "    <if test='query.dateTo != null and query.dateTo != \"\"'>",
        "      AND ucl.consultation_time &lt; TO_DATE(#{query.dateTo}, 'yyyy-MM-dd') + 1",
        "    </if>",
        "  )",
        "GROUP BY d.id_region",
        "</script>"
    })
    List<Map<String, Object>> countActiveUsersByRegion(@Param("query") UserActivityQueryDTO query);

    @Select({
        "<script>",
        "SELECT d.id_device AS idDevice, d.cd_device AS cdDevice, d.na_device AS naDevice,",
        "  d.id_org AS idOrg, o.na_org AS naOrg, d.id_region AS idRegion, r.na_region AS naRegion,",
        "  (SELECT na_doctor FROM (",
        "    SELECT ucl5.na_doctor FROM c_ai_user_consultation_log ucl5",
        "    WHERE ucl5.fg_active = '1' AND ucl5.id_device = d.id_device",
        "    ORDER BY ucl5.consultation_time DESC",
        "  ) WHERE ROWNUM = 1) AS naDoctor,",
        "  (SELECT MAX(ucl.consultation_time) FROM c_ai_user_consultation_log ucl",
        "   WHERE ucl.fg_active = '1' AND ucl.id_device = d.id_device",
        "   <if test='query.dateFrom != null and query.dateFrom != \"\"'>",
        "     AND ucl.consultation_time &gt;= TO_DATE(#{query.dateFrom}, 'yyyy-MM-dd')",
        "   </if>",
        "   <if test='query.dateTo != null and query.dateTo != \"\"'>",
        "     AND ucl.consultation_time &lt; TO_DATE(#{query.dateTo}, 'yyyy-MM-dd') + 1",
        "   </if>",
        "  ) AS lastActiveTime,",
        "  (SELECT COUNT(1) FROM c_ai_user_consultation_log ucl2",
        "   WHERE ucl2.fg_active = '1' AND ucl2.id_device = d.id_device",
        "   <if test='query.dateFrom != null and query.dateFrom != \"\"'>",
        "     AND ucl2.consultation_time &gt;= TO_DATE(#{query.dateFrom}, 'yyyy-MM-dd')",
        "   </if>",
        "   <if test='query.dateTo != null and query.dateTo != \"\"'>",
        "     AND ucl2.consultation_time &lt; TO_DATE(#{query.dateTo}, 'yyyy-MM-dd') + 1",
        "   </if>",
        "  ) AS consultationCount,",
        "  (SELECT COUNT(1) FROM c_ai_op_log opl",
        "   WHERE opl.fg_active = '1' AND opl.id_device = d.id_device",
        "   <if test='query.dateFrom != null and query.dateFrom != \"\"'>",
        "     AND opl.operation_time &gt;= TO_DATE(#{query.dateFrom}, 'yyyy-MM-dd')",
        "   </if>",
        "   <if test='query.dateTo != null and query.dateTo != \"\"'>",
        "     AND opl.operation_time &lt; TO_DATE(#{query.dateTo}, 'yyyy-MM-dd') + 1",
        "   </if>",
        "  ) AS operationCount",
        "FROM c_ai_device d",
        "LEFT JOIN c_ai_org o ON o.id_org = d.id_org AND o.fg_active = '1'",
        "LEFT JOIN c_ai_region r ON r.id_region = d.id_region AND r.fg_active = '1'",
        "WHERE d.fg_active = '1'",
        "<if test='query.idRegion != null and query.idRegion != \"\"'>",
        "  AND d.id_region = #{query.idRegion}",
        "</if>",
        "<if test='query.activeStatus == \"active\"'>",
        "  AND EXISTS (",
        "    SELECT 1 FROM c_ai_user_consultation_log ucl3",
        "    WHERE ucl3.fg_active = '1' AND ucl3.id_device = d.id_device",
        "    <if test='query.dateFrom != null and query.dateFrom != \"\"'>",
        "      AND ucl3.consultation_time &gt;= TO_DATE(#{query.dateFrom}, 'yyyy-MM-dd')",
        "    </if>",
        "    <if test='query.dateTo != null and query.dateTo != \"\"'>",
        "      AND ucl3.consultation_time &lt; TO_DATE(#{query.dateTo}, 'yyyy-MM-dd') + 1",
        "    </if>",
        "  )",
        "</if>",
        "<if test='query.activeStatus == \"inactive\"'>",
        "  AND NOT EXISTS (",
        "    SELECT 1 FROM c_ai_user_consultation_log ucl4",
        "    WHERE ucl4.fg_active = '1' AND ucl4.id_device = d.id_device",
        "    <if test='query.dateFrom != null and query.dateFrom != \"\"'>",
        "      AND ucl4.consultation_time &gt;= TO_DATE(#{query.dateFrom}, 'yyyy-MM-dd')",
        "    </if>",
        "    <if test='query.dateTo != null and query.dateTo != \"\"'>",
        "      AND ucl4.consultation_time &lt; TO_DATE(#{query.dateTo}, 'yyyy-MM-dd') + 1",
        "    </if>",
        "  )",
        "</if>",
        "ORDER BY lastActiveTime DESC NULLS LAST",
        "</script>"
    })
    List<Map<String, Object>> queryUserActivityList(@Param("query") UserActivityQueryDTO query);
}
