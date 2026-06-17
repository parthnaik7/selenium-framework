package com.framework.utils;

import io.qameta.allure.Step;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import java.util.ArrayList;
import java.util.List;

/**
 * DropdownUtils - Comprehensive HTML Select dropdown handling.
 *
 * Handles both native {@code <select>} elements and custom JS dropdowns
 * (click-to-open / list-item pattern).
 */
public class DropdownUtils {

    private static final Logger log = LogManager.getLogger(DropdownUtils.class);

    private final WebDriver driver;
    private final WaitUtils waitUtils;

    public DropdownUtils(WebDriver driver) {
        this.driver    = driver;
        this.waitUtils = new WaitUtils(driver);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Native <select>
    // ─────────────────────────────────────────────────────────────────────────

    @Step("Select by visible text '{text}' from: {locator}")
    public void selectByText(By locator, String text) {
        log.info("Selecting '{}' from {}", text, locator);
        new Select(waitUtils.waitForVisible(locator)).selectByVisibleText(text);
    }

    @Step("Select by value '{value}' from: {locator}")
    public void selectByValue(By locator, String value) {
        log.info("Selecting value '{}' from {}", value, locator);
        new Select(waitUtils.waitForVisible(locator)).selectByValue(value);
    }

    @Step("Select by index {index} from: {locator}")
    public void selectByIndex(By locator, int index) {
        log.info("Selecting index {} from {}", index, locator);
        new Select(waitUtils.waitForVisible(locator)).selectByIndex(index);
    }

    @Step("Deselect all options: {locator}")
    public void deselectAll(By locator) {
        new Select(waitUtils.waitForVisible(locator)).deselectAll();
    }

    @Step("Get first selected option: {locator}")
    public String getSelectedOption(By locator) {
        String text = new Select(waitUtils.waitForVisible(locator))
                .getFirstSelectedOption().getText().trim();
        log.debug("Selected: '{}'", text);
        return text;
    }

    @Step("Get all selected options: {locator}")
    public List<String> getAllSelectedOptions(By locator) {
        List<String> result = new ArrayList<>();
        new Select(waitUtils.waitForVisible(locator)).getAllSelectedOptions()
                .forEach(o -> result.add(o.getText().trim()));
        return result;
    }

    @Step("Get all options: {locator}")
    public List<String> getAllOptions(By locator) {
        List<String> result = new ArrayList<>();
        new Select(waitUtils.waitForVisible(locator)).getOptions()
                .forEach(o -> result.add(o.getText().trim()));
        return result;
    }

    @Step("Is option '{text}' present: {locator}")
    public boolean isOptionPresent(By locator, String text) {
        return getAllOptions(locator).contains(text);
    }

    @Step("Is multi-select: {locator}")
    public boolean isMultiSelect(By locator) {
        return new Select(waitUtils.waitForVisible(locator)).isMultiple();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Custom / JS Dropdowns (click-to-open pattern)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Handle custom dropdowns that work via click-to-expand + click-on-option.
     *
     * @param triggerLocator  the element to click to open the dropdown
     * @param optionsLocator  the locator for all available options
     * @param optionText      visible text of the option to select
     */
    @Step("Select '{optionText}' from custom dropdown")
    public void selectFromCustomDropdown(By triggerLocator, By optionsLocator, String optionText) {
        log.info("Custom dropdown: click trigger then select '{}'", optionText);
        waitUtils.waitForClickable(triggerLocator).click();
        waitUtils.waitForAllVisible(optionsLocator).stream()
                .filter(el -> el.getText().trim().equalsIgnoreCase(optionText))
                .findFirst()
                .orElseThrow(() -> new org.openqa.selenium.NoSuchElementException(
                        "Option '" + optionText + "' not found in custom dropdown"))
                .click();
    }

    /** Get currently displayed text in a custom dropdown (read the trigger text). */
    @Step("Get custom dropdown selected text: {triggerLocator}")
    public String getCustomDropdownText(By triggerLocator) {
        return waitUtils.waitForVisible(triggerLocator).getText().trim();
    }
}
