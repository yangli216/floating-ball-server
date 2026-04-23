package com.regionalai.floatingball.server.modules.stats.mapper;

import com.regionalai.floatingball.server.modules.stats.dto.OverviewStatsVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OverviewStatsMapper {

    @Select({
        "SELECT",
        "  (SELECT COUNT(1) FROM c_ai_region WHERE fg_active = '1') AS region_count,",
        "  (SELECT COUNT(1) FROM c_ai_org WHERE fg_active = '1') AS org_count,",
        "  (SELECT COUNT(1) FROM c_ai_device WHERE fg_active = '1') AS device_count,",
        "  (SELECT COUNT(1) FROM c_ai_config WHERE fg_active = '1') AS config_count,",
        "  (SELECT COUNT(1) FROM c_ai_prompt WHERE fg_active = '1') AS prompt_count,",
        "  (SELECT COUNT(1) FROM c_ai_symptom_template WHERE fg_active = '1') AS symptom_template_count,",
        "  (SELECT COUNT(1) FROM c_ai_data_package WHERE fg_active = '1') AS data_package_count,",
        "  (SELECT COUNT(1) FROM c_ai_op_log WHERE fg_active = '1') AS log_count,",
        "  (SELECT COUNT(1) FROM c_ai_user WHERE fg_active = '1') AS user_count,",
        "  (SELECT COUNT(1) FROM c_ai_role WHERE fg_active = '1') AS role_count",
        "FROM dual"
    })
    OverviewStatsVO selectOverviewStats();
}
