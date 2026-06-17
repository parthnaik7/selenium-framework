package com.framework.listeners;

import com.framework.config.ConfigReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

/**
 * RetryAnalyzer - Automatically re-runs failed tests up to a configurable limit.
 *
 * Usage - add to any @Test:
 * <pre>
 *   @Test(retryAnalyzer = RetryAnalyzer.class)
 *   public void flakyTest() { ... }
 * </pre>
 *
 * Or apply globally in testng.xml with a TransformerListener:
 * <pre>
 *   &lt;listener class-name="com.framework.listeners.RetryTransformer"/&gt;
 * </pre>
 *
 * Max retries is read from config.properties key {@code retry.max.count} (default: 2).
 */
public class RetryAnalyzer implements IRetryAnalyzer {

    private static final Logger log = LogManager.getLogger(RetryAnalyzer.class);
    private static final int MAX_RETRIES = ConfigReader.getInt("retry.max.count", 2);

    private int retryCount = 0;

    @Override
    public boolean retry(ITestResult result) {
        if (retryCount < MAX_RETRIES) {
            retryCount++;
            log.warn("Retrying test '{}' - attempt {}/{}",
                    result.getMethod().getMethodName(), retryCount, MAX_RETRIES);
            return true;
        }
        log.error("Test '{}' failed after {} retries - marking as FAILED",
                result.getMethod().getMethodName(), MAX_RETRIES);
        return false;
    }
}
