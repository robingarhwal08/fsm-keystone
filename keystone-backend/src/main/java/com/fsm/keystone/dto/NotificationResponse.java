package com.fsm.keystone.dto;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        String message,
        String type,
        Boolean readFlag,
        LocalDateTime createdAt,
        String workOrderNumber
) {
}
