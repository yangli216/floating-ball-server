package com.regionalai.floatingball.server.modules.useractivity.service;

import com.regionalai.floatingball.server.common.api.PageResponse;
import com.regionalai.floatingball.server.modules.useractivity.dto.RegionTreeNodeVO;
import com.regionalai.floatingball.server.modules.useractivity.dto.UserActivityItemVO;
import com.regionalai.floatingball.server.modules.useractivity.dto.UserActivityQueryDTO;
import com.regionalai.floatingball.server.modules.useractivity.dto.UserActivitySummaryVO;
import com.regionalai.floatingball.server.modules.useractivity.mapper.UserActivityMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserActivityService {

    private static final Logger log = LoggerFactory.getLogger(UserActivityService.class);

    private final UserActivityMapper userActivityMapper;

    public UserActivityService(UserActivityMapper userActivityMapper) {
        this.userActivityMapper = userActivityMapper;
    }

    public UserActivitySummaryVO getSummary(UserActivityQueryDTO query) {
        normalizeDateRange(query);

        UserActivitySummaryVO vo = new UserActivitySummaryVO();

        long activeUsers = userActivityMapper.countActiveUsers(query);
        long totalDevices = userActivityMapper.countTotalDevices(query);
        long inactiveUsers = totalDevices - activeUsers;

        String activityRate = totalDevices > 0
            ? formatPercent((double) activeUsers / totalDevices * 100) : "0";

        Double avgDuration = userActivityMapper.queryAvgUsageDuration(query);
        String avgUsageDuration = avgDuration != null
            ? formatDuration(avgDuration) : "0";

        vo.setActiveUsers(activeUsers);
        vo.setInactiveUsers(inactiveUsers);
        vo.setActivityRate(activityRate);
        vo.setAvgUsageDuration(avgUsageDuration);

        UserActivityQueryDTO prevQuery = buildPreviousPeriodQuery(query);
        long prevActive = userActivityMapper.countActiveUsers(prevQuery);
        long prevTotal = userActivityMapper.countTotalDevices(prevQuery);
        long prevInactive = prevTotal - prevActive;

        Double prevAvgDuration = userActivityMapper.queryAvgUsageDuration(prevQuery);

        vo.setActiveUsersGrowth(formatPercentGrowth(activeUsers, prevActive));
        vo.setInactiveUsersGrowth(formatAbsoluteGrowth(inactiveUsers, prevInactive));

        double prevRate = prevTotal > 0 ? (double) prevActive / prevTotal * 100 : 0;
        double curRate = totalDevices > 0 ? (double) activeUsers / totalDevices * 100 : 0;
        vo.setActivityRateGrowth(formatPercentDiffGrowth(curRate, prevRate));

        double prevDur = prevAvgDuration != null ? prevAvgDuration : 0;
        double curDur = avgDuration != null ? avgDuration : 0;
        vo.setAvgUsageDurationGrowth(formatDurationDiffGrowth(curDur, prevDur));

        return vo;
    }

    public List<RegionTreeNodeVO> getRegionTree(UserActivityQueryDTO query) {
        normalizeDateRange(query);

        List<Map<String, Object>> allRegions = userActivityMapper.queryAllRegions();
        List<Map<String, Object>> regionCounts = userActivityMapper.countActiveUsersByRegion(query);

        Map<String, Long> countMap = new HashMap<>();
        for (Map<String, Object> row : regionCounts) {
            String idRegion = String.valueOf(row.get("ID_REGION"));
            Object cntObj = row.get("CNT");
            long cnt = cntObj instanceof Number ? ((Number) cntObj).longValue() : 0L;
            countMap.put(idRegion, cnt);
        }

        Map<String, RegionTreeNodeVO> nodeMap = new LinkedHashMap<>();
        for (Map<String, Object> row : allRegions) {
            String id = String.valueOf(row.get("ID"));
            String name = String.valueOf(row.get("NAME"));
            String type = row.get("TYPE") != null ? String.valueOf(row.get("TYPE")) : "";
            String parentId = row.get("PARENTID") != null ? String.valueOf(row.get("PARENTID")) : null;

            RegionTreeNodeVO node = new RegionTreeNodeVO();
            node.setId(id);
            node.setName(name);
            node.setType(type);
            node.setUserCount(countMap.getOrDefault(id, 0L));
            node.setChildren(new ArrayList<>());
            nodeMap.put(id, node);
        }

        List<RegionTreeNodeVO> roots = new ArrayList<>();
        for (RegionTreeNodeVO node : nodeMap.values()) {
            String parentId = findParentId(allRegions, node.getId());
            if (parentId == null || !nodeMap.containsKey(parentId)) {
                roots.add(node);
            } else {
                nodeMap.get(parentId).getChildren().add(node);
            }
        }

        propagateCounts(roots);

        return roots;
    }

    public PageResponse<UserActivityItemVO> getUserList(UserActivityQueryDTO query, long current, long size) {
        normalizeDateRange(query);

        List<Map<String, Object>> rows = userActivityMapper.queryUserActivityList(query);

        List<UserActivityItemVO> items = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            UserActivityItemVO item = new UserActivityItemVO();
            item.setIdDevice(stringVal(row.get("IDDEVICE")));
            item.setCdDevice(stringVal(row.get("CDDEVICE")));
            item.setNaDevice(stringVal(row.get("NADEVICE")));
            item.setIdOrg(stringVal(row.get("IDORG")));
            item.setNaOrg(stringVal(row.get("NAORG")));
            item.setIdRegion(stringVal(row.get("IDREGION")));
            item.setNaRegion(stringVal(row.get("NAREGION")));
            item.setNaDoctor(stringVal(row.get("NADOCTOR")));
            item.setConsultationCount(longVal(row.get("CONSULTATIONCOUNT")));
            item.setOperationCount(longVal(row.get("OPERATIONCOUNT")));
            item.setLastActiveTime(row.get("LASTACTIVETIME") != null ? String.valueOf(row.get("LASTACTIVETIME")) : null);
            item.setActiveStatus(item.getConsultationCount() > 0 ? "active" : "inactive");
            items.add(item);
        }

        long total = items.size();
        int from = (int) Math.min((current - 1) * size, items.size());
        int to = (int) Math.min(from + size, items.size());
        List<UserActivityItemVO> page = items.subList(from, to);

        return new PageResponse<>(current, size, total, page);
    }

    private void normalizeDateRange(UserActivityQueryDTO query) {
        if (query.getDateFrom() != null && !query.getDateFrom().isEmpty()
            && query.getDateTo() != null && !query.getDateTo().isEmpty()) {
            return;
        }

        LocalDate now = LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE;
        String range = query.getTimeRange();

        if ("month".equals(range) || range == null || range.isEmpty()) {
            query.setDateFrom(now.withDayOfMonth(1).format(fmt));
            query.setDateTo(now.format(fmt));
        } else if ("lastMonth".equals(range)) {
            LocalDate prevMonth = now.minusMonths(1);
            query.setDateFrom(prevMonth.withDayOfMonth(1).format(fmt));
            query.setDateFrom(prevMonth.withDayOfMonth(prevMonth.lengthOfMonth()).format(fmt));
            query.setDateTo(prevMonth.withDayOfMonth(prevMonth.lengthOfMonth()).format(fmt));
        } else {
            query.setDateFrom(now.withDayOfMonth(1).format(fmt));
            query.setDateTo(now.format(fmt));
        }
    }

    private UserActivityQueryDTO buildPreviousPeriodQuery(UserActivityQueryDTO current) {
        UserActivityQueryDTO prev = new UserActivityQueryDTO();
        prev.setIdRegion(current.getIdRegion());
        prev.setActiveStatus(current.getActiveStatus());

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
            if ("month".equals(range) || range == null || range.isEmpty()) {
                LocalDate prevMonthEnd = from.minusDays(1);
                LocalDate prevMonthStart = prevMonthEnd.withDayOfMonth(1);
                prev.setDateFrom(prevMonthStart.format(fmt));
                prev.setDateTo(prevMonthEnd.format(fmt));
            } else if ("lastMonth".equals(range)) {
                LocalDate prevPrevEnd = from.minusDays(1);
                LocalDate prevPrevStart = prevPrevEnd.withDayOfMonth(1);
                prev.setDateFrom(prevPrevStart.format(fmt));
                prev.setDateTo(prevPrevEnd.format(fmt));
            } else {
                long span = ChronoUnit.DAYS.between(from, to);
                LocalDate prevFrom = from.minusDays(span + 1);
                LocalDate prevTo = from.minusDays(1);
                prev.setDateFrom(prevFrom.format(fmt));
                prev.setDateTo(prevTo.format(fmt));
            }
        } catch (Exception ex) {
            log.debug("user activity previous period calculation failed. error={}", ex.getMessage());
        }
        return prev;
    }

    private String findParentId(List<Map<String, Object>> allRegions, String id) {
        for (Map<String, Object> row : allRegions) {
            if (String.valueOf(row.get("ID")).equals(id)) {
                Object parentId = row.get("PARENTID");
                return parentId != null ? String.valueOf(parentId) : null;
            }
        }
        return null;
    }

    private long propagateCounts(List<RegionTreeNodeVO> nodes) {
        long total = 0;
        for (RegionTreeNodeVO node : nodes) {
            long childTotal = 0;
            if (node.getChildren() != null && !node.getChildren().isEmpty()) {
                childTotal = propagateCounts(node.getChildren());
            }
            node.setUserCount(node.getUserCount() + childTotal);
            total += node.getUserCount();
        }
        return total;
    }

    private String formatPercent(double value) {
        return new BigDecimal(value).setScale(1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private String formatDuration(double minutes) {
        if (minutes < 60) {
            return new BigDecimal(minutes).setScale(1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + " 分钟";
        }
        double hours = minutes / 60.0;
        return new BigDecimal(hours).setScale(1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + " 小时";
    }

    private String formatPercentGrowth(long current, long previous) {
        if (previous == 0) {
            return current > 0 ? "100" : "0";
        }
        double growth = (double) (current - previous) / previous * 100;
        return new BigDecimal(growth).setScale(1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private String formatAbsoluteGrowth(long current, long previous) {
        long diff = current - previous;
        return String.valueOf(diff);
    }

    private String formatPercentDiffGrowth(double current, double previous) {
        double diff = current - previous;
        return new BigDecimal(diff).setScale(1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private String formatDurationDiffGrowth(double currentMinutes, double previousMinutes) {
        double diff = currentMinutes - previousMinutes;
        if (Math.abs(diff) < 60) {
            return new BigDecimal(diff).setScale(1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + " 分钟";
        }
        double hours = diff / 60.0;
        return new BigDecimal(hours).setScale(1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + " 小时";
    }

    private String stringVal(Object obj) {
        return obj != null ? String.valueOf(obj) : null;
    }

    private long longVal(Object obj) {
        if (obj instanceof Number) {
            return ((Number) obj).longValue();
        }
        return 0L;
    }
}
