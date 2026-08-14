package com.fsm.keystone.dto;

import com.fsm.keystone.enums.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CreateWorkOrderRequest(
        @NotBlank String title,
        String description,
        Long customerId,
        @NotNull Long siteId,
        Long createdByUserId,
        Long assignedTechnicianId,
        Priority priority,
        LocalDateTime scheduledStart,
        LocalDateTime scheduledEnd) {}
