package com.framework.reporting;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.framework.annotations.XrayTest;
import com.framework.config.ConfigReader;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * XrayReporter - Publishes test execution results to Xray (JIRA plugin).
 *
 * Supports both modes:
 *
 *  ┌─────────────────────────────────────────────────────────────────────┐
 *  │  Xray Server/DC  - uses basic or bearer auth against your JIRA host │
 *  │  Endpoint: POST {jira.url}/rest/raven/1.0/import/execution          │
 *  │                                                                     │
 *  │  Xray Cloud      - exchanges Client ID+Secret for a token first     │
 *  │  Token endpoint : POST https://xray.cloud.getxray.app/api/v2/auth   │
 *  │  Import endpoint: POST https://xray.cloud.getxray.app/api/v2/       │
 *  │                       import/execution                              │
 *  └─────────────────────────────────────────────────────────────────────┘
 *
 * Required config.properties keys (when xray.enabled=true):
 * <pre>
 *   xray.mode                = server   # or: cloud
 *   xray.jira.url            = https://jira.yourcompany.com
 *   xray.project.key         = PROJ
 *   xray.execution.summary   = Automated Regression - Sprint 42
 *
 *   # Server / DC auth (choose one):
 *   xray.username            = automation-user
 *   xray.password            = s3cr3t
 *   # - or -
 *   xray.bearer.token        = eyJhbGciOi...
 *
 *   # Cloud auth:
 *   xray.cloud.client.id     = abc123
 *   xray.cloud.client.secret = xyz456
 * </pre>
 *
 * Results are buffered in-memory and sent in a single batch at suite end.
 */
public final class XrayReporter {

    private static final Logger log = LogManager.getLogger(XrayReporter.class);

    // Xray Cloud base URL
    private static final String XRAY_CLOUD_BASE  = "https://xray.cloud.getxray.app/api/v2";
    private static final String XRAY_CLOUD_AUTH  = XRAY_CLOUD_BASE + "/authenticate";
    private static final String XRAY_CLOUD_EXEC  = XRAY_CLOUD_BASE + "/import/execution";

    private static final ObjectMapper JSON = new ObjectMapper();

    /** Map of testMethod → result record (accumulated during the run). */
    private static final Map<String, ResultRecord> results = new ConcurrentHashMap<>();

    private static String cloudToken = null;   // cached after first auth

    private XrayReporter() {}

    // ─────────────────────────────────────────────────────────────────────────
    //  Lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    /** Called at suite start - validates config, obtains Cloud token if needed. */
    public static void init() {
        if (!ConfigReader.getBool("xray.enabled", false)) return;

        String mode = ConfigReader.get("xray.mode", "server");
        log.info("Xray Reporter initialised in [{}] mode", mode);

        if ("cloud".equalsIgnoreCase(mode)) {
            cloudToken = authenticateCloud();
        }
    }

