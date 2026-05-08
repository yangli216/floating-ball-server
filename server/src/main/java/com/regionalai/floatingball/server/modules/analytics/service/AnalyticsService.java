package com.regionalai.floatingball.server.modules.analytics.service;

import com.regionalai.floatingball.server.modules.analytics.dto.AnalyticsQueryDTO;
import com.regionalai.floatingball.server.modules.analytics.dto.AnalyticsSummaryVO;
import com.regionalai.floatingball.server.modules.analytics.dto.DistributionDataVO;
import com.regionalai.floatingball.server.modules.analytics.dto.DistributionItemVO;
import com.regionalai.floatingball.server.modules.analytics.dto.FunctionUsageItemVO;
import com.regionalai.floatingball.server.modules.analytics.dto.FunctionUsageQueryDTO;
import com.regionalai.floatingball.server.modules.analytics.dto.FunctionUsageResponseVO;
import com.regionalai.floatingball.server.modules.analytics.dto.FunctionUsageTrendVO;
import com.regionalai.floatingball.server.modules.analytics.dto.RegionDistributionItemVO;
import com.regionalai.floatingball.server.modules.analytics.dto.TrendDataVO;
import com.regionalai.floatingball.server.modules.analytics.mapper.AnalyticsMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class AnalyticsService {

    private final AnalyticsMapper analyticsMapper;

    public AnalyticsService(AnalyticsMapper analyticsMapper) {
        this.analyticsMapper = analyticsMapper;
    }

    public AnalyticsSummaryVO getSummary(AnalyticsQueryDTO query) {
        AnalyticsSummaryVO vo = new AnalyticsSummaryVO();

        long aiServiceTotal = analyticsMapper.countAiService(query);
        long consultationTotal = analyticsMapper.countConsultation(query);
        long activeDoctorCount = analyticsMapper.countActiveDoctors(query);
        long adoptedCount = analyticsMapper.countAdoptedConsultations(query);
        long finalizedCount = analyticsMapper.countFinalizedConsultations(query);
        long diagnosisMatchedCount = analyticsMapper.countDiagnosisMatchedConsultations(query);

        long days = computeDays(query);
        long avgDaily = days > 0 ? aiServiceTotal / days : 0;

        String adoptionRate = consultationTotal > 0
            ? formatPercent((double) adoptedCount / consultationTotal * 100) : "0";
        String matchRate = finalizedCount > 0
            ? formatPercent((double) diagnosisMatchedCount / finalizedCount * 100) : "0";

        vo.setAiServiceTotal(aiServiceTotal);
        vo.setAvgDailyAiService(avgDaily);
        vo.setAiAdoptionRate(adoptionRate);
        vo.setDiagnosisMatchRate(matchRate);
        vo.setActiveDoctorCount(activeDoctorCount);
        vo.setConsultationTotal(consultationTotal);

        AnalyticsQueryDTO prevQuery = buildPreviousPeriodQuery(query);
        long prevAiService = analyticsMapper.countAiService(prevQuery);
        long prevConsultation = analyticsMapper.countConsultation(prevQuery);
        long prevActiveDoctors = analyticsMapper.countActiveDoctors(prevQuery);
        long prevAdopted = analyticsMapper.countAdoptedConsultations(prevQuery);
        long prevFinalized = analyticsMapper.countFinalizedConsultations(prevQuery);
        long prevDiagnosisMatched = analyticsMapper.countDiagnosisMatchedConsultations(prevQuery);

        long prevDays = computeDays(prevQuery);
        long prevAvgDaily = prevDays > 0 ? prevAiService / prevDays : 0;

        vo.setAiServiceGrowth(formatGrowth(aiServiceTotal, prevAiService));
        vo.setAvgDailyGrowth(formatGrowth(avgDaily, prevAvgDaily));

        double prevAdoption = prevConsultation > 0 ? (double) prevAdopted / prevConsultation * 100 : 0;
        double curAdoption = consultationTotal > 0 ? (double) adoptedCount / consultationTotal * 100 : 0;
        vo.setAdoptionRateGrowth(formatPercentGrowth(curAdoption, prevAdoption));

        double prevMatch = prevFinalized > 0 ? (double) prevDiagnosisMatched / prevFinalized * 100 : 0;
        double curMatch = finalizedCount > 0 ? (double) diagnosisMatchedCount / finalizedCount * 100 : 0;
        vo.setMatchRateGrowth(formatPercentGrowth(curMatch, prevMatch));
        vo.setActiveDoctorGrowth(formatAbsoluteGrowth(activeDoctorCount, prevActiveDoctors));
        vo.setConsultationGrowth(formatGrowth(consultationTotal, prevConsultation));

        return vo;
    }

    public TrendDataVO getTrend(AnalyticsQueryDTO query) {
        TrendDataVO vo = new TrendDataVO();
        List<Map<String, Object>> aiRows = analyticsMapper.queryAiServiceTrend(query);
        List<Map<String, Object>> consRows = analyticsMapper.queryConsultationTrend(query);

        Map<String, Long> aiMap = toDayMap(aiRows);
        Map<String, Long> consMap = toDayMap(consRows);

        List<String> days = new ArrayList<>();
        List<Long> aiValues = new ArrayList<>();
        List<Long> consValues = new ArrayList<>();

        String fromStr = query.getDateFrom();
        String toStr = query.getDateTo();
        if (fromStr != null && !fromStr.isEmpty() && toStr != null && !toStr.isEmpty()) {
            LocalDate start = LocalDate.parse(fromStr, DateTimeFormatter.ISO_LOCAL_DATE);
            LocalDate end = LocalDate.parse(toStr, DateTimeFormatter.ISO_LOCAL_DATE);
            LocalDate cursor = start;
            while (!cursor.isAfter(end)) {
                String day = cursor.format(DateTimeFormatter.ISO_LOCAL_DATE);
                days.add(day);
                aiValues.add(aiMap.getOrDefault(day, 0L));
                consValues.add(consMap.getOrDefault(day, 0L));
                cursor = cursor.plusDays(1);
            }
        } else {
            for (String day : aiMap.keySet()) {
                days.add(day);
                aiValues.add(aiMap.get(day));
                consValues.add(consMap.getOrDefault(day, 0L));
            }
        }

        vo.setDays(days);
        vo.setAiServiceValues(aiValues);
        vo.setConsultationValues(consValues);
        return vo;
    }

    public DistributionDataVO getDistribution(AnalyticsQueryDTO query) {
        DistributionDataVO vo = new DistributionDataVO();

        List<DistributionItemVO> orgDist = analyticsMapper.queryOrgDistribution(query);
        List<DistributionItemVO> rawRegion = analyticsMapper.queryRegionDistributionRaw(query);

        long totalService = 0L;
        for (DistributionItemVO item : orgDist) {
            if (item.getValue() != null) {
                totalService += item.getValue();
            }
        }

        List<RegionDistributionItemVO> regionDist = new ArrayList<>();
        long regionTotal = 0L;
        for (DistributionItemVO raw : rawRegion) {
            if (raw.getValue() != null) {
                regionTotal += raw.getValue();
            }
        }
        for (DistributionItemVO raw : rawRegion) {
            RegionDistributionItemVO item = new RegionDistributionItemVO();
            item.setName(raw.getName());
            item.setValue(raw.getValue());
            long val = raw.getValue() != null ? raw.getValue() : 0L;
            String pct = regionTotal > 0 ? String.valueOf(Math.round((double) val / regionTotal * 100)) : "0";
            item.setPercentage(pct);
            regionDist.add(item);
        }

        vo.setOrgDistribution(orgDist);
        vo.setRegionDistribution(regionDist);
        vo.setTotalService(totalService > 0 ? totalService : regionTotal);
        return vo;
    }

    private long computeDays(AnalyticsQueryDTO query) {
        String fromStr = query.getDateFrom();
        String toStr = query.getDateTo();
        if (fromStr == null || fromStr.isEmpty() || toStr == null || toStr.isEmpty()) {
            return 30;
        }
        try {
            LocalDate from = LocalDate.parse(fromStr, DateTimeFormatter.ISO_LOCAL_DATE);
            LocalDate to = LocalDate.parse(toStr, DateTimeFormatter.ISO_LOCAL_DATE);
            return Math.max(1, ChronoUnit.DAYS.between(from, to) + 1);
        } catch (Exception e) {
            return 30;
        }
    }

    private static final WeekFields WEEK_FIELDS = WeekFields.of(Locale.CHINA);

    private AnalyticsQueryDTO buildPreviousPeriodQuery(AnalyticsQueryDTO current) {
        AnalyticsQueryDTO prev = new AnalyticsQueryDTO();
        prev.setIdOrg(current.getIdOrg());
        prev.setIdRegion(current.getIdRegion());

        String fromStr = current.getDateFrom();
        String toStr = current.getDateTo();
        if (fromStr == null || fromStr.isEmpty() || toStr == null || toStr.isEmpty()) {
            return prev;
        }
        try {
            LocalDate from = LocalDate.parse(fromStr, DateTimeFormatter.ISO_LOCAL_DATE);
            LocalDate to = LocalDate.parse(toStr, DateTimeFormatter.ISO_LOCAL_DATE);
            DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE;

            String range = current.getTimeRange();
            if ("today".equals(range)) {
                LocalDate yesterday = from.minusDays(1);
                prev.setDateFrom(yesterday.format(fmt));
                prev.setDateTo(yesterday.format(fmt));
            } else if ("week".equals(range)) {
                LocalDate prevWeekEnd = from.minusDays(1);
                LocalDate prevWeekStart = prevWeekEnd.minusDays(6);
                prev.setDateFrom(prevWeekStart.format(fmt));
                prev.setDateTo(prevWeekEnd.format(fmt));
            } else if ("month".equals(range)) {
                LocalDate prevMonthEnd = from.minusDays(1);
                LocalDate prevMonthStart = prevMonthEnd.withDayOfMonth(1);
                prev.setDateFrom(prevMonthStart.format(fmt));
                prev.setDateTo(prevMonthEnd.format(fmt));
            } else if ("quarter".equals(range)) {
                LocalDate prevQuarterEnd = from.minusDays(1);
                int prevQuarterMonth = ((prevQuarterEnd.getMonthValue() - 1) / 3) * 3 + 1;
                LocalDate prevQuarterStart = LocalDate.of(prevQuarterEnd.getYear(), prevQuarterMonth, 1);
                prev.setDateFrom(prevQuarterStart.format(fmt));
                prev.setDateTo(prevQuarterEnd.format(fmt));
            } else if ("year".equals(range)) {
                LocalDate prevYearEnd = from.minusDays(1);
                LocalDate prevYearStart = LocalDate.of(prevYearEnd.getYear(), 1, 1);
                prev.setDateFrom(prevYearStart.format(fmt));
                prev.setDateTo(prevYearEnd.format(fmt));
            } else {
                long span = ChronoUnit.DAYS.between(from, to);
                LocalDate prevFrom = from.minusDays(span + 1);
                LocalDate prevTo = from.minusDays(1);
                prev.setDateFrom(prevFrom.format(fmt));
                prev.setDateTo(prevTo.format(fmt));
            }
        } catch (Exception ignored) {
        }
        return prev;
    }

    private Map<String, Long> toDayMap(List<Map<String, Object>> rows) {
        Map<String, Long> map = new LinkedHashMap<>();
        if (rows == null) {
            return map;
        }
        for (Map<String, Object> row : rows) {
            String day = String.valueOf(row.get("DAY_STR"));
            Object cntObj = row.get("CNT");
            long cnt = 0;
            if (cntObj instanceof Number) {
                cnt = ((Number) cntObj).longValue();
            }
            map.put(day, cnt);
        }
        return map;
    }

    private String formatPercent(double value) {
        return new BigDecimal(value).setScale(1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private String formatGrowth(long current, long previous) {
        if (previous == 0) {
            return current > 0 ? "100" : "0";
        }
        double growth = (double) (current - previous) / previous * 100;
        return new BigDecimal(growth).setScale(1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private String formatPercentGrowth(double current, double previous) {
        if (previous == 0) {
            return current > 0 ? "100" : "0";
        }
        double growth = current - previous;
        return new BigDecimal(growth).setScale(1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private String formatAbsoluteGrowth(long current, long previous) {
        long diff = current - previous;
        return String.valueOf(diff);
    }

    public FunctionUsageResponseVO getFunctionUsage(FunctionUsageQueryDTO query) {
        FunctionUsageResponseVO vo = new FunctionUsageResponseVO();

        List<FunctionUsageItemVO> ranking = analyticsMapper.queryFunctionUsageRanking(query);
        for (FunctionUsageItemVO item : ranking) {
            item.setModuleName(toCnModule(item.getModuleName()));
        }

        long totalCallCount = 0;
        for (FunctionUsageItemVO item : ranking) {
            totalCallCount += item.getCallCount();
        }

        long days = computeDaysFromFunctionQuery(query);
        long avgDaily = days > 0 ? totalCallCount / days : 0;

        long activeModuleCount = ranking.size();
        long totalModules = analyticsMapper.queryDistinctModules().size();
        String usageRate = totalModules > 0 ? String.valueOf(Math.round((double) activeModuleCount / totalModules * 100)) + "%" : "0%";

        vo.setTotalCallCount(totalCallCount);
        vo.setAvgDailyCalls(avgDaily);
        vo.setUsageRate(usageRate);

        FunctionUsageQueryDTO prevQuery = buildPreviousFunctionUsageQuery(query);
        List<FunctionUsageItemVO> prevRanking = analyticsMapper.queryFunctionUsagePreviousRanking(prevQuery);
        Map<String, Long> prevCallMap = new LinkedHashMap<>();
        for (FunctionUsageItemVO item : prevRanking) {
            prevCallMap.put(toCnModule(item.getModuleName()), item.getCallCount());
        }
        for (FunctionUsageItemVO item : ranking) {
            Long prev = prevCallMap.get(item.getModuleName());
            if (prev == null) prev = 0L;
            item.setGrowthRate(formatGrowth(item.getCallCount(), prev));
        }

        vo.setRanking(ranking);
        vo.setTotal((long) ranking.size());

        int page = query.getCurrent() != null ? query.getCurrent() : 1;
        int size = query.getSize() != null ? query.getSize() : 20;
        int from = Math.min((page - 1) * size, ranking.size());
        int to = Math.min(from + size, ranking.size());
        vo.setRecords(ranking.subList(from, to));

        List<Map<String, Object>> trendRows = analyticsMapper.queryFunctionUsageTrend(query);
        Map<String, Map<String, Long>> trendMap = new LinkedHashMap<>();
        for (Map<String, Object> row : trendRows) {
            String module = toCnModule(String.valueOf(row.get("MODULENAME")));
            String day = String.valueOf(row.get("DAYSTR"));
            Object cntObj = row.get("CNT");
            long cnt = cntObj instanceof Number ? ((Number) cntObj).longValue() : 0L;
            trendMap.computeIfAbsent(module, k -> new LinkedHashMap<>()).put(day, cnt);
        }

        List<String> topModules = new ArrayList<>();
        int limit = Math.min(5, ranking.size());
        for (int i = 0; i < limit; i++) {
            topModules.add(ranking.get(i).getModuleName());
        }

        List<String> allDays = collectDays(query);
        List<List<Long>> trendValues = new ArrayList<>();

        for (String module : topModules) {
            Map<String, Long> moduleData = trendMap.getOrDefault(module, new LinkedHashMap<>());
            List<Long> vals = new ArrayList<>();
            for (String day : allDays) {
                vals.add(moduleData.getOrDefault(day, 0L));
            }
            trendValues.add(vals);
        }

        FunctionUsageTrendVO trendVO = new FunctionUsageTrendVO();
        trendVO.setModules(topModules);
        trendVO.setDays(allDays);
        trendVO.setValues(trendValues);
        vo.setTrend(trendVO);

        return vo;
    }

    private long computeDaysFromFunctionQuery(FunctionUsageQueryDTO query) {
        String fromStr = query.getDateFrom();
        String toStr = query.getDateTo();
        if (fromStr == null || fromStr.isEmpty() || toStr == null || toStr.isEmpty()) {
            return 30;
        }
        try {
            LocalDate from = LocalDate.parse(fromStr, DateTimeFormatter.ISO_LOCAL_DATE);
            LocalDate to = LocalDate.parse(toStr, DateTimeFormatter.ISO_LOCAL_DATE);
            return Math.max(1, ChronoUnit.DAYS.between(from, to) + 1);
        } catch (Exception e) {
            return 30;
        }
    }

    private FunctionUsageQueryDTO buildPreviousFunctionUsageQuery(FunctionUsageQueryDTO current) {
        FunctionUsageQueryDTO prev = new FunctionUsageQueryDTO();
        prev.setIdOrg(current.getIdOrg());
        prev.setIdRegion(current.getIdRegion());
        prev.setFunctionModules(current.getFunctionModules());

        String fromStr = current.getDateFrom();
        String toStr = current.getDateTo();
        if (fromStr == null || fromStr.isEmpty() || toStr == null || toStr.isEmpty()) {
            return prev;
        }
        try {
            LocalDate from = LocalDate.parse(fromStr, DateTimeFormatter.ISO_LOCAL_DATE);
            LocalDate to = LocalDate.parse(toStr, DateTimeFormatter.ISO_LOCAL_DATE);
            long span = ChronoUnit.DAYS.between(from, to);
            LocalDate prevFrom = from.minusDays(span + 1);
            LocalDate prevTo = from.minusDays(1);
            prev.setDateFrom(prevFrom.format(DateTimeFormatter.ISO_LOCAL_DATE));
            prev.setDateTo(prevTo.format(DateTimeFormatter.ISO_LOCAL_DATE));
        } catch (Exception ignored) {
        }
        return prev;
    }

    private List<String> collectDays(FunctionUsageQueryDTO query) {
        List<String> days = new ArrayList<>();
        String fromStr = query.getDateFrom();
        String toStr = query.getDateTo();
        if (fromStr == null || fromStr.isEmpty() || toStr == null || toStr.isEmpty()) {
            return days;
        }
        try {
            LocalDate start = LocalDate.parse(fromStr, DateTimeFormatter.ISO_LOCAL_DATE);
            LocalDate end = LocalDate.parse(toStr, DateTimeFormatter.ISO_LOCAL_DATE);
            LocalDate cursor = start;
            while (!cursor.isAfter(end)) {
                days.add(cursor.format(DateTimeFormatter.ISO_LOCAL_DATE));
                cursor = cursor.plusDays(1);
            }
        } catch (Exception ignored) {
        }
        return days;
    }

    private String toCnModule(String module) {
        if (module == null) return "";
        java.util.Map<String, String> map = new java.util.HashMap<>();
        map.put("aliyunSpeech", "语音识别");
        map.put("app_shell", "应用外壳");
        map.put("chat_panel", "对话面板");
        map.put("consultation_ai", "问诊AI");
        map.put("consultation_checklist", "鉴别排查");
        map.put("consultation_dynamic_symptom", "动态症状");
        map.put("consultation_page", "问诊页面");
        map.put("consultation_record", "病历草稿");
        map.put("consultation_reference", "PHIS引用");
        map.put("deep_link_listener", "DeepLink监听");
        map.put("diagnosis_path", "诊断路径");
        map.put("diagnosis_reviewer", "诊断审查");
        map.put("error_tracker", "错误追踪");
        map.put("examination_reviewer", "检查审查");
        map.put("fact_checker", "事实核查");
        map.put("feedback", "反馈面板");
        map.put("feedback_panel", "反馈面板");
        map.put("his_bridge", "HIS桥接");
        map.put("llm", "LLM调用");
        map.put("medicine_reviewer", "用药审查");
        map.put("navigation", "页面导航");
        map.put("reception_capsule", "接诊胶囊");
        map.put("reception_risk_analysis", "风险评估");
        map.put("regional_runtime", "区域化运行时");
        map.put("risk_alert_panel", "风险告警");
        map.put("settings_feedback", "设置反馈");
        map.put("settings_panel", "设置面板");
        map.put("tcm_diagnosis_reviewer", "中医诊断审查");
        map.put("tcm_medicine_reviewer", "中药审查");
        map.put("voice_capsule", "语音采集");
        map.put("voice_consultation_ai", "语音问诊AI");
        map.put("voice_consultation_result", "语音问诊结果");
        map.put("voice_intent", "语音意图识别");
        map.put("voice_recommendation", "语音推荐反馈");
        map.put("voice_record_field", "语音病例字段");
        map.put("voice_safety_reviewer", "语音安全审查");
        map.put("voice_session", "语音整页反馈");
        return map.getOrDefault(module, module);
    }

    public List<String> getFunctionModuleOptions() {
        List<String> modules = analyticsMapper.queryDistinctModules();
        List<String> result = new ArrayList<>();
        for (String m : modules) {
            result.add(toCnModule(m));
        }
        return result;
    }
}
