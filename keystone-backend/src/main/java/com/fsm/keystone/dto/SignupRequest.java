package com.fsm.keystone.dto;

import com.fsm.keystone.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SignupRequest(@NotBlank
                            String fullName,
                            @Email
                            @NotBlank
                            String email,
                            @NotBlank
                            String password,
                            String phone,
                            @NotNull Role role,
                            Long customerId) {}
