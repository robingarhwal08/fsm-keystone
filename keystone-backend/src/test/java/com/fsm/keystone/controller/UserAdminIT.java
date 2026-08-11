package com.fsm.keystone.controller;

import com.fsm.keystone.entity.AppUser;
import com.fsm.keystone.entity.UserAuditLog;
import com.fsm.keystone.enums.Role;
import com.fsm.keystone.fixtures.TestJwtFactory;
import com.fsm.keystone.fixtures.UserFixtures;
import com.fsm.keystone.repository.UserAuditLogRepository;
import com.fsm.keystone.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test verifying MANAGER-only access to UserController and that
 * role/activation changes produce audit records in the user_audit_log table.
 *
 * <p>Uses @SpringBootTest with Testcontainers and Hibernate DDL to build the schema.
 * Consistent with the integration test pattern established in WO-051.</p>
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@TestPropertySource(properties = {
        "app.cors.allowed-origin=http://localhost:5173",
        "app.jwt.secret=" + TestJwtFactory.TEST_SECRET,
        "app.jwt.expiration-ms=3600000",
        "spring.jpa.hibernate.ddl-auto=update",
        "spring.flyway.enabled=false"
})
class UserAdminIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired MockMvc               mockMvc;
    @Autowired UserRepository        userRepo;
    @Autowired UserAuditLogRepository auditLogRepo;

    private AppUser manager1;
    private AppUser manager2;
    private AppUser dispatcher;
    private AppUser technician;
    private AppUser customerUser;

    @BeforeEach
    void setUp() {
        auditLogRepo.deleteAll();
        userRepo.deleteAll();

        manager1 = userRepo.save(AppUser.builder()
                .fullName("Manager One")
                .email("manager1@admin.test")
                .password(UserFixtures.TEST_PASSWORD_PLACEHOLDER)
                .role(Role.MANAGER)
                .active(true)
                .build());

        manager2 = userRepo.save(AppUser.builder()
                .fullName("Manager Two")
                .email("manager2@admin.test")
                .password(UserFixtures.TEST_PASSWORD_PLACEHOLDER)
                .role(Role.MANAGER)
                .active(true)
                .build());

        dispatcher = userRepo.save(AppUser.builder()
                .fullName("Dispatcher")
                .email("dispatcher@admin.test")
                .password(UserFixtures.TEST_PASSWORD_PLACEHOLDER)
                .role(Role.DISPATCHER)
                .active(true)
                .build());

        technician = userRepo.save(AppUser.builder()
                .fullName("Technician")
                .email("technician@admin.test")
                .password(UserFixtures.TEST_PASSWORD_PLACEHOLDER)
                .role(Role.TECHNICIAN)
                .active(true)
                .build());

        customerUser = userRepo.save(AppUser.builder()
                .fullName("Customer User")
                .email("customer@admin.test")
                .password(UserFixtures.TEST_PASSWORD_PLACEHOLDER)
                .role(Role.CUSTOMER)
                .active(true)
                .build());
    }

    // ── Authorization boundary ────────────────────────────────────────────────

    @Test
    void technician_getAllUsers_returns403() throws Exception {
        String token = TestJwtFactory.issueFor(technician);

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void technician_updateUser_returns403() throws Exception {
        String token = TestJwtFactory.issueFor(technician);
        String body = "{\"fullName\":\"Attempt\",\"phone\":null,\"role\":\"MANAGER\",\"active\":true}";

        mockMvc.perform(put("/api/users/" + dispatcher.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void manager_getAllUsers_returns200() throws Exception {
        String token = TestJwtFactory.issueFor(manager1);

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    // ── Audit log written on role change ──────────────────────────────────────

    @Test
    void manager_updateUser_roleChange_createsAuditRecord() throws Exception {
        String token = TestJwtFactory.issueFor(manager1);
        String body = "{\"fullName\":\"Dispatcher\",\"phone\":null,\"role\":\"TECHNICIAN\",\"active\":true}";

        mockMvc.perform(put("/api/users/" + dispatcher.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        List<UserAuditLog> logs = auditLogRepo.findByTargetUserId(dispatcher.getId());
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getAction()).isEqualTo("ROLE_CHANGE");
        assertThat(logs.get(0).getPreviousValue()).isEqualTo("DISPATCHER");
        assertThat(logs.get(0).getNewValue()).isEqualTo("TECHNICIAN");
        assertThat(logs.get(0).getActorId()).isEqualTo(manager1.getId());
    }

    // ── Last-manager protection ───────────────────────────────────────────────

    @Test
    void manager_demoteLastManager_returns409() throws Exception {
        // Remove manager2 so only manager1 remains
        userRepo.delete(manager2);
        String token = TestJwtFactory.issueFor(manager1);
        String body = "{\"fullName\":\"Manager One\",\"phone\":null,\"role\":\"DISPATCHER\",\"active\":true}";

        mockMvc.perform(put("/api/users/" + manager1.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    // ── Self-profile read ─────────────────────────────────────────────────────

    @Test
    void technician_getMe_returns200WithOwnProfile() throws Exception {
        String token = TestJwtFactory.issueFor(technician);

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void anonymous_getMe_returns403() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isForbidden());
    }
}
