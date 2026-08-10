package com.fsm.keystone.security;

import com.fsm.keystone.controller.UserController;
import com.fsm.keystone.dto.UserResponse;
import com.fsm.keystone.enums.Role;
import com.fsm.keystone.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.util.Collections;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Security baseline @WebMvcTest for {@link UserController}.
 *
 * <p>All user management endpoints are {@code permitAll} with no {@code @PreAuthorize}.
 * This means anonymous actors can list, update, or delete any user — one of the most
 * significant findings captured by this baseline and an immediate target for hardening.</p>
 *
 * <p>{@code DELETE /api/users/{id}} returns HTTP 204 (not 200) because the controller
 * returns {@code ResponseEntity.noContent().build()} — this is captured in the matrix.</p>
 */
@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "app.cors.allowed-origin=http://localhost:5173",
        "app.jwt.secret=test-placeholder-jwt-secret-key-must-be-at-least-64-chars-long-0000",
        "app.jwt.expiration-ms=3600000"
})
class UserControllerSecurityBaselineTest extends BaseControllerSecurityTest {

    @MockBean
    private UserService userService;

    @BeforeEach
    void setUp() throws Exception {
        setupJwtFilterPassthrough();
        UserResponse stub = new UserResponse(1L, "Test", "test@example.test",
                "555-0001", Role.MANAGER, true, null, null);
        when(userService.getAllUsers()).thenReturn(Collections.emptyList());
        when(userService.getAllTechnicians()).thenReturn(Collections.emptyList());
        when(userService.updateUser(anyLong(), any())).thenReturn(stub);
        // deleteUser is void — no stub needed
    }

    @ParameterizedTest(name = "[{index}] {0} {1} as {2} → HTTP {3}")
    @MethodSource("userEndpointMatrix")
    void securityBaseline(String method, String path, String role, int expectedStatus)
            throws Exception {
        mockMvc.perform(buildRequest(method, path, role))
                .andExpect(status().is(expectedStatus));
    }

    static Stream<Arguments> userEndpointMatrix() {
        return MatrixLoader.forController("UserController")
                .flatMap(row -> Stream.of(
                        Arguments.of(row.method(), row.path(), "ANONYMOUS",  row.expectedStatusAnon()),
                        Arguments.of(row.method(), row.path(), "MANAGER",    row.expectedStatusManager()),
                        Arguments.of(row.method(), row.path(), "DISPATCHER", row.expectedStatusDispatcher()),
                        Arguments.of(row.method(), row.path(), "TECHNICIAN", row.expectedStatusTechnician()),
                        Arguments.of(row.method(), row.path(), "CUSTOMER",   row.expectedStatusCustomer())
                ));
    }
}
