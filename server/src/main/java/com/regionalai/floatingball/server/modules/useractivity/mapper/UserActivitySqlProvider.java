package com.regionalai.floatingball.server.modules.useractivity.mapper;

import com.regionalai.floatingball.server.common.db.DatabaseDialect;
import com.regionalai.floatingball.server.common.db.DatabaseDialectHolder;

public class UserActivitySqlProvider {

    private static final String DEVICE_SCOPE_JOIN = "JOIN c_ai_org o ON o.id_org = d.id_org AND o.fg_active = '1' AND o.sd_status = '1'";
    private static final String DEVICE_REGION_JOIN = "JOIN c_ai_region r ON r.id_region = o.id_region AND r.fg_active = '1' AND r.sd_status = '1'";

    public String countActiveUsers() {
        return "<script>"
            + "SELECT COUNT(DISTINCT d.id_device) AS cnt "
            + "FROM c_ai_device d "
            + DEVICE_SCOPE_JOIN + " "
            + DEVICE_REGION_JOIN + " "
            + "WHERE d.fg_active = '1' "
            + activeConsultationExists("")
            + scopeFilters()
            + "</script>";
    }

    public String countTotalDevices() {
        return "<script>"
            + "SELECT COUNT(1) AS cnt "
            + "FROM c_ai_device d "
            + DEVICE_SCOPE_JOIN + " "
            + DEVICE_REGION_JOIN + " "
            + "WHERE d.fg_active = '1' "
            + scopeFilters()
            + "</script>";
    }

    public String countEffectiveConsultations() {
        return consultationCount("AND ucl.status = 'completed'");
    }

    public String countConsultations() {
        return consultationCount("");
    }

    public String queryAllRegions() {
        return "SELECT id_region AS id, na_region AS name, sd_region_type AS type, id_parent AS parentId "
            + "FROM c_ai_region WHERE fg_active = '1' AND sd_status = '1' "
            + "ORDER BY sort_order, na_region";
    }

    public String countActiveUsersByRegion() {
        return "<script>"
            + "SELECT o.id_region, COUNT(DISTINCT d.id_device) AS cnt "
            + "FROM c_ai_device d "
            + DEVICE_SCOPE_JOIN + " "
            + DEVICE_REGION_JOIN + " "
            + "WHERE d.fg_active = '1' "
            + activeConsultationExists("")
            + scopeFilters()
            + " GROUP BY o.id_region"
            + "</script>";
    }

    public String queryUserActivityList() {
        DatabaseDialect dialect = DatabaseDialectHolder.get();
        String doctorSubquery;
        if (dialect.isPgCompatible()) {
            doctorSubquery = "(SELECT ucl5.na_doctor FROM c_ai_user_consultation_log ucl5 "
                + "WHERE ucl5.fg_active = '1' AND ucl5.id_device = d.id_device "
                + "ORDER BY ucl5.consultation_time DESC LIMIT 1) AS naDoctor,";
        } else {
            doctorSubquery = "(SELECT na_doctor FROM ("
                + "SELECT ucl5.na_doctor FROM c_ai_user_consultation_log ucl5 "
                + "WHERE ucl5.fg_active = '1' AND ucl5.id_device = d.id_device "
                + "ORDER BY ucl5.consultation_time DESC"
                + ") WHERE ROWNUM = 1) AS naDoctor,";
        }
        return "<script>"
            + "SELECT d.id_device AS idDevice, d.cd_device AS cdDevice, d.na_device AS naDevice, "
            + "o.id_org AS idOrg, o.na_org AS naOrg, o.id_region AS idRegion, r.na_region AS naRegion, "
            + doctorSubquery
            + "(SELECT MAX(ucl.consultation_time) FROM c_ai_user_consultation_log ucl "
            + " WHERE ucl.fg_active = '1' AND ucl.id_device = d.id_device "
            + consultationTimeFilters("ucl")
            + ") AS lastActiveTime, "
            + "(SELECT COUNT(1) FROM c_ai_user_consultation_log ucl2 "
            + " WHERE ucl2.fg_active = '1' AND ucl2.id_device = d.id_device "
            + consultationTimeFilters("ucl2")
            + ") AS consultationCount, "
            + "(SELECT COUNT(1) FROM c_ai_user_consultation_log ucl6 "
            + " WHERE ucl6.fg_active = '1' AND ucl6.id_device = d.id_device AND ucl6.status = 'completed' "
            + consultationTimeFilters("ucl6")
            + ") AS effectiveConsultationCount "
            + "FROM c_ai_device d "
            + DEVICE_SCOPE_JOIN + " "
            + DEVICE_REGION_JOIN + " "
            + "WHERE d.fg_active = '1' "
            + scopeFilters()
            + "<if test='query.activeStatus == \"active\"'>"
            + activeConsultationExists("ucl3")
            + "</if>"
            + "<if test='query.activeStatus == \"inactive\"'>"
            + " AND NOT EXISTS (SELECT 1 FROM c_ai_user_consultation_log ucl4 "
            + "WHERE ucl4.fg_active = '1' AND ucl4.id_device = d.id_device "
            + consultationTimeFilters("ucl4")
            + ")"
            + "</if>"
            + " ORDER BY lastActiveTime DESC NULLS LAST"
            + "</script>";
    }

    private String consultationCount(String extraCondition) {
        return "<script>"
            + "SELECT COUNT(1) AS cnt "
            + "FROM c_ai_user_consultation_log ucl "
            + "JOIN c_ai_device d ON d.id_device = ucl.id_device AND d.fg_active = '1' "
            + DEVICE_SCOPE_JOIN + " "
            + DEVICE_REGION_JOIN + " "
            + "WHERE ucl.fg_active = '1' "
            + extraCondition + " "
            + consultationTimeFilters("ucl")
            + scopeFilters()
            + "</script>";
    }

    private String activeConsultationExists(String alias) {
        String actualAlias = alias == null || alias.isEmpty() ? "ucl" : alias;
        return " AND EXISTS (SELECT 1 FROM c_ai_user_consultation_log " + actualAlias
            + " WHERE " + actualAlias + ".fg_active = '1' AND " + actualAlias + ".id_device = d.id_device "
            + consultationTimeFilters(actualAlias)
            + ")";
    }

    private String consultationTimeFilters(String alias) {
        return "<if test='query.dateFromTime != null'> AND " + alias + ".consultation_time &gt;= #{query.dateFromTime}</if>"
            + "<if test='query.dateToExclusiveTime != null'> AND " + alias + ".consultation_time &lt; #{query.dateToExclusiveTime}</if>";
    }

    private String scopeFilters() {
        return "<if test='query.idRegion != null and query.idRegion != \"\"'> AND o.id_region = #{query.idRegion}</if>"
            + "<if test='query.idOrg != null and query.idOrg != \"\"'> AND o.id_org = #{query.idOrg}</if>";
    }
}
