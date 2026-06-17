package com.framework.base;

import com.framework.utils.*;
import io.qameta.allure.Step;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * BasePage - Parent of every Page Object.
 *
 * Centralises ALL WebDriver interactions so page classes remain declarative
 * (locators + page-specific business methods only). Every public interaction
 * is annotated with {@code @Step} for Allure and logged via Log4j2.
 *
 * Principle: Open/Closed - extend this class to add specialised pages;
 *            do not modify it when adding page-specific logic.
 */
public abstract class BasePage {

    private static final Logger log = LogManager.getLogger(BasePage.class);

    protected final WebDriver      driver;
    protected final WaitUtils      waitUtils;
    protected final JavaScriptUtils jsUtils;
    protected final Actions         actions;

    protected BasePage() {
        this.driver    = BaseDriver.getDriver();
        this.waitUtils = new WaitUtils(driver);
        this.jsUtils   = new JavaScriptUtils(driver);
        this.actions   = new Actions(driver);
        PageFactory.initElements(driver, this);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Navigation
    // ═════════════════════════════════════════════════════════════════════════

    @Step("Navigate to URL: {url}")
    public void navigateTo(String url) {
        log.info("Navigating to: {}", url);
        driver.get(url);
    }

    @Step("Get current URL")
    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    @Step("Get page title")
    public String getPageTitle() {
        return driver.getTitle();
    }

    @Step("Refresh page")
    public void refreshPage() {
        log.info("Refreshing page");
        driver.navigate().refresh();
    }

    @Step("Navigate back")
    public void navigateBack() {
        driver.navigate().back();
    }

    @Step("Navigate forward")
    public void navigateForward() {
        driver.navigate().forward();
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Core Element Interactions
    // ═════════════════════════════════════════════════════════════════════════

    @Step("Click element: {locator}")
    public void click(By locator) {
        log.info("Clicking: {}", locator);
        waitUtils.waitForClickable(locator).click();
    }

    @Step("Click element")
    public void click(WebElement element) {
        log.info("Clicking WebElement: {}", element);
        waitUtils.waitForClickable(element).click();
    }

    /** Click via JavaScript - useful when overlays block normal click. */
    @Step("Click via JS: {locator}")
    public void jsClick(By locator) {
        log.info("JS-clicking: {}", locator);
        WebElement el = waitUtils.waitForPresence(locator);
        jsUtils.click(el);
    }

    @Step("Double-click: {locator}")
    public void doubleClick(By locator) {
        log.info("Double-clicking: {}", locator);
        WebElement el = waitUtils.waitForClickable(locator);
        actions.doubleClick(el).perform();
    }

    @Step("Right-click: {locator}")
    public void rightClick(By locator) {
        log.info("Right-clicking: {}", locator);
        WebElement el = waitUtils.waitForVisible(locator);
        actions.contextClick(el).perform();
    }

    @Step("Type '{text}' into: {locator}")
    public void type(By locator, String text) {
        log.info("Typing '{}' into {}", text, locator);
        WebElement el = waitUtils.waitForVisible(locator);
        el.clear();
        el.sendKeys(text);
    }

    @Step("Type '{text}' into element")
    public void type(WebElement element, String text) {
        waitUtils.waitForClickable(element);
        element.clear();
        element.sendKeys(text);
    }

    /** Append text without clearing first. */
    @Step("Append '{text}' to: {locator}")
    public void appendText(By locator, String text) {
        log.info("Appending '{}' to {}", text, locator);
        waitUtils.waitForVisible(locator).sendKeys(text);
    }

    @Step("Clear field: {locator}")
    public void clear(By locator) {
        log.info("Clearing field: {}", locator);
        waitUtils.waitForVisible(locator).clear();
    }

    @Step("Press key: {key}")
    public void pressKey(By locator, Keys key) {
        waitUtils.waitForVisible(locator).sendKeys(key);
    }

    @Step("Submit form at: {locator}")
    public void submit(By locator) {
        waitUtils.waitForVisible(locator).submit();
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Text / Attribute Reading
    // ═════════════════════════════════════════════════════════════════════════

    @Step("Get text of: {locator}")
    public String getText(By locator) {
        String text = waitUtils.waitForVisible(locator).getText().trim();
        log.debug("Text of {}: '{}'", locator, text);
        return text;
    }

    @Step("Get text of element")
    public String getText(WebElement element) {
        return element.getText().trim();
    }

    @Step("Get attribute '{attribute}' from: {locator}")
    public String getAttribute(By locator, String attribute) {
        return waitUtils.waitForPresence(locator).getAttribute(attribute);
    }

    @Step("Get value of: {locator}")
    public String getValue(By locator) {
        return getAttribute(locator, "value");
    }

    @Step("Get CSS value '{property}' from: {locator}")
    public String getCssValue(By locator, String property) {
        return waitUtils.waitForPresence(locator).getCssValue(property);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  State Checks
    // ═════════════════════════════════════════════════════════════════════════

    @Step("Is element displayed: {locator}")
    public boolean isDisplayed(By locator) {
        try {
            return driver.findElement(locator).isDisplayed();
        } catch (NoSuchElementException | StaleElementReferenceException e) {
            return false;
        }
    }

    @Step("Is element enabled: {locator}")
    public boolean isEnabled(By locator) {
        try {
            return driver.findElement(locator).isEnabled();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    @Step("Is element selected (checkbox/radio): {locator}")
    public boolean isSelected(By locator) {
        try {
            return driver.findElement(locator).isSelected();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    /** Wait up to {@code timeoutSec} for element to appear. */
    public boolean isElementPresent(By locator, int timeoutSec) {
        try {
            waitUtils.waitForPresence(locator, timeoutSec);
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Finding Elements
    // ═════════════════════════════════════════════════════════════════════════

    public WebElement findElement(By locator) {
        return waitUtils.waitForPresence(locator);
    }

    public List<WebElement> findElements(By locator) {
        waitUtils.waitForPresence(locator);
        return driver.findElements(locator);
    }

    public int getElementCount(By locator) {
        return driver.findElements(locator).size();
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Dropdown (Select)
    // ═════════════════════════════════════════════════════════════════════════

    @Step("Select by visible text '{text}': {locator}")
    public void selectByText(By locator, String text) {
        log.info("Selecting '{}' from dropdown {}", text, locator);
        new Select(waitUtils.waitForVisible(locator)).selectByVisibleText(text);
    }

    @Step("Select by value '{value}': {locator}")
    public void selectByValue(By locator, String value) {
        log.info("Selecting value '{}' from dropdown {}", value, locator);
        new Select(waitUtils.waitForVisible(locator)).selectByValue(value);
    }

    @Step("Select by index {index}: {locator}")
    public void selectByIndex(By locator, int index) {
        log.info("Selecting index {} from dropdown {}", index, locator);
        new Select(waitUtils.waitForVisible(locator)).selectByIndex(index);
    }

    @Step("Get selected option from: {locator}")
    public String getSelectedOption(By locator) {
        return new Select(waitUtils.waitForVisible(locator))
                .getFirstSelectedOption().getText().trim();
    }

    @Step("Get all dropdown options from: {locator}")
    public List<String> getAllOptions(By locator) {
        List<String> opts = new ArrayList<>();
        new Select(waitUtils.waitForVisible(locator))
                .getOptions()
                .forEach(o -> opts.add(o.getText().trim()));
        return opts;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Checkbox / Radio
    // ═════════════════════════════════════════════════════════════════════════

    @Step("Check checkbox: {locator}")
    public void check(By locator) {
        WebElement el = waitUtils.waitForClickable(locator);
        if (!el.isSelected()) el.click();
    }

    @Step("Uncheck checkbox: {locator}")
    public void uncheck(By locator) {
        WebElement el = waitUtils.waitForClickable(locator);
        if (el.isSelected()) el.click();
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Mouse Actions (Actions API)
    // ═════════════════════════════════════════════════════════════════════════

    @Step("Hover over: {locator}")
    public void hover(By locator) {
        log.info("Hovering over: {}", locator);
        WebElement el = waitUtils.waitForVisible(locator);
        actions.moveToElement(el).perform();
    }

    @Step("Drag from: {source} → to: {target}")
    public void dragAndDrop(By source, By target) {
        WebElement src  = waitUtils.waitForVisible(source);
        WebElement dest = waitUtils.waitForVisible(target);
        actions.dragAndDrop(src, dest).perform();
    }

    @Step("Drag and drop by offset ({xOffset}, {yOffset})")
    public void dragAndDropByOffset(By locator, int xOffset, int yOffset) {
        WebElement el = waitUtils.waitForVisible(locator);
        actions.dragAndDropBy(el, xOffset, yOffset).perform();
    }

    @Step("Click and hold: {locator}")
    public void clickAndHold(By locator) {
        actions.clickAndHold(waitUtils.waitForVisible(locator)).perform();
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Scrolling
    // ═════════════════════════════════════════════════════════════════════════

    @Step("Scroll to element: {locator}")
    public void scrollToElement(By locator) {
        log.info("Scrolling to: {}", locator);
        WebElement el = waitUtils.waitForPresence(locator);
        jsUtils.scrollIntoView(el);
    }

    @Step("Scroll to top of page")
    public void scrollToTop() {
        jsUtils.execute("window.scrollTo(0, 0);");
    }

    @Step("Scroll to bottom of page")
    public void scrollToBottom() {
        jsUtils.execute("window.scrollTo(0, document.body.scrollHeight);");
    }

    @Step("Scroll by ({x}, {y}) pixels")
    public void scrollBy(int x, int y) {
        jsUtils.execute(String.format("window.scrollBy(%d, %d);", x, y));
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Frames / iFrames
    // ═════════════════════════════════════════════════════════════════════════

    @Step("Switch to frame by index: {index}")
    public void switchToFrame(int index) {
        log.info("Switching to frame index {}", index);
        driver.switchTo().frame(index);
    }

    @Step("Switch to frame by name/id: {nameOrId}")
    public void switchToFrame(String nameOrId) {
        log.info("Switching to frame '{}'", nameOrId);
        driver.switchTo().frame(nameOrId);
    }

    @Step("Switch to frame by locator: {locator}")
    public void switchToFrame(By locator) {
        log.info("Switching to frame located by {}", locator);
        WebElement frameEl = waitUtils.waitForPresence(locator);
        driver.switchTo().frame(frameEl);
    }

    @Step("Switch to frame by WebElement")
    public void switchToFrame(WebElement frameElement) {
        driver.switchTo().frame(frameElement);
    }

    @Step("Switch to parent frame")
    public void switchToParentFrame() {
        driver.switchTo().parentFrame();
    }

    @Step("Switch to default content (exit all frames)")
    public void switchToDefaultContent() {
        driver.switchTo().defaultContent();
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Alerts
    // ═════════════════════════════════════════════════════════════════════════

    @Step("Accept alert")
    public void acceptAlert() {
        log.info("Accepting alert");
        waitUtils.waitForAlert().accept();
    }

    @Step("Dismiss alert")
    public void dismissAlert() {
        log.info("Dismissing alert");
        waitUtils.waitForAlert().dismiss();
    }

    @Step("Get alert text")
    public String getAlertText() {
        String text = waitUtils.waitForAlert().getText();
        log.info("Alert text: '{}'", text);
        return text;
    }

    @Step("Type '{text}' into alert")
    public void typeInAlert(String text) {
        log.info("Typing '{}' into alert", text);
        waitUtils.waitForAlert().sendKeys(text);
        waitUtils.waitForAlert().accept();
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Windows / Tabs
    // ═════════════════════════════════════════════════════════════════════════

    @Step("Get main window handle")
    public String getMainWindowHandle() {
        return driver.getWindowHandle();
    }

    @Step("Switch to new window/tab (latest)")
    public void switchToNewWindow(String originalHandle) {
        log.info("Switching to new window");
        for (String handle : driver.getWindowHandles()) {
            if (!handle.equals(originalHandle)) {
                driver.switchTo().window(handle);
                log.info("Switched to window: {}", handle);
                return;
            }
        }
        throw new NoSuchWindowException("No new window found");
    }

    @Step("Switch to window by title: {title}")
    public void switchToWindowByTitle(String title) {
        for (String handle : driver.getWindowHandles()) {
            driver.switchTo().window(handle);
            if (driver.getTitle().equalsIgnoreCase(title)) return;
        }
        throw new NoSuchWindowException("Window with title '" + title + "' not found");
    }

    @Step("Switch to window by URL contains: {urlPart}")
    public void switchToWindowByUrl(String urlPart) {
        for (String handle : driver.getWindowHandles()) {
            driver.switchTo().window(handle);
            if (driver.getCurrentUrl().contains(urlPart)) return;
        }
        throw new NoSuchWindowException("Window with URL containing '" + urlPart + "' not found");
    }

    @Step("Switch back to window: {handle}")
    public void switchToWindow(String handle) {
        driver.switchTo().window(handle);
    }

    @Step("Close current window and switch to: {handle}")
    public void closeCurrentWindowAndSwitch(String handle) {
        driver.close();
        driver.switchTo().window(handle);
    }

    @Step("Open new tab")
    public void openNewTab() {
        jsUtils.execute("window.open('about:blank','_blank');");
    }

    @Step("Get all window handles")
    public List<String> getAllWindowHandles() {
        return new ArrayList<>(driver.getWindowHandles());
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Waits (delegates to WaitUtils for explicit control)
    // ═════════════════════════════════════════════════════════════════════════

    @Step("Wait for element to be visible: {locator}")
    public WebElement waitForVisible(By locator) {
        return waitUtils.waitForVisible(locator);
    }

    @Step("Wait for element to be clickable: {locator}")
    public WebElement waitForClickable(By locator) {
        return waitUtils.waitForClickable(locator);
    }

    @Step("Wait for element to disappear: {locator}")
    public void waitForInvisibility(By locator) {
        waitUtils.waitForInvisibility(locator);
    }

    @Step("Wait for text '{text}' in element: {locator}")
    public void waitForText(By locator, String text) {
        waitUtils.waitForTextToBePresent(locator, text);
    }

    @Step("Wait for URL to contain: {urlFragment}")
    public void waitForUrlContains(String urlFragment) {
        waitUtils.waitForUrlContains(urlFragment);
    }

    @Step("Wait for page title to contain: {title}")
    public void waitForTitleContains(String title) {
        waitUtils.waitForTitleContains(title);
    }

    /** Hard pause - use sparingly; prefer explicit waits. */
    public void pause(long milliseconds) {
        log.warn("Hard pause for {}ms - consider replacing with explicit wait", milliseconds);
        try { Thread.sleep(milliseconds); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Screenshots (delegates to ScreenshotUtils)
    // ═════════════════════════════════════════════════════════════════════════

    /** Capture and attach to Allure. */
    public void captureScreenshot(String name) {
        ScreenshotUtils.captureAndAttachToAllure(driver, name);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Page Load Verification
    // ═════════════════════════════════════════════════════════════════════════

    /** Subclasses can override to assert the page loaded correctly. */
    public abstract boolean isPageLoaded();

    /** Verify current URL matches expected. */
    public boolean isOnPage(String expectedUrlFragment) {
        return driver.getCurrentUrl().contains(expectedUrlFragment);
    }
}
