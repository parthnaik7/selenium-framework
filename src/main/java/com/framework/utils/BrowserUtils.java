package com.framework.utils;

import io.qameta.allure.Step;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.*;

import java.util.Set;

/**
 * BrowserUtils - Browser-level helpers beyond element interaction.
 *
 * Covers: cookies, multi-tab management, window resizing, browser info.
 */
public class BrowserUtils {

    private static final Logger log = LogManager.getLogger(BrowserUtils.class);

    private final WebDriver driver;

    public BrowserUtils(WebDriver driver) {
        this.driver = driver;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Cookies
    // ─────────────────────────────────────────────────────────────────────────

    @Step("Add cookie: {name}={value}")
    public void addCookie(String name, String value) {
        driver.manage().addCookie(new Cookie(name, value));
        log.info("Cookie added: {}={}", name, value);
    }

    @Step("Add cookie with full properties")
    public void addCookie(Cookie cookie) {
        driver.manage().addCookie(cookie);
    }

    @Step("Get cookie value: '{name}'")
    public String getCookieValue(String name) {
        Cookie c = driver.manage().getCookieNamed(name);
        return c != null ? c.getValue() : null;
    }

    @Step("Get cookie: '{name}'")
    public Cookie getCookie(String name) {
        return driver.manage().getCookieNamed(name);
    }

    @Step("Get all cookies")
    public Set<Cookie> getAllCookies() {
        return driver.manage().getCookies();
    }

    @Step("Delete cookie: '{name}'")
    public void deleteCookie(String name) {
        driver.manage().deleteCookieNamed(name);
        log.info("Cookie deleted: {}", name);
    }

    @Step("Delete all cookies")
    public void deleteAllCookies() {
        driver.manage().deleteAllCookies();
        log.info("All cookies deleted");
    }

    /** Check whether a named cookie exists. */
    public boolean cookieExists(String name) {
        return driver.manage().getCookieNamed(name) != null;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Window Size / Position
    // ─────────────────────────────────────────────────────────────────────────

    @Step("Maximise window")
    public void maximise() {
        driver.manage().window().maximize();
    }

    @Step("Minimise window")
    public void minimise() {
        driver.manage().window().minimize();
    }

    @Step("Set window size to {width}x{height}")
    public void setWindowSize(int width, int height) {
        driver.manage().window().setSize(new Dimension(width, height));
    }

    @Step("Set window position to ({x}, {y})")
    public void setWindowPosition(int x, int y) {
        driver.manage().window().setPosition(new Point(x, y));
    }

    @Step("Enter fullscreen")
    public void fullscreen() {
        driver.manage().window().fullscreen();
    }

    @Step("Get window size")
    public Dimension getWindowSize() {
        return driver.manage().window().getSize();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Tabs
    // ─────────────────────────────────────────────────────────────────────────

    @Step("Open new tab")
    public void openNewTab() {
        driver.switchTo().newWindow(WindowType.TAB);
        log.info("Opened new tab");
    }

    @Step("Open new window")
    public void openNewWindow() {
        driver.switchTo().newWindow(WindowType.WINDOW);
        log.info("Opened new window");
    }

    @Step("Close current tab/window")
    public void closeCurrentTab() {
        driver.close();
    }

    @Step("Switch to tab by index: {index}")
    public void switchToTab(int index) {
        java.util.List<String> handles = new java.util.ArrayList<>(driver.getWindowHandles());
        if (index >= handles.size()) {
            throw new IndexOutOfBoundsException(
                    "Tab index " + index + " but only " + handles.size() + " tabs open");
        }
        driver.switchTo().window(handles.get(index));
        log.info("Switched to tab index {}", index);
    }

    @Step("Get open tab count")
    public int getTabCount() {
        return driver.getWindowHandles().size();
    }

    @Step("Get current window handle")
    public String getCurrentWindowHandle() {
        return driver.getWindowHandle();
    }

    @Step("Get all window handles")
    public Set<String> getAllWindowHandles() {
        return driver.getWindowHandles();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Navigation
    // ─────────────────────────────────────────────────────────────────────────

    @Step("Navigate to URL: {url}")
    public void navigateTo(String url) {
        log.info("Navigate to: {}", url);
        driver.get(url);
    }

    @Step("Navigate back")
    public void back() {
        driver.navigate().back();
    }

    @Step("Navigate forward")
    public void forward() {
        driver.navigate().forward();
    }

    @Step("Refresh page")
    public void refresh() {
        driver.navigate().refresh();
    }

    @Step("Get current URL")
    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    @Step("Get page title")
    public String getTitle() {
        return driver.getTitle();
    }

    @Step("Get page source")
    public String getPageSource() {
        return driver.getPageSource();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Frames
    // ─────────────────────────────────────────────────────────────────────────

    @Step("Switch to default content")
    public void switchToDefaultContent() {
        driver.switchTo().defaultContent();
    }

    @Step("Switch to parent frame")
    public void switchToParentFrame() {
        driver.switchTo().parentFrame();
    }
}
