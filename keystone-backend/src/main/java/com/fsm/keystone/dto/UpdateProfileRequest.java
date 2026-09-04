package com.fsm.keystone.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateProfileRequest(
        @NotBlank(message = "Full name is required")
        String fullName,
        String phone,
        String currentPassword,
        String newPassword
) {
}
