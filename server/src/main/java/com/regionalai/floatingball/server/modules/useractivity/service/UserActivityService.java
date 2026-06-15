package com.regionalai.floatingball.server.modules.useractivity.service;

import com.regionalai.floatingball.server.common.api.PageResponse;
import com.regionalai.floatingball.server.common.exception.BusinessException;
import com.regionalai.floatingball.server.modules.useractivity.dto.RegionTreeNodeVO;
import com.regionalai.floatingball.server.modules.useractivity.dto.UserActivityItemVO;
import com.regionalai.floatingball.server.modules.useractivity.dto.UserActivityQueryDTO;
import com.regionalai.floatingball.server.modules.useractivity.dto.UserActivitySummaryVO;
import com.regionalai.floatingball.server.modules.useractivity.mapper.UserActivityMapper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private static final long EXPORT_MAX_ROWS = 10000L;

    private final UserActivityMapper userActivityMapper;

    public UserActivityService(UserActivityMapper userActivityMapper) {
        this.userActivityMapper = userActivityMapper;
    }

    public UserActivitySummaryVO getSummary(UserActivityQueryDTO query) {
        normalizeDateRange(query);
        fillDateBounds(query);

        UserActivitySummaryVO vo = new UserActivitySummaryVO();

        long activeUsers = userActivityMapper.countActiveUsers(query);
        long totalDevices = userActivityMapper.countTotalDevices(query);
        long inactiveUsers = totalDevices - activeUsers;
        long totalConsultations = userActivityMapper.countConsultations(query);
        long effectiveConsultations = userActivityMapper.countEffectiveConsultations(query);

        String activityRate = totalDevices > 0
            ? formatPercent((double) activeUsers / totalDevices * 100) : "0";
        String effectiveConsultationRate = totalConsultations > 0
            ? formatPercent((double) effectiveConsultations / totalConsultations * 100) : "0";

        vo.setActiveUsers(activeUsers);
        vo.setInactiveUsers(inactiveUsers);
        vo.setActivityRate(activityRate);
        vo.setEffectiveConsultationRate(effectiveConsultationRate);

        UserActivityQueryDTO prevQuery = buildPreviousPeriodQuery(query);
        long prevActive = userActivityMapper.countActiveUsers(prevQuery);
        long prevTotal = userActivityMapper.countTotalDevices(prevQuery);
        long prevInactive = prevTotal - prevActive;
        long prevTotalConsultations = userActivityMapper.countConsultations(prevQuery);
        long prevEffectiveConsultations = userActivityMapper.countEffectiveConsultations(prevQuery);

        vo.setActiveUsersGrowth(formatPercentGrowth(activeUsers, prevActive));
        vo.setInactiveUsersGrowth(formatAbsoluteGrowth(inactiveUsers, prevInactive));

        double prevRate = prevTotal > 0 ? (double) prevActive / prevTotal * 100 : 0;
        double curRate = totalDevices > 0 ? (double) activeUsers / totalDevices * 100 : 0;
        vo.setActivityRateGrowth(formatPercentDiffGrowth(curRate, prevRate));

        double prevEffectiveRate = prevTotalConsultations > 0 ? (double) prevEffectiveConsultations / prevTotalConsultations * 100 : 0;
        double curEffectiveRate = totalConsultations > 0 ? (double) effectiveConsultations / totalConsultations * 100 : 0;
        vo.setEffectiveConsultationRateGrowth(formatPercentDiffGrowth(curEffectiveRate, prevEffectiveRate));

        return vo;
    }

    public List<RegionTreeNodeVO> getRegionTree(UserActivityQueryDTO query) {
        normalizeDateRange(query);
        fillDateBounds(query);

        List<Map<String, Object>> allRegions = userActivityMapper.queryAllRegions();
        List<Map<String, Object>> regionCounts = userActivityMapper.countActiveUsersByRegion(query);

        Map<String, Long> countMap = new HashMap<>();
        for (Map<String, Object> row : regionCounts) {
            String idRegion = String.valueOf(mapValue(row, "ID_REGION", "id_region"));
            Object cntObj = mapValue(row, "CNT", "cnt");
            long cnt = cntObj instanceof Number ? ((Number) cntObj).longValue() : 0L;
            countMap.put(idRegion, cnt);
        }

        Map<String, RegionTreeNodeVO> nodeMap = new LinkedHashMap<>();
        for (Map<String, Object> row : allRegions) {
            String id = String.valueOf(mapValue(row, "ID", "id"));
            String name = String.valueOf(mapValue(row, "NAME", "name"));
            Object typeValue = mapValue(row, "TYPE", "type");
            Object parentValue = mapValue(row, "PARENTID", "parentid");
            String type = typeValue != null ? String.valueOf(typeValue) : "";
            String parentId = parentValue != null ? String.valueOf(parentValue) : null;

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
        fillDateBounds(query);

        List<Map<String, Object>> rows = userActivityMapper.queryUserActivityList(query);

        List<UserActivityItemVO> items = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            UserActivityItemVO item = new UserActivityItemVO();
            item.setIdDevice(stringVal(mapValue(row, "IDDEVICE", "iddevice")));
            item.setCdDevice(stringVal(mapValue(row, "CDDEVICE", "cddevice")));
            item.setNaDevice(stringVal(mapValue(row, "NADEVICE", "nadevice")));
            item.setIdOrg(stringVal(mapValue(row, "IDORG", "idorg")));
            item.setNaOrg(stringVal(mapValue(row, "NAORG", "naorg")));
            item.setIdRegion(stringVal(mapValue(row, "IDREGION", "idregion")));
            item.setNaRegion(stringVal(mapValue(row, "NAREGION", "naregion")));
            item.setNaDoctor(stringVal(mapValue(row, "NADOCTOR", "nadoctor")));
            item.setConsultationCount(longVal(mapValue(row, "CONSULTATIONCOUNT", "consultationcount")));
            item.setEffectiveConsultationCount(longVal(mapValue(row, "EFFECTIVECONSULTATIONCOUNT", "effectiveconsultationcount")));
            Object lastActiveTime = mapValue(row, "LASTACTIVETIME", "lastactivetime");
            item.setLastActiveTime(lastActiveTime != null ? String.valueOf(lastActiveTime) : null);
            item.setActiveStatus(item.getConsultationCount() > 0 ? "active" : "inactive");
            items.add(item);
        }

        long total = items.size();
        int from = (int) Math.min((current - 1) * size, items.size());
        int to = (int) Math.min(from + size, items.size());
        List<UserActivityItemVO> page = items.subList(from, to);

        return new PageResponse<>(current, size, total, page);
    }

    public byte[] exportExcel(UserActivityQueryDTO query) {
        UserActivitySummaryVO summary = getSummary(query);
        PageResponse<UserActivityItemVO> page = getUserList(query, 1, EXPORT_MAX_ROWS);
        List<UserActivityItemVO> users = page.getRecords();

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            CellStyle headerStyle = createHeaderStyle(workbook);

            XSSFSheet summarySheet = workbook.createSheet("活跃度汇总");
            writeHeader(summarySheet, headerStyle, "指标", "数值", "对比变化");
            writeRow(summarySheet, 1, "活跃用户数", summary.getActiveUsers(), summary.getActiveUsersGrowth() + "%");
            writeRow(summarySheet, 2, "不活跃用户数", summary.getInactiveUsers(), summary.getInactiveUsersGrowth());
            writeRow(summarySheet, 3, "活跃率", summary.getActivityRate() + "%", summary.getActivityRateGrowth() + "%");
            writeRow(summarySheet, 4, "有效问诊率", summary.getEffectiveConsultationRate() + "%", summary.getEffectiveConsultationRateGrowth() + "%");
            autoSize(summarySheet, 3);

            XSSFSheet userSheet = workbook.createSheet("用户明细");
            writeHeader(userSheet, headerStyle, "医生姓名", "设备编码", "所属机构", "所属区域", "活跃状态", "问诊次数", "有效问诊数", "最后活跃时间");
            for (int i = 0; i < users.size(); i++) {
                UserActivityItemVO item = users.get(i);
                writeRow(
                    userSheet,
                    i + 1,
                    item.getNaDoctor(),
                    item.getCdDevice(),
                    item.getNaOrg(),
                    item.getNaRegion(),
                    "active".equals(item.getActiveStatus()) ? "活跃" : "不活跃",
                    item.getConsultationCount(),
                    item.getEffectiveConsultationCount(),
                    item.getLastActiveTime()
                );
            }
            autoSize(userSheet, 8);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new BusinessException("导出Excel失败：" + ex.getMessage());
        }
    }

    private void normalizeDateRange(UserActivityQueryDTO query) {
        if (query.getDateFrom() != null && !query.getDateFrom().isEmpty()
            && query.getDateTo() != null && !query.getDateTo().isEmpty()) {
            return;
        }

        LocalDate now = LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE;
        String range = query.getTimeRange();

        if ("today".equals(range)) {
            query.setDateFrom(now.format(fmt));
            query.setDateTo(now.format(fmt));
        } else if ("week".equals(range)) {
            query.setDateFrom(now.minusDays(now.getDayOfWeek().getValue() - 1L).format(fmt));
            query.setDateTo(now.format(fmt));
        } else if ("month".equals(range) || range == null || range.isEmpty()) {
            query.setDateFrom(now.withDayOfMonth(1).format(fmt));
            query.setDateTo(now.format(fmt));
        } else if ("quarter".equals(range)) {
            int quarterStartMonth = ((now.getMonthValue() - 1) / 3) * 3 + 1;
            query.setDateFrom(LocalDate.of(now.getYear(), quarterStartMonth, 1).format(fmt));
            query.setDateTo(now.format(fmt));
        } else if ("year".equals(range)) {
            query.setDateFrom(LocalDate.of(now.getYear(), 1, 1).format(fmt));
            query.setDateTo(now.format(fmt));
        } else {
            query.setDateFrom(now.withDayOfMonth(1).format(fmt));
            query.setDateTo(now.format(fmt));
        }
    }

    private UserActivityQueryDTO buildPreviousPeriodQuery(UserActivityQueryDTO current) {
        UserActivityQueryDTO prev = new UserActivityQueryDTO();
        prev.setIdRegion(current.getIdRegion());
        prev.setIdOrg(current.getIdOrg());
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
            if ("today".equals(range)) {
                LocalDate yesterday = from.minusDays(1);
                prev.setDateFrom(yesterday.format(fmt));
                prev.setDateTo(yesterday.format(fmt));
            } else if ("week".equals(range)) {
                LocalDate prevWeekEnd = from.minusDays(1);
                LocalDate prevWeekStart = prevWeekEnd.minusDays(6);
                prev.setDateFrom(prevWeekStart.format(fmt));
                prev.setDateTo(prevWeekEnd.format(fmt));
            } else if ("month".equals(range) || range == null || range.isEmpty()) {
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
        } catch (Exception ex) {
            log.debug("user activity previous period calculation failed. error={}", ex.getMessage());
        }
        fillDateBounds(prev);
        return prev;
    }

    private String findParentId(List<Map<String, Object>> allRegions, String id) {
        for (Map<String, Object> row : allRegions) {
            if (String.valueOf(mapValue(row, "ID", "id")).equals(id)) {
                Object parentId = mapValue(row, "PARENTID", "parentid");
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

    private String stringVal(Object obj) {
        return obj != null ? String.valueOf(obj) : null;
    }

    private long longVal(Object obj) {
        if (obj instanceof Number) {
            return ((Number) obj).longValue();
        }
        return 0L;
    }

    private void fillDateBounds(UserActivityQueryDTO query) {
        if (query == null) {
            return;
        }
        query.setDateFromTime(parseDateStart(query.getDateFrom()));
        query.setDateToExclusiveTime(parseDateEndExclusive(query.getDateTo()));
    }

    private LocalDateTime parseDateStart(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay();
        } catch (Exception ex) {
            return null;
        }
    }

    private LocalDateTime parseDateEndExclusive(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE).plusDays(1).atStartOfDay();
        } catch (Exception ex) {
            return null;
        }
    }

    private Object mapValue(Map<String, Object> row, String upperKey, String lowerKey) {
        Object value = row.get(upperKey);
        return value != null ? value : row.get(lowerKey);
    }

    private static CellStyle createHeaderStyle(XSSFWorkbook workbook) {
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        return headerStyle;
    }

    private static void writeHeader(XSSFSheet sheet, CellStyle headerStyle, String... headers) {
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            createCell(headerRow, i, headers[i], headerStyle);
        }
    }

    private static void writeRow(XSSFSheet sheet, int rowIndex, Object... values) {
        Row row = sheet.createRow(rowIndex);
        for (int i = 0; i < values.length; i++) {
            createCell(row, i, values[i], null);
        }
    }

    private static void createCell(Row row, int columnIndex, Object value, CellStyle style) {
        Cell cell = row.createCell(columnIndex);
        if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else {
            cell.setCellValue(value != null ? String.valueOf(value) : "");
        }
        if (style != null) {
            cell.setCellStyle(style);
        }
    }

    private static void autoSize(XSSFSheet sheet, int columns) {
        for (int i = 0; i < columns; i++) {
            sheet.autoSizeColumn(i);
        }
    }
}
