package com.fsm.keystone.controller;

import com.fsm.keystone.dto.UserResponse;
import com.fsm.keystone.enums.Role;
import com.fsm.keystone.fixtures.Personas;
import com.fsm.keystone.fixtures.TestJwtFactory;
import com.fsm.keystone.security.JwtAuthenticationFilter;
import com.fsm.keystone.security.JwtService;
import com.fsm.keystone.security.SecurityConfig;
import com.fsm.keystone.service.UserService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("slice")
@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class})
@TestPropertySource(properties = {
        "app.cors.allowed-origin=http://localhost:5173",
        "app.jwt.secret=" + TestJwtFactory.TEST_SECRET,
        "app.jwt.expiration-ms=3600000"
})
class UserControllerAuthorizationTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean AuthenticationProvider authenticationProvider;
    @MockBean UserDetailsService     userDetailsService;
    @MockBean UserService            userService;

    private static final UserResponse STUB_USER = new UserResponse(
            1L, "Test", "t@example.test", null, Role.MANAGER, true, null, null);
    private static final String UPDATE_BODY =
            "{\"fullName\":\"Updated\",\"phone\":null,\"role\":\"DISPATCHER\",\"active\":true}";

    // ── MANAGER access ────────────────────────────────────────────────────────

    @Nested
    class ManagerAccess {

        @Test
        @WithMockUser(roles = "MANAGER")
        void getAllUsers_manager_allowed() throws Exception {
            when(userService.getAllUsers()).thenReturn(Collections.emptyList());
            mockMvc.perform(get("/api/users"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "MANAGER")
        void getAllTechnicians_manager_allowed() throws Exception {
            when(userService.getAllTechnicians()).thenReturn(Collections.emptyList());
            mockMvc.perform(get("/api/users/technicians"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "MANAGER")
        void getMe_manager_allowed() throws Exception {
            when(userService.getMyProfile(any())).thenReturn(STUB_USER);
            mockMvc.perform(get("/api/users/me"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "MANAGER")
        void updateUser_manager_allowed() throws Exception {
            when(userService.updateUser(anyLong(), any())).thenReturn(STUB_USER);
            mockMvc.perform(put("/api/users/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(UPDATE_BODY))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "MANAGER")
        void deleteUser_manager_allowed() throws Exception {
            mockMvc.perform(delete("/api/users/1"))
                    .andExpect(status().isNoContent());
        }
    }

    // ── DISPATCHER access ─────────────────────────────────────────────────────

    @Nested
    class DispatcherAccess {

        @Test
        @WithMockUser(roles = "DISPATCHER")
        void getAllUsers_dispatcher_forbidden() throws Exception {
            mockMvc.perform(get("/api/users")).andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "DISPATCHER")
        void getAllTechnicians_dispatcher_forbidden() throws Exception {
            mockMvc.perform(get("/api/users/technicians")).andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "DISPATCHER")
        void getMe_dispatcher_allowed() throws Exception {
            when(userService.getMyProfile(any())).thenReturn(STUB_USER);
            mockMvc.perform(get("/api/users/me")).andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "DISPATCHER")
        void updateUser_dispatcher_forbidden() throws Exception {
            mockMvc.perform(put("/api/users/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(UPDATE_BODY))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "DISPATCHER")
        void deleteUser_dispatcher_forbidden() throws Exception {
            mockMvc.perform(delete("/api/users/1")).andExpect(status().isForbidden());
        }
    }

    // ── TECHNICIAN access ─────────────────────────────────────────────────────

    @Nested
    class TechnicianAccess {

        @Test
        @WithMockUser(roles = "TECHNICIAN")
        void getAllUsers_technician_forbidden() throws Exception {
            mockMvc.perform(get("/api/users")).andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "TECHNICIAN")
        void getAllTechnicians_technician_forbidden() throws Exception {
            mockMvc.perform(get("/api/users/technicians")).andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "TECHNICIAN")
        void getMe_technician_allowed() throws Exception {
            when(userService.getMyProfile(any())).thenReturn(STUB_USER);
            mockMvc.perform(get("/api/users/me")).andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "TECHNICIAN")
        void updateUser_technician_forbidden() throws Exception {
            mockMvc.perform(put("/api/users/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(UPDATE_BODY))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "TECHNICIAN")
        void deleteUser_technician_forbidden() throws Exception {
            mockMvc.perform(delete("/api/users/1")).andExpect(status().isForbidden());
        }
    }

    // ── CUSTOMER access ───────────────────────────────────────────────────────

    @Nested
    class CustomerAccess {

        @Test
        @WithMockUser(roles = "CUSTOMER")
        void getAllUsers_customer_forbidden() throws Exception {
            mockMvc.perform(get("/api/users")).andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        void getAllTechnicians_customer_forbidden() throws Exception {
            mockMvc.perform(get("/api/users/technicians")).andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        void getMe_customer_allowed() throws Exception {
            when(userService.getMyProfile(any())).thenReturn(STUB_USER);
            mockMvc.perform(get("/api/users/me")).andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        void updateUser_customer_forbidden() throws Exception {
            mockMvc.perform(put("/api/users/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(UPDATE_BODY))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        void deleteUser_customer_forbidden() throws Exception {
            mockMvc.perform(delete("/api/users/1")).andExpect(status().isForbidden());
        }
    }

    // ── Anonymous access ──────────────────────────────────────────────────────

    @Nested
    class AnonymousAccess {

        @Test
        void getAllUsers_anonymous_forbidden() throws Exception {
            mockMvc.perform(get("/api/users")).andExpect(status().isForbidden());
        }

        @Test
        void getAllTechnicians_anonymous_forbidden() throws Exception {
            mockMvc.perform(get("/api/users/technicians")).andExpect(status().isForbidden());
        }

        @Test
        void getMe_anonymous_forbidden() throws Exception {
            mockMvc.perform(get("/api/users/me")).andExpect(status().isForbidden());
        }

        @Test
        void updateUser_anonymous_forbidden() throws Exception {
            mockMvc.perform(put("/api/users/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(UPDATE_BODY))
                    .andExpect(status().isForbidden());
        }

        @Test
        void deleteUser_anonymous_forbidden() throws Exception {
            mockMvc.perform(delete("/api/users/1")).andExpect(status().isForbidden());
        }
    }

    // ── JWT bearer token coverage ─────────────────────────────────────────────

    @Test
    void bearerToken_manager_getAllUsers_returns200() throws Exception {
        when(userDetailsService.loadUserByUsername(Personas.MANAGER.getEmail()))
                .thenReturn(Personas.MANAGER);
        when(userService.getAllUsers()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/users")
                        .header("Authorization", TestJwtFactory.bearerHeaderFor(Personas.MANAGER)))
                .andExpect(status().isOk());
    }

    @Test
    void bearerToken_technician_getAllUsers_returns403() throws Exception {
        when(userDetailsService.loadUserByUsername(Personas.TECHNICIAN.getEmail()))
                .thenReturn(Personas.TECHNICIAN);

        mockMvc.perform(get("/api/users")
                        .header("Authorization", TestJwtFactory.bearerHeaderFor(Personas.TECHNICIAN)))
                .andExpect(status().isForbidden());
    }

    @Test
    void bearerToken_manager_getMe_returns200() throws Exception {
        when(userDetailsService.loadUserByUsername(Personas.MANAGER.getEmail()))
                .thenReturn(Personas.MANAGER);
        when(userService.getMyProfile(any())).thenReturn(STUB_USER);

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", TestJwtFactory.bearerHeaderFor(Personas.MANAGER)))
                .andExpect(status().isOk());
    }
}
