package com.fsm.keystone.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Domain-error counter for the fsm-keystone API.
 *
 * <p>Increments the {@code fsm_api_errors_total} counter tagged by {@code code}
 * (stable ErrorCode name) and {@code status} (HTTP status integer as string).
 * Tag cardinality is bounded: {@code code} values come from the ErrorCode enum
 * plus the handful of inline string constants in GlobalExceptionHandler, and
 * {@code status} is a small set of HTTP status codes.</p>
 *
 * <p>Usage: inject this component into {@code GlobalExceptionHandler} and call
 * {@link #incrementError(String, int)} from every handled branch so
 * {@code INSUFFICIENT_STOCK}, {@code WORK_ORDER_NOT_FOUND} and other domain
 * error rates are graphable in the Prometheus / Grafana stack.</p>
 */
@Component
public class ApiErrorMetrics {

    static final String COUNTER_NAME = "fsm_api_errors_total";
    static final String TAG_CODE = "code";
    static final String TAG_STATUS = "status";

    private final MeterRegistry registry;

    public ApiErrorMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /**
     * Increments {@code fsm_api_errors_total} tagged with the given error code and HTTP status.
     *
     * @param code   stable ErrorCode name (e.g. {@code WORK_ORDER_NOT_FOUND}) or inline code
     * @param status HTTP status integer (e.g. 404)
     */
    public void incrementError(String code, int status) {
        Counter.builder(COUNTER_NAME)
                .tag(TAG_CODE, code)
                .tag(TAG_STATUS, String.valueOf(status))
                .description("Total number of API errors by error code and HTTP status")
                .register(registry)
                .increment();
    }
}
