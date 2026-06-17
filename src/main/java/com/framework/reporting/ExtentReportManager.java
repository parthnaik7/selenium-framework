package com.framework.reporting;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.MediaEntityBuilder;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import com.framework.config.ConfigReader;
import com.framework.utils.DateUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.*;
import java.util.Base64;

/**
 * ExtentReportManager - Thread-safe HTML Extent Report management.
 *
 * Uses a ThreadLocal ExtentTest so parallel test classes each get their own
 * test node without data races. A single ExtentReports instance is shared
 * (with synchronised write) and flushed after the suite completes.
 *
 * Output: target/extent-report/ExtentReport_<timestamp>.html
 */
public final class ExtentReportManager {

    private static final Logger log = LogManager.getLogger(ExtentReportManager.class);

    private static ExtentReports extent;
    private static final ThreadLocal<ExtentTest> testHolder = new ThreadLocal<>();

    private static final String REPORT_DIR  = "target/extent-report";
    private static final String REPORT_NAME = "ExtentReport_" + DateUtils.timestamp() + ".html";

    private ExtentReportManager() {}

    // ─────────────────────────────────────────────────────────────────────────
    //  Lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    /** Called once per suite. Creates the Spark (HTML) reporter. */
    public static synchronized void initReport() {
        try {
            Files.createDirectories(Path.of(REPORT_DIR));
        } catch (IOException e) {
            log.warn("Could not create report directory: {}", e.getMessage());
        }

        String reportPath = REPORT_DIR + "/" + REPORT_NAME;
        ExtentSparkReporter spark = new ExtentSparkReporter(reportPath);

        spark.config().setDocumentTitle(
                ConfigReader.get("report.title", "Selenium Automation Report"));
        spark.config().setReportName(
                ConfigReader.get("report.name", "Test Execution Report"));
        spark.config().setTheme(Theme.DARK);
        spark.config().setEncoding("UTF-8");
        spark.config().setTimeStampFormat("dd MMM yyyy HH:mm:ss");

        extent = new ExtentReports();
        extent.attachReporter(spark);

        extent.setSystemInfo("Environment",  ConfigReader.get("env",      "default"));
        extent.setSystemInfo("Browser",      ConfigReader.get("browser",  "chrome"));
        extent.setSystemInfo("Base URL",     ConfigReader.get("base.url", ""));
        extent.setSystemInfo("OS",           System.getProperty("os.name"));
        extent.setSystemInfo("Java Version", System.getProperty("java.version"));

        log.info("Extent Report initialised: {}", reportPath);
    }

    /** Flush all data to disk. Call once at suite end. */
    public static synchronized void flushReport() {
        if (extent != null) {
            extent.flush();
            log.info("Extent Report flushed.");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Test Node Management
    // ─────────────────────────────────────────────────────────────────────────

    public static synchronized void createTest(String testName, String description) {
        if (extent == null) return;
        ExtentTest test = (description != null && !description.isBlank())
                ? extent.createTest(testName, description)
                : extent.createTest(testName);
        testHolder.set(test);
    }

    public static ExtentTest getTest() {
        return testHolder.get();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Result Marking
    // ─────────────────────────────────────────────────────────────────────────

    public static void markPass(String message) {
        ExtentTest test = testHolder.get();
        if (test != null) test.pass(message);
    }

    public static void markFail(String message, Throwable t) {
        ExtentTest test = testHolder.get();
        if (test == null) return;
        if (t != null) test.fail(t);
        else           test.fail(message);
    }

    public static void markSkip(String testName, String reason) {
        ExtentTest test = testHolder.get();
        if (test != null) test.skip(reason);
    }

    public static void markWarning(String message) {
        ExtentTest test = testHolder.get();
        if (test != null) test.warning(message);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Steps
    // ─────────────────────────────────────────────────────────────────────────

    public static void logStep(String stepText) {
        ExtentTest test = testHolder.get();
        if (test != null) test.info(stepText);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Screenshots
    // ─────────────────────────────────────────────────────────────────────────

    public static void addScreenshot(byte[] screenshotBytes) {
        ExtentTest test = testHolder.get();
        if (test == null || screenshotBytes == null) return;
        try {
            String base64 = Base64.getEncoder().encodeToString(screenshotBytes);
            test.addScreenCaptureFromBase64String(base64, "Screenshot");
        } catch (Exception e) {
            log.warn("Failed to add screenshot to Extent: {}", e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Labels / Categories
    // ─────────────────────────────────────────────────────────────────────────

    public static void assignCategory(String... categories) {
        ExtentTest test = testHolder.get();
        if (test != null) test.assignCategory(categories);
    }

    public static void assignAuthor(String... authors) {
        ExtentTest test = testHolder.get();
        if (test != null) test.assignAuthor(authors);
    }
}
