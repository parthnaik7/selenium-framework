package com.tests.tests;

import com.framework.annotations.XrayTest;
import com.framework.base.BaseTest;
import com.framework.data.JsonDataProvider;
import com.framework.listeners.RetryAnalyzer;
import com.tests.pages.HomePage;
import com.tests.pages.LoginPage;
import io.qameta.allure.*;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Map;

/**
 * LoginTest - Demonstrates the full framework feature set:
 *
 * ✅ BaseTest setup/teardown (browser, reports, Xray)
 * ✅ Page Object Model (LoginPage, HomePage)
 * ✅ Allure annotations (@Epic, @Feature, @Story, @Step, @Severity)
 * ✅ Xray annotation (@XrayTest) for JIRA linking
 * ✅ Data-driven via JSON DataProvider
 * ✅ Retry on failure (RetryAnalyzer)
 * ✅ Proper assertions with descriptive messages
 *
 * Target site: <a href="https://the-internet.herokuapp.com/login">Demo site</a> (public demo)
 * Valid credentials: tomsmith / SuperSecretPassword!
 */
@Epic("Authentication")
@Feature("User Login")
public class LoginTest extends BaseTest {

    private LoginPage loginPage;

    @BeforeMethod(alwaysRun = true)
    public void openLoginPage() {
        loginPage = new LoginPage();
        loginPage.navigateTo("https://the-internet.herokuapp.com/login");
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Happy-path tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test(description = "Valid credentials allow access to the secure area")
    @Story("Valid login")
    @Severity(SeverityLevel.BLOCKER)
    @XrayTest(keys = {"PROJ-101"})
    public void validLoginNavigatesToSecureArea() {
        HomePage homePage = loginPage.login("tomsmith", "SuperSecretPassword!");

        Assert.assertTrue(homePage.isPageLoaded(),
                "Secure area should be visible after valid login");
        String welcomeMsg = homePage.getWelcomeMessage();
        Assert.assertFalse(welcomeMsg.isBlank(),
                "Welcome message should not be empty after successful login");
        Assert.assertTrue(welcomeMsg.toLowerCase().contains("secure"),
                "Welcome message should mention 'Secure Area'");
    }

    @Test(description = "Logout returns the user to the login page")
    @Story("Logout")
    @Severity(SeverityLevel.CRITICAL)
    @XrayTest(keys = {"PROJ-102"})
    public void logoutReturnsToLoginPage() {
        HomePage homePage = loginPage.login("tomsmith", "SuperSecretPassword!");
        LoginPage afterLogout = homePage.logout();

        Assert.assertTrue(afterLogout.isPageLoaded(),
                "Login page should be shown after logout");
        Assert.assertTrue(afterLogout.isSuccessDisplayed(),
                "A logout success message should appear");
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Negative tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test(description = "Wrong password shows an error message")
    @Story("Invalid login")
    @Severity(SeverityLevel.CRITICAL)
    @XrayTest(keys = {"PROJ-103"})
    public void invalidPasswordShowsError() {
        LoginPage page = loginPage.loginExpectingError("tomsmith", "WRONG_PASSWORD");

        Assert.assertTrue(page.isErrorDisplayed(),
                "Error message should appear for wrong password");
        String errorMsg = page.getErrorMessage().toLowerCase();
        Assert.assertFalse(errorMsg.isBlank(),
                "Error message should not be empty for invalid password");
        Assert.assertTrue(errorMsg.contains("invalid") || errorMsg.contains("incorrect") || errorMsg.contains("password"),
                "Error message should indicate invalid credentials");
    }

    @Test(description = "Wrong username shows an error message")
    @Story("Invalid login")
    @Severity(SeverityLevel.NORMAL)
    @XrayTest(keys = {"PROJ-104"})
    public void invalidUsernameShowsError() {
        LoginPage page = loginPage.loginExpectingError("nonexistentuser", "SuperSecretPassword!");

        Assert.assertTrue(page.isErrorDisplayed(),
                "Error message should appear for unknown username");
        String errorMsg = page.getErrorMessage().toLowerCase();
        Assert.assertFalse(errorMsg.isBlank(),
                "Error message should not be empty for invalid username");
        Assert.assertTrue(errorMsg.contains("invalid") || errorMsg.contains("incorrect") || errorMsg.contains("username"),
                "Error message should indicate invalid credentials");
    }

    @Test(description = "Empty credentials shows a validation error")
    @Story("Invalid login")
    @Severity(SeverityLevel.MINOR)
    @XrayTest(keys = {"PROJ-105"})
    public void emptyCredentialsShowsError() {
        LoginPage page = loginPage.loginExpectingError("", "");

        Assert.assertTrue(page.isErrorDisplayed(),
                "Validation error should appear for empty credentials");
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Page-level checks
    // ─────────────────────────────────────────────────────────────────────────

    @Test(description = "Login page renders required elements")
    @Story("Page rendering")
    @Severity(SeverityLevel.NORMAL)
    @XrayTest(keys = {"PROJ-106"})
    public void loginPageElementsAreVisible() {
        Assert.assertTrue(loginPage.isPageLoaded(),
                "Login form submit button should be visible");
        Assert.assertTrue(loginPage.isUsernameFieldDisplayed(),
                "Username field should be visible");

        String title = loginPage.getPageTitle();
        Assert.assertFalse(title.isBlank(), "Page title should not be blank");
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Data-driven (JSON DataProvider)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Reads from src/test/resources/testdata/login_data.json.
     * Each JSON object provides "username", "password", "expectedResult".
     *
     * Example login_data.json:
     * <pre>
     * [
     *   {\"username\":\"tomsmith\",\"password\":\"SuperSecretPassword!\",\"expectedResult\":\"success\"},
     *   {\"username\":\"baduser\",\"password\":\"badpass\",\"expectedResult\":\"error\"}
     * ]
     * </pre>
     */
    @Test(dataProvider = "loginJsonData",
          dataProviderClass = JsonDataProvider.class,
          description = "Data-driven login scenarios from JSON")
    @Story("Data-driven login")
    @Severity(SeverityLevel.NORMAL)
    @XrayTest(keys = {"PROJ-107"})
    public void loginWithMultipleCredentials(Map<String, Object> data) {
        String username       = String.valueOf(data.get("username"));
        String password       = String.valueOf(data.get("password"));
        String expectedResult = String.valueOf(data.get("expectedResult"));

        Allure.parameter("username",       username);
        Allure.parameter("expectedResult", expectedResult);

        if ("success".equalsIgnoreCase(expectedResult)) {
            HomePage home = loginPage.login(username, password);
            Assert.assertTrue(home.isPageLoaded(),
                    "Expected successful login for user: " + username);
        } else {
            LoginPage page = loginPage.loginExpectingError(username, password);
            Assert.assertTrue(page.isErrorDisplayed(),
                    "Expected error for user: " + username);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Retry example
    // ─────────────────────────────────────────────────────────────────────────

    @Test(description = "Example: test with automatic retry on failure",
          retryAnalyzer = RetryAnalyzer.class)
    @Story("Retry behaviour")
    @Severity(SeverityLevel.MINOR)
    public void loginWithRetryOnFailure() {
        // This is a sample - in a real suite retry is useful for network flakiness
        HomePage homePage = loginPage.login("tomsmith", "SuperSecretPassword!");
        Assert.assertTrue(homePage.isPageLoaded(), "Page should be loaded");
    }
}
