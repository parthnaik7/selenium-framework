# 🤖 Java Selenium Test Framework

A production-ready, generic Java Selenium 4 automation framework built on **TestNG**, **Page Object Model**, dual HTML/Allure reporting, and optional **Xray/JIRA** integration. Designed to automate any web application with zero boilerplate in test classes.

---

## 📋 Table of Contents

- [Architecture Overview](#architecture-overview)
- [Prerequisites](#prerequisites)
- [Project Setup](#project-setup)
- [Configuration](#configuration)
- [Running Tests](#running-tests)
- [Writing Tests](#writing-tests)
- [Page Object Model](#page-object-model)
- [Utilities Reference](#utilities-reference)
- [Test Data](#test-data)
- [Reporting](#reporting)
- [Xray / JIRA Integration](#xray--jira-integration)
- [CI/CD Integration](#cicd-integration)
- [Troubleshooting](#troubleshooting)

---

## Architecture Overview

```
selenium-framework/
├── pom.xml                                      # Maven build + all dependencies
└── src/
    ├── main/java/com/framework/
    │   ├── base/
    │   │   ├── BaseDriver.java                  # ThreadLocal WebDriver factory
    │   │   ├── BasePage.java                    # All reusable Selenium interactions
    │   │   └── BaseTest.java                    # TestNG setup / teardown
    │   ├── config/
    │   │   └── ConfigReader.java                # Properties + system property override
    │   ├── utils/
    │   │   ├── WaitUtils.java                   # Explicit, fluent, custom waits
    │   │   ├── TableUtils.java                  # HTML table reading & searching
    │   │   ├── JavaScriptUtils.java             # JS execution, scroll, highlight
    │   │   ├── ScreenshotUtils.java             # PNG capture → Allure / file
    │   │   ├── BrowserUtils.java                # Cookies, tabs, window size
    │   │   ├── DropdownUtils.java               # Native & custom dropdowns
    │   │   ├── FileUtils.java                   # Read / write test files
    │   │   └── DateUtils.java                   # Date formatting helpers
    │   ├── listeners/
    │   │   ├── TestNGListener.java              # ITestListener (Allure + Extent + log)
    │   │   ├── RetryAnalyzer.java               # IRetryAnalyzer - retry flaky tests
    │   │   └── RetryTransformer.java            # Apply retry globally via testng.xml
    │   ├── reporting/
    │   │   ├── AllureManager.java               # Allure steps, attachments, labels
    │   │   ├── ExtentReportManager.java         # Thread-safe Extent HTML report
    │   │   └── XrayReporter.java                # Xray Cloud + Server/DC publisher
    │   ├── data/
    │   │   ├── ExcelDataProvider.java           # Apache POI xlsx reader + @DataProvider
    │   │   └── JsonDataProvider.java            # Jackson JSON reader + @DataProvider
    │   └── annotations/
    │       └── XrayTest.java                    # @XrayTest(keys={"PROJ-101"})
    └── test/
        ├── java/com/tests/
        │   ├── pages/
        │   │   ├── LoginPage.java               # Sample Page Object
        │   │   └── HomePage.java                # Sample Page Object
        │   └── tests/
        │       └── LoginTest.java               # Sample test class
        └── resources/
            ├── testng.xml                       # Full regression suite
            ├── testng-smoke.xml                 # Smoke suite (groups=smoke)
            ├── testng-parallel.xml              # Parallel execution suite
            ├── allure.properties                # Allure output + JIRA link patterns
            └── testdata/
                └── login_data.json             # Sample JSON test data
```

### Key Design Principles

| Principle | How it's applied |
|-----------|-----------------|
| **KISS** | Each class has one clear job; no "god" classes |
| **DRY** | All Selenium interactions live in `BasePage` / utils; tests never repeat driver code |
| **SOLID - SRP** | `BaseDriver` only manages WebDriver lifecycle; `WaitUtils` only manages waits |
| **SOLID - OCP** | Extend `BasePage` and `BaseTest`; never modify them for new pages/tests |
| **SOLID - DIP** | Test classes depend on Page Objects, not on WebDriver directly |
| **Thread Safety** | `ThreadLocal<WebDriver>` in `BaseDriver` → safe parallel execution |
| **Fail-fast Config** | `ConfigReader.getRequired()` throws immediately if a key is missing |

---

## Prerequisites

| Tool | Minimum Version | Notes |
|------|----------------|-------|
| **Java JDK** | 17 | `java -version` |
| **Maven** | 3.9+ | `mvn -version` |
| **Chrome** | Latest stable | Auto-managed by WebDriverManager |
| **Firefox** | Latest | Optional |
| **Allure CLI** | 2.27+ | Only needed to open interactive report |

### Install Allure CLI (optional, for interactive report)

```bash
# macOS
brew install allure

# Linux
sudo apt-get install allure

# Windows (Scoop)
scoop install allure
```

---

## Project Setup

### 1 · Clone the repository

```bash
git clone https://github.com/your-org/selenium-framework.git
cd selenium-framework
```

### 2 · Install dependencies

```bash
mvn clean install -DskipTests
```

### 3 · Verify the build

```bash
mvn test -Dbrowser=chrome -Dheadless=true
```

If you see `BUILD SUCCESS` and a report in `target/extent-report/`, the framework is wired up correctly.

---

## Configuration

All settings live in `src/main/resources/config.properties`. **Never commit secrets** - use environment variables or a CI secret manager instead.

```properties
# Application
env          = dev
base.url     = https://your-app.example.com

# Browser: chrome | firefox | edge | safari
browser      = chrome
headless     = false

# Timeouts (seconds)
explicit.wait.seconds      = 10
page.load.timeout.seconds  = 30

# Reporting
report.title = My Project - Automation Report

# Xray/JIRA (optional)
xray.enabled         = false
xray.mode            = server          # or: cloud
xray.jira.url        = https://jira.example.com
xray.project.key     = PROJ
```

### Environment-specific overrides

Create `config-staging.properties` alongside `config.properties`:

```properties
base.url = https://staging.your-app.example.com
```

Then run with `-Denv=staging` and the staging file is merged on top of the base config.

### Runtime overrides (highest priority)

Any property can be overridden from the command line:

```bash
mvn test -Dbrowser=firefox -Dheadless=true -Denv=staging -Dbase.url=https://qa.example.com
```

---

## Running Tests

### Basic run

```bash
# Default suite (testng.xml), Chrome, headed
mvn test

# Firefox, headless
mvn test -Dbrowser=firefox -Dheadless=true

# Against staging environment
mvn test -Denv=staging
```

### Suite selection

```bash
# Full regression (default)
mvn test -Pdefault

# Smoke tests only  (groups = "smoke")
mvn test -Psmoke

# Parallel execution (4 threads)
mvn test -Pparallel

# Custom XML
mvn test -Dtestng.suite.file=src/test/resources/testng-smoke.xml
```

### Run a single test class

```bash
mvn test -Dtest=LoginTest
```

### Run a single test method

```bash
mvn test -Dtest="LoginTest#validLoginNavigatesToSecureArea"
```

### Parallel execution

Edit `testng-parallel.xml` to change thread count:

```xml
<suite parallel="methods" thread-count="4">
```

`BaseDriver` uses `ThreadLocal` so each thread gets its own browser - no sharing.

---

## Viewing Reports

### Extent Report (HTML - always available)

```bash
# Open after a test run (auto-generated, no extra install needed)
open target/extent-report/ExtentReport_*.html
# Windows: start target\extent-report\ExtentReport_*.html
```

### Allure Report (interactive - requires Allure CLI)

```bash
# Generate and open
mvn allure:report
mvn allure:open

# OR single command (serves live)
allure serve target/allure-results
```

### Logs

```
target/logs/test-run.log    # Full run log
target/logs/errors.log      # Errors only
```

### Screenshots

Failed-test screenshots are automatically saved to:
```
target/screenshots/<TestName>_<timestamp>.png
```
and attached to both Allure and Extent reports.

---

## Writing Tests

### Minimal test class

```java
@Epic("Shopping Cart")
@Feature("Add to Cart")
public class CartTest extends BaseTest {

    private CartPage cartPage;

    @BeforeMethod
    public void openCartPage() {
        cartPage = new CartPage();
        cartPage.navigateTo("/cart");
    }

    @Test(description = "Item count increments after adding a product")
    @Story("Add item")
    @Severity(SeverityLevel.CRITICAL)
    @XrayTest(keys = {"PROJ-200"})
    public void addItemIncrementsCount() {
        cartPage.addProduct("Widget A");
        Assert.assertEquals(cartPage.getCartCount(), 1, "Cart should have 1 item");
    }
}
```

### Groups (smoke / regression / etc.)

```java
@Test(groups = {"smoke", "regression"}, description = "Homepage loads successfully")
public void homepageLoads() { ... }
```

Run only smoke: `mvn test -Psmoke`

### Data-driven with JSON

```java
@Test(dataProvider = "loginJsonData", dataProviderClass = JsonDataProvider.class)
public void loginScenarios(Map<String, Object> data) {
    String user = (String) data.get("username");
    // ...
}
```

### Data-driven with Excel

```java
@Test(dataProvider = "loginData", dataProviderClass = ExcelDataProvider.class)
public void loginFromExcel(Map<String, String> data) {
    loginPage.login(data.get("username"), data.get("password"));
}
```

---

## Page Object Model

Every page extends `BasePage`:

```java
public class SearchPage extends BasePage {

    // PageFactory locators (recommended for pages with many elements)
    @FindBy(name = "q")
    private WebElement searchInput;

    // Or inline By locators (simpler for small pages)
    private static final By RESULTS = By.cssSelector(".result-item");

    public SearchPage() { super(); }

    @Override
    public boolean isPageLoaded() {
        return isDisplayed(By.name("q"));
    }

    @Step("Search for: {term}")
    public SearchPage search(String term) {
        type(searchInput, term);
        pressKey(By.name("q"), Keys.RETURN);
        return this;
    }

    @Step("Get result count")
    public int getResultCount() {
        return getElementCount(RESULTS);
    }
}
```

### Page chaining (fluent API)

```java
new LoginPage()
    .enterUsername("admin")
    .enterPassword("secret")
    .clickLogin()                 // returns HomePage
    .goTo("settings")
    .logout();
```

---

## Utilities Reference

All utilities are available via delegation from `BasePage` (no need to instantiate them in test classes).

### WaitUtils

```java
waitForVisible(By.id("result"));
waitForClickable(By.cssSelector(".btn"));
waitForInvisibility(By.id("spinner"));
waitForText(By.id("msg"), "Success");
waitForUrlContains("/dashboard");
waitForAlert();

// Fluent wait (custom)
waitUtils.fluentWait(20, 1000, NoSuchElementException.class)
         .until(d -> d.findElement(By.id("data")).isDisplayed());
```

### TableUtils

```java
TableUtils table = new TableUtils(driver);
By TABLE = By.id("dataTable");

int rows = table.getRowCount(TABLE);
List<String> headers = table.getHeaders(TABLE);
String cell = table.getCellText(TABLE, 2, 3);      // row 2, col 3 (1-based)
List<Map<String,String>> data = table.getTableData(TABLE);
int row = table.findRowNumber(TABLE, "Name", "Alice");
table.clickActionInRow(TABLE, "Name", "Alice", By.linkText("Edit"));
boolean sorted = table.isColumnSortedAscending(TABLE, "Date");
```

### Alerts

```java
String msg = getAlertText();
acceptAlert();
dismissAlert();
typeInAlert("confirm input");
```

### Frames / iFrames

```java
switchToFrame("iFrameId");
switchToFrame(0);
switchToFrame(By.cssSelector("iframe.modal"));
switchToParentFrame();
switchToDefaultContent();
```

### Windows / Tabs

```java
String main = getMainWindowHandle();
openNewTab();
switchToNewWindow(main);
switchToWindowByTitle("Payment Gateway");
closeCurrentWindowAndSwitch(main);
```

### Dropdowns

```java
selectByText(By.id("country"), "New Zealand");
selectByValue(By.id("state"), "AKL");
selectByIndex(By.id("size"), 2);
String selected = getSelectedOption(By.id("country"));
List<String> options = getAllOptions(By.id("country"));
```

### JavaScript

```java
jsClick(By.id("hiddenBtn"));
scrollToElement(By.id("footer"));
jsUtils.highlight(element);
jsUtils.setValue(element, "overridden text");
jsUtils.getLocalStorageItem("token");
boolean visible = jsUtils.isInViewport(element);
```

---

## Test Data

### JSON

Place files in `src/test/resources/testdata/`:

```json
[
  { "username": "admin", "password": "secret", "role": "ADMIN" },
  { "username": "viewer", "password": "pass",  "role": "READ_ONLY" }
]
```

Read programmatically:

```java
List<Map<String, Object>> data = JsonDataProvider.getArrayData("users.json");
List<Map<String, Object>> admins = JsonDataProvider.filterBy(data, "role", "ADMIN");
```

### Excel

Place `.xlsx` files in `src/test/resources/testdata/`. Row 1 = headers.

```java
List<Map<String, String>> rows = ExcelDataProvider.getSheetData("TestData.xlsx", "Sheet1");
String val = ExcelDataProvider.getCellValue("TestData.xlsx", "Sheet1", 0, "Username");

// Filtered read
List<Map<String, String>> active = ExcelDataProvider.getFilteredData(
        "TestData.xlsx", "Users", "Status", "Active");
```

---

## Xray / JIRA Integration

### Enable Xray

In `config.properties`:

```properties
xray.enabled    = true
xray.mode       = server          # or cloud
xray.jira.url   = https://jira.yourcompany.com
xray.project.key = PROJ
xray.username   = automation-bot
xray.password   = ${JIRA_PASSWORD}   # use CI env var
```

### Tag your tests

```java
@Test
@XrayTest(keys = {"PROJ-101", "PROJ-102"})   // link to one or more JIRA tests
public void checkoutFlow() { ... }
```

### What happens at runtime

1. Suite starts → `XrayReporter.init()` authenticates (Cloud only)
2. Each `@AfterMethod` buffers the result (PASS / FAIL / TODO)
3. Suite ends → `XrayReporter.publishResults()` POSTs one JSON payload

### Xray Cloud setup

```properties
xray.mode                = cloud
xray.cloud.client.id     = <from Xray Cloud API Keys page>
xray.cloud.client.secret = <from Xray Cloud API Keys page>
xray.project.key         = PROJ
```

### Verify payload (dry run)

Set `xray.enabled=false` and check the log output at DEBUG level - the payload JSON is logged before the HTTP call.

---

## CI/CD Integration

### GitHub Actions

```yaml
name: Selenium Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Run Tests
        run: mvn test -Dheadless=true -Dbrowser=chrome
        env:
          XRAY_PASSWORD: ${{ secrets.XRAY_PASSWORD }}

      - name: Upload Allure Results
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: allure-results
          path: target/allure-results/

      - name: Allure Report
        uses: simple-elf/allure-report-action@master
        if: always()
        with:
          allure_results: target/allure-results

      - name: Upload Extent Report
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: extent-report
          path: target/extent-report/
```

### Jenkins Pipeline

```groovy
pipeline {
    agent any
    tools { maven 'Maven-3.9'; jdk 'JDK-17' }

    stages {
        stage('Checkout') { steps { checkout scm } }

        stage('Test') {
            steps {
                sh 'mvn clean test -Dheadless=true -Dbrowser=chrome'
            }
            post {
                always {
                    allure([
                        includeProperties: false,
                        jdk: '',
                        results: [[path: 'target/allure-results']]
                    ])
                    publishHTML([
                        allowMissing: true,
                        alwaysLinkToLastBuild: true,
                        keepAll: true,
                        reportDir: 'target/extent-report',
                        reportFiles: '*.html',
                        reportName: 'Extent Report'
                    ])
                }
            }
        }
    }
}
```

---

## Troubleshooting

### `WebDriver is not initialised for this thread`

You called a page method before `BaseTest.setUp()` ran, or your test class does not extend `BaseTest`. Ensure every test class `extends BaseTest`.

### `ChromeDriver version mismatch`

WebDriverManager auto-downloads the correct chromedriver. If you're behind a proxy:

```properties
# in config.properties
wdm.proxy=http://proxy.example.com:8080
wdm.proxyUser=user
wdm.proxyPass=pass
```

Or set `WDM_PROXY` environment variable.

### `TimeoutException` on waits

1. Increase `explicit.wait.seconds` in `config.properties`
2. Verify the locator is correct (use browser DevTools)
3. Check if the element is inside an iFrame - call `switchToFrame()` first
4. Check for a page-loading spinner blocking the element

### Allure report is empty

1. Confirm `target/allure-results/` contains `.json` files after the run
2. Run `allure serve target/allure-results` (not `allure report`)
3. Check the AspectJ agent is in the Surefire `argLine` in `pom.xml`

### Xray results not appearing in JIRA

1. Confirm `xray.enabled=true` in config or `-Dxray.enabled=true`
2. Check logs for `Xray Reporter` entries at INFO/ERROR level
3. Confirm the test keys in `@XrayTest` exist as issues in the project
4. For Cloud: verify the Client ID/Secret have `Import Test Execution Results` permission

### Parallel tests interfere with each other

Ensure all state is via `BaseDriver.getDriver()` (ThreadLocal) and not stored in static fields on page objects. Each `BasePage` constructor fetches the driver fresh from the thread-local store.

---

## Contributing

1. Fork → feature branch → PR
2. One class, one responsibility
3. All new utilities must include `@Step` annotations for Allure tracing
4. Add a corresponding test or update an existing one
5. Run `mvn test -Dheadless=true` before raising the PR

---

## License

MIT - free to use, modify, and distribute.
