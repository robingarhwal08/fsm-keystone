package com.fsm.keystone.service;

import com.fsm.keystone.dto.UpdateUserRequest;
import com.fsm.keystone.dto.UserResponse;
import com.fsm.keystone.entity.AppUser;
import com.fsm.keystone.enums.Role;
import com.fsm.keystone.exception.BusinessRuleException;
import com.fsm.keystone.exception.ErrorCode;
import com.fsm.keystone.exception.ResourceNotFoundException;
import com.fsm.keystone.fixtures.Personas;
import com.fsm.keystone.fixtures.UserFixtures;
import com.fsm.keystone.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    UserAdminAuditService userAdminAuditService;

    @InjectMocks
    UserService userService;

    private AppUser managerUser;
    private AppUser technicianUser;
    private AppUser targetDispatcher;

    @BeforeEach
    void setUp() {
        managerUser = UserFixtures.aManager().id(1L).build();
        technicianUser = UserFixtures.aTechnician().id(2L).build();
        targetDispatcher = UserFixtures.aDispatcher().id(10L).build();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ── getMyProfile ──────────────────────────────────────────────────────────

    @Test
    void getMyProfile_returnsCallerOwnProfile() {
        UserResponse result = userService.getMyProfile(managerUser);

        assertThat(result.id()).isEqualTo(managerUser.getId());
        assertThat(result.email()).isEqualTo(managerUser.getEmail());
        assertThat(result.role()).isEqualTo(Role.MANAGER);
    }

    // ── role-change guard ─────────────────────────────────────────────────────

    @Test
    void updateUser_roleChange_rejectedForNonManager() {
        setSecurityContext(technicianUser);
        when(userRepository.findById(10L)).thenReturn(Optional.of(targetDispatcher));

        UpdateUserRequest request = new UpdateUserRequest(
                "Updated Name", null, Role.MANAGER, true);

        assertThatThrownBy(() -> userService.updateUser(10L, request))
                .isInstanceOf(AccessDeniedException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUser_roleChange_allowedForManager() {
        setSecurityContext(managerUser);
        AppUser saved = UserFixtures.aDispatcher().id(10L).role(Role.MANAGER).build();
        when(userRepository.findById(10L)).thenReturn(Optional.of(targetDispatcher));
        when(userRepository.countByRoleAndActive(Role.MANAGER, true)).thenReturn(2L);
        when(userRepository.save(any())).thenReturn(saved);

        UpdateUserRequest request = new UpdateUserRequest(
                "Updated Name", null, Role.MANAGER, true);

        UserResponse result = userService.updateUser(10L, request);

        assertThat(result).isNotNull();
        verify(userAdminAuditService).recordRoleChange(anyLong(), eq(10L), eq(Role.DISPATCHER), eq(Role.MANAGER));
    }

    // ── last-manager protection ───────────────────────────────────────────────

    @Test
    void updateUser_demoteLastManager_throwsLastManager() {
        AppUser lastManager = UserFixtures.aManager().id(5L).build();
        setSecurityContext(managerUser);
        when(userRepository.findById(5L)).thenReturn(Optional.of(lastManager));
        when(userRepository.countByRoleAndActive(Role.MANAGER, true)).thenReturn(1L);

        UpdateUserRequest request = new UpdateUserRequest(
                "Last Manager", null, Role.DISPATCHER, true);

        assertThatThrownBy(() -> userService.updateUser(5L, request))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(ex -> ((BusinessRuleException) ex).getCode())
                .isEqualTo(ErrorCode.LAST_MANAGER);

        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUser_deactivateLastManager_throwsLastManager() {
        AppUser lastManager = UserFixtures.aManager().id(5L).build();
        setSecurityContext(managerUser);
        when(userRepository.findById(5L)).thenReturn(Optional.of(lastManager));
        when(userRepository.countByRoleAndActive(Role.MANAGER, true)).thenReturn(1L);

        UpdateUserRequest request = new UpdateUserRequest(
                "Last Manager", null, Role.MANAGER, false);

        assertThatThrownBy(() -> userService.updateUser(5L, request))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(ex -> ((BusinessRuleException) ex).getCode())
                .isEqualTo(ErrorCode.LAST_MANAGER);

        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUser_demoteWhenMultipleManagers_succeeds() {
        AppUser anotherManager = UserFixtures.aManager().id(5L).build();
        AppUser saved = UserFixtures.aManager().id(5L).role(Role.DISPATCHER).build();
        setSecurityContext(managerUser);
        when(userRepository.findById(5L)).thenReturn(Optional.of(anotherManager));
        when(userRepository.countByRoleAndActive(Role.MANAGER, true)).thenReturn(2L);
        when(userRepository.save(any())).thenReturn(saved);

        UpdateUserRequest request = new UpdateUserRequest(
                "Former Manager", null, Role.DISPATCHER, true);

        UserResponse result = userService.updateUser(5L, request);

        assertThat(result).isNotNull();
        verify(userAdminAuditService).recordRoleChange(anyLong(), eq(5L), eq(Role.MANAGER), eq(Role.DISPATCHER));
    }

    // ── not-found ─────────────────────────────────────────────────────────────

    @Test
    void updateUser_userNotFound_throwsResourceNotFoundException() {
        setSecurityContext(managerUser);
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        UpdateUserRequest request = new UpdateUserRequest("Name", null, Role.DISPATCHER, true);

        assertThatThrownBy(() -> userService.updateUser(999L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteUser_userNotFound_throwsResourceNotFoundException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteUser(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void setSecurityContext(AppUser user) {
        SecurityContextHolder.setContext(
                new SecurityContextImpl(Personas.authenticationFor(user)));
    }
}
