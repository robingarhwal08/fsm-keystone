package com.fsm.keystone.schema;

import com.fsm.keystone.entity.AppUser;
import com.fsm.keystone.entity.Customer;
import com.fsm.keystone.entity.Part;
import com.fsm.keystone.entity.Site;
import com.fsm.keystone.entity.WorkOrder;
import com.fsm.keystone.enums.Priority;
import com.fsm.keystone.enums.Role;
import com.fsm.keystone.enums.WorkOrderStatus;
import com.fsm.keystone.repository.CustomerRepository;
import com.fsm.keystone.repository.PartRepository;
import com.fsm.keystone.repository.SiteRepository;
import com.fsm.keystone.repository.TimeLogRepository;
import com.fsm.keystone.repository.UserRepository;
import com.fsm.keystone.repository.WorkOrderRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository slice tests verifying that the Flyway-built V1 schema is fully compatible
 * with every JPA entity and repository in the application.
 *
 * <p>Uses {@code @DataJpaTest} with a real PostgreSQL 16 container and
 * {@code ddl-auto=none} so Flyway (not Hibernate) creates the schema. Each test
 * class loads {@code seed-data.sql} via {@code @Sql}; the enclosing {@code @Transactional}
 * rolls back all inserts after each method.</p>
 */
@Tag("integration")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.flyway.enabled=true",
        "spring.flyway.baseline-on-migrate=false"
})
@Sql("/fixtures/seed-data.sql")
class V1SchemaRepositorySlicesIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    // ── AppUser ───────────────────────────────────────────────────────────────

    @Nested
    class AppUserRepositorySlice {

        @Autowired UserRepository     userRepo;
        @Autowired CustomerRepository customerRepo;

        @Test
        void findByEmail_returnsUser() {
            assertThat(userRepo.findByEmail("seed.manager@example.test"))
                    .isPresent()
                    .hasValueSatisfying(u -> {
                        assertThat(u.getFullName()).isEqualTo("Seed Manager");
                        assertThat(u.getRole()).isEqualTo(Role.MANAGER);
                        assertThat(u.getActive()).isTrue();
                        assertThat(u.getCreatedAt()).isNotNull();
                    });
        }

        @Test
        void findByEmail_customerUser_hasCustomerLink() {
            assertThat(userRepo.findByEmail("seed.customer@example.test"))
                    .isPresent()
                    .hasValueSatisfying(u -> {
                        assertThat(u.getRole()).isEqualTo(Role.CUSTOMER);
                        assertThat(u.getCustomer()).isNotNull();
                        assertThat(u.getCustomer().getName()).isEqualTo("Alpha Industries");
                    });
        }

        @Test
        void existsByEmail_returnsTrue_forSeedUser() {
            assertThat(userRepo.existsByEmail("seed.dispatcher@example.test")).isTrue();
        }

        @Test
        void countByRoleAndActive_returnsOne_forManager() {
            assertThat(userRepo.countByRoleAndActive(Role.MANAGER, true)).isEqualTo(1L);
        }

        @Test
        void findByRole_returnsTechnicianList() {
            assertThat(userRepo.findByRole(Role.TECHNICIAN))
                    .hasSize(1)
                    .first()
                    .satisfies(u -> assertThat(u.getEmail()).isEqualTo("seed.technician@example.test"));
        }

        @Test
        void save_persistsNewUser_withGeneratedId() {
            AppUser newUser = AppUser.builder()
                    .fullName("New User")
                    .email("new.user@example.test")
                    .password("placeholder-hash")
                    .role(Role.DISPATCHER)
                    .build();
            AppUser saved = userRepo.save(newUser);
            assertThat(saved.getId()).isNotNull().isPositive();
            assertThat(saved.getCreatedAt()).isNotNull(); // @PrePersist sets createdAt
            assertThat(saved.getActive()).isTrue();       // @PrePersist defaults active=true
        }
    }

    // ── Customer ──────────────────────────────────────────────────────────────

    @Nested
    class CustomerRepositorySlice {

        @Autowired CustomerRepository customerRepo;

        @Test
        void findAll_returnsTwoSeedCustomers() {
            assertThat(customerRepo.findAll()).hasSize(2);
        }

        @Test
        void findById_returnsAlpha() {
            assertThat(customerRepo.findById(601L))
                    .isPresent()
                    .hasValueSatisfying(c -> {
                        assertThat(c.getName()).isEqualTo("Alpha Industries");
                        assertThat(c.getEmail()).isEqualTo("alpha@example.test");
                        assertThat(c.getCreatedAt()).isNotNull();
                        assertThat(c.getUpdatedAt()).isNotNull();
                    });
        }

        @Test
        void save_persistsNewCustomer_withGeneratedId() {
            Customer c = Customer.builder().name("Gamma Corp").email("gamma@example.test").build();
            Customer saved = customerRepo.save(c);
            assertThat(saved.getId()).isNotNull().isPositive();
            assertThat(saved.getCreatedAt()).isNotNull();
        }
    }

    // ── Site ──────────────────────────────────────────────────────────────────

    @Nested
    class SiteRepositorySlice {

        @Autowired SiteRepository siteRepo;
        @Autowired CustomerRepository customerRepo;

        @Test
        void findByCustomerId_returnsAlphaSite() {
            assertThat(siteRepo.findByCustomerId(601L))
                    .hasSize(1)
                    .first()
                    .satisfies(s -> {
                        assertThat(s.getSiteName()).isEqualTo("Alpha HQ");
                        assertThat(s.getCity()).isEqualTo("Springfield");
                        assertThat(s.getLatitude()).isNull();   // not set in seed
                    });
        }

        @Test
        void save_persistsNewSite_withLatLong() {
            Customer alpha = customerRepo.findById(601L).orElseThrow();
            Site site = Site.builder()
                    .siteName("Alpha Warehouse")
                    .address("99 Alpha Blvd")
                    .city("Springfield")
                    .state("IL")
                    .latitude(39.7817)
                    .longitude(-89.6501)
                    .customer(alpha)
                    .build();
            Site saved = siteRepo.save(site);
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getLatitude()).isEqualTo(39.7817);
        }
    }

    // ── WorkOrder ─────────────────────────────────────────────────────────────

    @Nested
    class WorkOrderRepositorySlice {

        @Autowired WorkOrderRepository workOrderRepo;

        @Test
        void countByStatus_returnsOne_forCreated() {
            assertThat(workOrderRepo.countByStatus(WorkOrderStatus.CREATED)).isEqualTo(1L);
        }

        @Test
        void countByPriority_returnsOne_forCritical() {
            assertThat(workOrderRepo.countByPriority(Priority.CRITICAL)).isEqualTo(1L);
        }

        @Test
        void findTop8ByOrderByCreatedAtDesc_returnsAllThreeSeedOrders() {
            assertThat(workOrderRepo.findTop8ByOrderByCreatedAtDesc()).hasSize(3);
        }

        @Test
        void findById_returnsWorkOrderWithEnums() {
            assertThat(workOrderRepo.findById(802L))
                    .isPresent()
                    .hasValueSatisfying(wo -> {
                        assertThat(wo.getStatus()).isEqualTo(WorkOrderStatus.ASSIGNED);
                        assertThat(wo.getPriority()).isEqualTo(Priority.HIGH);
                        assertThat(wo.getWorkOrderNumber()).isEqualTo("WO-SEED-0002");
                        assertThat(wo.getDescription()).contains("pump");
                        assertThat(wo.getAssignedTechnician()).isNotNull();
                        assertThat(wo.getAssignedTechnician().getEmail())
                                .isEqualTo("seed.technician@example.test");
                    });
        }
    }

    // ── Part ──────────────────────────────────────────────────────────────────

    @Nested
    class PartRepositorySlice {

        @Autowired PartRepository partRepo;

        @Test
        void countByStockQuantityLessThanEqual_10_returnsOne() {
            // Gasket Set (id=752) has stock_quantity=5 which is ≤ 10
            assertThat(partRepo.countByStockQuantityLessThanEqual(10)).isEqualTo(1L);
        }

        @Test
        void findById_returnsPart_withNumericPrice() {
            assertThat(partRepo.findById(751L))
                    .isPresent()
                    .hasValueSatisfying(p -> {
                        assertThat(p.getPartName()).isEqualTo("Bearing Kit");
                        assertThat(p.getPartNumber()).isEqualTo("SEED-BEARING-01");
                        assertThat(p.getUnitPrice()).isNotNull();
                        assertThat(p.getStockQuantity()).isEqualTo(50);
                        assertThat(p.getActive()).isTrue();
                    });
        }

        @Test
        void save_persistsPart_withGeneratedId() {
            Part p = Part.builder()
                    .partName("Test Part")
                    .partNumber("TEST-PART-001")
                    .unitPrice(new java.math.BigDecimal("9.99"))
                    .build();
            Part saved = partRepo.save(p);
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getActive()).isTrue();
            assertThat(saved.getStockQuantity()).isEqualTo(0);
        }
    }

    // ── TimeLog ───────────────────────────────────────────────────────────────

    @Nested
    class TimeLogRepositorySlice {

        @Autowired TimeLogRepository timeLogRepo;

        @Test
        void findByWorkOrderId_returnsOneLog() {
            assertThat(timeLogRepo.findByWorkOrderId(802L))
                    .hasSize(1)
                    .first()
                    .satisfies(tl -> {
                        assertThat(tl.getHoursSpent()).isNotNull();
                        assertThat(tl.getWorkDescription()).contains("pump");
                    });
        }

        @Test
        void findByTechnicianId_returnsOneLog() {
            assertThat(timeLogRepo.findByTechnicianId(653L)).hasSize(1);
        }
    }
}
