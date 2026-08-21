package com.fsm.keystone.dto;

import java.time.LocalDateTime;

public record StatusHistoryResponse(
        Long id,
        String oldStatus,
        String newStatus,
        String comment,
        String changedBy,
        LocalDateTime changedAt
) {
}
