package com.framework.reporting;

import com.framework.config.ConfigReader;
import io.qameta.allure.Allure;
import io.qameta.allure.model.Label;
import io.qameta.allure.model.Link;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/**
 * AllureManager - Programmatic Allure enrichment utilities.
 *
 * Complements the declarative Allure annotations (@Step, @Description, etc.)
 * with runtime-driven additions such as dynamic labels, attachments from variables,
 * and environment metadata.
 *
 * All methods are static - call them from anywhere in the test layer.
 */
@SuppressWarnings("all")
public final class AllureManager {

    private static final Logger log = LogManager.getLogger(AllureManager.class);

    private AllureManager() {}

    // ─────────────────────────────────────────────────────────────────────────
    //  Environment
    // ─────────────────────────────────────────────────────────────────────────

    /** Add standard environment properties to the Allure report. */
    public static void addEnvironmentInfo() {
        Allure.getLifecycle().updateTestCase(tc -> {
            tc.getParameters().add(
                new io.qameta.allure.model.Parameter()
                    .setName("env").setValue(ConfigReader.get("env", "default")));
            tc.getParameters().add(
                new io.qameta.allure.model.Parameter()
                    .setName("browser").setValue(ConfigReader.get("browser", "chrome")));
            tc.getParameters().add(
                new io.qameta.allure.model.Parameter()
                    .setName("base.url").setValue(ConfigReader.get("base.url", "")));
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Steps (runtime)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Log a named step in the Allure timeline (useful for steps that can't
     * use the @Step annotation, e.g. utility lambdas).
     */
    public static void step(String name) {
        Allure.step(name);
    }


    // ─────────────────────────────────────────────────────────────────────────
    //  Attachments
    // ─────────────────────────────────────────────────────────────────────────

    public static void attachScreenshot(byte[] bytes, String name) {
        if (bytes == null) { log.warn("Screenshot bytes were null - skipping attachment"); return; }
        Allure.addAttachment(name, "image/png", new ByteArrayInputStream(bytes), "png");
    }

    public static void attachText(String content, String name) {
        Allure.addAttachment(name, "text/plain",
                new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)), "txt");
    }

    @SuppressWarnings("unused")
    public static void attachHtml(String html, String name) {
        Allure.addAttachment(name, "text/html",
                new ByteArrayInputStream(html.getBytes(StandardCharsets.UTF_8)), "html");
    }

    @SuppressWarnings("unused")
    public static void attachJson(String json, String name) {
        Allure.addAttachment(name, "application/json",
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)), "json");
    }

    @SuppressWarnings("unused")
    public static void attachXml(String xml, String name) {
        Allure.addAttachment(name, "application/xml",
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), "xml");
    }

    @SuppressWarnings("unused")
    public static void attachCsv(String csv, String name) {
        Allure.addAttachment(name, "text/csv",
                new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), "csv");
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Labels / Links (runtime - useful when key isn't known at compile time)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Add severity label to the test case.
     * Valid values: "blocker", "critical", "normal", "minor", "trivial"
     */
    @SuppressWarnings("unused")
    public static void addSeverity(String severity) {
        Allure.getLifecycle().updateTestCase(
            tc -> tc.getLabels().add(
                new Label().setName("severity").setValue(severity)));
    }

    @SuppressWarnings("unused")
    public static void addEpic(String epic) {
        Allure.getLifecycle().updateTestCase(
            tc -> tc.getLabels().add(
                new Label().setName("epic").setValue(epic)));
    }

    @SuppressWarnings("unused")
    public static void addFeature(String feature) {
        Allure.getLifecycle().updateTestCase(
            tc -> tc.getLabels().add(
                new Label().setName("feature").setValue(feature)));
    }

    @SuppressWarnings("unused")
    public static void addStory(String story) {
        Allure.getLifecycle().updateTestCase(
            tc -> tc.getLabels().add(
                new Label().setName("story").setValue(story)));
    }

    @SuppressWarnings("unused")
    public static void addTag(String tag) {
        Allure.getLifecycle().updateTestCase(
            tc -> tc.getLabels().add(
                new Label().setName("tag").setValue(tag)));
    }

    @SuppressWarnings("unused")
    public static void addOwner(String owner) {
        Allure.getLifecycle().updateTestCase(
            tc -> tc.getLabels().add(
                new Label().setName("owner").setValue(owner)));
    }

    @SuppressWarnings("unused")
    public static void addJiraLink(String issueKey) {
        String jiraUrl = ConfigReader.get("jira.url", "https://jira.example.com");
        Allure.getLifecycle().updateTestCase(tc ->
            tc.getLinks().add(
                new Link()
                    .setName(issueKey)
                    .setUrl(jiraUrl + "/browse/" + issueKey)
                    .setType("issue")));
    }

    @SuppressWarnings("unused")
    public static void addXrayTestLink(String testKey) {
        String jiraUrl = ConfigReader.get("jira.url", "https://jira.example.com");
        Allure.getLifecycle().updateTestCase(tc ->
            tc.getLinks().add(
                new Link()
                    .setName(testKey)
                    .setUrl(jiraUrl + "/browse/" + testKey)
                    .setType("tms")));
    }
}
