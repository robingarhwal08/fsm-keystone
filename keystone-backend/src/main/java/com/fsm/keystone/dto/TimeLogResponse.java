package com.fsm.keystone.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TimeLogResponse(
        Long id,
        Long workOrderId,
        String workOrderNumber,
        String workOrderTitle,
        String technicianName,
        LocalDateTime startTime,
        LocalDateTime endTime,
        BigDecimal hoursSpent,
        Integer minutesSpent,
        String workDescription
) {
}
