package com.regionalai.floatingball.server.modules.useractivity.mapper;

import com.regionalai.floatingball.server.modules.useractivity.dto.UserActivityQueryDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;

import java.util.List;
import java.util.Map;

@Mapper
public interface UserActivityMapper {

    @SelectProvider(type = UserActivitySqlProvider.class, method = "countActiveUsers")
    long countActiveUsers(@Param("query") UserActivityQueryDTO query);

    @SelectProvider(type = UserActivitySqlProvider.class, method = "countTotalDevices")
    long countTotalDevices(@Param("query") UserActivityQueryDTO query);

    @SelectProvider(type = UserActivitySqlProvider.class, method = "countEffectiveConsultations")
    long countEffectiveConsultations(@Param("query") UserActivityQueryDTO query);

    @SelectProvider(type = UserActivitySqlProvider.class, method = "countConsultations")
    long countConsultations(@Param("query") UserActivityQueryDTO query);

    @SelectProvider(type = UserActivitySqlProvider.class, method = "queryAllRegions")
    List<Map<String, Object>> queryAllRegions();

    @SelectProvider(type = UserActivitySqlProvider.class, method = "countActiveUsersByRegion")
    List<Map<String, Object>> countActiveUsersByRegion(@Param("query") UserActivityQueryDTO query);

    @SelectProvider(type = UserActivitySqlProvider.class, method = "queryUserActivityList")
    List<Map<String, Object>> queryUserActivityList(@Param("query") UserActivityQueryDTO query);
}
