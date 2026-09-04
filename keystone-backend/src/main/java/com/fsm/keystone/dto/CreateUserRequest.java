package com.fsm.keystone.dto;

import com.fsm.keystone.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank(message = "Full name is required")
        String fullName,

        @Email(message = "Valid email is required")
        @NotBlank(message = "Email is required")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 6, message = "Password must be at least 6 characters")
        String password,

        String phone,

        @NotNull(message = "Role is required")
        Role role,

        Boolean active,

        Long customerId
) {
}
