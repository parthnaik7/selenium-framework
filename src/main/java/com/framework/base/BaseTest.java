package com.framework.base;

import com.framework.config.ConfigReader;
import com.framework.reporting.AllureManager;
import com.framework.reporting.ExtentReportManager;
import com.framework.reporting.XrayReporter;
import com.framework.utils.ScreenshotUtils;
import io.qameta.allure.Allure;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.ITestResult;
import org.testng.annotations.*;

/**
 * BaseTest - Parent of every test class.
 *
 * Responsibilities:
 *  1. @BeforeSuite  - Extent report initialisation, Xray session setup
 *  2. @BeforeMethod - Browser launch, navigate to base URL
 *  3. @AfterMethod  - Screenshot on failure, report test result, driver quit
 *  4. @AfterSuite   - Flush reports, publish results to Xray/JIRA
 *
 * Principle: Template Method - subclasses hook in via @Test without touching
 *            setup/teardown boilerplate.
 */
public abstract class BaseTest {

    private static final Logger log = LogManager.getLogger(BaseTest.class);

    // ─────────────────────────────────────────────────────────────────────────
    //  Suite-level lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    @BeforeSuite(alwaysRun = true)
    public void beforeSuite() {
        log.info("═══════════════════════ TEST SUITE STARTED ═══════════════════════");
        ExtentReportManager.initReport();

        if (ConfigReader.getBool("xray.enabled", false)) {
            XrayReporter.init();
        }
    }

    @AfterSuite(alwaysRun = true)
    public void afterSuite() {
        ExtentReportManager.flushReport();

        if (ConfigReader.getBool("xray.enabled", false)) {
            XrayReporter.publishResults();
        }

        log.info("═══════════════════════ TEST SUITE COMPLETED ══════════════════════");
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Method-level lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Browser is initialised before each test method.
     * Override {@link #getBrowser()} or {@link #isHeadless()} in a subclass to
     * customise per-class behaviour without touching this base.
     */
    @BeforeMethod(alwaysRun = true)
    public void setUp(ITestResult result) {
        String testName = result.getMethod().getMethodName();
        log.info("──────────── STARTING TEST: {} ────────────", testName);

        BaseDriver.initDriver(getBrowser(), isHeadless());
        ExtentReportManager.createTest(testName, result.getMethod().getDescription());
        AllureManager.addEnvironmentInfo();

        String baseUrl = ConfigReader.get("base.url", "");
        if (!baseUrl.isBlank()) {
            BaseDriver.getDriver().get(baseUrl);
            log.info("Navigated to base URL: {}", baseUrl);
        }
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown(ITestResult result) {
        String testName = result.getMethod().getMethodName();

        try {
            handleTestResult(result, testName);
        } finally {
            BaseDriver.quitDriver();
            log.info("──────────── FINISHED TEST: {} [{}] ────────────",
                    testName, statusLabel(result.getStatus()));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Result handling
    // ─────────────────────────────────────────────────────────────────────────

    private void handleTestResult(ITestResult result, String testName) {
        int status = result.getStatus();

        if (status == ITestResult.FAILURE || status == ITestResult.SKIP) {
            if (BaseDriver.isDriverAlive()) {
                byte[] screenshot = ScreenshotUtils.captureAsBytes(BaseDriver.getDriver());
                if (screenshot != null) {
                    AllureManager.attachScreenshot(screenshot, "Failure Screenshot");
                    ExtentReportManager.addScreenshot(screenshot);
                }
            }
        }

        switch (status) {
            case ITestResult.SUCCESS -> {
                log.info("✅ PASS: {}", testName);
                ExtentReportManager.markPass(testName);
                reportToXray(testName, "PASS", null);
            }
            case ITestResult.FAILURE -> {
                String errorMsg = result.getThrowable() != null
                        ? result.getThrowable().getMessage() : "Unknown error";
                log.error("❌ FAIL: {} - {}", testName, errorMsg);
                ExtentReportManager.markFail(testName, result.getThrowable());
                AllureManager.attachText(errorMsg, "Failure Reason");
                reportToXray(testName, "FAIL", errorMsg);
            }
            case ITestResult.SKIP -> {
                log.warn("⏭ SKIP: {}", testName);
                ExtentReportManager.markSkip(testName, "Test was skipped");
                reportToXray(testName, "TODO", "Test skipped");
            }
        }
    }

    private void reportToXray(String testName, String status, String comment) {
        if (ConfigReader.getBool("xray.enabled", false)) {
            XrayReporter.recordResult(testName, status, comment);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Extension points - override in subclasses as needed
    // ─────────────────────────────────────────────────────────────────────────

    /** Override to select a specific browser for a test class. Default from config. */
    protected String getBrowser() {
        return System.getProperty("browser", ConfigReader.get("browser", "chrome"));
    }

    /** Override to force headless for a test class. Default from config. */
    protected boolean isHeadless() {
        return Boolean.parseBoolean(
                System.getProperty("headless", ConfigReader.get("headless", "false")));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Convenience accessor
    // ─────────────────────────────────────────────────────────────────────────

    private static String statusLabel(int status) {
        return switch (status) {
            case ITestResult.SUCCESS -> "PASS";
            case ITestResult.FAILURE -> "FAIL";
            case ITestResult.SKIP    -> "SKIP";
            default                  -> "UNKNOWN";
        };
    }
}
