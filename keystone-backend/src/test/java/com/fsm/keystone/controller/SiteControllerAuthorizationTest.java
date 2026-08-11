package com.fsm.keystone.controller;

import com.fsm.keystone.entity.AppUser;
import com.fsm.keystone.entity.Customer;
import com.fsm.keystone.enums.Role;
import com.fsm.keystone.fixtures.TestJwtFactory;
import com.fsm.keystone.security.JwtAuthenticationFilter;
import com.fsm.keystone.security.JwtService;
import com.fsm.keystone.security.SecurityConfig;
import com.fsm.keystone.service.SiteService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @WebMvcTest slice asserting the SiteController role matrix.
 *
 * <p>Covers allow and deny outcomes for all five handlers across MANAGER, DISPATCHER,
 * TECHNICIAN, CUSTOMER and anonymous. @WithMockUser is used for all roles except the
 * CUSTOMER byCustomer ownership test, which requires a real AppUser principal (with a
 * customer FK) so the SpEL expression can evaluate {@code authentication.principal.customer?.id}.
 * Those two cases use a JWT signed with the test secret and a mocked UserDetailsService
 * returning an AppUser with the appropriate customer id.</p>
 */
@Tag("slice")
@WebMvcTest(SiteController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class})
@TestPropertySource(properties = {
        "app.cors.allowed-origin=http://localhost:5173",
        "app.jwt.secret=" + TestJwtFactory.TEST_SECRET,
        "app.jwt.expiration-ms=3600000"
})
class SiteControllerAuthorizationTest {

    @Autowired MockMvc mockMvc;

    @MockBean AuthenticationProvider authenticationProvider;
    @MockBean UserDetailsService     userDetailsService;
    @MockBean SiteService            siteService;

    // ── Minimal JSON bodies required by POST/PUT ──────────────────────────────
    private static final String SITE_BODY =
            "{\"siteName\":\"Test Site\",\"address\":\"1 Main St\",\"city\":\"Springfield\"," +
            "\"state\":\"IL\",\"customer\":{\"id\":1}}";

    // ── MANAGER: full access to all five handlers ─────────────────────────────

    @Nested
    class ManagerAccess {

