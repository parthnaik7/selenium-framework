package com.framework.data;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.annotations.DataProvider;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * JsonDataProvider - Load test data from JSON files on the classpath.
 *
 * Supported JSON structures:
 *
 *  1. Array of objects  → each object becomes one test row:
 *     <pre>[ {"user":"admin","pass":"secret"}, {"user":"guest","pass":"abc"} ]</pre>
 *
 *  2. Named datasets    → top-level object containing arrays:
 *     <pre>{ "validLogins": [...], "invalidLogins": [...] }</pre>
 *
 * Files live in {@code src/test/resources/testdata/}.
 *
 * Usage:
 * <pre>
 *   @Test(dataProvider = "loginJsonData", dataProviderClass = JsonDataProvider.class)
 *   public void loginTest(Map&lt;String, Object&gt; data) { ... }
 * </pre>
 */
public final class JsonDataProvider {

    private static final Logger log = LogManager.getLogger(JsonDataProvider.class);
    private static final String TESTDATA_DIR = "testdata/";
    private static final ObjectMapper MAPPER  = new ObjectMapper();

    private JsonDataProvider() {}

    // ─────────────────────────────────────────────────────────────────────────
    //  Built-in DataProviders
    // ─────────────────────────────────────────────────────────────────────────

    @DataProvider(name = "loginJsonData")
    public static Object[][] loginJsonData() {
        return toObjectArray(getArrayData("login_data.json"));
    }

    @DataProvider(name = "searchJsonData")
    public static Object[][] searchJsonData() {
        return toObjectArray(getArrayData("search_data.json"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Core Reading API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Read a JSON file that contains an array at the top level.
     * Each element becomes one {@code Map<String,Object>} data row.
     *
     * @param fileName filename inside {@code src/test/resources/testdata/}
     */
    public static List<Map<String, Object>> getArrayData(String fileName) {
        String path = TESTDATA_DIR + fileName;
        try (InputStream is = JsonDataProvider.class.getClassLoader().getResourceAsStream(path)) {
            if (is == null) throw new IllegalArgumentException("File not found on classpath: " + path);
            List<Map<String, Object>> data =
                    MAPPER.readValue(is, new TypeReference<List<Map<String, Object>>>() {});
            log.info("Loaded {} records from {}", data.size(), fileName);
            return data;
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse JSON file: " + path, e);
        }
    }

    /**
     * Read a named array from a JSON file that has an object at the top level.
     *
     * @param fileName  e.g. "users.json"
     * @param arrayKey  e.g. "validLogins"
     */
    public static List<Map<String, Object>> getNamedArray(String fileName, String arrayKey) {
        String path = TESTDATA_DIR + fileName;
        try (InputStream is = JsonDataProvider.class.getClassLoader().getResourceAsStream(path)) {
            if (is == null) throw new IllegalArgumentException("File not found: " + path);
            Map<String, List<Map<String, Object>>> root =
                    MAPPER.readValue(is, new TypeReference<Map<String, List<Map<String, Object>>>>() {});
            List<Map<String, Object>> data = root.get(arrayKey);
            if (data == null) throw new IllegalArgumentException(
                    "Key '" + arrayKey + "' not found in " + fileName);
            log.info("Loaded {} records from {}/{}", data.size(), fileName, arrayKey);
            return data;
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse JSON: " + path, e);
        }
    }

    /**
     * Deserialise a JSON file directly into a POJO class.
     *
     * @param fileName  filename in testdata dir
     * @param clazz     target type
     */
    public static <T> T readAs(String fileName, Class<T> clazz) {
        String path = TESTDATA_DIR + fileName;
        try (InputStream is = JsonDataProvider.class.getClassLoader().getResourceAsStream(path)) {
            if (is == null) throw new IllegalArgumentException("File not found: " + path);
            return MAPPER.readValue(is, clazz);
        } catch (IOException e) {
            throw new RuntimeException("Failed to deserialise JSON: " + path, e);
        }
    }

    /**
     * Read raw JSON as a generic Map (for deeply nested structures).
     */
    public static Map<String, Object> readAsMap(String fileName) {
        String path = TESTDATA_DIR + fileName;
        try (InputStream is = JsonDataProvider.class.getClassLoader().getResourceAsStream(path)) {
            if (is == null) throw new IllegalArgumentException("File not found: " + path);
            return MAPPER.readValue(is, new TypeReference<Map<String, Object>>() {});
        } catch (IOException e) {
            throw new RuntimeException("Failed to read JSON map: " + path, e);
        }
    }

    /**
     * Filter data rows where a key equals a value.
     */
    public static List<Map<String, Object>> filterBy(List<Map<String, Object>> data,
                                                       String key, Object value) {
        List<Map<String, Object>> filtered = new ArrayList<>();
        for (Map<String, Object> row : data) {
            Object v = row.get(key);
            if (v != null && v.toString().equalsIgnoreCase(value.toString())) filtered.add(row);
        }
        return filtered;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Conversion
    // ─────────────────────────────────────────────────────────────────────────

    /** Convert list of maps to TestNG DataProvider 2D array. */
    public static Object[][] toObjectArray(List<Map<String, Object>> data) {
        Object[][] result = new Object[data.size()][1];
        for (int i = 0; i < data.size(); i++) result[i][0] = data.get(i);
        return result;
    }

    /** Serialise any object to a JSON string (useful for logging). */
    public static String toJson(Object obj) {
        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (Exception e) {
            return obj.toString();
        }
    }
}
