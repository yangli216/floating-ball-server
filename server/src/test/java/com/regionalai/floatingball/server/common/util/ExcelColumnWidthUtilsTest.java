package com.regionalai.floatingball.server.common.util;

import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExcelColumnWidthUtilsTest {

    @Test
    void fitColumnsShouldEstimateWidthsWithoutFontMetricsAndApplyLimits() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("export");
            sheet.createRow(0).createCell(0).setCellValue("机构");
            sheet.getRow(0).createCell(1).setCellValue("ID");
            sheet.getRow(0).createCell(2).setCellValue("long");
            sheet.createRow(1).createCell(0).setCellValue("区域中心医院");
            sheet.getRow(1).createCell(2).setCellValue(repeat("x", 100));

            ExcelColumnWidthUtils.fitColumns(sheet, 3);

            assertEquals(14 * 256, sheet.getColumnWidth(0));
            assertEquals(10 * 256, sheet.getColumnWidth(1));
            assertEquals(60 * 256, sheet.getColumnWidth(2));
        }
    }

    private static String repeat(String value, int count) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < count; i++) {
            result.append(value);
        }
        return result.toString();
    }
}
