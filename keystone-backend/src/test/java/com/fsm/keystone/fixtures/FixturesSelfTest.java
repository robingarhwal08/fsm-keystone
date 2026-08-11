package com.fsm.keystone.fixtures;

import com.fsm.keystone.entity.AppUser;
import com.fsm.keystone.entity.Customer;
import com.fsm.keystone.entity.Part;
import com.fsm.keystone.entity.PartUsage;
import com.fsm.keystone.entity.Site;
import com.fsm.keystone.entity.StatusHistory;
import com.fsm.keystone.entity.TimeLog;
import com.fsm.keystone.entity.WorkOrder;
import com.fsm.keystone.enums.Role;
import com.fsm.keystone.enums.WorkOrderStatus;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure unit tests validating every fixture builder and the two-tenant scenario invariants.
 *
 * <p>No Spring context required — all assertions operate on in-memory Java objects.</p>
 */
@Tag("unit")
class FixturesSelfTest {

    // ── FixtureClock ──────────────────────────────────────────────────────────

    @Test
    void fixtureClock_constantsAreOrdered() {
        assertTrue(FixtureClock.NOW.isBefore(FixtureClock.PLUS_ONE_HOUR));
        assertTrue(FixtureClock.PLUS_ONE_HOUR.isBefore(FixtureClock.PLUS_EIGHT_HOURS));
    }

    // ── CustomerFixtures ─────────────────────────────────────────────────────

    @Test
    void customerFixtures_aCustomer_hasRequiredFields() {
        Customer c = CustomerFixtures.aCustomer().build();
        assertNotNull(c.getName(), "name must not be null");
    }

    @Test
    void customerFixtures_acme_hasExpectedId() {
        Customer acme = CustomerFixtures.acme().build();
        assertEquals(101L, acme.getId());
        assertTrue(acme.getEmail().endsWith("@example.test"), "email must use example.test domain");
    }

    @Test
    void customerFixtures_globex_hasExpectedId() {
        Customer globex = CustomerFixtures.globex().build();
        assertEquals(102L, globex.getId());
    }

    // ── SiteFixtures ─────────────────────────────────────────────────────────

    @Test
    void siteFixtures_aSite_hasRequiredFields() {
        Customer customer = CustomerFixtures.aCustomer().build();
        Site site = SiteFixtures.aSite(customer).build();
        assertNotNull(site.getSiteName(), "siteName must not be null");
        assertNotNull(site.getCustomer(), "customer FK must not be null");
    }

    @Test
    void siteFixtures_acmeHqAndWarehouse_belongToAcme() {
        Customer acme = CustomerFixtures.acme().build();
        Site hq = SiteFixtures.acmeHq(acme).build();
        Site warehouse = SiteFixtures.acmeWarehouse(acme).build();
        assertEquals(acme, hq.getCustomer());
        assertEquals(acme, warehouse.getCustomer());
        assertNotEquals(hq.getId(), warehouse.getId(), "sites must have distinct IDs");
    }

    @Test
    void siteFixtures_globexPlant_belongsToGlobex() {
        Customer globex = CustomerFixtures.globex().build();
        Site plant = SiteFixtures.globexPlant(globex).build();
        assertEquals(globex, plant.getCustomer());
        assertEquals(203L, plant.getId());
    }

    // ── UserFixtures ─────────────────────────────────────────────────────────

    @Test
    void userFixtures_aManager_hasManagerRole() {
        AppUser manager = UserFixtures.aManager().build();
        assertEquals(Role.MANAGER, manager.getRole());
        assertTrue(Boolean.TRUE.equals(manager.getActive()));
        assertTrue(manager.getEmail().endsWith("@example.test"), "email must use example.test domain");
        assertNull(manager.getCustomer(), "manager must not be tenant-linked");
    }

    @Test
    void userFixtures_aTechnician_hasNoCustomerLink() {
        AppUser tech = UserFixtures.aTechnician().build();
        assertEquals(Role.TECHNICIAN, tech.getRole());
        assertNull(tech.getCustomer());
    }

    @Test
    void userFixtures_aCustomerUser_isLinkedToGivenCustomer() {
        Customer customer = CustomerFixtures.acme().build();
        AppUser user = UserFixtures.aCustomerUser(customer).build();
        assertEquals(Role.CUSTOMER, user.getRole());
        assertEquals(customer, user.getCustomer());
    }

    @Test
    void userFixtures_customerUserWithNoCustomer_isConstructible() {
        // Production signup allows null customer; this behaviour must remain expressible
        AppUser user = UserFixtures.aCustomerUserWithNoCustomer().build();
        assertEquals(Role.CUSTOMER, user.getRole());
        assertNull(user.getCustomer(), "null-customer CUSTOMER user must be constructible");
    }

