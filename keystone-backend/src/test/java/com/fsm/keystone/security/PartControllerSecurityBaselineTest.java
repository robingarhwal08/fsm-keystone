package com.fsm.keystone.security;

import com.fsm.keystone.controller.PartController;
import com.fsm.keystone.entity.Part;
import com.fsm.keystone.service.PartService;
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
 * Security baseline @WebMvcTest for {@link PartController}.
 *
 * <p>Parts are under {@code anyRequest().authenticated()} so anonymous actors
 * receive 403. Only MANAGER can create, update, or delete parts.</p>
 */
@WebMvcTest(PartController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "app.cors.allowed-origin=http://localhost:5173",
        "app.jwt.secret=test-placeholder-jwt-secret-key-must-be-at-least-64-chars-long-0000",
        "app.jwt.expiration-ms=3600000"
})
class PartControllerSecurityBaselineTest extends BaseControllerSecurityTest {

    @MockBean
    private PartService partService;

    @BeforeEach
    void setUp() throws Exception {
        setupJwtFilterPassthrough();
        Part stub = new Part();
        stub.setId(1L);
        stub.setPartName("Test Part");
        when(partService.createPart(any())).thenReturn(stub);
        when(partService.getAllParts()).thenReturn(Collections.emptyList());
        when(partService.getPartById(anyLong())).thenReturn(stub);
        when(partService.updatePart(anyLong(), any())).thenReturn(stub);
        // deletePart is void — no stub needed
    }

    @ParameterizedTest(name = "[{index}] {0} {1} as {2} → HTTP {3}")
    @MethodSource("partEndpointMatrix")
    void securityBaseline(String method, String path, String role, int expectedStatus)
            throws Exception {
        mockMvc.perform(buildRequest(method, path, role))
                .andExpect(status().is(expectedStatus));
    }

    static Stream<Arguments> partEndpointMatrix() {
        return MatrixLoader.forController("PartController")
                .flatMap(row -> Stream.of(
                        Arguments.of(row.method(), row.path(), "ANONYMOUS",  row.expectedStatusAnon()),
                        Arguments.of(row.method(), row.path(), "MANAGER",    row.expectedStatusManager()),
                        Arguments.of(row.method(), row.path(), "DISPATCHER", row.expectedStatusDispatcher()),
                        Arguments.of(row.method(), row.path(), "TECHNICIAN", row.expectedStatusTechnician()),
                        Arguments.of(row.method(), row.path(), "CUSTOMER",   row.expectedStatusCustomer())
                ));
    }
}
