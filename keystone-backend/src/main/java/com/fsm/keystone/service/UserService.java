package com.fsm.keystone.service;

import com.fsm.keystone.dto.UpdateUserRequest;
import com.fsm.keystone.dto.UserResponse;
import com.fsm.keystone.entity.AppUser;
import com.fsm.keystone.enums.Role;
import com.fsm.keystone.exception.BusinessRuleException;
import com.fsm.keystone.exception.ErrorCode;
import com.fsm.keystone.exception.ResourceNotFoundException;
import com.fsm.keystone.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserAdminAuditService userAdminAuditService;

    public List<UserResponse> getAllUsers() {
        return userRepository.findByRoleNot(Role.MANAGER)
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

    public UserResponse getMyProfile(AppUser principal) {
        return UserResponse.fromEntity(principal);
    }

    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean callerIsManager = auth != null && auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_MANAGER"::equals);

        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("User", id));

        boolean roleChanging = request.role() != null && request.role() != user.getRole();
        boolean activeChanging = request.active() != null
                && !request.active().equals(user.getActive());
        boolean deactivating = activeChanging && Boolean.FALSE.equals(request.active());

        // Defense-in-depth: service layer rejects role changes from non-managers
        if (roleChanging && !callerIsManager) {
            throw new AccessDeniedException("Role changes require MANAGER authority");
        }

        // Last-manager protection: reject demotion or deactivation of the last active manager
        if (user.getRole() == Role.MANAGER && Boolean.TRUE.equals(user.getActive())
                && (roleChanging || deactivating)) {
            long activeManagerCount = userRepository.countByRoleAndActive(Role.MANAGER, true);
            if (activeManagerCount <= 1) {
                throw new BusinessRuleException(ErrorCode.LAST_MANAGER,
                        "Cannot demote or deactivate the last remaining active MANAGER");
            }
        }

        Role oldRole = user.getRole();
        Boolean oldActive = user.getActive();

        user.setFullName(request.fullName());
        user.setPhone(request.phone());
        user.setRole(request.role());
        user.setActive(request.active());

        AppUser updatedUser = userRepository.save(user);

        Long actorId = resolveActorId(auth);
        if (roleChanging) {
            userAdminAuditService.recordRoleChange(actorId, id, oldRole, request.role());
        }
        if (activeChanging) {
            userAdminAuditService.recordActivationChange(actorId, id, oldActive, request.active());
        }

        return UserResponse.fromEntity(updatedUser);
    }

    public void deleteUser(Long id) {
        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("User", id));
        userRepository.delete(user);
    }

    private static Long resolveActorId(Authentication auth) {
        if (auth != null && auth.getPrincipal() instanceof AppUser appUser) {
            return appUser.getId();
        }
        return null;
    }
}
