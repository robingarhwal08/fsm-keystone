package com.fsm.keystone.fixtures;

import com.fsm.keystone.entity.Customer;
import com.fsm.keystone.entity.Site;

/**
 * Object-mother factory for {@link Site} entities.
 *
 * <p>Minimum valid shape: {@code siteName} and {@code customer} are required
 * ({@code customer} is {@code optional = false} on the FK).</p>
 */
public final class SiteFixtures {

    private SiteFixtures() {}

    /** Returns a builder for a generic site linked to the given customer. */
    public static Site.SiteBuilder aSite(Customer customer) {
        return Site.builder()
                .siteName("Test Site")
                .address("1 Site Road")
                .city("Springfield")
                .state("IL")
                .pincode("62700")
                .contactPerson("Site Contact")
                .contactPhone("555-0200")
                .customer(customer);
    }

    /** ACME HQ — first site for ACME Corp (id 201). */
    public static Site.SiteBuilder acmeHq(Customer acme) {
        return aSite(acme)
                .id(201L)
                .siteName("ACME HQ")
                .address("10 Main Street")
                .city("Springfield")
                .state("IL")
                .pincode("62701")
                .contactPerson("Alice Manager")
                .contactPhone("555-2001");
    }

    /** ACME Warehouse — second site for ACME Corp (id 202). */
    public static Site.SiteBuilder acmeWarehouse(Customer acme) {
        return aSite(acme)
                .id(202L)
                .siteName("ACME Warehouse")
                .address("20 Park Avenue")
                .city("Shelbyville")
                .state("IL")
                .pincode("62702")
                .contactPerson("Bob Supervisor")
                .contactPhone("555-2002");
    }

    /** GLOBEX Plant — single site for GLOBEX Ltd (id 203). */
    public static Site.SiteBuilder globexPlant(Customer globex) {
        return aSite(globex)
                .id(203L)
                .siteName("GLOBEX Plant")
                .address("30 Oak Road")
                .city("Capital City")
                .state("IL")
                .pincode("62703")
                .contactPerson("Carol Plant Manager")
                .contactPhone("555-2003");
    }
}