    /**
     * Record a single test result.  Called from BaseTest.tearDown() after each test.
     *
     * @param testMethodName  the Java method name (used as key)
     * @param xrayStatus      Xray status string: PASS | FAIL | TODO | EXECUTING
     * @param comment         optional comment (may be null)
     */
    public static void recordResult(String testMethodName, String xrayStatus, String comment) {
        results.put(testMethodName, new ResultRecord(testMethodName, xrayStatus, comment,
                ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)));
        log.debug("Xray: buffered result [{} → {}]", testMethodName, xrayStatus);
    }

    /**
     * Record a result when we have the test method's reflection object (so we can
     * read the {@link XrayTest} annotation for test keys).
     */
    public static void recordResult(Method method, String xrayStatus, String comment) {
        String[] keys = resolveKeys(method);
        String methodName = method.getName();
        results.put(methodName, new ResultRecord(methodName, xrayStatus, comment,
                ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME), keys));
        log.debug("Xray: buffered result [{} → {} keys={}]",
                methodName, xrayStatus, Arrays.toString(keys));
    }

    /** Send all buffered results to Xray. Called once at suite end. */
    public static void publishResults() {
        if (results.isEmpty()) {
            log.info("Xray: no results to publish.");
            return;
        }

        try {
            String mode = ConfigReader.get("xray.mode", "server");
            String payload = buildPayload();
            log.debug("Xray payload:\n{}", payload);

            if ("cloud".equalsIgnoreCase(mode)) {
                postToCloud(payload);
            } else {
                postToServer(payload);
            }
        } catch (Exception e) {
            log.error("Xray: failed to publish results - {}", e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Payload Building
    // ─────────────────────────────────────────────────────────────────────────

    private static String buildPayload() throws Exception {
        ObjectNode root   = JSON.createObjectNode();
        ObjectNode info   = root.putObject("info");
        ArrayNode  tests  = root.putArray("tests");

        String projectKey = ConfigReader.getRequired("xray.project.key");
        String summary    = ConfigReader.get("xray.execution.summary",
                "Automated Test Execution - " + now());

        info.put("summary",     summary);
        info.put("description", "Automated execution via Selenium Framework");
        info.put("project",     projectKey);

        // Optional: fix version / test plan
        String version = ConfigReader.get("xray.fix.version", "");
        if (!version.isBlank()) info.put("version", version);

        String testPlan = ConfigReader.get("xray.test.plan.key", "");
        if (!testPlan.isBlank()) info.put("testPlanKey", testPlan);

        for (ResultRecord r : results.values()) {
            if (r.keys().length == 0) {
                // No annotation keys - publish by method name (best-effort)
                ObjectNode t = tests.addObject();
                t.put("testKey", "");        // Xray ignores empty key - creates evidence-only
                t.put("status",  r.status());
                t.put("comment", orEmpty(r.comment()));
                t.put("finish",  r.finishedAt());
            } else {
                // Each key gets its own test result entry
                for (String key : r.keys()) {
                    ObjectNode t = tests.addObject();
                    t.put("testKey", key);
                    t.put("status",  r.status());
                    t.put("comment", orEmpty(r.comment()));
                    t.put("finish",  r.finishedAt());
                }
            }
        }

        return JSON.writerWithDefaultPrettyPrinter().writeValueAsString(root);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  HTTP: Server / DC
    // ─────────────────────────────────────────────────────────────────────────

    private static void postToServer(String payload) throws Exception {
        String jiraUrl = ConfigReader.getRequired("xray.jira.url");
        String url     = jiraUrl.replaceAll("/+$", "") + "/rest/raven/1.0/import/execution";

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(url);
            request.setHeader("Content-Type", "application/json");
            request.setHeader("Accept",       "application/json");
            applyServerAuth(request);
            request.setEntity(new StringEntity(payload, ContentType.APPLICATION_JSON));

            try (CloseableHttpResponse response = client.execute(request)) {
                int code = response.getCode();
                if (code == 200 || code == 201) {
                    log.info("✅ Xray (Server): results published successfully [HTTP {}]", code);
                } else {
                    log.error("❌ Xray (Server): HTTP {} - check credentials and project key", code);
                }
            }
        }
    }

    private static void applyServerAuth(HttpPost request) {
        String bearer = ConfigReader.get("xray.bearer.token", "");
        if (!bearer.isBlank()) {
            request.setHeader("Authorization", "Bearer " + bearer);
            return;
        }
        String username = ConfigReader.get("xray.username", "");
        String password = ConfigReader.get("xray.password", "");
        if (!username.isBlank()) {
            String encoded = Base64.getEncoder().encodeToString(
                    (username + ":" + password).getBytes(StandardCharsets.UTF_8));
            request.setHeader("Authorization", "Basic " + encoded);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  HTTP: Cloud
    // ─────────────────────────────────────────────────────────────────────────

    private static String authenticateCloud() {
        String clientId     = ConfigReader.getRequired("xray.cloud.client.id");
        String clientSecret = ConfigReader.getRequired("xray.cloud.client.secret");

        ObjectNode body = JSON.createObjectNode();
        body.put("client_id",     clientId);
        body.put("client_secret", clientSecret);

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(XRAY_CLOUD_AUTH);
            request.setHeader("Content-Type", "application/json");
            request.setEntity(new StringEntity(body.toString(), ContentType.APPLICATION_JSON));

            try (CloseableHttpResponse response = client.execute(request)) {
                int code = response.getCode();
                if (code == 200) {
                    String token = new String(
                            response.getEntity().getContent().readAllBytes(),
                            StandardCharsets.UTF_8).replace("\"", "");
                    log.info("Xray Cloud: authentication successful");
                    return token;
                } else {
                    log.error("Xray Cloud auth failed: HTTP {}", code);
                }
            }
        } catch (Exception e) {
            log.error("Xray Cloud auth error: {}", e.getMessage(), e);
        }
        return null;
    }

    private static void postToCloud(String payload) throws Exception {
        if (cloudToken == null) {
            log.error("Xray Cloud: no auth token - cannot publish");
            return;
        }

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(XRAY_CLOUD_EXEC);
            request.setHeader("Authorization", "Bearer " + cloudToken);
            request.setHeader("Content-Type",  "application/json");
            request.setHeader("Accept",        "application/json");
            request.setEntity(new StringEntity(payload, ContentType.APPLICATION_JSON));

            try (CloseableHttpResponse response = client.execute(request)) {
                int code = response.getCode();
                if (code == 200 || code == 201) {
                    log.info("✅ Xray Cloud: results published successfully [HTTP {}]", code);
                } else {
                    String body = new String(
                            response.getEntity().getContent().readAllBytes(),
                            StandardCharsets.UTF_8);
                    log.error("❌ Xray Cloud: HTTP {} - {}", code, body);
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private static String[] resolveKeys(Method method) {
        XrayTest annotation = method.getAnnotation(XrayTest.class);
        return annotation != null ? annotation.keys() : new String[0];
    }

    private static String orEmpty(String s) {
        return s != null ? s : "";
    }

    /** Avoid circular dependency - inline the DateUtils.now() here. */
    private static String now() {
        return java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Internal Result Record
    // ─────────────────────────────────────────────────────────────────────────

    private record ResultRecord(
            String methodName,
            String status,
            String comment,
            String finishedAt,
            String... keys) {

        ResultRecord(String methodName, String status, String comment, String finishedAt) {
            this(methodName, status, comment, finishedAt, new String[0]);
        }
    }
}
