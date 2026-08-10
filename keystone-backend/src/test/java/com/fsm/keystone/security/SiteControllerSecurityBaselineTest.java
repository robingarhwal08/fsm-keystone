package com.fsm.keystone.security;

import com.fsm.keystone.controller.SiteController;
import com.fsm.keystone.entity.Site;
import com.fsm.keystone.service.SiteService;
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
 * Security baseline @WebMvcTest for {@link SiteController}.
 *
 * <p>Sites are under {@code anyRequest().authenticated()} with no {@code @PreAuthorize}
 * on any handler. Any authenticated user can perform CRUD on sites — a finding for
 * the role-scoping epic.</p>
 */
@WebMvcTest(SiteController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "app.cors.allowed-origin=http://localhost:5173",
        "app.jwt.secret=test-placeholder-jwt-secret-key-must-be-at-least-64-chars-long-0000",
        "app.jwt.expiration-ms=3600000"
})
class SiteControllerSecurityBaselineTest extends BaseControllerSecurityTest {

    @MockBean
    private SiteService siteService;

    @BeforeEach
    void setUp() throws Exception {
        setupJwtFilterPassthrough();
        Site stub = new Site();
        stub.setId(1L);
        stub.setSiteName("Test Site");
        when(siteService.createSite(any())).thenReturn(stub);
        when(siteService.getAllSites()).thenReturn(Collections.emptyList());
        when(siteService.getSitesByCustomerId(anyLong())).thenReturn(Collections.emptyList());
        when(siteService.updateSite(anyLong(), any())).thenReturn(stub);
        // deleteSite is void — no stub needed
    }

    @ParameterizedTest(name = "[{index}] {0} {1} as {2} → HTTP {3}")
    @MethodSource("siteEndpointMatrix")
    void securityBaseline(String method, String path, String role, int expectedStatus)
            throws Exception {
        mockMvc.perform(buildRequest(method, path, role))
                .andExpect(status().is(expectedStatus));
    }

    static Stream<Arguments> siteEndpointMatrix() {
        return MatrixLoader.forController("SiteController")
                .flatMap(row -> Stream.of(
                        Arguments.of(row.method(), row.path(), "ANONYMOUS",  row.expectedStatusAnon()),
                        Arguments.of(row.method(), row.path(), "MANAGER",    row.expectedStatusManager()),
                        Arguments.of(row.method(), row.path(), "DISPATCHER", row.expectedStatusDispatcher()),
                        Arguments.of(row.method(), row.path(), "TECHNICIAN", row.expectedStatusTechnician()),
                        Arguments.of(row.method(), row.path(), "CUSTOMER",   row.expectedStatusCustomer())
                ));
    }
}
