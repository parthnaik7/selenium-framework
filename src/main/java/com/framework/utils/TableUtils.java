package com.framework.utils;

import io.qameta.allure.Step;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.*;

/**
 * TableUtils - Generic HTML table interactions.
 *
 * Works with standard {@code <table>/<thead>/<tbody>/<tr>/<td>/<th>} markup.
 * All methods accept a root table {@link By} locator and operate only within it,
 * so multiple tables on a page are handled independently.
 */
public class TableUtils {

    private static final Logger log = LogManager.getLogger(TableUtils.class);

    private static final By ROW_SELECTOR    = By.tagName("tr");
    private static final By HEADER_SELECTOR = By.tagName("th");
    private static final By CELL_SELECTOR   = By.tagName("td");

    private final WebDriver driver;

    public TableUtils(WebDriver driver) {
        this.driver = driver;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Row / Column Counts
    // ─────────────────────────────────────────────────────────────────────────

    @Step("Get row count of table: {tableLocator}")
    public int getRowCount(By tableLocator) {
        WebElement table = driver.findElement(tableLocator);
        // Exclude header row if in <thead>
        List<WebElement> rows = table.findElements(By.cssSelector("tbody tr"));
        if (rows.isEmpty()) {
            rows = table.findElements(ROW_SELECTOR);
        }
        int count = rows.size();
        log.debug("Table {} has {} body rows", tableLocator, count);
        return count;
    }

    @Step("Get column count of table: {tableLocator}")
    public int getColumnCount(By tableLocator) {
        WebElement table = driver.findElement(tableLocator);
        WebElement firstRow = table.findElements(ROW_SELECTOR).get(0);
        int count = firstRow.findElements(HEADER_SELECTOR).size();
        if (count == 0) count = firstRow.findElements(CELL_SELECTOR).size();
        log.debug("Table {} has {} columns", tableLocator, count);
        return count;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Header Reading
    // ─────────────────────────────────────────────────────────────────────────

    @Step("Get column headers of table: {tableLocator}")
    public List<String> getHeaders(By tableLocator) {
        WebElement table = driver.findElement(tableLocator);
        List<String> headers = new ArrayList<>();
        // Try <thead th> first, fall back to first <tr th>
        List<WebElement> ths = table.findElements(By.cssSelector("thead th"));
        if (ths.isEmpty()) {
            ths = table.findElements(ROW_SELECTOR).get(0).findElements(HEADER_SELECTOR);
        }
        ths.forEach(th -> headers.add(th.getText().trim()));
        log.debug("Headers: {}", headers);
        return headers;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Cell Reading
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Get text of a specific cell (1-based row and column index).
     *
     * @param tableLocator the root table locator
     * @param row          1-based row index (body rows only)
     * @param col          1-based column index
     */
    @Step("Get cell text at row {row}, col {col}")
    public String getCellText(By tableLocator, int row, int col) {
        WebElement cell = getCell(tableLocator, row, col);
        String text = cell.getText().trim();
        log.debug("Cell[{},{}] = '{}'", row, col, text);
        return text;
    }

    /** Get the WebElement for a specific cell. */
    public WebElement getCell(By tableLocator, int row, int col) {
        List<WebElement> rows = getBodyRows(tableLocator);
        if (row < 1 || row > rows.size()) {
            throw new IndexOutOfBoundsException(
                    "Row " + row + " out of bounds. Table has " + rows.size() + " rows.");
        }
        List<WebElement> cells = rows.get(row - 1).findElements(CELL_SELECTOR);
        if (col < 1 || col > cells.size()) {
            throw new IndexOutOfBoundsException(
                    "Column " + col + " out of bounds. Row has " + cells.size() + " cells.");
        }
        return cells.get(col - 1);
    }

    /** Get all text values in a column by column index (1-based). */
    @Step("Get all values in column {colIndex}")
    public List<String> getColumnValues(By tableLocator, int colIndex) {
        List<String> values = new ArrayList<>();
        for (WebElement row : getBodyRows(tableLocator)) {
            List<WebElement> cells = row.findElements(CELL_SELECTOR);
            if (colIndex <= cells.size()) {
                values.add(cells.get(colIndex - 1).getText().trim());
            }
        }
        return values;
    }

    /** Get all text values in a column by header name. */
    @Step("Get all values in column '{columnHeader}'")
    public List<String> getColumnValues(By tableLocator, String columnHeader) {
        int colIndex = getColumnIndex(tableLocator, columnHeader);
        return getColumnValues(tableLocator, colIndex);
    }

    /** Get all text in a specific row (1-based). */
    @Step("Get all cells in row {rowIndex}")
    public List<String> getRowValues(By tableLocator, int rowIndex) {
        List<WebElement> rows = getBodyRows(tableLocator);
        List<String> values = new ArrayList<>();
        rows.get(rowIndex - 1).findElements(CELL_SELECTOR)
                .forEach(td -> values.add(td.getText().trim()));
        return values;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Full Table Extraction
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Extract the entire table as a list of maps: column header → cell value.
     * Ideal for data-driven assertions.
     *
     * @return List of rows where each row is a LinkedHashMap preserving column order.
     */
    @Step("Extract full table data: {tableLocator}")
    public List<Map<String, String>> getTableData(By tableLocator) {
        List<String> headers = getHeaders(tableLocator);
        List<Map<String, String>> data = new ArrayList<>();

        for (WebElement row : getBodyRows(tableLocator)) {
            List<WebElement> cells = row.findElements(CELL_SELECTOR);
            Map<String, String> rowMap = new LinkedHashMap<>();
            for (int i = 0; i < headers.size(); i++) {
                String cellText = (i < cells.size()) ? cells.get(i).getText().trim() : "";
                rowMap.put(headers.get(i), cellText);
            }
            data.add(rowMap);
        }
        log.debug("Extracted {} rows from table {}", data.size(), tableLocator);
        return data;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Searching
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Find rows where a specific column contains the given text.
     *
     * @return List of row maps matching the search
     */
    @Step("Find rows where column '{columnHeader}' contains '{value}'")
    public List<Map<String, String>> findRowsByColumnValue(By tableLocator,
                                                            String columnHeader,
                                                            String value) {
        List<Map<String, String>> results = new ArrayList<>();
        for (Map<String, String> row : getTableData(tableLocator)) {
            String cellValue = row.getOrDefault(columnHeader, "");
            if (cellValue.contains(value)) results.add(row);
        }
        log.debug("Found {} rows where '{}' contains '{}'",
                results.size(), columnHeader, value);
        return results;
    }

    /**
     * Find the row index (1-based) of the first row where a column equals a value.
     *
     * @return row index, or -1 if not found
     */
    @Step("Find row number where column '{columnHeader}' = '{value}'")
    public int findRowNumber(By tableLocator, String columnHeader, String value) {
        int colIndex = getColumnIndex(tableLocator, columnHeader);
        List<WebElement> rows = getBodyRows(tableLocator);
        for (int i = 0; i < rows.size(); i++) {
            List<WebElement> cells = rows.get(i).findElements(CELL_SELECTOR);
            if (colIndex <= cells.size()
                    && cells.get(colIndex - 1).getText().trim().equals(value)) {
                log.debug("Row with '{}={}' found at index {}", columnHeader, value, i + 1);
                return i + 1;
            }
        }
        log.debug("No row found where '{}={}'", columnHeader, value);
        return -1;
    }

    /** Check whether any cell in the table contains the given text. */
    @Step("Table contains text '{text}'")
    public boolean tableContainsText(By tableLocator, String text) {
        WebElement table = driver.findElement(tableLocator);
        return table.getText().contains(text);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Actions on table cells
    // ─────────────────────────────────────────────────────────────────────────

    /** Click a cell at the given row/column. */
    @Step("Click cell at row {row}, col {col}")
    public void clickCell(By tableLocator, int row, int col) {
        getCell(tableLocator, row, col).click();
    }

    /** Click an element inside a cell (e.g. a button or link). */
    @Step("Click element in cell at row {row}, col {col}")
    public void clickElementInCell(By tableLocator, int row, int col, By innerLocator) {
        getCell(tableLocator, row, col).findElement(innerLocator).click();
    }

    /** Click the action button (matched by inner locator) in the row where columnHeader=value. */
    @Step("Click action in row where '{columnHeader}'='{value}'")
    public void clickActionInRow(By tableLocator, String columnHeader,
                                  String value, By actionLocator) {
        int rowNum = findRowNumber(tableLocator, columnHeader, value);
        if (rowNum < 0) throw new NoSuchElementException(
                "No row where " + columnHeader + " = " + value);
        clickElementInCell(tableLocator, rowNum,
                getColumnCount(tableLocator), actionLocator);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Sorting Verification
    // ─────────────────────────────────────────────────────────────────────────

    /** Verify that a column is sorted in ascending order (lexicographic). */
    @Step("Verify column '{columnHeader}' is sorted ascending")
    public boolean isColumnSortedAscending(By tableLocator, String columnHeader) {
        List<String> values = getColumnValues(tableLocator, columnHeader);
        for (int i = 0; i < values.size() - 1; i++) {
            if (values.get(i).compareToIgnoreCase(values.get(i + 1)) > 0) return false;
        }
        return true;
    }

    /** Verify that a column is sorted in descending order (lexicographic). */
    @Step("Verify column '{columnHeader}' is sorted descending")
    public boolean isColumnSortedDescending(By tableLocator, String columnHeader) {
        List<String> values = getColumnValues(tableLocator, columnHeader);
        for (int i = 0; i < values.size() - 1; i++) {
            if (values.get(i).compareToIgnoreCase(values.get(i + 1)) < 0) return false;
        }
        return true;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Internal helpers
    // ─────────────────────────────────────────────────────────────────────────

    private List<WebElement> getBodyRows(By tableLocator) {
        WebElement table = driver.findElement(tableLocator);
        List<WebElement> rows = table.findElements(By.cssSelector("tbody tr"));
        if (rows.isEmpty()) {
            // No <tbody> - get all rows and skip header
            rows = table.findElements(ROW_SELECTOR);
            if (!rows.isEmpty() && !rows.get(0).findElements(HEADER_SELECTOR).isEmpty()) {
                rows = rows.subList(1, rows.size());
            }
        }
        return rows;
    }

    private int getColumnIndex(By tableLocator, String headerName) {
        List<String> headers = getHeaders(tableLocator);
        int idx = headers.indexOf(headerName);
        if (idx < 0) throw new NoSuchElementException(
                "Column header '" + headerName + "' not found. Available: " + headers);
        return idx + 1; // 1-based
    }
}
