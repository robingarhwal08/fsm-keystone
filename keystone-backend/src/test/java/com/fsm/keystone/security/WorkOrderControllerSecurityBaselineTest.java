package com.fsm.keystone.security;

import com.fsm.keystone.controller.WorkOrderController;
import com.fsm.keystone.entity.PartUsage;
import com.fsm.keystone.entity.TimeLog;
import com.fsm.keystone.entity.WorkOrder;
import com.fsm.keystone.service.WorkOrderService;
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
 * Security baseline @WebMvcTest for {@link WorkOrderController}.
 *
 * <p>Work orders are under {@code anyRequest().authenticated()} (anonymous → 403) and
 * several handlers carry {@code @PreAuthorize} annotations:</p>
 * <ul>
 *   <li>{@code create}: MANAGER, DISPATCHER, CUSTOMER — TECHNICIAN is blocked.</li>
 *   <li>{@code assign}: MANAGER, DISPATCHER only.</li>
 *   <li>{@code status}: MANAGER, DISPATCHER, TECHNICIAN — CUSTOMER is blocked.</li>
 * </ul>
 *
 * <p>All other work-order handlers have no {@code @PreAuthorize}; any authenticated
 * user (including CUSTOMER) can access them — a finding for the role-scoping epic.</p>
 */
@WebMvcTest(WorkOrderController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "app.cors.allowed-origin=http://localhost:5173",
        "app.jwt.secret=test-placeholder-jwt-secret-key-must-be-at-least-64-chars-long-0000",
        "app.jwt.expiration-ms=3600000"
})
class WorkOrderControllerSecurityBaselineTest extends BaseControllerSecurityTest {

    @MockBean
    private WorkOrderService workOrderService;

    @BeforeEach
    void setUp() throws Exception {
        setupJwtFilterPassthrough();
        WorkOrder stub = new WorkOrder();
        stub.setId(1L);
        when(workOrderService.createWorkOrder(any())).thenReturn(stub);
        when(workOrderService.getAllWorkOrders()).thenReturn(Collections.emptyList());
        when(workOrderService.getWorkOrderById(anyLong())).thenReturn(stub);
        when(workOrderService.assignTechnician(anyLong(), any())).thenReturn(stub);
        when(workOrderService.updateStatus(anyLong(), any())).thenReturn(stub);
        when(workOrderService.getWorkOrderHistory(anyLong())).thenReturn(Collections.emptyList());
        when(workOrderService.addPartUsage(anyLong(), any())).thenReturn(new PartUsage());
        when(workOrderService.addTimeLog(anyLong(), any())).thenReturn(new TimeLog());
        when(workOrderService.updateWorkOrder(anyLong(), any())).thenReturn(stub);
        // deleteWorkOrder is void — no stub needed
    }

    @ParameterizedTest(name = "[{index}] {0} {1} as {2} → HTTP {3}")
    @MethodSource("workOrderEndpointMatrix")
    void securityBaseline(String method, String path, String role, int expectedStatus)
            throws Exception {
        mockMvc.perform(buildRequest(method, path, role))
                .andExpect(status().is(expectedStatus));
    }

    static Stream<Arguments> workOrderEndpointMatrix() {
        return MatrixLoader.forController("WorkOrderController")
                .flatMap(row -> Stream.of(
                        Arguments.of(row.method(), row.path(), "ANONYMOUS",  row.expectedStatusAnon()),
                        Arguments.of(row.method(), row.path(), "MANAGER",    row.expectedStatusManager()),
                        Arguments.of(row.method(), row.path(), "DISPATCHER", row.expectedStatusDispatcher()),
                        Arguments.of(row.method(), row.path(), "TECHNICIAN", row.expectedStatusTechnician()),
                        Arguments.of(row.method(), row.path(), "CUSTOMER",   row.expectedStatusCustomer())
                ));
    }
}
