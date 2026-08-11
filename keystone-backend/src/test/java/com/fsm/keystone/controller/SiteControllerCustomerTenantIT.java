package com.fsm.keystone.controller;

import com.fsm.keystone.entity.AppUser;
import com.fsm.keystone.entity.Customer;
import com.fsm.keystone.entity.Site;
import com.fsm.keystone.enums.Role;
import com.fsm.keystone.fixtures.TestJwtFactory;
import com.fsm.keystone.repository.CustomerRepository;
import com.fsm.keystone.repository.SiteRepository;
import com.fsm.keystone.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * System integration test for the CUSTOMER tenant-ownership enforcement on
 * {@code GET /api/sites/customer/{customerId}}.
 *
 * <p>Boots the full Spring context against a Testcontainers PostgreSQL 16 database and
 * verifies that a CUSTOMER principal can access their own customer's sites (200) but
 * receives 403 when requesting another customer's sites. Satisfies AC 6 of WO-051.</p>
 *
 * <p>Uses {@code spring.jpa.hibernate.ddl-auto=update} and Flyway disabled so Hibernate
 * builds the schema from annotations — identical to the other integration tests in this
 * module that run outside the V1 Flyway path.</p>
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
class SiteControllerCustomerTenantIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired MockMvc mockMvc;

    @Autowired CustomerRepository customerRepo;
    @Autowired SiteRepository     siteRepo;
    @Autowired UserRepository     userRepo;

    private Customer customer1;
    private Customer customer2;
    private AppUser  customerUser;

    @BeforeEach
    void setUp() {
        // Ensure clean state for each test in the suite (state is shared via @SpringBootTest).
        siteRepo.deleteAll();
        userRepo.deleteAll();
        customerRepo.deleteAll();

        customer1 = customerRepo.save(Customer.builder()
                .name("Tenant IT Customer One")
                .email("tenant.one@example.test")
                .build());

        customer2 = customerRepo.save(Customer.builder()
                .name("Tenant IT Customer Two")
                .email("tenant.two@example.test")
                .build());

        siteRepo.save(Site.builder()
                .siteName("Customer One HQ")
                .address("1 One Street")
                .city("Springfield")
                .state("IL")
                .customer(customer1)
                .build());

        siteRepo.save(Site.builder()
                .siteName("Customer Two Plant")
                .address("2 Two Avenue")
                .city("Shelbyville")
                .state("IL")
                .customer(customer2)
                .build());

        // CUSTOMER user bound to customer1.
        customerUser = userRepo.save(AppUser.builder()
                .fullName("IT Customer User")
                .email("it.tenant.customer@example.test")
                .password("placeholder-not-used-for-jwt-auth")
                .role(Role.CUSTOMER)
                .customer(customer1)
                .build());
    }

    @Test
    void customerUser_ownCustomerId_returns200() throws Exception {
        String token = TestJwtFactory.issueFor(customerUser);

        mockMvc.perform(get("/api/sites/customer/" + customer1.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void customerUser_otherCustomerId_returns403() throws Exception {
        String token = TestJwtFactory.issueFor(customerUser);

        mockMvc.perform(get("/api/sites/customer/" + customer2.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void managerUser_otherCustomerId_returns200() throws Exception {
        AppUser manager = userRepo.save(AppUser.builder()
                .fullName("IT Manager")
                .email("it.tenant.manager@example.test")
                .password("placeholder")
                .role(Role.MANAGER)
                .build());
        String token = TestJwtFactory.issueFor(manager);

        // MANAGER is permitted unconditionally on byCustomer.
        mockMvc.perform(get("/api/sites/customer/" + customer2.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void anonymous_returns401() throws Exception {
        mockMvc.perform(get("/api/sites/customer/" + customer1.getId()))
                .andExpect(status().isUnauthorized());
    }
}
