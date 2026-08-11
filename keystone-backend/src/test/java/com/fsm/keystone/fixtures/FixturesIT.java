package com.fsm.keystone.fixtures;

import com.fsm.keystone.repository.CustomerRepository;
import com.fsm.keystone.repository.PartRepository;
import com.fsm.keystone.repository.SiteRepository;
import com.fsm.keystone.repository.UserRepository;
import com.fsm.keystone.repository.WorkOrderRepository;
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

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Container-backed integration test proving that {@code seed-two-tenants.sql} and
 * {@link TenantScenario} describe the same world.
 *
 * <p>Applies the seed SQL before each test and asserts repository row counts match
 * the Java constants declared in {@link TenantScenario}. If the SQL and the Java
 * fixture factories ever diverge, this test will catch it.</p>
 *
 * <p>Uses {@code @DataJpaTest} (JPA slice — no web layer, no Flyway) so the schema
 * is created by Hibernate and only JPA repositories are loaded.</p>
 */
@Tag("integration")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=update",
        "spring.flyway.enabled=false"
})
@Sql(scripts = "/fixtures/seed-two-tenants.sql",
     executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class FixturesIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired CustomerRepository customerRepo;
    @Autowired SiteRepository siteRepo;
    @Autowired UserRepository userRepo;
    @Autowired WorkOrderRepository workOrderRepo;
    @Autowired PartRepository partRepo;

    @Test
    void seedSql_customerCount_matchesJavaScenario() {
        assertEquals(TenantScenario.CUSTOMER_COUNT, customerRepo.count(),
                "seed-two-tenants.sql must insert exactly " + TenantScenario.CUSTOMER_COUNT
                        + " customers");
    }

    @Test
    void seedSql_siteCount_matchesJavaScenario() {
        assertEquals(TenantScenario.SITE_COUNT, siteRepo.count(),
                "seed-two-tenants.sql must insert exactly " + TenantScenario.SITE_COUNT + " sites");
    }

    @Test
    void seedSql_userCount_matchesJavaScenario() {
        assertEquals(TenantScenario.USER_COUNT, userRepo.count(),
                "seed-two-tenants.sql must insert exactly " + TenantScenario.USER_COUNT + " users");
    }

    @Test
    void seedSql_workOrderCount_matchesJavaScenario() {
        assertEquals(TenantScenario.TOTAL_WORK_ORDER_COUNT, workOrderRepo.count(),
                "seed-two-tenants.sql must insert exactly "
                        + TenantScenario.TOTAL_WORK_ORDER_COUNT + " work orders");
    }

    @Test
    void seedSql_partCount_matchesJavaScenario() {
        assertEquals(TenantScenario.PART_COUNT, partRepo.count(),
                "seed-two-tenants.sql must insert exactly " + TenantScenario.PART_COUNT + " parts");
    }

    @Test
    void seedSql_acmeSiteCount_isTwo() {
        assertEquals(TenantScenario.ACME_WORK_ORDER_COUNT,
                workOrderRepo.findAll().stream()
                        .filter(wo -> wo.getCustomer() != null
                                && wo.getCustomer().getId() == 101L)
                        .count(),
                "ACME must have exactly " + TenantScenario.ACME_WORK_ORDER_COUNT + " work orders");
    }

    @Test
    void seedSql_globexWorkOrderCount_isTwo() {
        assertEquals(TenantScenario.GLOBEX_WORK_ORDER_COUNT,
                workOrderRepo.findAll().stream()
                        .filter(wo -> wo.getCustomer() != null
                                && wo.getCustomer().getId() == 102L)
                        .count(),
                "GLOBEX must have exactly " + TenantScenario.GLOBEX_WORK_ORDER_COUNT
                        + " work orders");
    }

    @Test
    void seedSql_sharedTechnician_appearsInWorkOrdersAcrossBothTenants() {
        long acmeTechWos = workOrderRepo.findAll().stream()
                .filter(wo -> wo.getAssignedTechnician() != null
                        && wo.getAssignedTechnician().getId() == 403L
                        && wo.getCustomer().getId() == 101L)
                .count();
        long globexTechWos = workOrderRepo.findAll().stream()
                .filter(wo -> wo.getAssignedTechnician() != null
                        && wo.getAssignedTechnician().getId() == 403L
                        && wo.getCustomer().getId() == 102L)
                .count();
        assertEquals(2, acmeTechWos,
                "shared technician must be assigned to 2 ACME work orders");
        assertEquals(1, globexTechWos,
                "shared technician must be assigned to 1 GLOBEX work order");
    }

    @Test
    void seedSql_acmeSitesByRepo_isTwo() {
        assertEquals(2, siteRepo.findByCustomerId(101L).size(),
                "ACME must have 2 sites in the repository");
    }

    @Test
    void seedSql_globexSitesByRepo_isOne() {
        assertEquals(1, siteRepo.findByCustomerId(102L).size(),
                "GLOBEX must have 1 site in the repository");
    }

    @Test
    void seedSql_lowStockPartsCount_isAtLeastTwo() {
        // Parts 502 (qty=1) and 503 (qty=0) are both at or below threshold of 5
        long lowStock = partRepo.countByStockQuantityLessThanEqual(5);
        assertEquals(2, lowStock,
                "seed must contain exactly 2 parts with stockQuantity <= 5 (low-stock + zero-stock)");
    }
}
