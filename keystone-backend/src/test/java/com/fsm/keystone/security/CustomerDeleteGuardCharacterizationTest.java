package com.fsm.keystone.security;

import com.fsm.keystone.controller.CustomerController;
import com.fsm.keystone.service.CustomerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CHARACTERIZATION TEST — records the current, broken behaviour of
 * {@code DELETE /api/customers/{id}}.
 *
 * <h2>Current behaviour (DEFECT)</h2>
 * {@code CustomerController.delete()} has its {@code @PreAuthorize("hasRole('MANAGER')")}
 * annotation commented out. Combined with the {@code /api/customers/**} {@code permitAll}
 * filter-chain rule, any caller — including an unauthenticated anonymous request — can
 * delete a customer and receives HTTP 200.
 *
 * <h2>Target behaviour (post-hardening)</h2>
 * Once the authorization epic restores the guard:
 * <ol>
 *   <li>Anonymous callers → 403 Forbidden</li>
 *   <li>DISPATCHER, TECHNICIAN, CUSTOMER roles → 403 Forbidden</li>
 *   <li>MANAGER role → 200 OK (or 204 No Content, depending on the response shape change)</li>
 * </ol>
 * When the guard is restored, flip these assertions and remove the {@code CHARACTERIZATION}
 * comment so the test documents the intended security contract.
 */
@Tag("slice")
@WebMvcTest(CustomerController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class})
@TestPropertySource(properties = {
        "app.cors.allowed-origin=http://localhost:5173",
        "app.jwt.secret=test-fixture-jwt-secret-key-must-be-at-least-64-chars-00000000",
        "app.jwt.expiration-ms=3600000"
})
class CustomerDeleteGuardCharacterizationTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean AuthenticationProvider authenticationProvider;
    @MockBean UserDetailsService     userDetailsService;
    @MockBean CustomerService        customerService;

    @BeforeEach
    void stubDeleteToNoOp() {
        // void method — Mockito default is no-op, no stub needed
    }

    /**
     * CHARACTERIZATION: anonymous DELETE is currently HTTP 200.
     *
     * <p>Target post-hardening: 403 — un-comment {@code @PreAuthorize} on
     * {@code CustomerController.delete()} to fix this.</p>
     */
    @Test
    void anonymousDelete_isCurrently200_characterization() throws Exception {
        // CHARACTERIZATION: @PreAuthorize("hasRole('MANAGER')") is commented out on
        // CustomerController.delete(). The /api/customers/** permitAll rule allows this
        // through the filter chain, and without a method-level guard, anonymous succeeds.
        // TARGET: 403 once the annotation is restored.
        mockMvc.perform(delete("/api/customers/1"))
                .andExpect(status().is2xxSuccessful());
    }

    /**
     * CHARACTERIZATION: non-MANAGER authenticated users can also delete customers.
     *
     * <p>TECHNICIAN getting 200 on a destructive operation is an unintended gap that the
     * authorization epic must close. Target post-hardening: 403 for all non-MANAGER roles.</p>
     */
    @Test
    void technicianDelete_isCurrently200_characterization() throws Exception {
        // CHARACTERIZATION: TECHNICIAN should be forbidden once @PreAuthorize is restored.
        mockMvc.perform(delete("/api/customers/1")
                        .with(org.springframework.security.test.web.servlet.request
                                .SecurityMockMvcRequestPostProcessors.user("t@fixture.test").roles("TECHNICIAN")))
                .andExpect(status().is2xxSuccessful());
    }
}
