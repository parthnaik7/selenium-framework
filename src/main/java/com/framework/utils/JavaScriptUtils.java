package com.framework.utils;

import io.qameta.allure.Step;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * JavaScriptUtils - JavaScript execution helpers.
 *
 * Provides common JS-based interactions useful when standard WebDriver
 * methods are blocked by overlays, animations, or browser quirks.
 */
public class JavaScriptUtils {

    private static final Logger log = LogManager.getLogger(JavaScriptUtils.class);

    private final JavascriptExecutor js;

    public JavaScriptUtils(WebDriver driver) {
        this.js = (JavascriptExecutor) driver;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Execution
    // ─────────────────────────────────────────────────────────────────────────

    /** Execute a script with no return value. */
    public void execute(String script, Object... args) {
        log.debug("Executing JS: {}", script);
        js.executeScript(script, args);
    }

    /** Execute a script and return the result. */
    public Object executeAndReturn(String script, Object... args) {
        log.debug("Executing JS (return): {}", script);
        return js.executeScript(script, args);
    }

    /** Execute async script (waits for callback). */
    public Object executeAsync(String script, Object... args) {
        log.debug("Executing async JS");
        return js.executeAsyncScript(script, args);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Click / Focus
    // ─────────────────────────────────────────────────────────────────────────

    @Step("JS click on element")
    public void click(WebElement element) {
        js.executeScript("arguments[0].click();", element);
    }

    @Step("Focus element via JS")
    public void focus(WebElement element) {
        js.executeScript("arguments[0].focus();", element);
    }

    @Step("Blur (unfocus) element via JS")
    public void blur(WebElement element) {
        js.executeScript("arguments[0].blur();", element);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Text / Value
    // ─────────────────────────────────────────────────────────────────────────

    @Step("Set value via JS: '{value}'")
    public void setValue(WebElement element, String value) {
        js.executeScript("arguments[0].value = arguments[1];", element, value);
    }

    @Step("Get inner text via JS")
    public String getInnerText(WebElement element) {
        return (String) js.executeScript("return arguments[0].innerText;", element);
    }

    @Step("Get inner HTML via JS")
    public String getInnerHtml(WebElement element) {
        return (String) js.executeScript("return arguments[0].innerHTML;", element);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Scrolling
    // ─────────────────────────────────────────────────────────────────────────

    @Step("Scroll element into view")
    public void scrollIntoView(WebElement element) {
        js.executeScript("arguments[0].scrollIntoView({behavior:'smooth',block:'center'});",
                element);
    }

    @Step("Scroll to page top")
    public void scrollToTop() {
        js.executeScript("window.scrollTo(0, 0);");
    }

    @Step("Scroll to page bottom")
    public void scrollToBottom() {
        js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
    }

    @Step("Scroll by ({x}, {y})")
    public void scrollBy(int x, int y) {
        js.executeScript(String.format("window.scrollBy(%d, %d);", x, y));
    }

    @Step("Scroll to position ({x}, {y})")
    public void scrollTo(int x, int y) {
        js.executeScript(String.format("window.scrollTo(%d, %d);", x, y));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Highlighting (useful for debugging / screenshots)
    // ─────────────────────────────────────────────────────────────────────────

    @Step("Highlight element")
    public void highlight(WebElement element) {
        js.executeScript(
            "arguments[0].style.border='3px solid red';" +
            "arguments[0].style.backgroundColor='yellow';", element);
    }

    @Step("Remove highlight from element")
    public void removeHighlight(WebElement element) {
        js.executeScript(
            "arguments[0].style.border='';" +
            "arguments[0].style.backgroundColor='';", element);
    }

    /** Flash element border briefly - useful for visual debugging. */
    public void flash(WebElement element) {
        String originalStyle = element.getAttribute("style");
        for (int i = 0; i < 3; i++) {
            js.executeScript("arguments[0].style.border='3px solid red';", element);
            try { Thread.sleep(200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            js.executeScript("arguments[0].style.border='';", element);
            try { Thread.sleep(200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        js.executeScript("arguments[0].setAttribute('style', arguments[1]);",
                element, originalStyle);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Page Info
    // ─────────────────────────────────────────────────────────────────────────

    @Step("Get document ready state")
    public String getReadyState() {
        return (String) js.executeScript("return document.readyState;");
    }

    @Step("Get page scroll Y position")
    public long getScrollY() {
        return (Long) js.executeScript("return window.scrollY;");
    }

    @Step("Get page scroll X position")
    public long getScrollX() {
        return (Long) js.executeScript("return window.scrollX;");
    }

    @Step("Get viewport height")
    public long getViewportHeight() {
        return (Long) js.executeScript("return window.innerHeight;");
    }

    @Step("Get page height")
    public long getPageHeight() {
        return (Long) js.executeScript("return document.body.scrollHeight;");
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  localStorage / sessionStorage
    // ─────────────────────────────────────────────────────────────────────────

    @Step("Get localStorage item: '{key}'")
    public String getLocalStorageItem(String key) {
        return (String) js.executeScript(
                "return window.localStorage.getItem(arguments[0]);", key);
    }

    @Step("Set localStorage item: '{key}'='{value}'")
    public void setLocalStorageItem(String key, String value) {
        js.executeScript(
                "window.localStorage.setItem(arguments[0], arguments[1]);", key, value);
    }

    @Step("Clear localStorage")
    public void clearLocalStorage() {
        js.executeScript("window.localStorage.clear();");
    }

    @Step("Get sessionStorage item: '{key}'")
    public String getSessionStorageItem(String key) {
        return (String) js.executeScript(
                "return window.sessionStorage.getItem(arguments[0]);", key);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Cookies
    // ─────────────────────────────────────────────────────────────────────────

    @Step("Get cookie value: '{name}'")
    public String getCookieValue(String name) {
        String script =
            "var name = arguments[0] + '=';" +
            "var decodedCookie = decodeURIComponent(document.cookie);" +
            "var ca = decodedCookie.split(';');" +
            "for (var i = 0; i < ca.length; i++) {" +
            "  var c = ca[i].trim();" +
            "  if (c.indexOf(name) == 0) { return c.substring(name.length, c.length); }" +
            "}" +
            "return '';";
        return (String) js.executeScript(script, name);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Element Geometry
    // ─────────────────────────────────────────────────────────────────────────

    @Step("Get element bounding rect")
    public java.util.Map<String, Long> getBoundingRect(WebElement element) {
        @SuppressWarnings("unchecked")
        java.util.Map<String, Long> rect =
            (java.util.Map<String, Long>) js.executeScript(
                "var r = arguments[0].getBoundingClientRect();" +
                "return {top: r.top, left: r.left, width: r.width, height: r.height," +
                "        bottom: r.bottom, right: r.right};", element);
        return rect;
    }

    /** Check if the element is visible in the current viewport. */
    @Step("Is element in viewport")
    public boolean isInViewport(WebElement element) {
        return (Boolean) js.executeScript(
            "var rect = arguments[0].getBoundingClientRect();" +
            "return (rect.top >= 0 && rect.left >= 0 && " +
            "rect.bottom <= (window.innerHeight || document.documentElement.clientHeight) &&" +
            "rect.right <= (window.innerWidth || document.documentElement.clientWidth));",
            element);
    }
}
