package com.fsm.keystone.dto;

import com.fsm.keystone.enums.PartUsageStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PartUsageResponse(
        Long id,
        Long workOrderId,
        String workOrderNumber,
        String workOrderTitle,
        String siteName,
        Long partId,
        String partName,
        String partNumber,
        Integer quantityUsed,
        BigDecimal unitPriceAtUsage,
        BigDecimal totalCost,
        Integer remainingStock,
        PartUsageStatus usageStatus,
        String usedBy,
        String technicianName,
        LocalDateTime usedAt
) {
}
