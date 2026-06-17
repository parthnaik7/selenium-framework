package com.framework.listeners;

import com.framework.base.BaseDriver;
import com.framework.reporting.ExtentReportManager;
import com.framework.utils.ScreenshotUtils;
import io.qameta.allure.Allure;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.*;

import java.io.ByteArrayInputStream;

/**
 * TestNGListener - Plugs into TestNG's event lifecycle.
 *
 * Registered in testng.xml {@code <listeners>} block so it applies automatically
 * to every test in the suite without any annotation on test classes.
 *
 * Responsibilities:
 *  - Enrich Allure report with extra metadata and on-failure screenshots
 *  - Update Extent Reports on each result
 *  - Log test lifecycle events consistently
 */
public class TestNGListener implements ITestListener, ISuiteListener {

    private static final Logger log = LogManager.getLogger(TestNGListener.class);

    // ─────────────────────────────────────────────────────────────────────────
    //  ISuiteListener
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void onStart(ISuite suite) {
        log.info("╔══════════════════════════════════════════════╗");
        log.info("║  SUITE STARTED: {}", suite.getName());
        log.info("╚══════════════════════════════════════════════╝");
    }

    @Override
    public void onFinish(ISuite suite) {
        log.info("╔══════════════════════════════════════════════╗");
        log.info("║  SUITE FINISHED: {}", suite.getName());
        log.info("╚══════════════════════════════════════════════╝");
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  ITestListener
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void onTestStart(ITestResult result) {
        String name = qualifiedName(result);
        log.info("▶  START  : {}", name);
        Allure.getLifecycle().updateTestCase(tc -> tc.setName(name));
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        log.info("✅ PASS   : {}", qualifiedName(result));
        ExtentReportManager.markPass(result.getMethod().getMethodName());
    }

    @Override
    public void onTestFailure(ITestResult result) {
        String name = qualifiedName(result);
        log.error("❌ FAIL   : {} - {}", name,
                result.getThrowable() != null ? result.getThrowable().getMessage() : "n/a");

        attachScreenshotOnFailure(name);
        attachErrorToAllure(result);
        ExtentReportManager.markFail(result.getMethod().getMethodName(), result.getThrowable());
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        log.warn("⏭  SKIP   : {}", qualifiedName(result));
        ExtentReportManager.markSkip(result.getMethod().getMethodName(),
                "Test skipped - dependency or configuration issue");
    }

    @Override
    public void onTestFailedWithTimeout(ITestResult result) {
        log.error("⏰ TIMEOUT: {}", qualifiedName(result));
        onTestFailure(result);
    }

    @Override
    public void onFinish(ITestContext context) {
        log.info("── Context finished: {} | Passed={} Failed={} Skipped={}",
                context.getName(),
                context.getPassedTests().size(),
                context.getFailedTests().size(),
                context.getSkippedTests().size());
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private static String qualifiedName(ITestResult result) {
        return result.getTestClass().getRealClass().getSimpleName()
                + "#" + result.getMethod().getMethodName();
    }

    private static void attachScreenshotOnFailure(String testName) {
        if (!BaseDriver.isDriverAlive()) return;
        try {
            byte[] screenshot = ScreenshotUtils.captureAsBytes(BaseDriver.getDriver());
            if (screenshot != null) {
                Allure.addAttachment(
                    "Failure Screenshot - " + testName,
                    "image/png",
                    new ByteArrayInputStream(screenshot),
                    "png"
                );
            }
        } catch (Exception e) {
            log.warn("Screenshot on failure failed: {}", e.getMessage());
        }
    }

    private static void attachErrorToAllure(ITestResult result) {
        if (result.getThrowable() != null) {
            String stackTrace = org.apache.commons.lang3.exception.ExceptionUtils
                    .getStackTrace(result.getThrowable());
            Allure.addAttachment("Stack Trace", "text/plain", stackTrace);
        }
    }
}
