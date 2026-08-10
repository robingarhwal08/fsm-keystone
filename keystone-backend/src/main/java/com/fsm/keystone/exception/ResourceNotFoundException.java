package com.fsm.keystone.exception;

import java.util.Map;

/**
 * Thrown when a requested resource cannot be found (HTTP 404).
 *
 * <p>Use the static factory {@link #of(String, Object)} to generate a message that
 * matches the legacy RuntimeException format used throughout the service layer, so
 * existing log searches keep matching after WO-022 migrates the throw sites.</p>
 */
public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(ErrorCode code, String message) {
        super(code, message, Map.of());
    }

    public ResourceNotFoundException(ErrorCode code, String message, Throwable cause) {
        super(code, message, Map.of(), cause);
    }

    public ResourceNotFoundException(ErrorCode code, String message,
                                     Map<String, Object> details) {
        super(code, message, details);
    }

    /**
     * Convenience factory that reproduces the exact legacy message format:
     * {@code "<ResourceType> not found with id: <id>"}.
     *
     * <p>A null {@code id} produces {@code "... with id: null"} rather than throwing
     * an NPE inside an already-failing code path.</p>
     *
     * @param resourceType human-readable resource name, e.g. {@code "Work order"}
     * @param id           the identifier used in the lookup
     */
    public static ResourceNotFoundException of(String resourceType, Object id) {
        String message = resourceType + " not found with id: " + id;
        ErrorCode code = resolveCode(resourceType);
        return new ResourceNotFoundException(code, message);
    }

    private static ErrorCode resolveCode(String resourceType) {
        if (resourceType == null) {
            return ErrorCode.RESOURCE_NOT_FOUND;
        }
        return switch (resourceType.toLowerCase().replace(" ", "_").replace("-", "_")) {
            case "work_order"  -> ErrorCode.WORK_ORDER_NOT_FOUND;
            case "customer"    -> ErrorCode.CUSTOMER_NOT_FOUND;
            case "site"        -> ErrorCode.SITE_NOT_FOUND;
            case "user", "technician" -> ErrorCode.USER_NOT_FOUND;
            case "part"        -> ErrorCode.PART_NOT_FOUND;
            default            -> ErrorCode.RESOURCE_NOT_FOUND;
        };
    }
}
