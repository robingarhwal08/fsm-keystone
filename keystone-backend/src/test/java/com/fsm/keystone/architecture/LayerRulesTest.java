package com.fsm.keystone.architecture;

import com.fsm.keystone.architecture.fixtures.controller.FakeController;
import com.fsm.keystone.architecture.fixtures.controller.HandlerWithoutPreAuthorize;
import com.fsm.keystone.architecture.fixtures.service.CompliantService;
import com.fsm.keystone.architecture.fixtures.service.ViolatingService;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reporting-only ArchUnit harness for layer-violation and annotation-coverage rules.
 *
 * <p><strong>Phase 0 — REPORTING ONLY.</strong> These tests assert that the violation
 * count matches the documented baseline in {@code docs/baseline/layer-violation-triage.md},
 * so the build stays green while the metrics become reproducible. Counts are promoted to
 * zero-assertion gates once the violations are remediated in later epics.</p>
 *
 * <p>Cross-reference: {@code com.fsm.keystone.arch.EndpointAuthorizationInventoryTest}
 * produces {@code target/endpoint-authorization-inventory.txt} documenting all 34 endpoints.
 * The annotation-coverage rule must discover the same set of mapping-annotated methods.</p>
 */
@Tag("unit")
class LayerRulesTest {

    // -----------------------------------------------------------------------
    // Baseline constants — update these after committing a fix to the register
    // -----------------------------------------------------------------------

    /**
     * Expected service→controller layer violations in production code.
     * All 15 reported static-analysis edges are false positives (name-collision artifacts);
     * ArchUnit class-dependency analysis confirms zero genuine violations.
     * See docs/baseline/layer-violation-triage.md.
     */
    static final int EXPECTED_LAYER_VIOLATIONS = 0;

    /**
     * Expected handler methods lacking @PreAuthorize in production code.
     * Baseline as of WO-005. Includes AuthController.signup and AuthController.login (2),
     * which are intentionally public (permitAll) and will receive annotations in the
     * annotation-sweep epic. See docs/baseline/layer-violation-triage.md, AC-6.
     * Cross-check: EndpointAuthorizationInventoryTest reports EXPECTED_ENDPOINT_COUNT = 34;
     * 10 of those carry @PreAuthorize; 24 do not.
     */
    static final int EXPECTED_ANNOTATION_COVERAGE_VIOLATIONS = 24;

    // -----------------------------------------------------------------------
    // Imported class sets
    // -----------------------------------------------------------------------

    /** Production classes only (test classes excluded). */
    static final JavaClasses PROD_CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.fsm.keystone");

    // -----------------------------------------------------------------------
    // Rules
    // -----------------------------------------------------------------------

    static final ArchRule SERVICE_MUST_NOT_DEPEND_ON_CONTROLLER =
            noClasses()
                    .that().resideInAPackage("..service..")
                    .should().dependOnClassesThat().resideInAPackage("..controller..")
                    .because("Service layer must not reach into the controller layer (ADR-0002). "
                            + "Perceived violations in static-analysis output are name-collision "
                            + "artifacts — see docs/baseline/layer-violation-triage.md.");

    static final ArchRule HANDLER_MUST_HAVE_PREAUTHORIZE =
            methods()
                    .that(haveMappingAnnotation())
                    .should().beAnnotatedWith(PreAuthorize.class)
                    .because("All handler methods must declare @PreAuthorize for role-based "
                            + "access control (ADR-0002). Missing annotations are tracked in "
                            + "docs/baseline/layer-violation-triage.md.");

    // -----------------------------------------------------------------------
    // Production-code rule tests (reporting-only)
    // -----------------------------------------------------------------------

    @Test
    void serviceLayer_mustNotDependOnController_reportingOnly() throws IOException {
        EvaluationResult result = SERVICE_MUST_NOT_DEPEND_ON_CONTROLLER.evaluate(PROD_CLASSES);
        List<String> violations = result.getFailureReport().getDetails();

        writeReport("target/layer-violation-report.txt",
                "Layer Violation Report (service → controller)",
                "SERVICE_MUST_NOT_DEPEND_ON_CONTROLLER",
                "docs/baseline/layer-violation-triage.md",
                violations);

        assertEquals(EXPECTED_LAYER_VIOLATIONS, violations.size(),
                buildDriftMessage("EXPECTED_LAYER_VIOLATIONS", EXPECTED_LAYER_VIOLATIONS,
                        violations.size()));
    }

