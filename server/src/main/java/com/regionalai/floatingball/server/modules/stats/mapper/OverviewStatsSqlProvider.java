package com.regionalai.floatingball.server.modules.stats.mapper;

import com.regionalai.floatingball.server.common.db.DatabaseDialectHolder;

public class OverviewStatsSqlProvider {

    public String selectOverviewStats() {
        String sql = "SELECT "
            + "(SELECT COUNT(1) FROM c_ai_region WHERE fg_active = '1') AS region_count, "
            + "(SELECT COUNT(1) FROM c_ai_org WHERE fg_active = '1') AS org_count, "
            + "(SELECT COUNT(1) FROM c_ai_device WHERE fg_active = '1') AS device_count, "
            + "(SELECT COUNT(1) FROM c_ai_config WHERE fg_active = '1') AS config_count, "
            + "(SELECT COUNT(1) FROM c_ai_symptom_template WHERE fg_active = '1') AS symptom_template_count, "
            + "(SELECT COUNT(1) FROM c_ai_op_log WHERE fg_active = '1') AS log_count, "
            + "(SELECT COUNT(1) FROM c_ai_user WHERE fg_active = '1') AS user_count, "
            + "(SELECT COUNT(1) FROM c_ai_role WHERE fg_active = '1') AS role_count";
        return DatabaseDialectHolder.get().isPgCompatible() ? sql : sql + " FROM dual";
    }
}
