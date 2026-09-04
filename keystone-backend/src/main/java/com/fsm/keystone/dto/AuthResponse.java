package com.fsm.keystone.dto;
import com.fsm.keystone.enums.Role;
public record AuthResponse(
        String token,
        Long userId,
        String fullName,
        String email,
        Role role,
        Long customerId,
        String customerName,
        Boolean pendingApproval
) {}