    @Test
    void handlerMethods_mustHavePreAuthorize_reportingOnly() throws IOException {
        EvaluationResult result = HANDLER_MUST_HAVE_PREAUTHORIZE.evaluate(PROD_CLASSES);
        List<String> violations = result.getFailureReport().getDetails();

        writeReport("target/annotation-coverage-report.txt",
                "Annotation Coverage Report (@PreAuthorize on handler methods)",
                "HANDLER_MUST_HAVE_PREAUTHORIZE",
                "docs/baseline/layer-violation-triage.md",
                violations);

        assertEquals(EXPECTED_ANNOTATION_COVERAGE_VIOLATIONS, violations.size(),
                buildDriftMessage("EXPECTED_ANNOTATION_COVERAGE_VIOLATIONS",
                        EXPECTED_ANNOTATION_COVERAGE_VIOLATIONS, violations.size()));
    }

    // -----------------------------------------------------------------------
    // Harness self-verification: synthetic fixture tests
    // -----------------------------------------------------------------------

    /**
     * Verifies that the layer-violation rule detects the synthetic ViolatingService,
     * which imports a class from the "controller" sub-package.
     */
    @Test
    void harness_layerRule_detectsViolatingService() {
        JavaClasses fixtures = new ClassFileImporter()
                .importClasses(ViolatingService.class, FakeController.class);

        EvaluationResult result = SERVICE_MUST_NOT_DEPEND_ON_CONTROLLER.evaluate(fixtures);

        assertFalse(result.hasNoViolation(),
                "Expected ViolatingService to be flagged for depending on FakeController, "
                        + "but the rule reported no violations.");
        assertTrue(result.getFailureReport().toString().contains("ViolatingService"),
                "Violation report should name ViolatingService, but it does not. "
                        + "Report: " + result.getFailureReport());
    }

    /**
     * Verifies that the layer-violation rule passes the synthetic CompliantService,
     * which has no dependency on anything in the "controller" sub-package.
     */
    @Test
    void harness_layerRule_passesCompliantService() {
        JavaClasses fixtures = new ClassFileImporter()
                .importClasses(CompliantService.class);

        EvaluationResult result = SERVICE_MUST_NOT_DEPEND_ON_CONTROLLER.evaluate(fixtures);

        assertTrue(result.hasNoViolation(),
                "CompliantService should pass the layer rule, but violations were reported: "
                        + result.getFailureReport());
    }

    /**
     * Verifies that the annotation-coverage rule detects HandlerWithoutPreAuthorize,
     * which has @GetMapping but no @PreAuthorize.
     */
    @Test
    void harness_annotationRule_detectsHandlerWithoutPreAuthorize() {
        JavaClasses fixtures = new ClassFileImporter()
                .importClasses(HandlerWithoutPreAuthorize.class);

        EvaluationResult result = HANDLER_MUST_HAVE_PREAUTHORIZE.evaluate(fixtures);

        assertFalse(result.hasNoViolation(),
                "Expected HandlerWithoutPreAuthorize.noAuth() to be flagged for missing "
                        + "@PreAuthorize, but the rule reported no violations.");
        assertTrue(result.getFailureReport().toString().contains("HandlerWithoutPreAuthorize"),
                "Violation report should name HandlerWithoutPreAuthorize, but it does not. "
                        + "Report: " + result.getFailureReport());
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static DescribedPredicate<JavaMethod> haveMappingAnnotation() {
        return DescribedPredicate.describe(
                "have a Spring HTTP mapping annotation (@GetMapping, @PostMapping, "
                        + "@PutMapping, @PatchMapping, or @DeleteMapping)",
                m -> m.isAnnotatedWith(GetMapping.class)
                        || m.isAnnotatedWith(PostMapping.class)
                        || m.isAnnotatedWith(PutMapping.class)
                        || m.isAnnotatedWith(PatchMapping.class)
                        || m.isAnnotatedWith(DeleteMapping.class));
    }

    private static void writeReport(
            String path,
            String title,
            String ruleName,
            String registerPath,
            List<String> violations) throws IOException {

        Files.createDirectories(Paths.get("target"));

        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(title).append("\n");
        sb.append("# Rule: ").append(ruleName).append("\n");
        sb.append("# Register: ").append(registerPath).append("\n");
        sb.append("# Total violations: ").append(violations.size()).append("\n");
        sb.append("#\n");

        if (violations.isEmpty()) {
            sb.append("No violations found.\n");
        } else {
            for (int i = 0; i < violations.size(); i++) {
                sb.append(String.format("[%03d] %s%n", i + 1, violations.get(i)));
            }
        }

        Files.writeString(Paths.get(path), sb.toString(), StandardCharsets.UTF_8);
    }

    private static String buildDriftMessage(String constantName, int expected, int actual) {
        return String.format(
                "Baseline drift detected for %s: expected %d but found %d.%n"
                        + "If this is a regression (count increased): fix the violation and update "
                        + "docs/baseline/layer-violation-triage.md.%n"
                        + "If this is an improvement (count decreased): update %s to %d and "
                        + "record the fix in docs/baseline/layer-violation-triage.md.",
                constantName, expected, actual, constantName, actual);
    }
}
