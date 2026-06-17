package com.framework.utils;

import com.framework.config.ConfigReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;

import java.time.Duration;
import java.util.List;
import java.util.function.Function;

/**
 * WaitUtils - All Selenium wait strategies in one place.
 *
 * Uses explicit WebDriverWait (recommended over implicit waits).
 * Polling interval is configurable via config.properties.
 *
 * Principle: DRY - test code never duplicates wait logic.
 */
public class WaitUtils {

    private static final Logger log = LogManager.getLogger(WaitUtils.class);

    private final WebDriver driver;
    private final long defaultTimeoutSec;
    private final long pollingMillis;
    private final WebDriverWait defaultWait;

    public WaitUtils(WebDriver driver) {
        this.driver           = driver;
        this.defaultTimeoutSec = ConfigReader.getLong("explicit.wait.seconds", 10);
        this.pollingMillis     = ConfigReader.getLong("polling.interval.millis", 500);
        this.defaultWait       = buildWait(defaultTimeoutSec);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Element Presence / Visibility
    // ─────────────────────────────────────────────────────────────────────────

    /** Wait until element is present in DOM (not necessarily visible). */
    public WebElement waitForPresence(By locator) {
        return waitForPresence(locator, defaultTimeoutSec);
    }

    public WebElement waitForPresence(By locator, long timeoutSec) {
        log.debug("Waiting for presence of: {}", locator);
        return buildWait(timeoutSec).until(
                ExpectedConditions.presenceOfElementLocated(locator));
    }

    /** Wait until element is visible on page. */
    public WebElement waitForVisible(By locator) {
        return waitForVisible(locator, defaultTimeoutSec);
    }

    public WebElement waitForVisible(By locator, long timeoutSec) {
        log.debug("Waiting for visibility of: {}", locator);
        return buildWait(timeoutSec).until(
                ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public WebElement waitForVisible(WebElement element) {
        return defaultWait.until(ExpectedConditions.visibilityOf(element));
    }

    /** Wait until element is visible AND enabled (ready to click). */
    public WebElement waitForClickable(By locator) {
        return waitForClickable(locator, defaultTimeoutSec);
    }

    public WebElement waitForClickable(By locator, long timeoutSec) {
        log.debug("Waiting for clickability of: {}", locator);
        return buildWait(timeoutSec).until(
                ExpectedConditions.elementToBeClickable(locator));
    }

    public WebElement waitForClickable(WebElement element) {
        return defaultWait.until(ExpectedConditions.elementToBeClickable(element));
    }

    /** Wait until ALL matching elements are visible. */
    public List<WebElement> waitForAllVisible(By locator) {
        return defaultWait.until(
                ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Invisibility / Staleness
    // ─────────────────────────────────────────────────────────────────────────

    /** Wait until element is not visible (or not in DOM). */
    public void waitForInvisibility(By locator) {
        waitForInvisibility(locator, defaultTimeoutSec);
    }

    public void waitForInvisibility(By locator, long timeoutSec) {
        log.debug("Waiting for invisibility of: {}", locator);
        buildWait(timeoutSec).until(
                ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    /** Wait until the given element becomes stale (detached from DOM). */
    public void waitForStaleness(WebElement element) {
        defaultWait.until(ExpectedConditions.stalenessOf(element));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Text / Attribute Conditions
    // ─────────────────────────────────────────────────────────────────────────

    public void waitForTextToBePresent(By locator, String text) {
        defaultWait.until(
                ExpectedConditions.textToBePresentInElementLocated(locator, text));
    }

    public void waitForTextToBePresentInValue(By locator, String text) {
        defaultWait.until(
                ExpectedConditions.textToBePresentInElementValue(locator, text));
    }

    public void waitForAttributeToContain(By locator, String attribute, String value) {
        defaultWait.until(
                ExpectedConditions.attributeContains(locator, attribute, value));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  URL / Title
    // ─────────────────────────────────────────────────────────────────────────

    public void waitForUrlContains(String fragment) {
        log.debug("Waiting for URL to contain: '{}'", fragment);
        defaultWait.until(ExpectedConditions.urlContains(fragment));
    }

    public void waitForUrlMatches(String regex) {
        defaultWait.until(ExpectedConditions.urlMatches(regex));
    }

    public void waitForTitleContains(String titleFragment) {
        log.debug("Waiting for title to contain: '{}'", titleFragment);
        defaultWait.until(ExpectedConditions.titleContains(titleFragment));
    }

    public void waitForTitleIs(String exactTitle) {
        defaultWait.until(ExpectedConditions.titleIs(exactTitle));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Alerts
    // ─────────────────────────────────────────────────────────────────────────

    public Alert waitForAlert() {
        return waitForAlert(defaultTimeoutSec);
    }

    public Alert waitForAlert(long timeoutSec) {
        log.debug("Waiting for alert");
        return buildWait(timeoutSec).until(ExpectedConditions.alertIsPresent());
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Frame
    // ─────────────────────────────────────────────────────────────────────────

    public void waitForFrameAndSwitch(By locator) {
        defaultWait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(locator));
    }

    public void waitForFrameAndSwitch(int index) {
        defaultWait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(index));
    }

    public void waitForFrameAndSwitch(String nameOrId) {
        defaultWait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(nameOrId));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Page Load
    // ─────────────────────────────────────────────────────────────────────────

    /** Wait until document.readyState === 'complete'. */
    public void waitForPageLoad() {
        waitForPageLoad(defaultTimeoutSec);
    }

    public void waitForPageLoad(long timeoutSec) {
        log.debug("Waiting for page to fully load");
        buildWait(timeoutSec).until(
            driver -> ((JavascriptExecutor) driver)
                .executeScript("return document.readyState").equals("complete")
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Element Count
    // ─────────────────────────────────────────────────────────────────────────

    /** Wait until at least {@code minCount} elements match. */
    public List<WebElement> waitForMinimumElements(By locator, int minCount) {
        return defaultWait.until(
                ExpectedConditions.numberOfElementsToBeMoreThan(locator, minCount - 1));
    }

    /** Wait until exactly {@code count} elements match. */
    public List<WebElement> waitForExactElementCount(By locator, int count) {
        return defaultWait.until(
                ExpectedConditions.numberOfElementsToBe(locator, count));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Selection State
    // ─────────────────────────────────────────────────────────────────────────

    public void waitForElementToBeSelected(By locator) {
        defaultWait.until(ExpectedConditions.elementToBeSelected(locator));
    }

    public void waitForElementToBeDeselected(By locator) {
        defaultWait.until(ExpectedConditions.elementSelectionStateToBe(locator, false));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Fluent Wait - custom polling with ignored exceptions
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Create a fluent wait with custom timeout, polling, and ignored exceptions.
     *
     * <pre>
     *   waitUtils.fluentWait(20, 1000, NoSuchElementException.class)
     *            .until(d -> d.findElement(By.id("result")).isDisplayed());
     * </pre>
     */
    @SafeVarargs
    public final FluentWait<WebDriver> fluentWait(long timeoutSec, long pollMillis,
                                                   Class<? extends Throwable>... ignoredExceptions) {
        FluentWait<WebDriver> wait = new FluentWait<>(driver)
                .withTimeout(Duration.ofSeconds(timeoutSec))
                .pollingEvery(Duration.ofMillis(pollMillis));
        for (Class<? extends Throwable> ex : ignoredExceptions) {
            wait.ignoring(ex);
        }
        return wait;
    }

    /** Generic wait with a custom condition lambda. */
    public <T> T waitUntil(Function<WebDriver, T> condition) {
        return defaultWait.until(condition);
    }

    public <T> T waitUntil(Function<WebDriver, T> condition, long timeoutSec) {
        return buildWait(timeoutSec).until(condition);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Internal
    // ─────────────────────────────────────────────────────────────────────────

    private WebDriverWait buildWait(long timeoutSec) {
        return new WebDriverWait(driver, Duration.ofSeconds(timeoutSec),
                Duration.ofMillis(pollingMillis));
    }
}
