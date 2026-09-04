package com.fsm.keystone.config;

import com.fsm.keystone.entity.AppUser;
import com.fsm.keystone.enums.Role;
import com.fsm.keystone.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminSeedRunner implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.admin-email:admin@keystone.com}")
    private String adminEmail;

    @Value("${app.seed.admin-password:Admin@123}")
    private String adminPassword;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByEmail(adminEmail)) {
            return;
        }

        userRepository.save(AppUser.builder()
                .fullName("System Admin")
                .email(adminEmail)
                .password(passwordEncoder.encode(adminPassword))
                .role(Role.ADMIN)
                .active(true)
                .build());
    }
}
