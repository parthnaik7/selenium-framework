package com.framework.annotations;

import java.lang.annotation.*;

/**
 * XrayTest - Link a test method to one or more Xray/JIRA test keys.
 *
 * Usage:
 * <pre>
 *   @Test
 *   @XrayTest(keys = {"PROJ-101", "PROJ-102"})
 *   public void verifyLogin() { ... }
 * </pre>
 *
 * The {@link com.framework.reporting.XrayReporter} reads this annotation via
 * reflection and associates execution results with the correct JIRA issues.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface XrayTest {

    /** One or more Xray test-issue keys (e.g. "PROJ-101"). */
    String[] keys();

    /**
     * Optional: the test type for Xray (Generic, Manual, Automated, Cucumber).
     * Defaults to "Automated".
     */
    String type() default "Automated";
}
