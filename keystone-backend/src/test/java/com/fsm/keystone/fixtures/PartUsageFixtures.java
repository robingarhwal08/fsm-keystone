package com.fsm.keystone.fixtures;

import com.fsm.keystone.entity.AppUser;
import com.fsm.keystone.entity.Part;
import com.fsm.keystone.entity.PartUsage;
import com.fsm.keystone.entity.WorkOrder;

import java.math.BigDecimal;

/**
 * Object-mother factory for {@link PartUsage} entities.
 *
 * <p>Minimum valid shape: {@code workOrder} and {@code part} are required
 * ({@code optional = false} on FKs). {@code unitPriceAtUsage} is a price snapshot
 * captured at the time of usage (prevents retroactive cost changes).</p>
 */
public final class PartUsageFixtures {

    private PartUsageFixtures() {}

    /**
     * Returns a builder for a usage of 2 units of the given part on the given work order.
     * {@code unitPriceAtUsage} is copied from {@link Part#getUnitPrice()} for consistency;
     * callers may override it to test price-snapshot behaviour.
     */
    public static PartUsage.PartUsageBuilder aPartUsage(
            WorkOrder workOrder, Part part, AppUser usedBy) {
        BigDecimal unitPrice = part.getUnitPrice() != null
                ? part.getUnitPrice()
                : new BigDecimal("10.00");
        int qty = 2;
        return PartUsage.builder()
                .quantityUsed(qty)
                .unitPriceAtUsage(unitPrice)
                .totalCost(unitPrice.multiply(BigDecimal.valueOf(qty)))
                .usedAt(FixtureClock.NOW)
                .workOrder(workOrder)
                .part(part)
                .usedBy(usedBy);
    }
}
