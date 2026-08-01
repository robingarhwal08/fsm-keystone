package com.fsm.keystone.dto;

public record PartUsageRequest(

        Long workOrderId,

        Long partId,

        Integer quantityUsed,

        Long usedByUserId

) {
}