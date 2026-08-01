package com.fsm.keystone.service;

import com.fsm.keystone.dto.AuthRequest;
import com.fsm.keystone.dto.AuthResponse;
import com.fsm.keystone.dto.SignupRequest;
import com.fsm.keystone.entity.AppUser;
import com.fsm.keystone.entity.Customer;
import com.fsm.keystone.repository.AppUserRepository;
import com.fsm.keystone.repository.CustomerRepository;
import com.fsm.keystone.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse signup(SignupRequest req) {

        if (userRepository.existsByEmail(req.email())) {
            throw new RuntimeException("Email already exists");
        }

        Customer customer = req.customerId() == null ? null : customerRepository.findById(req.customerId())
                .orElseThrow(() ->
                        new RuntimeException("Customer not found"));

        AppUser user = AppUser.builder()
                .fullName(req.fullName())
                .email(req.email())
                .password(passwordEncoder.encode(req.password()))
                .phone(req.phone())
                .role(req.role())
                .customer(customer)
                .active(true)
                .build();

        userRepository.save(user);

        String token = jwtService.generateToken(user);

        return new AuthResponse(
                token,
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getCustomer() != null ? user.getCustomer().getId() : null,
                user.getCustomer() != null ? user.getCustomer().getName() : null
        );
    }

    public AuthResponse login(AuthRequest req) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        req.email(),
                        req.password()
                )
        );

        AppUser user = userRepository.findByEmail(req.email())
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        String token = jwtService.generateToken(user);

        return new AuthResponse(
                token,
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getCustomer() != null ? user.getCustomer().getId() : null,
                user.getCustomer() != null ? user.getCustomer().getName() : null
        );
    }
}