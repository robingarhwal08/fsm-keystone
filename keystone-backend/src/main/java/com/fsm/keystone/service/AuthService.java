package com.fsm.keystone.service;

import com.fsm.keystone.dto.AuthRequest;
import com.fsm.keystone.dto.AuthResponse;
import com.fsm.keystone.dto.SignupRequest;
import com.fsm.keystone.entity.AppUser;
import com.fsm.keystone.entity.Customer;
import com.fsm.keystone.enums.Role;
import com.fsm.keystone.exception.BusinessException;
import com.fsm.keystone.repository.UserRepository;
import com.fsm.keystone.repository.CustomerRepository;
import com.fsm.keystone.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final NotificationService notificationService;

    public AuthResponse signup(SignupRequest req) {

        if (userRepository.existsByEmail(req.email())) {
            throw new RuntimeException("Email already exists");
        }

        if (req.role() == Role.CUSTOMER && req.customerId() == null) {
            throw new BusinessException("Customer users must select a customer organisation");
        }

        if (req.role() == Role.ADMIN) {
            throw new BusinessException("This role cannot be self-registered");
        }

        Customer customer = req.customerId() == null ? null : customerRepository.findById(req.customerId())
                .orElseThrow(() ->
                        new RuntimeException("Customer not found"));

        boolean requiresApproval = requiresAdminApproval(req.role());

        AppUser user = AppUser.builder()
                .fullName(req.fullName())
                .email(req.email())
                .password(passwordEncoder.encode(req.password()))
                .phone(req.phone())
                .role(req.role())
                .customer(customer)
                .active(!requiresApproval)
                .build();

        userRepository.save(user);

        if (requiresApproval) {
            notificationService.notifyAdminsOfSignupRequest(user);
            return toAuthResponse(user, null, true);
        }

        String token = jwtService.generateToken(user);
        return toAuthResponse(user, token, false);
    }

    public AuthResponse login(AuthRequest req) {
        AppUser user = userRepository.findByEmailWithCustomer(req.email())
                .orElseThrow(() -> new BusinessException("Invalid email or password"));

        if (!passwordEncoder.matches(req.password(), user.getPassword())) {
            throw new BusinessException("Invalid email or password");
        }

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new BusinessException("Your account is pending admin approval. Please try again after approval.");
        }

        String token = jwtService.generateToken(user);
        return toAuthResponse(user, token, false);
    }

    private boolean requiresAdminApproval(Role role) {
        return role == Role.MANAGER || role == Role.DISPATCHER || role == Role.TECHNICIAN;
    }

    private AuthResponse toAuthResponse(AppUser user, String token, boolean pendingApproval) {
        return new AuthResponse(
                token,
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getCustomer() != null ? user.getCustomer().getId() : null,
                user.getCustomer() != null ? user.getCustomer().getName() : null,
                pendingApproval
        );
    }
}
