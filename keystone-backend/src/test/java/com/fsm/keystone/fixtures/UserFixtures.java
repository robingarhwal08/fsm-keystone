package com.fsm.keystone.fixtures;

import com.fsm.keystone.entity.AppUser;
import com.fsm.keystone.entity.Customer;
import com.fsm.keystone.enums.Role;

/**
 * Object-mother factory for {@link AppUser} entities.
 *
 * <p>Minimum valid shape: {@code fullName}, {@code email}, {@code password},
 * and {@code role} are non-null columns. {@code active} defaults to {@code true}
 * via {@code @PrePersist} but is set explicitly here for unit-test determinism.</p>
 *
 * <p>Password is a placeholder BCrypt-formatted string — never the production
 * encoding of a real password. Tests authenticate via JWT (see {@link TestJwtFactory}),
 * not via credentials.</p>
 *
 * <p>All emails use the {@code example.test} domain (synthetic, per PII policy).</p>
 */
public final class UserFixtures {

    /**
     * Placeholder BCrypt-formatted password stored in fixture users.
     * Not a real encoded password; never use for production or real authentication tests.
     */
    public static final String TEST_PASSWORD_PLACEHOLDER =
            "$2a$10$test.fixture.password.placeholder.not.for.production.00";

    private UserFixtures() {}

    /** Returns a builder for a generic active MANAGER user with no tenant link. */
    public static AppUser.AppUserBuilder aManager() {
        return AppUser.builder()
                .fullName("Test Manager")
                .email("manager@example.test")
                .password(TEST_PASSWORD_PLACEHOLDER)
                .phone("555-4001")
                .role(Role.MANAGER)
                .active(true)
                .createdAt(FixtureClock.NOW)
                .customer(null);
    }

    /** Returns a builder for a generic active DISPATCHER user with no tenant link. */
    public static AppUser.AppUserBuilder aDispatcher() {
        return AppUser.builder()
                .fullName("Test Dispatcher")
                .email("dispatcher@example.test")
                .password(TEST_PASSWORD_PLACEHOLDER)
                .phone("555-4002")
                .role(Role.DISPATCHER)
                .active(true)
                .createdAt(FixtureClock.NOW)
                .customer(null);
    }

    /** Returns a builder for a generic active TECHNICIAN user with no tenant link. */
    public static AppUser.AppUserBuilder aTechnician() {
        return AppUser.builder()
                .fullName("Test Technician")
                .email("technician@example.test")
                .password(TEST_PASSWORD_PLACEHOLDER)
                .phone("555-4003")
                .role(Role.TECHNICIAN)
                .active(true)
                .createdAt(FixtureClock.NOW)
                .customer(null);
    }

    /**
     * Returns a builder for a CUSTOMER user linked to the given customer aggregate.
     * The {@code customer} reference is the tenancy anchor used for row-level scoping.
     */
    public static AppUser.AppUserBuilder aCustomerUser(Customer customer) {
        return AppUser.builder()
                .fullName("Test Customer User")
                .email("customer.user@example.test")
                .password(TEST_PASSWORD_PLACEHOLDER)
                .phone("555-4004")
                .role(Role.CUSTOMER)
                .active(true)
                .createdAt(FixtureClock.NOW)
                .customer(customer);
    }

    /**
     * Returns a builder for a CUSTOMER persona with a {@code null} customer association.
     * Constructible on purpose: production signup allows this and later tests pin the behaviour.
     */
    public static AppUser.AppUserBuilder aCustomerUserWithNoCustomer() {
        return AppUser.builder()
                .fullName("Unlinked Customer User")
                .email("unlinked.customer@example.test")
                .password(TEST_PASSWORD_PLACEHOLDER)
                .phone("555-4005")
                .role(Role.CUSTOMER)
                .active(true)
                .createdAt(FixtureClock.NOW)
                .customer(null);
    }

    // ── Pre-built TenantScenario personas (ids match seed-two-tenants.sql) ────

    /** MANAGER persona — id 401, no tenant link. */
    public static AppUser.AppUserBuilder scenarioManager() {
        return aManager().id(401L).email("scenario.manager@example.test");
    }

    /** DISPATCHER persona — id 402, no tenant link. */
    public static AppUser.AppUserBuilder scenarioDispatcher() {
        return aDispatcher().id(402L).email("scenario.dispatcher@example.test");
    }

    /**
     * Shared TECHNICIAN persona — id 403.
     * Assigned to work orders across both ACME and GLOBEX tenants in {@link TenantScenario}.
     */
    public static AppUser.AppUserBuilder scenarioTechnician() {
        return aTechnician().id(403L).email("scenario.technician@example.test");
    }

    /** ACME CUSTOMER user persona — id 404, linked to ACME (id 101). */
    public static AppUser.AppUserBuilder scenarioAcmeCustomerUser(Customer acme) {
        return aCustomerUser(acme).id(404L).email("scenario.acme.user@example.test");
    }

    /** GLOBEX CUSTOMER user persona — id 405, linked to GLOBEX (id 102). */
    public static AppUser.AppUserBuilder scenarioGlobexCustomerUser(Customer globex) {
        return aCustomerUser(globex).id(405L).email("scenario.globex.user@example.test");
    }
}
