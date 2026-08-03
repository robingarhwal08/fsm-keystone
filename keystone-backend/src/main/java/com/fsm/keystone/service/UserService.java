package com.fsm.keystone.service;

import com.fsm.keystone.dto.UpdateUserRequest;
import com.fsm.keystone.dto.UserResponse;
import com.fsm.keystone.entity.AppUser;
import com.fsm.keystone.enums.Role;
import com.fsm.keystone.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    // Get all users except MANAGER
    public List<UserResponse> getAllUsers() {
        return userRepository.findByRoleNot(Role.MANAGER)
                .stream()
                .map(UserResponse::fromEntity)
                .toList();
    }

    // Get all technicians
    public List<UserResponse> getAllTechnicians() {
        return userRepository.findByRole(Role.TECHNICIAN)
                .stream()
                .map(UserResponse::fromEntity)
                .toList();
    }

    // Update user
    public UserResponse updateUser(Long id, UpdateUserRequest request) {

        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setFullName(request.fullName());
        user.setPhone(request.phone());
        user.setRole(request.role());
        user.setActive(request.active());

        AppUser updatedUser = userRepository.save(user);

        return UserResponse.fromEntity(updatedUser);
    }

    // Delete user
    public void deleteUser(Long id) {

        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        userRepository.delete(user);
    }
}