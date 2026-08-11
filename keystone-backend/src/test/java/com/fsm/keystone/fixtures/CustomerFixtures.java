package com.fsm.keystone.fixtures;

import com.fsm.keystone.entity.Customer;

/**
 * Object-mother factory for {@link Customer} entities.
 *
 * <p>Minimum valid shape: {@code name} is the only non-null column.
 * Returns a pre-populated {@link Customer.CustomerBuilder} so callers can
 * override any field before calling {@code .build()}.</p>
 *
 * <p>All emails use the {@code example.test} domain (synthetic, per PII policy).</p>
 */
public final class CustomerFixtures {

    /** Placeholder BCrypt hash — never used for real authentication. */
    static final String TEST_BCRYPT_PLACEHOLDER =
            "$2a$10$test.fixture.password.placeholder.not.for.production.00";

    private CustomerFixtures() {}

    /** Returns a builder for a generic customer with sensible defaults. */
    public static Customer.CustomerBuilder aCustomer() {
        return Customer.builder()
                .name("Test Customer")
                .email("customer@example.test")
                .phone("555-0100")
                .billingAddress("1 Test Street, Springfield, IL 62700")
                .createdAt(FixtureClock.NOW)
                .updatedAt(FixtureClock.NOW);
    }

    /** Returns a builder for the ACME Corp tenant used in {@link TenantScenario}. */
    public static Customer.CustomerBuilder acme() {
        return aCustomer()
                .id(101L)
                .name("ACME Corp")
                .email("acme@example.test")
                .phone("555-0101")
                .billingAddress("101 ACME Way, Springfield, IL 62701");
    }

    /** Returns a builder for the GLOBEX Ltd tenant used in {@link TenantScenario}. */
    public static Customer.CustomerBuilder globex() {
        return aCustomer()
                .id(102L)
                .name("GLOBEX Ltd")
                .email("globex@example.test")
                .phone("555-0102")
                .billingAddress("102 Globex Avenue, Capital City, IL 62702");
    }
}
