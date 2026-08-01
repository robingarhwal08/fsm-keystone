package com.fsm.keystone.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record TimeLogRequest(

        @NotNull
        Long workOrderId,

        @NotNull
        Long technicianId,

        @NotNull
        LocalDateTime startTime,

        @NotNull
        LocalDateTime endTime,

        String workDescription
) {
}