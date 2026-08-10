package com.fsm.keystone.security;

/**
 * Represents a single row in the authorization-matrix.csv source file.
 *
 * <p>Fields map 1-to-1 to the CSV columns:</p>
 * <pre>
 * controller,method,path,filterChainRule,hasPreAuthorize,preAuthorizeExpr,
 * expectedStatusAnon,expectedStatusManager,expectedStatusDispatcher,
 * expectedStatusTechnician,expectedStatusCustomer
 * </pre>
 *
 * <p>{@code preAuthorizeExpr} is the literal @PreAuthorize expression, empty when the
 * annotation is absent, or the sentinel {@code ANNOTATION_COMMENTED_OUT} when the
 * annotation is present in source but commented out (CustomerController.delete).</p>
 */
public record MatrixRow(
        String controller,
        String method,
        String path,
        String filterChainRule,
        boolean hasPreAuthorize,
        String preAuthorizeExpr,
        int expectedStatusAnon,
        int expectedStatusManager,
        int expectedStatusDispatcher,
        int expectedStatusTechnician,
        int expectedStatusCustomer
) {
    /** Returns the expected status for the given actor label (ANONYMOUS / MANAGER / etc.). */
    public int expectedStatusFor(String actor) {
        return switch (actor.toUpperCase()) {
            case "ANONYMOUS"  -> expectedStatusAnon;
            case "MANAGER"    -> expectedStatusManager;
            case "DISPATCHER" -> expectedStatusDispatcher;
            case "TECHNICIAN" -> expectedStatusTechnician;
            case "CUSTOMER"   -> expectedStatusCustomer;
            default -> throw new IllegalArgumentException("Unknown actor: " + actor);
        };
    }

    /** Human-readable key used in error messages and deduplication. */
    public String matrixKey() {
        return method + " " + path;
    }

    /**
     * Returns {@code true} when the annotation is absent (not commented-out; actually not present).
     * Callers must distinguish this from the commented-out case via {@link #preAuthorizeExpr}.
     */
    public boolean isAnnotationCommentedOut() {
        return "ANNOTATION_COMMENTED_OUT".equals(preAuthorizeExpr);
    }
}
