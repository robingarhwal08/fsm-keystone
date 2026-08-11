package com.fsm.keystone.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit tests for {@link ApiErrorMetrics} verifying counter registration and increment behaviour.
 *
 * <p>Uses {@link SimpleMeterRegistry} so no Spring context is required.</p>
 */
@Tag("unit")
class ApiErrorMetricsTest {

    private SimpleMeterRegistry registry;
    private ApiErrorMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new ApiErrorMetrics(registry);
    }

    @Test
    void incrementError_registersCounterWithExpectedTags() {
        metrics.incrementError("WORK_ORDER_NOT_FOUND", 404);

        Counter counter = registry.find(ApiErrorMetrics.COUNTER_NAME)
                .tag(ApiErrorMetrics.TAG_CODE, "WORK_ORDER_NOT_FOUND")
                .tag(ApiErrorMetrics.TAG_STATUS, "404")
                .counter();

        assertNotNull(counter, "Counter must be registered with code and status tags");
        assertEquals(1.0, counter.count(), 0.001);
    }

    @Test
    void incrementError_calledTwice_countIsTwo() {
        metrics.incrementError("INSUFFICIENT_STOCK", 409);
        metrics.incrementError("INSUFFICIENT_STOCK", 409);

        Counter counter = registry.find(ApiErrorMetrics.COUNTER_NAME)
                .tag(ApiErrorMetrics.TAG_CODE, "INSUFFICIENT_STOCK")
                .tag(ApiErrorMetrics.TAG_STATUS, "409")
                .counter();

        assertNotNull(counter);
        assertEquals(2.0, counter.count(), 0.001);
    }

    @Test
    void incrementError_differentCodes_trackedSeparately() {
        metrics.incrementError("WORK_ORDER_NOT_FOUND", 404);
        metrics.incrementError("AUTHENTICATION_FAILED", 401);
        metrics.incrementError("AUTHENTICATION_FAILED", 401);

        Counter notFound = registry.find(ApiErrorMetrics.COUNTER_NAME)
                .tag(ApiErrorMetrics.TAG_CODE, "WORK_ORDER_NOT_FOUND")
                .counter();
        Counter authFailed = registry.find(ApiErrorMetrics.COUNTER_NAME)
                .tag(ApiErrorMetrics.TAG_CODE, "AUTHENTICATION_FAILED")
                .counter();

        assertNotNull(notFound);
        assertNotNull(authFailed);
        assertEquals(1.0, notFound.count(), 0.001);
        assertEquals(2.0, authFailed.count(), 0.001);
    }

    @Test
    void incrementError_internalError_registeredWithCode() {
        metrics.incrementError("INTERNAL_ERROR", 500);

        Counter counter = registry.find(ApiErrorMetrics.COUNTER_NAME)
                .tag(ApiErrorMetrics.TAG_CODE, "INTERNAL_ERROR")
                .tag(ApiErrorMetrics.TAG_STATUS, "500")
                .counter();

        assertNotNull(counter, "5xx errors must also be counted");
        assertEquals(1.0, counter.count(), 0.001);
    }
}
