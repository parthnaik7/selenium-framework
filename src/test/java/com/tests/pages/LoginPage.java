package com.tests.pages;

import com.framework.base.BasePage;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.WebElement;

/**
 * LoginPage - Page Object for the Login screen.
 *
 * Two locator styles are intentionally shown:
 *  - @FindBy (PageFactory): declared as fields, initialised by PageFactory in BasePage
 *  - By.*   (inline):       passed directly to BasePage interaction methods
 *
 * Both are valid; choose one style consistently within your project.
 * The @FindBy approach is cleaner for pages with many elements.
 */
public class LoginPage extends BasePage {

    // ─── @FindBy locators (PageFactory) ──────────────────────────────────────
    @FindBy(id = "username")
    private WebElement usernameField;

    @FindBy(id = "password")
    private WebElement passwordField;

    @FindBy(css = "button[type='submit']")
    private WebElement loginButton;

    @FindBy(css = ".flash.error")
    private WebElement errorMessage;

    @FindBy(css = ".flash.success")
    private WebElement successMessage;

    // ─── By locators (inline style) ──────────────────────────────────────────
    private static final By USERNAME  = By.id("username");
    private static final By PASSWORD  = By.id("password");
    private static final By SUBMIT    = By.cssSelector("button[type='submit']");
    private static final By ERROR_MSG = By.cssSelector(".flash.error");
    private static final By SUCCESS_MSG = By.cssSelector(".flash.success");

    // ─────────────────────────────────────────────────────────────────────────

    public LoginPage() {
        super();
    }

    @Override
    public boolean isPageLoaded() {
        return isDisplayed(SUBMIT);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Page Actions
    // ─────────────────────────────────────────────────────────────────────────

    @Step("Enter username: {username}")
    public LoginPage enterUsername(String username) {
        type(usernameField, username);
        return this;
    }

    @Step("Enter password")
    public LoginPage enterPassword(String password) {
        type(passwordField, password);
        return this;
    }

    @Step("Click Login button")
    public HomePage clickLogin() {
        click(loginButton);
        return new HomePage();
    }

    @Step("Click Login - expecting failure")
    public LoginPage clickLoginExpectingFailure() {
        click(loginButton);
        return this;
    }

    /**
     * Full login flow - chainable shorthand.
     *
     * @return HomePage on successful navigation
     */
    @Step("Login with username '{username}'")
    public HomePage login(String username, String password) {
        return enterUsername(username)
               .enterPassword(password)
               .clickLogin();
    }

    @Step("Login expecting error")
    public LoginPage loginExpectingError(String username, String password) {
        enterUsername(username);
        enterPassword(password);
        return clickLoginExpectingFailure();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Assertions / Verifications
    // ─────────────────────────────────────────────────────────────────────────

    @Step("Get error message text")
    public String getErrorMessage() {
        waitForVisible(ERROR_MSG);
        return getText(errorMessage).replace("×", "").trim();
    }

    @Step("Get success message text")
    public String getSuccessMessage() {
        waitForVisible(SUCCESS_MSG);
        return getText(successMessage).replace("×", "").trim();
    }

    @Step("Is error message displayed")
    public boolean isErrorDisplayed() {
        return isDisplayed(ERROR_MSG);
    }

    @Step("Is success message displayed")
    public boolean isSuccessDisplayed() {
        return isDisplayed(SUCCESS_MSG);
    }

    @Step("Is username field displayed")
    public boolean isUsernameFieldDisplayed() {
        return isDisplayed(USERNAME);
    }

    @Step("Get username field placeholder")
    public String getUsernamePlaceholder() {
        return getAttribute(USERNAME, "placeholder");
    }
}
