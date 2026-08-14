package com.fsm.keystone.dto;

import com.fsm.keystone.enums.PartUsageStatus;

public record PartUsageRequest(

        Long workOrderId,

        Long partId,

        Integer quantityUsed,

        Long usedByUserId,

        PartUsageStatus usageStatus

) {
}
