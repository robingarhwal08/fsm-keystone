package com.fsm.keystone.security;

/**
 * Typed representation of one row from {@code route-role-matrix.csv}.
 *
 * <p>Path templates follow the Spring MVC convention: {@code {id}} and
 * {@code {customerId}} are substituted with the fixed test id "1" when
 * building actual MockMvc request paths.</p>
 */
public record RouteRoleMatrixRow(
        String method,
        String pathTemplate,
        boolean requiresBody,
        String bodyFixtureKey,
        int expectedAnonymous,
        int expectedManager,
        int expectedDispatcher,
        int expectedTechnician,
        int expectedCustomer) {

    private static final String TEST_ID = "1";

    /** Returns the expected HTTP status for the named actor. */
    public int expectedStatusFor(String actor) {
        return switch (actor.toUpperCase()) {
            case "ANONYMOUS"   -> expectedAnonymous;
            case "MANAGER"     -> expectedManager;
            case "DISPATCHER"  -> expectedDispatcher;
            case "TECHNICIAN"  -> expectedTechnician;
            case "CUSTOMER"    -> expectedCustomer;
            default -> throw new IllegalArgumentException(
                    "Unknown actor '" + actor + "' — must be one of ANONYMOUS, MANAGER, DISPATCHER, TECHNICIAN, CUSTOMER");
        };
    }

    /**
     * Returns the path with all template variables replaced by the fixed test id "1".
     * Substitutes {@code {id}} and {@code {customerId}}.
     */
    public String resolvedPath() {
        return pathTemplate
                .replace("{id}", TEST_ID)
                .replace("{customerId}", TEST_ID);
    }

    /** Stable lookup key: "METHOD /path/template". */
    public String matrixKey() {
        return method + " " + pathTemplate;
    }

    /**
     * Returns whether the expected outcome for all actors is 2xx.
     * Useful for debugging: a fully-open endpoint is a potential security gap.
     */
    public boolean isFullyOpen() {
        return expectedAnonymous >= 200 && expectedAnonymous < 300
                && expectedManager >= 200 && expectedManager < 300
                && expectedDispatcher >= 200 && expectedDispatcher < 300
                && expectedTechnician >= 200 && expectedTechnician < 300
                && expectedCustomer >= 200 && expectedCustomer < 300;
    }
}
