package com.fsm.keystone.security;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link RouteRoleMatrixLoader} and {@link RouteRoleMatrixRow}.
 *
 * <p>Covers CSV parsing, path-variable substitution, body-fixture loading, and
 * expected error conditions for malformed inputs. No Spring context required.</p>
 */
@Tag("unit")
class RouteRoleMatrixLoaderTest {

    // ── loader — happy path ──────────────────────────────────────────────────

    @Test
    void loader_parsesAllRows() {
        RouteRoleMatrixLoader loader = new RouteRoleMatrixLoader();
        List<RouteRoleMatrixRow> rows = loader.rows();
        assertFalse(rows.isEmpty(), "matrix must contain at least one row");
    }

    @Test
    void loader_rowCountMatches_endpointInventoryBaseline() {
        // EXPECTED_ENDPOINT_COUNT from EndpointAuthorizationInventoryTest (WO-010 inventory)
        // Cross-check: if a new endpoint is added to a controller, that test is updated to 35,
        // and this assertion will catch that route-role-matrix.csv was not updated accordingly.
        int EXPECTED_ENDPOINT_COUNT = 34;
        int matrixCount = new RouteRoleMatrixLoader().rows().size();
        assertEquals(EXPECTED_ENDPOINT_COUNT, matrixCount,
                "route-role-matrix.csv has " + matrixCount + " rows but WO-010 inventory expects "
                + EXPECTED_ENDPOINT_COUNT + ". Update route-role-matrix.csv when adding or removing endpoints.");
    }

    @Test
    void loader_rowsIsImmutable() {
        RouteRoleMatrixLoader loader = new RouteRoleMatrixLoader();
        assertThrows(UnsupportedOperationException.class,
                () -> loader.rows().add(null),
                "rows() must return an immutable list");
    }

    @Test
    void loader_findRow_returnsByMethodAndPath() {
        RouteRoleMatrixLoader loader = new RouteRoleMatrixLoader();
        RouteRoleMatrixRow row = loader.findRow("GET", "/api/customers");
        assertEquals("GET", row.method());
        assertEquals("/api/customers", row.pathTemplate());
    }

