package com.framework.utils;

import io.qameta.allure.Allure;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * ScreenshotUtils - Centralised screenshot handling.
 *
 * Captures screenshots as raw bytes (for Allure) and as files (for Extent Reports,
 * CI artefacts, debugging). Never throws - failure to capture is logged but not fatal.
 */
public final class ScreenshotUtils {

    private static final Logger log = LogManager.getLogger(ScreenshotUtils.class);
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");
    private static final String SCREENSHOTS_DIR = "target/screenshots";

    private ScreenshotUtils() {}

    // ─────────────────────────────────────────────────────────────────────────
    //  Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Capture screenshot as a byte array.
     *
     * @return PNG bytes, or {@code null} if capture failed.
     */
    public static byte[] captureAsBytes(WebDriver driver) {
        try {
            return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
        } catch (Exception e) {
            log.warn("Failed to capture screenshot: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Capture and attach the screenshot to the current Allure report step.
     *
     * @param name label for the Allure attachment
     */
    public static void captureAndAttachToAllure(WebDriver driver, String name) {
        try {
            byte[] bytes = captureAsBytes(driver);
            if (bytes != null) {
                Allure.addAttachment(name, "image/png", new ByteArrayInputStream(bytes), "png");
                log.debug("Screenshot '{}' attached to Allure", name);
            }
        } catch (Exception e) {
            log.warn("Failed to attach screenshot to Allure: {}", e.getMessage());
        }
    }

    /**
     * Capture and save the screenshot to {@code target/screenshots/<name>_<timestamp>.png}.
     *
     * @return absolute path to the saved file, or {@code null} on failure
     */
    public static String captureAndSave(WebDriver driver, String name) {
        try {
            byte[] bytes = captureAsBytes(driver);
            if (bytes == null) return null;

            Path dir = Paths.get(SCREENSHOTS_DIR);
            Files.createDirectories(dir);

            String filename = sanitise(name) + "_" + LocalDateTime.now().format(TIMESTAMP) + ".png";
            Path file = dir.resolve(filename);
            Files.write(file, bytes);

            log.info("Screenshot saved: {}", file.toAbsolutePath());
            return file.toAbsolutePath().toString();

        } catch (IOException e) {
            log.warn("Failed to save screenshot '{}': {}", name, e.getMessage());
            return null;
        }
    }

    /**
     * Capture, save to file AND attach to Allure - everything in one call.
     *
     * @return file path (may be null if save failed)
     */
    public static String captureAllAndSave(WebDriver driver, String name) {
        captureAndAttachToAllure(driver, name);
        return captureAndSave(driver, name);
    }

    /**
     * Capture a screenshot of a specific element only.
     *
     * @return PNG bytes, or null on failure
     */
    public static byte[] captureElement(WebElement element) {
        try {
            return element.getScreenshotAs(OutputType.BYTES);
        } catch (Exception e) {
            log.warn("Failed to capture element screenshot: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Capture element screenshot and attach to Allure.
     */
    public static void captureElementAndAttach(WebElement element, String name) {
        try {
            byte[] bytes = captureElement(element);
            if (bytes != null) {
                Allure.addAttachment(name, "image/png", new ByteArrayInputStream(bytes), "png");
            }
        } catch (Exception e) {
            log.warn("Failed to attach element screenshot '{}': {}", name, e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private static String sanitise(String name) {
        return name.replaceAll("[^a-zA-Z0-9_\\-]", "_");
    }
}
