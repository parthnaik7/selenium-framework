package com.framework.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * ConfigReader - Centralised configuration access.
 *
 * Load order (later wins):
 *   1. src/main/resources/config.properties
 *   2. src/main/resources/config-{env}.properties  (env = system prop "env")
 *   3. JVM system properties  (-Dkey=value on command line / CI)
 *
 * This means CI/CD can always override any property without changing source.
 *
 * Principle: Single Source of Truth for config values.
 */
public final class ConfigReader {

    private static final Logger    log   = LogManager.getLogger(ConfigReader.class);
    private static final Properties props = new Properties();

    static {
        loadFile("config.properties");          // base defaults

        String env = System.getProperty("env"); // e.g. -Denv=staging
        if (env != null && !env.isBlank()) {
            loadFile("config-" + env.toLowerCase() + ".properties");
        }
    }

    private ConfigReader() {}

    // ─────────────────────────────────────────────────────────────────────────
    //  Public API
    // ─────────────────────────────────────────────────────────────────────────

    /** Returns the value for {@code key}, or {@code defaultValue} if missing. */
    public static String get(String key, String defaultValue) {
        // JVM -D flag wins over config file
        String sysProp = System.getProperty(key);
        if (sysProp != null) return sysProp;
        return props.getProperty(key, defaultValue);
    }

    /** Returns the value for {@code key}. Throws if absent. */
    public static String getRequired(String key) {
        String value = get(key, null);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Required config property '" + key + "' is not set.");
        }
        return value;
    }

    public static int getInt(String key, int defaultValue) {
        try { return Integer.parseInt(get(key, String.valueOf(defaultValue))); }
        catch (NumberFormatException e) { return defaultValue; }
    }

    public static long getLong(String key, long defaultValue) {
        try { return Long.parseLong(get(key, String.valueOf(defaultValue))); }
        catch (NumberFormatException e) { return defaultValue; }
    }

    public static boolean getBool(String key, boolean defaultValue) {
        return Boolean.parseBoolean(get(key, String.valueOf(defaultValue)));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private static void loadFile(String filename) {
        try (InputStream is = ConfigReader.class.getClassLoader().getResourceAsStream(filename)) {
            if (is == null) {
                log.debug("Config file '{}' not found on classpath - skipping.", filename);
                return;
            }
            Properties temp = new Properties();
            temp.load(is);
            props.putAll(temp);
            log.info("Loaded config: {}", filename);
        } catch (IOException e) {
            log.error("Failed to load config file '{}': {}", filename, e.getMessage());
        }
    }
}
