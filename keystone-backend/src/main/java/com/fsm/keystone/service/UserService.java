package com.fsm.keystone.service;

import com.fsm.keystone.dto.CreateUserRequest;
import com.fsm.keystone.dto.UpdateProfileRequest;
import com.fsm.keystone.dto.UpdateUserRequest;
import com.fsm.keystone.dto.UserResponse;
import com.fsm.keystone.entity.AppUser;
import com.fsm.keystone.entity.Customer;
import com.fsm.keystone.enums.Role;
import com.fsm.keystone.exception.BusinessException;
import com.fsm.keystone.repository.CustomerRepository;
import com.fsm.keystone.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final List<Role> ADMIN_MANAGED_ROLES = List.of(
            Role.MANAGER,
            Role.DISPATCHER,
            Role.TECHNICIAN,
            Role.CUSTOMER
    );

    private static final List<Role> MANAGER_MANAGED_ROLES = List.of(
            Role.DISPATCHER,
            Role.TECHNICIAN,
            Role.CUSTOMER
    );

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final CascadeDeleteService cascadeDeleteService;
    private final CurrentUserService currentUserService;
    private final PasswordEncoder passwordEncoder;

    public List<UserResponse> listManagedUsers() {
        AppUser actor = requireUserManager();
        List<Role> roles = actor.getRole() == Role.ADMIN
                ? ADMIN_MANAGED_ROLES
                : MANAGER_MANAGED_ROLES;
        return userRepository.findByRoleInWithCustomer(roles)
                .stream()
                .map(UserResponse::fromEntity)
                .toList();
    }

    public List<UserResponse> getAllTechnicians() {
        return userRepository.findByRole(Role.TECHNICIAN)
                .stream()
                .map(UserResponse::fromEntity)
                .toList();
    }

    public UserResponse createUser(CreateUserRequest request) {
        AppUser actor = requireUserManager();
        validateAssignableRole(actor, request.role());

        if (userRepository.existsByEmail(request.email().trim().toLowerCase())) {
            throw new BusinessException("Email already exists");
        }

        AppUser user = AppUser.builder()
                .fullName(request.fullName().trim())
                .email(request.email().trim().toLowerCase())
                .password(passwordEncoder.encode(request.password()))
                .phone(StringUtils.hasText(request.phone()) ? request.phone().trim() : null)
                .role(request.role())
                .active(request.active() == null || request.active())
                .build();

        applyCustomerLink(user, request.role(), request.customerId());
        AppUser saved = userRepository.save(user);

        return UserResponse.fromEntity(
                userRepository.findByIdWithCustomer(saved.getId())
                        .orElseThrow(() -> new RuntimeException("User not found"))
        );
    }

    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        AppUser actor = requireUserManager();
        AppUser user = userRepository.findByIdWithCustomer(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        assertCanManageTarget(actor, user);
        validateAssignableRole(actor, request.role());

        user.setFullName(request.fullName().trim());
        user.setPhone(StringUtils.hasText(request.phone()) ? request.phone().trim() : null);
        user.setRole(request.role());
        user.setActive(request.active());
        applyCustomerLink(user, request.role(), request.customerId());

        if (StringUtils.hasText(request.newPassword())) {
            if (request.newPassword().length() < 6) {
                throw new BusinessException("New password must be at least 6 characters");
            }
            user.setPassword(passwordEncoder.encode(request.newPassword()));
        }

        userRepository.save(user);

        return UserResponse.fromEntity(
                userRepository.findByIdWithCustomer(id)
                        .orElseThrow(() -> new RuntimeException("User not found"))
        );
    }

    public UserResponse getMyProfile() {
        AppUser actor = currentUserService.requireUser();
        AppUser user = userRepository.findByIdWithCustomer(actor.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return UserResponse.fromEntity(user);
    }

    public UserResponse updateMyProfile(UpdateProfileRequest request) {
        AppUser actor = currentUserService.requireUser();
        AppUser user = userRepository.findByIdWithCustomer(actor.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setFullName(request.fullName().trim());
        user.setPhone(StringUtils.hasText(request.phone()) ? request.phone().trim() : null);

        if (StringUtils.hasText(request.newPassword())) {
            if (!StringUtils.hasText(request.currentPassword())) {
                throw new BusinessException("Current password is required to set a new password");
            }
            if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
                throw new BusinessException("Current password is incorrect");
            }
            if (request.newPassword().length() < 6) {
                throw new BusinessException("New password must be at least 6 characters");
            }
            user.setPassword(passwordEncoder.encode(request.newPassword()));
        }

        userRepository.save(user);
        return UserResponse.fromEntity(user);
    }

    public void deleteUser(Long id) {
        AppUser actor = requireUserManager();
        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (actor.getId().equals(user.getId())) {
            throw new BusinessException("You cannot delete your own account");
        }

        assertCanManageTarget(actor, user);
        cascadeDeleteService.deleteUser(id);
    }

    public UserResponse approveUser(Long id) {
        AppUser actor = currentUserService.requireUser();
        if (actor.getRole() != Role.ADMIN) {
            throw new BusinessException("Only admins can approve accounts");
        }

        AppUser user = userRepository.findByIdWithCustomer(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (Boolean.TRUE.equals(user.getActive())) {
            throw new BusinessException("This account is already approved");
        }

        user.setActive(true);
        userRepository.save(user);

        return UserResponse.fromEntity(
                userRepository.findByIdWithCustomer(id)
                        .orElseThrow(() -> new RuntimeException("User not found"))
        );
    }

    private AppUser requireUserManager() {
        AppUser actor = currentUserService.requireUser();
        if (actor.getRole() != Role.MANAGER && actor.getRole() != Role.ADMIN) {
            throw new BusinessException("Not allowed to manage users");
        }
        return actor;
    }

    private void assertCanManageTarget(AppUser actor, AppUser target) {
        if (actor.getRole() == Role.ADMIN) {
            if (target.getRole() == Role.ADMIN) {
                throw new BusinessException("Admin accounts cannot manage other admin users");
            }
            return;
        }

        if (target.getRole() == Role.MANAGER || target.getRole() == Role.ADMIN) {
            throw new BusinessException("Cannot manage this user");
        }
    }

    private void validateAssignableRole(AppUser actor, Role role) {
        if (role == Role.ADMIN) {
            throw new BusinessException("Admin role cannot be assigned from user management");
        }
        if (actor.getRole() == Role.ADMIN && !ADMIN_MANAGED_ROLES.contains(role)) {
            throw new BusinessException("Cannot assign this role");
        }
        if (actor.getRole() == Role.MANAGER && !MANAGER_MANAGED_ROLES.contains(role)) {
            throw new BusinessException("Cannot assign this role");
        }
    }

    private void applyCustomerLink(AppUser user, Role role, Long customerId) {
        if (role != Role.CUSTOMER) {
            user.setCustomer(null);
            return;
        }
        if (customerId == null) {
            throw new BusinessException("Customer users must be linked to a customer organisation");
        }
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new BusinessException("Customer organisation not found"));
        user.setCustomer(customer);
    }
}
