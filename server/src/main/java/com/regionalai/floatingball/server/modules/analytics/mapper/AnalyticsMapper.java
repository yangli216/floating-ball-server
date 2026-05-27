package com.regionalai.floatingball.server.modules.analytics.mapper;

import com.regionalai.floatingball.server.modules.analytics.dto.AnalyticsQueryDTO;
import com.regionalai.floatingball.server.modules.analytics.dto.DistributionItemVO;
import com.regionalai.floatingball.server.modules.analytics.dto.FunctionUsageItemVO;
import com.regionalai.floatingball.server.modules.analytics.dto.FunctionUsageQueryDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface AnalyticsMapper {

    long countAiService(@Param("query") AnalyticsQueryDTO query);

    long countConsultation(@Param("query") AnalyticsQueryDTO query);

    long countActiveDoctors(@Param("query") AnalyticsQueryDTO query);

    long countAdoptedConsultations(@Param("query") AnalyticsQueryDTO query);

    long countFinalizedConsultations(@Param("query") AnalyticsQueryDTO query);

    long countDiagnosisMatchedConsultations(@Param("query") AnalyticsQueryDTO query);

    List<Map<String, Object>> queryAiServiceTrend(@Param("query") AnalyticsQueryDTO query);

    List<Map<String, Object>> queryConsultationTrend(@Param("query") AnalyticsQueryDTO query);

    List<DistributionItemVO> queryOrgDistribution(@Param("query") AnalyticsQueryDTO query);

    List<DistributionItemVO> queryRegionDistributionRaw(@Param("query") AnalyticsQueryDTO query);

    List<String> queryDistinctModules();

    List<FunctionUsageItemVO> queryFunctionUsageRanking(@Param("query") FunctionUsageQueryDTO query);

    List<FunctionUsageItemVO> queryFunctionUsagePreviousRanking(@Param("query") FunctionUsageQueryDTO query);

    List<Map<String, Object>> queryFunctionUsageTrend(@Param("query") FunctionUsageQueryDTO query);
}
