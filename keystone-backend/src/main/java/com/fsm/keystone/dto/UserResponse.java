package com.fsm.keystone.dto;

import com.fsm.keystone.entity.AppUser;
import com.fsm.keystone.enums.Role;

import java.time.LocalDateTime;

public record UserResponse(

        Long id,

        String fullName,

        String email,

        String phone,

        Role role,

        Boolean active,

        Long customerId,

        LocalDateTime createdAt

) {

    public static UserResponse fromEntity(AppUser user) {
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.getActive(),
                user.getCustomer() != null ? user.getCustomer().getId() : null,
                user.getCreatedAt()
        );
    }
}