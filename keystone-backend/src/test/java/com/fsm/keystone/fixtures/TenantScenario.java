package com.fsm.keystone.fixtures;

import com.fsm.keystone.entity.AppUser;
import com.fsm.keystone.entity.Customer;
import com.fsm.keystone.entity.Part;
import com.fsm.keystone.entity.Site;
import com.fsm.keystone.entity.WorkOrder;

/**
 * Two-tenant scenario for cross-tenant assertion tests.
 *
 * <p>The scenario models:
 * <ul>
 *   <li><b>ACME Corp</b> (id 101) — 2 sites, 3 work orders, 1 customer user</li>
 *   <li><b>GLOBEX Ltd</b> (id 102) — 1 site, 2 work orders, 1 customer user</li>
 *   <li>One shared {@link #technician} assigned to work orders across both tenants</li>
 * </ul>
 *
 * <p>This deliberate cross-tenant overlap means later tenancy tests can assert that:
 * <ul>
 *   <li>An ACME CUSTOMER user sees exactly 3 work orders (not 5)</li>
 *   <li>A GLOBEX CUSTOMER user sees exactly 2 work orders (not 5)</li>
 *   <li>The shared technician sees 4 assigned work orders across both tenants</li>
 * </ul>
 *
 * <p>Entity ids match the SQL in {@code src/test/resources/fixtures/seed-two-tenants.sql}
 * so the same assertions hold in both unit tests (Java objects) and integration tests
 * (repository queries after seed is applied).</p>
 *
 * @param acme              ACME Corp customer aggregate
 * @param globex            GLOBEX Ltd customer aggregate
 * @param acmeHq            ACME HQ site (belongs to ACME)
 * @param acmeWarehouse     ACME Warehouse site (belongs to ACME)
 * @param globexPlant       GLOBEX Plant site (belongs to GLOBEX)
 * @param manager           MANAGER user — creates all work orders, no tenant link
 * @param dispatcher        DISPATCHER user — no tenant link
 * @param technician        TECHNICIAN user — shared across ACME and GLOBEX work orders
 * @param acmeCustomerUser  CUSTOMER user linked to ACME
 * @param globexCustomerUser CUSTOMER user linked to GLOBEX
 * @param acmeWo1           ACME WO-1: CREATED, no technician
 * @param acmeWo2           ACME WO-2: ASSIGNED to shared technician
 * @param acmeWo3           ACME WO-3: IN_PROGRESS with shared technician
 * @param globexWo1         GLOBEX WO-1: CREATED, no technician
 * @param globexWo2         GLOBEX WO-2: IN_PROGRESS with shared technician
 * @param normalStockPart   Part with adequate stock (qty=100)
 * @param lowStockPart      Part with low stock (qty=1) — triggers low-stock dashboard counts
 * @param zeroStockPart     Part with zero stock (qty=0)
 */
public record TenantScenario(
        Customer acme,
        Customer globex,
        Site acmeHq,
        Site acmeWarehouse,
        Site globexPlant,
        AppUser manager,
        AppUser dispatcher,
        AppUser technician,
        AppUser acmeCustomerUser,
        AppUser globexCustomerUser,
        WorkOrder acmeWo1,
        WorkOrder acmeWo2,
        WorkOrder acmeWo3,
        WorkOrder globexWo1,
        WorkOrder globexWo2,
        Part normalStockPart,
        Part lowStockPart,
        Part zeroStockPart
) {

    /** Number of customers in this scenario. */
    public static final int CUSTOMER_COUNT = 2;

    /** Number of sites in this scenario. */
    public static final int SITE_COUNT = 3;

    /** Total number of work orders across all tenants. */
    public static final int TOTAL_WORK_ORDER_COUNT = 5;

    /** Number of work orders belonging to ACME. */
    public static final int ACME_WORK_ORDER_COUNT = 3;

    /** Number of work orders belonging to GLOBEX. */
    public static final int GLOBEX_WORK_ORDER_COUNT = 2;

    /** Number of users in this scenario. */
    public static final int USER_COUNT = 5;

    /** Number of parts in this scenario. */
    public static final int PART_COUNT = 3;

    /**
     * Builds a fully-populated {@link TenantScenario} from the static fixture factories.
     * All returned objects have explicit ids matching {@code seed-two-tenants.sql}.
     */
    public static TenantScenario build() {
        Customer acme = CustomerFixtures.acme().build();
        Customer globex = CustomerFixtures.globex().build();

        Site acmeHq = SiteFixtures.acmeHq(acme).build();
        Site acmeWarehouse = SiteFixtures.acmeWarehouse(acme).build();
        Site globexPlant = SiteFixtures.globexPlant(globex).build();

        AppUser manager = UserFixtures.scenarioManager().build();
        AppUser dispatcher = UserFixtures.scenarioDispatcher().build();
        AppUser technician = UserFixtures.scenarioTechnician().build();
        AppUser acmeCustomerUser = UserFixtures.scenarioAcmeCustomerUser(acme).build();
        AppUser globexCustomerUser = UserFixtures.scenarioGlobexCustomerUser(globex).build();

        WorkOrder acmeWo1 = WorkOrderFixtures.acmeWo1(acme, acmeHq, manager).build();
        WorkOrder acmeWo2 = WorkOrderFixtures.acmeWo2(acme, acmeWarehouse, manager, technician).build();
        WorkOrder acmeWo3 = WorkOrderFixtures.acmeWo3(acme, acmeHq, manager, technician).build();
        WorkOrder globexWo1 = WorkOrderFixtures.globexWo1(globex, globexPlant, manager).build();
        WorkOrder globexWo2 = WorkOrderFixtures.globexWo2(globex, globexPlant, manager, technician).build();

        Part normalStockPart = PartFixtures.normalStockPart().build();
        Part lowStockPart = PartFixtures.lowStockPart().build();
        Part zeroStockPart = PartFixtures.zeroStockPart().build();

        return new TenantScenario(
                acme, globex,
                acmeHq, acmeWarehouse, globexPlant,
                manager, dispatcher, technician,
                acmeCustomerUser, globexCustomerUser,
                acmeWo1, acmeWo2, acmeWo3,
                globexWo1, globexWo2,
                normalStockPart, lowStockPart, zeroStockPart);
    }
}
