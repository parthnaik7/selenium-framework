package com.tests.pages;

import com.framework.base.BasePage;
import com.framework.utils.TableUtils;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.Map;

/**
 * HomePage - Page Object for the post-login secure area.
 *
 * Also demonstrates table interaction and alert handling patterns.
 */
public class HomePage extends BasePage {

    @FindBy(css = "h4.subheader")
    private WebElement welcomeBanner;

    @FindBy(linkText = "Logout")
    private WebElement logoutLink;

    private static final By WELCOME_BANNER = By.cssSelector("h4.subheader");
    private static final By LOGOUT_LINK    = By.linkText("Logout");
    private static final By FLASH_MESSAGE  = By.id("flash");

    private final TableUtils tableUtils;

    public HomePage() {
        super();
        this.tableUtils = new TableUtils(driver);
    }

    @Override
    public boolean isPageLoaded() {
        return isDisplayed(WELCOME_BANNER);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Actions
    // ─────────────────────────────────────────────────────────────────────────

    @Step("Get welcome message")
    public String getWelcomeMessage() {
        return getText(welcomeBanner);
    }

    @Step("Logout")
    public LoginPage logout() {
        click(logoutLink);
        return new LoginPage();
    }

    @Step("Get flash message")
    public String getFlashMessage() {
        return getText(FLASH_MESSAGE).replace("×", "").trim();
    }

    @Step("Navigate to sub-page: {path}")
    public HomePage goTo(String path) {
        String base = driver.getCurrentUrl().replaceAll("/secure$", "");
        navigateTo(base + "/" + path);
        return this;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Table interactions (example - adapt to your app's table locator)
    // ─────────────────────────────────────────────────────────────────────────

    @Step("Get all data from main table")
    public List<Map<String, String>> getTableData(By tableLocator) {
        return tableUtils.getTableData(tableLocator);
    }

    @Step("Get row count from table")
    public int getTableRowCount(By tableLocator) {
        return tableUtils.getRowCount(tableLocator);
    }

    @Step("Get table headers")
    public List<String> getTableHeaders(By tableLocator) {
        return tableUtils.getHeaders(tableLocator);
    }

    @Step("Find row where '{column}' = '{value}'")
    public int findTableRow(By tableLocator, String column, String value) {
        return tableUtils.findRowNumber(tableLocator, column, value);
    }
}
