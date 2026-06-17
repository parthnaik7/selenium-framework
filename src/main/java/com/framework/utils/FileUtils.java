package com.framework.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.Properties;

/**
 * FileUtils - Reading/writing files for test data, configs, and artefacts.
 */
public final class FileUtils {

    private static final Logger log = LogManager.getLogger(FileUtils.class);

    private FileUtils() {}

    /** Read classpath resource as a String (e.g. JSON test data files). */
    public static String readClasspathResource(String resourcePath) {
        try (InputStream is = FileUtils.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) throw new IllegalArgumentException("Resource not found: " + resourcePath);
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read classpath resource: " + resourcePath, e);
        }
    }

    /** Read file from a filesystem path as a String. */
    public static String readFile(String filePath) {
        try {
            return Files.readString(Path.of(filePath), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file: " + filePath, e);
        }
    }

    /** Read file as list of lines. */
    public static List<String> readLines(String filePath) {
        try {
            return Files.readAllLines(Path.of(filePath), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read lines from: " + filePath, e);
        }
    }

    /** Write content to a file (creating parent dirs as needed). */
    public static void writeFile(String filePath, String content) {
        try {
            Path path = Path.of(filePath);
            Files.createDirectories(path.getParent());
            Files.writeString(path, content, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            log.info("Wrote file: {}", filePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write file: " + filePath, e);
        }
    }

    /** Append content to an existing file. */
    public static void appendFile(String filePath, String content) {
        try {
            Files.writeString(Path.of(filePath), content, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new RuntimeException("Failed to append to file: " + filePath, e);
        }
    }

    /** Check if file exists at path. */
    public static boolean fileExists(String filePath) {
        return Files.exists(Path.of(filePath));
    }

    /** Load properties from a classpath resource. */
    public static Properties loadProperties(String resourcePath) {
        Properties props = new Properties();
        try (InputStream is = FileUtils.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) throw new IllegalArgumentException("Resource not found: " + resourcePath);
            props.load(is);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load properties: " + resourcePath, e);
        }
        return props;
    }

    /** Create directories (no-op if already exists). */
    public static void createDirs(String dirPath) {
        try {
            Files.createDirectories(Path.of(dirPath));
        } catch (IOException e) {
            throw new RuntimeException("Failed to create directories: " + dirPath, e);
        }
    }

    /** Delete a file silently (no exception if missing). */
    public static void deleteQuietly(String filePath) {
        try {
            Files.deleteIfExists(Path.of(filePath));
        } catch (IOException e) {
            log.warn("Could not delete {}: {}", filePath, e.getMessage());
        }
    }

    /** Get the absolute path to a classpath resource (useful for upload fields). */
    public static String getAbsoluteResourcePath(String resourceName) {
        var url = FileUtils.class.getClassLoader().getResource(resourceName);
        if (url == null) throw new IllegalArgumentException("Resource not found: " + resourceName);
        return Path.of(url.getPath()).toAbsolutePath().toString();
    }
}
