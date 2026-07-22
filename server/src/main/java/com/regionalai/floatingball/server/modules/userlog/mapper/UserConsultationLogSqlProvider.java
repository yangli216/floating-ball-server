package com.regionalai.floatingball.server.modules.userlog.mapper;

public class UserConsultationLogSqlProvider {

    public String selectLatestUserNames() {
        return "<script>"
            + "SELECT idDevice, naUser FROM ("
            + "SELECT ucl.id_device AS idDevice, ucl.na_doctor AS naUser, "
            + "ROW_NUMBER() OVER (PARTITION BY ucl.id_device "
            + "ORDER BY ucl.consultation_time DESC, ucl.id_log DESC) AS rowNumber "
            + "FROM c_ai_user_consultation_log ucl "
            + "WHERE ucl.fg_active = '1' AND ucl.na_doctor IS NOT NULL "
            + "AND LENGTH(TRIM(ucl.na_doctor)) &gt; 0 "
            + "<choose>"
            + "<when test='deviceIds != null and deviceIds.size() > 0'>"
            + "AND ucl.id_device IN "
            + "<foreach collection='deviceIds' item='deviceId' open='(' separator=',' close=')'>"
            + "#{deviceId}"
            + "</foreach>"
            + "</when>"
            + "<otherwise>AND 1 = 0</otherwise>"
            + "</choose>"
            + ") latest WHERE rowNumber = 1"
            + "</script>";
    }
}