    // ── UserDetails contract ──────────────────────────────────────────────────

    @Test
    void userFixtures_allPersonas_satisfyUserDetailsContract() {
        for (AppUser user : new AppUser[]{
                UserFixtures.aManager().build(),
                UserFixtures.aDispatcher().build(),
                UserFixtures.aTechnician().build(),
                UserFixtures.aCustomerUser(CustomerFixtures.acme().build()).build()
        }) {
            assertTrue(user.isEnabled(), user.getEmail() + " must be enabled");
            assertTrue(user.isAccountNonExpired());
            assertTrue(user.isAccountNonLocked());
            assertTrue(user.isCredentialsNonExpired());
            assertEquals(user.getEmail(), user.getUsername());
            assertFalse(user.getAuthorities().isEmpty(),
                    user.getEmail() + " must have at least one authority");
        }
    }

    @Test
    void userFixtures_managerAuthority_containsRolePrefix() {
        AppUser manager = UserFixtures.aManager().build();
        String authorities = manager.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining());
        assertTrue(authorities.contains("ROLE_MANAGER"),
                "MANAGER must have ROLE_MANAGER authority, got: " + authorities);
    }

    // ── PartFixtures ─────────────────────────────────────────────────────────

    @Test
    void partFixtures_aPart_hasRequiredFields() {
        Part part = PartFixtures.aPart().build();
        assertNotNull(part.getPartName(), "partName must not be null");
        assertNotNull(part.getPartNumber(), "partNumber must not be null");
    }

    @Test
    void partFixtures_lowStockPart_hasLowQuantity() {
        Part lowStock = PartFixtures.lowStockPart().build();
        assertTrue(lowStock.getStockQuantity() <= 5,
                "lowStockPart must have stock <= 5, got: " + lowStock.getStockQuantity());
    }

    @Test
    void partFixtures_zeroStockPart_hasZeroQuantity() {
        Part zero = PartFixtures.zeroStockPart().build();
        assertEquals(0, zero.getStockQuantity());
    }

    // ── WorkOrderFixtures ────────────────────────────────────────────────────

    @Test
    void workOrderFixtures_aCreatedWorkOrder_hasNoTechnician() {
        Customer customer = CustomerFixtures.acme().build();
        Site site = SiteFixtures.acmeHq(customer).build();
        AppUser manager = UserFixtures.aManager().build();
        WorkOrder wo = WorkOrderFixtures.aCreatedWorkOrder(customer, site, manager).build();

        assertEquals(WorkOrderStatus.CREATED, wo.getStatus());
        assertNull(wo.getAssignedTechnician(), "CREATED work order must have no technician");
        assertNotNull(wo.getCustomer());
        assertNotNull(wo.getSite());
    }

    @Test
    void workOrderFixtures_anAssignedWorkOrder_hasStatusAndTechnician() {
        Customer customer = CustomerFixtures.acme().build();
        Site site = SiteFixtures.acmeHq(customer).build();
        AppUser manager = UserFixtures.aManager().build();
        AppUser tech = UserFixtures.aTechnician().build();
        WorkOrder wo = WorkOrderFixtures.anAssignedWorkOrder(customer, site, manager, tech).build();

        assertEquals(WorkOrderStatus.ASSIGNED, wo.getStatus());
        assertEquals(tech, wo.getAssignedTechnician());
    }

    // ── StatusHistoryFixtures ─────────────────────────────────────────────────

    @Test
    void statusHistoryFixtures_aStatusTransition_hasRequiredWorkOrder() {
        Customer customer = CustomerFixtures.acme().build();
        Site site = SiteFixtures.acmeHq(customer).build();
        AppUser manager = UserFixtures.aManager().build();
        WorkOrder wo = WorkOrderFixtures.aCreatedWorkOrder(customer, site, manager).build();

        StatusHistory history = StatusHistoryFixtures.aStatusTransition(wo, manager).build();
        assertNotNull(history.getWorkOrder(), "workOrder FK must not be null");
        assertNotNull(history.getNewStatus());
    }

    // ── TimeLogFixtures ───────────────────────────────────────────────────────

    @Test
    void timeLogFixtures_aTimeLog_hasValidDuration() {
        Customer customer = CustomerFixtures.acme().build();
        Site site = SiteFixtures.acmeHq(customer).build();
        AppUser manager = UserFixtures.aManager().build();
        AppUser tech = UserFixtures.aTechnician().build();
        WorkOrder wo = WorkOrderFixtures.aCreatedWorkOrder(customer, site, manager).build();
        TimeLog log = TimeLogFixtures.aTimeLog(wo, tech).build();

        assertTrue(log.getEndTime().isAfter(log.getStartTime()),
                "endTime must be after startTime");
        assertNotNull(log.getHoursSpent());
        assertNotNull(log.getWorkOrder());
        assertNotNull(log.getTechnician());
    }

    // ── PartUsageFixtures ─────────────────────────────────────────────────────

    @Test
    void partUsageFixtures_aPartUsage_hasConsistentTotalCost() {
        Customer customer = CustomerFixtures.acme().build();
        Site site = SiteFixtures.acmeHq(customer).build();
        AppUser manager = UserFixtures.aManager().build();
        AppUser tech = UserFixtures.aTechnician().build();
        WorkOrder wo = WorkOrderFixtures.aCreatedWorkOrder(customer, site, manager).build();
        Part part = PartFixtures.normalStockPart().build();

        PartUsage usage = PartUsageFixtures.aPartUsage(wo, part, tech).build();
        assertNotNull(usage.getQuantityUsed());
        assertNotNull(usage.getUnitPriceAtUsage());
        assertNotNull(usage.getTotalCost());
        assertEquals(
                usage.getUnitPriceAtUsage().multiply(
                        java.math.BigDecimal.valueOf(usage.getQuantityUsed())),
                usage.getTotalCost(),
                "totalCost must equal unitPriceAtUsage * quantityUsed");
    }

    // ── Personas ──────────────────────────────────────────────────────────────

    @Test
    void personas_allConstantsAreDefined() {
        assertNotNull(Personas.MANAGER);
        assertNotNull(Personas.DISPATCHER);
        assertNotNull(Personas.TECHNICIAN);
        assertNotNull(Personas.ACME_CUSTOMER_USER);
        assertNotNull(Personas.GLOBEX_CUSTOMER_USER);
        assertNull(Personas.UNAUTHENTICATED, "UNAUTHENTICATED marker must be null");
    }

    @Test
    void personas_authenticationFor_wrapsUserWithAuthorities() {
        Authentication auth = Personas.authenticationFor(Personas.MANAGER);
        assertTrue(auth.isAuthenticated());
        assertEquals(Personas.MANAGER.getEmail(), ((AppUser) auth.getPrincipal()).getEmail());
        assertFalse(auth.getAuthorities().isEmpty());
    }

    @Test
    void personas_acmeAndGlobexCustomerUsers_linkToDistinctTenants() {
        Long acmeCid = Personas.ACME_CUSTOMER_USER.getCustomer().getId();
        Long globexCid = Personas.GLOBEX_CUSTOMER_USER.getCustomer().getId();
        assertNotEquals(acmeCid, globexCid,
                "ACME and GLOBEX customer users must be linked to different customer aggregates");
    }

    // ── TestJwtFactory ────────────────────────────────────────────────────────

    @Test
    void testJwtFactory_issueFor_producesNonNullToken() {
        String token = TestJwtFactory.issueFor(Personas.MANAGER);
        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void testJwtFactory_bearerHeader_startsWithBearerPrefix() {
        String header = TestJwtFactory.bearerHeaderFor(Personas.TECHNICIAN);
        assertTrue(header.startsWith("Bearer "), "Bearer header must start with 'Bearer '");
    }

    @Test
    void testJwtFactory_tokenSubjectIsEmail() {
        // Parse subject without signature verification to check claim structure
        String token = TestJwtFactory.issueFor(Personas.MANAGER);
        // JWT structure: header.payload.signature — payload is Base64URL-encoded JSON
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length, "JWT must have 3 dot-separated parts");
        // JWT payloads use unpadded Base64URL; add = padding before decoding
        String encoded = parts[1];
        int pad = (4 - encoded.length() % 4) % 4;
        encoded = encoded + "=".repeat(pad);
        byte[] payloadBytes = java.util.Base64.getUrlDecoder().decode(encoded);
        String payload = new String(payloadBytes, java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(payload.contains(Personas.MANAGER.getEmail()),
                "JWT payload must contain the manager's email as subject");
    }

    @Test
    void testJwtFactory_testSecretLength_isAtLeast64Chars() {
        assertTrue(TestJwtFactory.TEST_SECRET.length() >= 64,
                "TEST_SECRET must be at least 64 chars for HS256 key derivation, "
                        + "was: " + TestJwtFactory.TEST_SECRET.length());
    }

    // ── TenantScenario ────────────────────────────────────────────────────────

    @Test
    void tenantScenario_build_producesAllEntities() {
        TenantScenario s = TenantScenario.build();
        assertNotNull(s.acme());
        assertNotNull(s.globex());
        assertNotNull(s.acmeHq());
        assertNotNull(s.acmeWarehouse());
        assertNotNull(s.globexPlant());
        assertNotNull(s.manager());
        assertNotNull(s.dispatcher());
        assertNotNull(s.technician());
        assertNotNull(s.acmeCustomerUser());
        assertNotNull(s.globexCustomerUser());
        assertNotNull(s.acmeWo1());
        assertNotNull(s.acmeWo2());
        assertNotNull(s.acmeWo3());
        assertNotNull(s.globexWo1());
        assertNotNull(s.globexWo2());
        assertNotNull(s.normalStockPart());
        assertNotNull(s.lowStockPart());
        assertNotNull(s.zeroStockPart());
    }

    @Test
    void tenantScenario_workOrderCounts_matchConstants() {
        assertEquals(5, TenantScenario.TOTAL_WORK_ORDER_COUNT);
        assertEquals(3, TenantScenario.ACME_WORK_ORDER_COUNT);
        assertEquals(2, TenantScenario.GLOBEX_WORK_ORDER_COUNT);
        assertEquals(TenantScenario.ACME_WORK_ORDER_COUNT + TenantScenario.GLOBEX_WORK_ORDER_COUNT,
                TenantScenario.TOTAL_WORK_ORDER_COUNT);
    }

    @Test
    void tenantScenario_acmeSites_belongToAcme() {
        TenantScenario s = TenantScenario.build();
        assertEquals(s.acme().getId(), s.acmeHq().getCustomer().getId());
        assertEquals(s.acme().getId(), s.acmeWarehouse().getCustomer().getId());
    }

    @Test
    void tenantScenario_globexSite_belongsToGlobex() {
        TenantScenario s = TenantScenario.build();
        assertEquals(s.globex().getId(), s.globexPlant().getCustomer().getId());
    }

    @Test
    void tenantScenario_sharedTechnician_assignedAcrossTenantsWithDistinctCustomerIds() {
        TenantScenario s = TenantScenario.build();
        AppUser technician = s.technician();

        // technician is assigned to ACME WO-2, ACME WO-3, GLOBEX WO-2
        assertEquals(technician, s.acmeWo2().getAssignedTechnician());
        assertEquals(technician, s.acmeWo3().getAssignedTechnician());
        assertEquals(technician, s.globexWo2().getAssignedTechnician());

        // the two work orders from different tenants have distinct customer IDs
        assertNotEquals(s.acmeWo2().getCustomer().getId(), s.globexWo2().getCustomer().getId(),
                "ACME and GLOBEX work orders must belong to different customer aggregates");
    }

    @Test
    void tenantScenario_createdWorkOrders_haveNoAssignedTechnician() {
        TenantScenario s = TenantScenario.build();
        assertNull(s.acmeWo1().getAssignedTechnician(),
                "ACME WO-1 is CREATED and must have no assigned technician");
        assertNull(s.globexWo1().getAssignedTechnician(),
                "GLOBEX WO-1 is CREATED and must have no assigned technician");
    }

    @Test
    void tenantScenario_customerUsers_linkedToCorrectTenants() {
        TenantScenario s = TenantScenario.build();
        assertEquals(s.acme().getId(), s.acmeCustomerUser().getCustomer().getId());
        assertEquals(s.globex().getId(), s.globexCustomerUser().getCustomer().getId());
    }

    @Test
    void tenantScenario_partStockLevels_coverAllScenarios() {
        TenantScenario s = TenantScenario.build();
        assertTrue(s.normalStockPart().getStockQuantity() > 5, "normalStockPart must have adequate stock");
        assertTrue(s.lowStockPart().getStockQuantity() > 0
                        && s.lowStockPart().getStockQuantity() <= 5,
                "lowStockPart must have stock between 1 and 5");
        assertEquals(0, s.zeroStockPart().getStockQuantity(),
                "zeroStockPart must have zero stock");
    }

    @Test
    void tenantScenario_acmeCustomerUser_emailUsesExampleTestDomain() {
        TenantScenario s = TenantScenario.build();
        assertTrue(s.acmeCustomerUser().getEmail().endsWith("@example.test"),
                "PII policy: email must use example.test domain");
        assertTrue(s.globexCustomerUser().getEmail().endsWith("@example.test"),
                "PII policy: email must use example.test domain");
    }
}
