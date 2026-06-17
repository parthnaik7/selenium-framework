package com.framework.base;

import com.framework.config.ConfigReader;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.safari.SafariDriver;

import java.time.Duration;

/**
 * BaseDriver - Thread-safe WebDriver factory.
 *
 * Uses ThreadLocal so parallel test execution never shares a driver instance.
 * Browser type and options are driven from config.properties (overridable via
 * system property), enabling CI/CD pipeline flexibility.
 *
 * Principle: Single Responsibility - this class only manages WebDriver lifecycle.
 */
@SuppressWarnings("all")
public final class BaseDriver {

    private static final Logger log = LogManager.getLogger(BaseDriver.class);

    /** One WebDriver instance per thread - safe for parallel execution. */
    private static final ThreadLocal<WebDriver> driverHolder = new ThreadLocal<>();

    private BaseDriver() { /* utility class - no instantiation */ }

    // ─────────────────────────────────────────────────────────────────────────
    //  Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns the WebDriver for the current thread.
     * Throws if {@link #initDriver()} was not called first.
     */
    public static WebDriver getDriver() {
        WebDriver driver = driverHolder.get();
        if (driver == null) {
            throw new IllegalStateException(
                "WebDriver is not initialised for this thread. Call BaseDriver.initDriver() first.");
        }
        return driver;
    }

    /** Initialise the driver using settings from config.properties. */
    public static void initDriver() {
        String browser  = System.getProperty("browser",  ConfigReader.get("browser",   "chrome"));
        boolean headless = Boolean.parseBoolean(
                System.getProperty("headless", ConfigReader.get("headless", "false")));
        initDriver(browser, headless);
    }

    /** Initialise the driver with explicit browser / headless flag. */
    public static void initDriver(String browser, boolean headless) {
        if (driverHolder.get() != null) {
            log.warn("Driver already initialised for this thread - skipping re-init.");
            return;
        }

        WebDriver driver = createDriver(browser.toLowerCase().trim(), headless);
        applyTimeouts(driver);
        driver.manage().window().maximize();

        driverHolder.set(driver);
        log.info("Driver initialised  [browser={}, headless={}, thread={}]",
                browser, headless, Thread.currentThread().getName());
    }

    /** Quit and remove the driver for the current thread. */
    public static void quitDriver() {
        WebDriver driver = driverHolder.get();
        if (driver != null) {
            try {
                driver.quit();
                log.info("Driver quit  [thread={}]", Thread.currentThread().getName());
            } catch (Exception e) {
                log.warn("Exception while quitting driver: {}", e.getMessage());
            } finally {
                driverHolder.remove();
            }
        }
    }

    /** @return {@code true} if a driver exists for the current thread. */
    public static boolean isDriverAlive() {
        return driverHolder.get() != null;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private static WebDriver createDriver(String browser, boolean headless) {
        return switch (browser) {
            case "firefox" -> buildFirefox(headless);
            case "edge"    -> buildEdge(headless);
            case "safari"  -> buildSafari();
            default        -> buildChrome(headless);
        };
    }

    private static WebDriver buildChrome(boolean headless) {
        WebDriverManager.chromedriver().setup();
        ChromeOptions opts = new ChromeOptions();
        if (headless) opts.addArguments("--headless=new");
        opts.addArguments(
            "--no-sandbox",
            "--disable-dev-shm-usage",
            "--disable-gpu",
            "--window-size=1920,1080",
            "--disable-extensions",
            "--remote-allow-origins=*",
            "--disable-infobars",
            "--disable-notifications"
        );
        return new ChromeDriver(opts);
    }

    private static WebDriver buildFirefox(boolean headless) {
        WebDriverManager.firefoxdriver().setup();
        FirefoxOptions opts = new FirefoxOptions();
        if (headless) opts.addArguments("--headless");
        opts.addArguments("--width=1920", "--height=1080");
        return new FirefoxDriver(opts);
    }

    private static WebDriver buildEdge(boolean headless) {
        WebDriverManager.edgedriver().setup();
        EdgeOptions opts = new EdgeOptions();
        if (headless) opts.addArguments("--headless=new");
        opts.addArguments("--no-sandbox", "--disable-dev-shm-usage");
        return new EdgeDriver(opts);
    }

    private static WebDriver buildSafari() {
        return new SafariDriver();   // Safari requires manual enable in System Preferences
    }

    private static void applyTimeouts(WebDriver driver) {
        long implicit   = Long.parseLong(ConfigReader.get("implicit.wait.seconds",   "0"));
        long pageLoad   = Long.parseLong(ConfigReader.get("page.load.timeout.seconds","30"));
        long scriptTimeout = Long.parseLong(ConfigReader.get("script.timeout.seconds","30"));

        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(implicit));
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(pageLoad));
        driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(scriptTimeout));
    }
}
