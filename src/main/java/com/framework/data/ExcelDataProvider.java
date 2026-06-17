package com.framework.data;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.testng.annotations.DataProvider;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * ExcelDataProvider - Read test data from Excel (.xlsx) workbooks.
 *
 * Conventions:
 *  - Row 0 = column headers
 *  - Rows 1..N = data rows
 *  - Blank rows are skipped automatically
 *  - Files live in {@code src/test/resources/testdata/}
 *
 * Usage in a test class:
 * <pre>
 *   @Test(dataProvider = "loginData", dataProviderClass = ExcelDataProvider.class)
 *   public void loginTest(Map&lt;String, String&gt; data) {
 *       loginPage.login(data.get("username"), data.get("password"));
 *   }
 * </pre>
 *
 * For custom files / sheets, use the static {@link #getSheetData(String, String)} method
 * and wrap it in your own @DataProvider.
 */
public final class ExcelDataProvider {

    private static final Logger log = LogManager.getLogger(ExcelDataProvider.class);
    private static final String TESTDATA_DIR = "testdata/";

    private ExcelDataProvider() {}

    // ─────────────────────────────────────────────────────────────────────────
    //  Built-in DataProviders (extend as needed per project)
    // ─────────────────────────────────────────────────────────────────────────

    @DataProvider(name = "loginData", parallel = false)
    public static Object[][] loginData() {
        return toObjectArray(getSheetData("LoginData.xlsx", "LoginCreds"));
    }

    @DataProvider(name = "registrationData", parallel = false)
    public static Object[][] registrationData() {
        return toObjectArray(getSheetData("RegistrationData.xlsx", "Users"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Core Reading API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Read all data rows from a sheet as a list of header→value maps.
     *
     * @param fileName  filename inside {@code src/test/resources/testdata/}
     * @param sheetName name of the sheet tab
     * @return list of row maps (header name → cell value as String)
     */
    public static List<Map<String, String>> getSheetData(String fileName, String sheetName) {
        List<Map<String, String>> data = new ArrayList<>();
        String path = TESTDATA_DIR + fileName;

        try (InputStream is = ExcelDataProvider.class.getClassLoader().getResourceAsStream(path);
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) throw new IllegalArgumentException(
                    "Sheet '" + sheetName + "' not found in " + fileName);

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) return data;
            List<String> headers = readRowAsStrings(headerRow);

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null || isRowBlank(row)) continue;

                Map<String, String> rowMap = new LinkedHashMap<>();
                for (int c = 0; c < headers.size(); c++) {
                    Cell cell = row.getCell(c, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    rowMap.put(headers.get(c), getCellValueAsString(cell));
                }
                data.add(rowMap);
            }
            log.info("Loaded {} rows from {}/{}", data.size(), fileName, sheetName);

        } catch (IOException e) {
            throw new RuntimeException("Failed to read Excel file: " + path, e);
        }
        return data;
    }

    /**
     * Read a single cell value by its header name and row index.
     *
     * @param rowIndex 0-based data row (excludes header row)
     */
    public static String getCellValue(String fileName, String sheetName,
                                       int rowIndex, String headerName) {
        List<Map<String, String>> data = getSheetData(fileName, sheetName);
        if (rowIndex >= data.size()) throw new IndexOutOfBoundsException(
                "Row " + rowIndex + " exceeds data size " + data.size());
        return data.get(rowIndex).getOrDefault(headerName, "");
    }

    /** Get all values in a specific column across all rows. */
    public static List<String> getColumnValues(String fileName, String sheetName,
                                                String headerName) {
        List<String> values = new ArrayList<>();
        getSheetData(fileName, sheetName).forEach(row ->
                values.add(row.getOrDefault(headerName, "")));
        return values;
    }

    /** Get only rows where {@code filterColumn} equals {@code filterValue}. */
    public static List<Map<String, String>> getFilteredData(String fileName, String sheetName,
                                                             String filterColumn, String filterValue) {
        List<Map<String, String>> filtered = new ArrayList<>();
        for (Map<String, String> row : getSheetData(fileName, sheetName)) {
            if (filterValue.equalsIgnoreCase(row.get(filterColumn))) filtered.add(row);
        }
        return filtered;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Conversion Helpers
    // ─────────────────────────────────────────────────────────────────────────

    /** Convert list of row-maps to a 2D Object array for TestNG DataProvider. */
    public static Object[][] toObjectArray(List<Map<String, String>> data) {
        Object[][] result = new Object[data.size()][1];
        for (int i = 0; i < data.size(); i++) {
            result[i][0] = data.get(i);
        }
        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Private Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private static List<String> readRowAsStrings(Row row) {
        List<String> cells = new ArrayList<>();
        row.forEach(cell -> cells.add(getCellValueAsString(cell).trim()));
        return cells;
    }

    private static String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue().trim();
            case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                    ? cell.getLocalDateTimeCellValue().toString()
                    : String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCachedFormulaResultType() == CellType.NUMERIC
                    ? String.valueOf((long) cell.getNumericCellValue())
                    : cell.getStringCellValue();
            default      -> "";
        };
    }

    private static boolean isRowBlank(Row row) {
        for (Cell cell : row) {
            if (cell != null && cell.getCellType() != CellType.BLANK
                    && !getCellValueAsString(cell).isBlank()) return false;
        }
        return true;
    }
}