        @Test
        @WithMockUser(roles = "MANAGER")
        void create_manager_allowed() throws Exception {
            when(siteService.createSite(any())).thenReturn(new com.fsm.keystone.entity.Site());
            mockMvc.perform(post("/api/sites")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(SITE_BODY))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "MANAGER")
        void all_manager_allowed() throws Exception {
            when(siteService.getAllSites()).thenReturn(List.of());
            mockMvc.perform(get("/api/sites"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "MANAGER")
        void byCustomer_manager_allowed() throws Exception {
            when(siteService.getSitesByCustomerId(anyLong())).thenReturn(List.of());
            mockMvc.perform(get("/api/sites/customer/1"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "MANAGER")
        void update_manager_allowed() throws Exception {
            when(siteService.updateSite(anyLong(), any())).thenReturn(new com.fsm.keystone.entity.Site());
            mockMvc.perform(put("/api/sites/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(SITE_BODY))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "MANAGER")
        void delete_manager_allowed() throws Exception {
            mockMvc.perform(delete("/api/sites/1"))
                    .andExpect(status().isOk());
        }
    }

    // ── DISPATCHER: read-only except byCustomer (also allowed) ───────────────

    @Nested
    class DispatcherAccess {

        @Test
        @WithMockUser(roles = "DISPATCHER")
        void create_dispatcher_forbidden() throws Exception {
            mockMvc.perform(post("/api/sites")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(SITE_BODY))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "DISPATCHER")
        void all_dispatcher_allowed() throws Exception {
            when(siteService.getAllSites()).thenReturn(List.of());
            mockMvc.perform(get("/api/sites"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "DISPATCHER")
        void byCustomer_dispatcher_allowed() throws Exception {
            when(siteService.getSitesByCustomerId(anyLong())).thenReturn(List.of());
            mockMvc.perform(get("/api/sites/customer/1"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "DISPATCHER")
        void update_dispatcher_forbidden() throws Exception {
            mockMvc.perform(put("/api/sites/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(SITE_BODY))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "DISPATCHER")
        void delete_dispatcher_forbidden() throws Exception {
            mockMvc.perform(delete("/api/sites/1"))
                    .andExpect(status().isForbidden());
        }
    }

    // ── TECHNICIAN: GET /api/sites only ──────────────────────────────────────

    @Nested
    class TechnicianAccess {

        @Test
        @WithMockUser(roles = "TECHNICIAN")
        void create_technician_forbidden() throws Exception {
            mockMvc.perform(post("/api/sites")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(SITE_BODY))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "TECHNICIAN")
        void all_technician_allowed() throws Exception {
            when(siteService.getAllSites()).thenReturn(List.of());
            mockMvc.perform(get("/api/sites"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "TECHNICIAN")
        void byCustomer_technician_forbidden() throws Exception {
            mockMvc.perform(get("/api/sites/customer/1"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "TECHNICIAN")
        void update_technician_forbidden() throws Exception {
            mockMvc.perform(put("/api/sites/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(SITE_BODY))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "TECHNICIAN")
        void delete_technician_forbidden() throws Exception {
            mockMvc.perform(delete("/api/sites/1"))
                    .andExpect(status().isForbidden());
        }
    }

    // ── CUSTOMER: GET /api/sites + byCustomer own-id only ────────────────────
    //
    // The byCustomer ownership check evaluates authentication.principal.customer?.id,
    // which requires the principal to be an actual AppUser (not the stub User returned
    // by @WithMockUser). These two tests use a JWT so the filter loads the real principal.

    @Nested
    class CustomerAccess {

        @Test
        @WithMockUser(roles = "CUSTOMER")
        void create_customer_forbidden() throws Exception {
            mockMvc.perform(post("/api/sites")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(SITE_BODY))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        void all_customer_allowed() throws Exception {
            when(siteService.getAllSites()).thenReturn(List.of());
            mockMvc.perform(get("/api/sites"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        void update_customer_forbidden() throws Exception {
            mockMvc.perform(put("/api/sites/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(SITE_BODY))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        void delete_customer_forbidden() throws Exception {
            mockMvc.perform(delete("/api/sites/1"))
                    .andExpect(status().isForbidden());
        }

        /**
         * CUSTOMER requesting sites for their OWN customer id — must be allowed.
         * Uses a JWT so the AppUser principal (with customer.id=42) is set in the context.
         */
        @Test
        void byCustomer_customer_ownId_allowed() throws Exception {
            AppUser customerUser = appUserWithCustomerId(42L);
            stubJwtUser(customerUser);
            when(siteService.getSitesByCustomerId(42L)).thenReturn(List.of());

            mockMvc.perform(get("/api/sites/customer/42")
                            .header("Authorization",
                                    "Bearer " + TestJwtFactory.issueFor(customerUser)))
                    .andExpect(status().isOk());
        }

        /**
         * CUSTOMER requesting sites for a DIFFERENT customer id — must be denied with 403.
         * Uses a JWT so the AppUser principal (with customer.id=42) is set in the context.
         */
        @Test
        void byCustomer_customer_otherCustomerId_forbidden() throws Exception {
            AppUser customerUser = appUserWithCustomerId(42L);
            stubJwtUser(customerUser);

            mockMvc.perform(get("/api/sites/customer/99")
                            .header("Authorization",
                                    "Bearer " + TestJwtFactory.issueFor(customerUser)))
                    .andExpect(status().isForbidden());
        }
    }

    // ── Anonymous: all endpoints return 401 ──────────────────────────────────

    @Nested
    class AnonymousAccess {

        @Test
        void create_anonymous_unauthorized() throws Exception {
            mockMvc.perform(post("/api/sites")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(SITE_BODY))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void all_anonymous_unauthorized() throws Exception {
            mockMvc.perform(get("/api/sites"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void byCustomer_anonymous_unauthorized() throws Exception {
            mockMvc.perform(get("/api/sites/customer/1"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void update_anonymous_unauthorized() throws Exception {
            mockMvc.perform(put("/api/sites/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(SITE_BODY))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void delete_anonymous_unauthorized() throws Exception {
            mockMvc.perform(delete("/api/sites/1"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Builds an AppUser with the given customer id, suitable for the byCustomer JWT tests.
     * The customer is attached but only the id is needed for SpEL evaluation.
     */
    private AppUser appUserWithCustomerId(Long customerId) {
        Customer customer = new Customer();
        customer.setId(customerId);

        return AppUser.builder()
                .id(999L)
                .fullName("Slice Customer")
                .email("slice.customer@example.test")
                .password("placeholder")
                .role(Role.CUSTOMER)
                .active(true)
                .customer(customer)
                .build();
    }

    /** Stubs UserDetailsService to return the given AppUser by email. */
    private void stubJwtUser(AppUser user) {
        when(userDetailsService.loadUserByUsername(anyString())).thenReturn(user);
    }
}
