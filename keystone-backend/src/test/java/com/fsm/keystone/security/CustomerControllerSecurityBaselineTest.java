package com.fsm.keystone.security;

import com.fsm.keystone.controller.CustomerController;
import com.fsm.keystone.entity.Customer;
import com.fsm.keystone.service.CustomerService;
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
 * Security baseline @WebMvcTest for {@link CustomerController}.
 *
 * <p>Key findings recorded by this baseline:</p>
 * <ul>
 *   <li>{@code GET /api/customers} is {@code permitAll} with no {@code @PreAuthorize} —
 *       the full customer list is accessible to anonymous users.</li>
 *   <li>{@code DELETE /api/customers/{id}} has its {@code @PreAuthorize} COMMENTED OUT —
 *       any actor (including anonymous) can delete customers.</li>
 * </ul>
 */
@WebMvcTest(CustomerController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "app.cors.allowed-origin=http://localhost:5173",
        "app.jwt.secret=test-placeholder-jwt-secret-key-must-be-at-least-64-chars-long-0000",
        "app.jwt.expiration-ms=3600000"
})
class CustomerControllerSecurityBaselineTest extends BaseControllerSecurityTest {

    @MockBean
    private CustomerService customerService;

    @BeforeEach
    void setUp() throws Exception {
        setupJwtFilterPassthrough();
        Customer stub = new Customer();
        stub.setId(1L);
        stub.setName("Test Customer");
        when(customerService.createCustomer(any())).thenReturn(stub);
        when(customerService.getAllCustomers()).thenReturn(Collections.emptyList());
        when(customerService.getCustomerById(anyLong())).thenReturn(stub);
        when(customerService.updateCustomer(anyLong(), any())).thenReturn(stub);
        // deleteCustomer is void — no stub needed
    }

    @ParameterizedTest(name = "[{index}] {0} {1} as {2} → HTTP {3}")
    @MethodSource("customerEndpointMatrix")
    void securityBaseline(String method, String path, String role, int expectedStatus)
            throws Exception {
        mockMvc.perform(buildRequest(method, path, role))
                .andExpect(status().is(expectedStatus));
    }

    static Stream<Arguments> customerEndpointMatrix() {
        return MatrixLoader.forController("CustomerController")
                .flatMap(row -> Stream.of(
                        Arguments.of(row.method(), row.path(), "ANONYMOUS",  row.expectedStatusAnon()),
                        Arguments.of(row.method(), row.path(), "MANAGER",    row.expectedStatusManager()),
                        Arguments.of(row.method(), row.path(), "DISPATCHER", row.expectedStatusDispatcher()),
                        Arguments.of(row.method(), row.path(), "TECHNICIAN", row.expectedStatusTechnician()),
                        Arguments.of(row.method(), row.path(), "CUSTOMER",   row.expectedStatusCustomer())
                ));
    }
}
