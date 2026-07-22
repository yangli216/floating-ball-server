package com.regionalai.floatingball.server.common.util;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

public final class ExcelColumnWidthUtils {

    private static final int MIN_WIDTH = 10;
    private static final int MAX_WIDTH = 60;
    private static final int WIDTH_PADDING = 2;

    private ExcelColumnWidthUtils() {
    }

    public static void fitColumns(Sheet sheet, int columns) {
        for (int columnIndex = 0; columnIndex < columns; columnIndex++) {
            int width = MIN_WIDTH;
            for (Row row : sheet) {
                Cell cell = row.getCell(columnIndex);
                if (cell != null) {
                    width = Math.max(width, estimateDisplayWidth(cell.toString()) + WIDTH_PADDING);
                }
            }
            sheet.setColumnWidth(columnIndex, Math.min(width, MAX_WIDTH) * 256);
        }
    }

    static int estimateDisplayWidth(String value) {
        int width = 0;
        for (int offset = 0; offset < value.length(); ) {
            int codePoint = value.codePointAt(offset);
            width += codePoint <= 0xFF ? 1 : 2;
            offset += Character.charCount(codePoint);
        }
        return width;
    }
}
