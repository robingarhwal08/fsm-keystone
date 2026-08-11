package com.fsm.keystone.fixtures;

import com.fsm.keystone.entity.Part;

import java.math.BigDecimal;

/**
 * Object-mother factory for {@link Part} entities.
 *
 * <p>Minimum valid shape: {@code partName} and {@code partNumber} are non-null columns.
 * {@code active} and {@code stockQuantity} default via {@code @PrePersist} but are set
 * explicitly here for unit-test determinism.</p>
 */
public final class PartFixtures {

    private PartFixtures() {}

    /** Returns a builder for a generic active part with adequate stock. */
    public static Part.PartBuilder aPart() {
        return Part.builder()
                .partName("Test Part")
                .partNumber("TEST-001")
                .description("Generic test part for fixture use")
                .unitPrice(new BigDecimal("25.00"))
                .stockQuantity(50)
                .active(true);
    }

    /**
     * Returns a builder for a part with adequate stock (id 501).
     * Used in {@link TenantScenario} for normal part-usage scenarios.
     */
    public static Part.PartBuilder normalStockPart() {
        return aPart()
                .id(501L)
                .partName("HVAC Filter")
                .partNumber("HVAC-FILTER-01")
                .description("Standard HVAC air filter 16x20x1")
                .unitPrice(new BigDecimal("12.99"))
                .stockQuantity(100);
    }

    /**
     * Returns a builder for a part with low stock (id 502, stockQuantity=1).
     * Low-stock dashboard counts depend on parts with quantity at or near the reorder threshold.
     */
    public static Part.PartBuilder lowStockPart() {
        return aPart()
                .id(502L)
                .partName("Pump Seal Kit")
                .partNumber("PUMP-SEAL-01")
                .description("Replacement seal kit for centrifugal pumps — low stock")
                .unitPrice(new BigDecimal("89.50"))
                .stockQuantity(1);
    }

    /**
     * Returns a builder for a part with zero stock (id 503).
     * Tests that distinguish zero-stock from low-stock need both variants.
     */
    public static Part.PartBuilder zeroStockPart() {
        return aPart()
                .id(503L)
                .partName("Compressor Valve")
                .partNumber("COMP-VALVE-01")
                .description("High-pressure compressor valve — out of stock")
                .unitPrice(new BigDecimal("145.00"))
                .stockQuantity(0);
    }
}
