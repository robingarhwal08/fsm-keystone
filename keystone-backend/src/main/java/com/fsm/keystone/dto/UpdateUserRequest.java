package com.fsm.keystone.dto;

import com.fsm.keystone.enums.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateUserRequest(

        @NotBlank(message = "Full name is required")
        String fullName,

        String phone,

        @NotNull(message = "Role is required")
        Role role,

        @NotNull(message = "Active status is required")
        Boolean active

) {
}