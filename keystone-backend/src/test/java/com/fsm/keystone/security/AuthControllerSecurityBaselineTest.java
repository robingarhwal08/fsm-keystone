package com.fsm.keystone.security;

import com.fsm.keystone.controller.AuthController;
import com.fsm.keystone.dto.AuthResponse;
import com.fsm.keystone.enums.Role;
import com.fsm.keystone.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Security baseline @WebMvcTest for {@link AuthController}.
 *
 * <p>Verifies CSRF is disabled and that auth endpoints are accessible to all actors
 * (anonymous and all roles) as they are declared {@code permitAll} with no
 * {@code @PreAuthorize} annotation.</p>
 *
 * <p>Expected statuses are driven entirely from {@code authorization-matrix.csv};
 * changing this test to fail means the security surface changed.</p>
 */
@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "app.cors.allowed-origin=http://localhost:5173",
        "app.jwt.secret=test-placeholder-jwt-secret-key-must-be-at-least-64-chars-long-0000",
        "app.jwt.expiration-ms=3600000"
})
class AuthControllerSecurityBaselineTest extends BaseControllerSecurityTest {

    @MockBean
    private AuthService authService;

    @BeforeEach
    void setUp() throws Exception {
        setupJwtFilterPassthrough();
        AuthResponse stub = new AuthResponse(
                "stub-token", 99L, "Test", "test@example.test", Role.CUSTOMER, null, null);
        when(authService.signup(any())).thenReturn(stub);
        when(authService.login(any())).thenReturn(stub);
    }

    // ─── CSRF disabled assertion ─────────────────────────────────────────────

    @Test
    void csrf_isDisabled_postWithoutCsrfTokenSucceeds() throws Exception {
        // If CSRF were enabled, a POST without CSRF token would return 403.
        // This explicit test documents and guards the current disabled state.
        mockMvc.perform(buildRequest("POST", "/api/auth/login", "ANONYMOUS"))
                .andExpect(status().isNot(403)); // 200 expected; would be 403 if CSRF re-enabled
    }

    // ─── Parameterized security matrix ──────────────────────────────────────

    @ParameterizedTest(name = "[{index}] {0} {1} as {2} → HTTP {3}")
    @MethodSource("authEndpointMatrix")
    void securityBaseline(String method, String path, String role, int expectedStatus)
            throws Exception {
        mockMvc.perform(buildRequest(method, path, role))
                .andExpect(status().is(expectedStatus));
    }

    static Stream<Arguments> authEndpointMatrix() {
        return MatrixLoader.forController("AuthController")
                .flatMap(row -> Stream.of(
                        Arguments.of(row.method(), row.path(), "ANONYMOUS",  row.expectedStatusAnon()),
                        Arguments.of(row.method(), row.path(), "MANAGER",    row.expectedStatusManager()),
                        Arguments.of(row.method(), row.path(), "DISPATCHER", row.expectedStatusDispatcher()),
                        Arguments.of(row.method(), row.path(), "TECHNICIAN", row.expectedStatusTechnician()),
                        Arguments.of(row.method(), row.path(), "CUSTOMER",   row.expectedStatusCustomer())
                ));
    }
}
