package com.fsm.keystone.security;

import com.fsm.keystone.controller.TimeLogController;
import com.fsm.keystone.entity.TimeLog;
import com.fsm.keystone.service.WorkOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Security baseline @WebMvcTest for {@link TimeLogController}.
 *
 * <p>{@code POST /api/time-logs} is {@code permitAll} with no {@code @PreAuthorize},
 * meaning any anonymous actor can log billable time — an identified finding for the
 * default-deny hardening epic.</p>
 *
 * <p>A valid request body with all @NotNull fields is required to avoid a 400
 * validation error and confirm the route is accessible (200) rather than just
 * "not blocked by security".</p>
 */
@WebMvcTest(TimeLogController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "app.cors.allowed-origin=http://localhost:5173",
        "app.jwt.secret=test-placeholder-jwt-secret-key-must-be-at-least-64-chars-long-0000",
        "app.jwt.expiration-ms=3600000"
})
class TimeLogControllerSecurityBaselineTest extends BaseControllerSecurityTest {

    @MockBean
    private WorkOrderService workOrderService;

    @BeforeEach
    void setUp() throws Exception {
        setupJwtFilterPassthrough();
        when(workOrderService.addTimeLog(anyLong(), any())).thenReturn(new TimeLog());
    }

    @ParameterizedTest(name = "[{index}] {0} {1} as {2} → HTTP {3}")
    @MethodSource("timeLogEndpointMatrix")
    void securityBaseline(String method, String path, String role, int expectedStatus)
            throws Exception {
        mockMvc.perform(buildRequest(method, path, role))
                .andExpect(status().is(expectedStatus));
    }

    static Stream<Arguments> timeLogEndpointMatrix() {
        return MatrixLoader.forController("TimeLogController")
                .flatMap(row -> Stream.of(
                        Arguments.of(row.method(), row.path(), "ANONYMOUS",  row.expectedStatusAnon()),
                        Arguments.of(row.method(), row.path(), "MANAGER",    row.expectedStatusManager()),
                        Arguments.of(row.method(), row.path(), "DISPATCHER", row.expectedStatusDispatcher()),
                        Arguments.of(row.method(), row.path(), "TECHNICIAN", row.expectedStatusTechnician()),
                        Arguments.of(row.method(), row.path(), "CUSTOMER",   row.expectedStatusCustomer())
                ));
    }
}