    @Test
    void loader_findRow_throwsForUnknownKey() {
        RouteRoleMatrixLoader loader = new RouteRoleMatrixLoader();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> loader.findRow("DELETE", "/api/nonexistent"));
        assertTrue(ex.getMessage().contains("No matrix row"), ex.getMessage());
    }

    // ── loader — body fixture loading ────────────────────────────────────────

    @Test
    void loader_loadBody_returnsNonBlankJsonForAllBodyRows() {
        RouteRoleMatrixLoader loader = new RouteRoleMatrixLoader();
        loader.rows().stream()
                .filter(RouteRoleMatrixRow::requiresBody)
                .forEach(row -> {
                    String body = loader.loadBody(row.bodyFixtureKey());
                    assertFalse(body.isBlank(),
                            "Body fixture for key '" + row.bodyFixtureKey() + "' must not be blank");
                    assertTrue(body.startsWith("{") || body.startsWith("["),
                            "Body fixture for key '" + row.bodyFixtureKey() + "' must be JSON, got: " + body.substring(0, Math.min(30, body.length())));
                });
    }

    @Test
    void loader_loadBody_throwsForBlankKey() {
        RouteRoleMatrixLoader loader = new RouteRoleMatrixLoader();
        assertThrows(IllegalArgumentException.class, () -> loader.loadBody(""));
        assertThrows(IllegalArgumentException.class, () -> loader.loadBody(null));
    }

    @Test
    void loader_loadBody_throwsForMissingFixture() {
        RouteRoleMatrixLoader loader = new RouteRoleMatrixLoader();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> loader.loadBody("nonexistent-fixture-key"));
        assertTrue(ex.getMessage().contains("Body fixture not found"), ex.getMessage());
    }

    // ── RouteRoleMatrixRow — path variable substitution ──────────────────────

    @Test
    void row_resolvedPath_substitutesIdVariable() {
        RouteRoleMatrixRow row = new RouteRoleMatrixRow(
                "GET", "/api/customers/{id}", false, "", 200, 200, 200, 403, 403);
        assertEquals("/api/customers/1", row.resolvedPath());
    }

    @Test
    void row_resolvedPath_substitutesCustomerIdVariable() {
        RouteRoleMatrixRow row = new RouteRoleMatrixRow(
                "GET", "/api/sites/customer/{customerId}", false, "", 403, 200, 200, 200, 200);
        assertEquals("/api/sites/customer/1", row.resolvedPath());
    }

    @Test
    void row_resolvedPath_substitutesMultipleVariables() {
        RouteRoleMatrixRow row = new RouteRoleMatrixRow(
                "PATCH", "/api/work-orders/{id}/assign", true, "work-order-assign", 403, 200, 200, 403, 403);
        assertEquals("/api/work-orders/1/assign", row.resolvedPath());
    }

    @Test
    void row_resolvedPath_noVariables_unchanged() {
        RouteRoleMatrixRow row = new RouteRoleMatrixRow(
                "GET", "/api/customers", false, "", 200, 200, 200, 200, 200);
        assertEquals("/api/customers", row.resolvedPath());
    }

    // ── RouteRoleMatrixRow — expectedStatusFor ───────────────────────────────

    @Test
    void row_expectedStatusFor_returnsCorrectValuePerActor() {
        RouteRoleMatrixRow row = new RouteRoleMatrixRow(
                "GET", "/api/dashboard/summary", false, "", 403, 200, 200, 403, 403);
        assertEquals(403, row.expectedStatusFor("ANONYMOUS"));
        assertEquals(200, row.expectedStatusFor("MANAGER"));
        assertEquals(200, row.expectedStatusFor("DISPATCHER"));
        assertEquals(403, row.expectedStatusFor("TECHNICIAN"));
        assertEquals(403, row.expectedStatusFor("CUSTOMER"));
    }

    @Test
    void row_expectedStatusFor_isCaseInsensitive() {
        RouteRoleMatrixRow row = new RouteRoleMatrixRow(
                "GET", "/api/parts", false, "", 403, 200, 200, 200, 200);
        assertEquals(403, row.expectedStatusFor("anonymous"));
        assertEquals(200, row.expectedStatusFor("manager"));
        assertEquals(200, row.expectedStatusFor("Dispatcher"));
    }

    @Test
    void row_expectedStatusFor_throwsForUnknownActor() {
        RouteRoleMatrixRow row = new RouteRoleMatrixRow(
                "GET", "/api/parts", false, "", 403, 200, 200, 200, 200);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> row.expectedStatusFor("ADMIN"));
        assertTrue(ex.getMessage().contains("Unknown actor"), ex.getMessage());
    }

    // ── RouteRoleMatrixRow — matrixKey ───────────────────────────────────────

    @Test
    void row_matrixKey_includesMethodAndTemplate() {
        RouteRoleMatrixRow row = new RouteRoleMatrixRow(
                "DELETE", "/api/work-orders/{id}", false, "", 403, 200, 200, 200, 200);
        assertEquals("DELETE /api/work-orders/{id}", row.matrixKey());
    }

    // ── RouteRoleMatrixRow — isFullyOpen ─────────────────────────────────────

    @Test
    void row_isFullyOpen_trueWhenAllRolesGetTwoHundred() {
        RouteRoleMatrixRow row = new RouteRoleMatrixRow(
                "GET", "/api/customers", false, "", 200, 200, 200, 200, 200);
        assertTrue(row.isFullyOpen(), "permitAll endpoint without @PreAuthorize must be fully open");
    }

    @Test
    void row_isFullyOpen_falseWhenAnyRoleIsForbidden() {
        RouteRoleMatrixRow row = new RouteRoleMatrixRow(
                "POST", "/api/customers", true, "customer-create", 403, 200, 403, 403, 403);
        assertFalse(row.isFullyOpen(), "MANAGER-only endpoint must not be fully open");
    }

    // ── all body-bearing rows have fixture files ─────────────────────────────

    @Test
    void allRequiresBody_rows_haveNonBlankFixtureKey() {
        new RouteRoleMatrixLoader().rows().stream()
                .filter(RouteRoleMatrixRow::requiresBody)
                .forEach(row -> assertFalse(row.bodyFixtureKey().isBlank(),
                        "Row " + row.matrixKey() + " has requiresBody=true but empty bodyFixtureKey"));
    }

    // ── parameterized: known endpoints present in matrix ─────────────────────

    @ParameterizedTest
    @ValueSource(strings = {
            "GET /api/customers",
            "POST /api/work-orders",
            "PATCH /api/work-orders/{id}/assign",
            "GET /api/dashboard/summary",
            "DELETE /api/customers/{id}",
            "DELETE /api/users/{id}",
            "POST /api/part-usage",
            "POST /api/time-logs"
    })
    void knownEndpoints_presentInMatrix(String matrixKey) {
        String[] parts = matrixKey.split(" ", 2);
        RouteRoleMatrixLoader loader = new RouteRoleMatrixLoader();
        RouteRoleMatrixRow row = loader.findRow(parts[0], parts[1]);
        assertNotNull(row, "Endpoint '" + matrixKey + "' must be present in route-role-matrix.csv");
    }
}
