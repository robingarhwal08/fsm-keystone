package com.fsm.keystone.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Default-value guard tests for {@code application.properties}.
 *
 * <h2>Phase 0 — reporting-only</h2>
 * <p>{@link #realApplicationPropertiesHasKnownViolationCount()} asserts exactly
 * {@link #KNOWN_VIOLATION_COUNT} violations (currently 2: APP_JWT_SECRET and
 * SPRING_DATASOURCE_PASSWORD both have literal fallbacks). This means:</p>
 * <ul>
 *   <li>Adding a new literal fallback for a required-no-default key → test fails
 *       (regression caught).</li>
 *   <li>Removing a fallback (Phase 2) → count drops, test fails → developer updates
 *       assertion and promotes the guard to a blocking zero-violation check.</li>
 * </ul>
 *
 * <h2>Synthetic fixtures</h2>
 * <p>The two remaining tests operate on controlled synthetic properties files under
 * {@code src/test/resources/guard/} to verify the detection logic in isolation,
 * independent of the live application configuration.</p>
 */
class DefaultValueGuardTest {

    /**
     * Environment variables declared required-no-default in
     * {@code docs/baseline/required-properties.md}.
     * Update this set when the manifest changes.
     */
    private static final Set<String> REQUIRED_NO_DEFAULT = Set.of(
            "APP_JWT_SECRET",
            "SPRING_DATASOURCE_PASSWORD"
    );

    /**
     * Known number of violations in the CURRENT {@code application.properties}.
     *
     * <p><strong>Phase 2 action:</strong> when the literal fallbacks are removed,
     * this constant must be set to {@code 0} and the test description updated to
     * reflect the guard is now a blocking zero-tolerance check.</p>
     */
    private static final int KNOWN_VIOLATION_COUNT = 2;

    // ─── Real application.properties check ──────────────────────────────────

    @Test
    @DisplayName("application.properties has exactly the known number of required-no-default violations (Phase 0 baseline)")
    void realApplicationPropertiesHasKnownViolationCount() throws IOException {
        Properties props = DefaultValueGuard.loadFromFile(
                "src/main/resources/application.properties");

        List<String> violations = DefaultValueGuard.findViolations(props, REQUIRED_NO_DEFAULT);

        String report = DefaultValueGuard.formatReport(violations);

        // Phase 0: assert the known count — not zero.
        // If this fails with "expected: 2 but was: 3", a new fallback was added.
        // If this fails with "expected: 2 but was: 1 or 0", a fallback was removed —
        // update KNOWN_VIOLATION_COUNT to 0 and promote this guard to blocking.
        assertEquals(
                KNOWN_VIOLATION_COUNT,
                violations.size(),
                "Default-value guard violation count changed.\n\n"
                        + "Current report (no secret values are printed):\n" + report
                        + "\n\nIf fallbacks were removed, set KNOWN_VIOLATION_COUNT=0 "
                        + "in DefaultValueGuardTest to make this a blocking zero-violation gate.");
    }

    // ─── Synthetic fixture tests ─────────────────────────────────────────────

    @Test
    @DisplayName("Synthetic violating.properties: guard flags the literal fallback for a required-no-default key")
    void syntheticViolatingPropertiesAreDetected() throws IOException {
        Properties props = DefaultValueGuard.loadFromClasspath("/guard/violating.properties");

        List<String> violations = DefaultValueGuard.findViolations(props,
                Set.of("MY_API_SECRET"));

        assertFalse(violations.isEmpty(),
                "Expected the guard to detect a violation in violating.properties but found none. "
                        + "Check that /guard/violating.properties contains "
                        + "'${MY_API_SECRET:some-literal}'.");

        assertEquals(1, violations.size(),
                "Expected exactly 1 violation in violating.properties but found: "
                        + violations.size() + ". Report:\n"
                        + DefaultValueGuard.formatReport(violations));

        // Verify the violation message names the property and variable, not the literal
        String msg = violations.get(0);
        assertTrue(msg.contains("MY_API_SECRET"),
                "Violation message should contain the variable name 'MY_API_SECRET'");
        assertFalse(msg.contains("placeholder-literal-value"),
                "Violation message must NOT echo the literal value (security requirement)");
    }

    @Test
    @DisplayName("Synthetic compliant.properties: guard reports zero violations when no literal fallback is present")
    void syntheticCompliantPropertiesPassClean() throws IOException {
        Properties props = DefaultValueGuard.loadFromClasspath("/guard/compliant.properties");

        List<String> violations = DefaultValueGuard.findViolations(props,
                Set.of("MY_API_SECRET"));

        assertTrue(violations.isEmpty(),
                "Expected no violations in compliant.properties but found: "
                        + violations.size() + ". Report:\n"
                        + DefaultValueGuard.formatReport(violations));
    }

    // ─── Manifest parseable assertion ────────────────────────────────────────

    @Test
    @DisplayName("REQUIRED_NO_DEFAULT set is non-empty and contains the documented keys")
    void requiredNoDefaultSetIsCorrect() {
        assertFalse(REQUIRED_NO_DEFAULT.isEmpty(),
                "REQUIRED_NO_DEFAULT must not be empty");
        assertTrue(REQUIRED_NO_DEFAULT.contains("APP_JWT_SECRET"),
                "APP_JWT_SECRET must be in REQUIRED_NO_DEFAULT");
        assertTrue(REQUIRED_NO_DEFAULT.contains("SPRING_DATASOURCE_PASSWORD"),
                "SPRING_DATASOURCE_PASSWORD must be in REQUIRED_NO_DEFAULT");
    }
}
