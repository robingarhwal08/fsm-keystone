package com.fsm.keystone.service;


import com.fsm.keystone.entity.AppUser;
import com.fsm.keystone.enums.Role;
import com.fsm.keystone.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final AppUserRepository userRepository;

    public List<AppUser> getAllUsers() {
        return userRepository.findAll();
    }

    public List<AppUser> getAllTechnicians() {
        return userRepository.findByRole(Role.TECHNICIAN);
    }
}