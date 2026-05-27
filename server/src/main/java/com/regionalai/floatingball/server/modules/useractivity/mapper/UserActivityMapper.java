package com.regionalai.floatingball.server.modules.useractivity.mapper;

import com.regionalai.floatingball.server.modules.useractivity.dto.UserActivityQueryDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface UserActivityMapper {

    long countActiveUsers(@Param("query") UserActivityQueryDTO query);

    long countTotalDevices(@Param("query") UserActivityQueryDTO query);

    long countEffectiveConsultations(@Param("query") UserActivityQueryDTO query);

    long countConsultations(@Param("query") UserActivityQueryDTO query);

    List<Map<String, Object>> queryAllRegions();

    List<Map<String, Object>> countActiveUsersByRegion(@Param("query") UserActivityQueryDTO query);

    List<Map<String, Object>> queryUserActivityList(@Param("query") UserActivityQueryDTO query);
}
