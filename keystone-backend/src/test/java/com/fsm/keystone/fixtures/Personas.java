package com.fsm.keystone.fixtures;

import com.fsm.keystone.entity.AppUser;
import com.fsm.keystone.entity.Customer;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

/**
 * Pre-built persona principals for use in security slice tests and service unit tests.
 *
 * <p>Each constant is a fully constructed {@link AppUser} that satisfies the
 * {@link org.springframework.security.core.userdetails.UserDetails} contract already
 * implemented on {@code AppUser} (role authority mapping, {@code active=true}).
 * Personas are usable both as {@code @WithUserDetails}-style principals and via
 * {@link #authenticationFor(AppUser)} for direct {@code Authentication} construction.</p>
 *
 * <p>Persona constants correspond to the users inserted by {@code seed-two-tenants.sql}.
 * The two CUSTOMER personas are wired to their respective tenants and are reachable
 * via {@link TenantScenario}; the static helpers here use ids only.</p>
 */
public final class Personas {

    /** MANAGER persona — full administration access, no tenant link. */
    public static final AppUser MANAGER =
            UserFixtures.scenarioManager().build();

    /** DISPATCHER persona — assignment/scheduling access, no tenant link. */
    public static final AppUser DISPATCHER =
            UserFixtures.scenarioDispatcher().build();

    /**
     * Shared TECHNICIAN persona — assigned to work orders across both ACME and GLOBEX.
     * Used to assert that a technician sees cross-tenant jobs they are assigned to
     * while a CUSTOMER user must not.
     */
    public static final AppUser TECHNICIAN =
            UserFixtures.scenarioTechnician().build();

    /**
     * ACME CUSTOMER user persona.
     * Linked to a detached {@link Customer} (id 101) suitable for unit assertions.
     * For integration tests use {@link TenantScenario} to get the persisted customer.
     */
    public static final AppUser ACME_CUSTOMER_USER =
            UserFixtures.scenarioAcmeCustomerUser(CustomerFixtures.acme().build()).build();

    /**
     * GLOBEX CUSTOMER user persona.
     * Linked to a detached {@link Customer} (id 102) suitable for unit assertions.
     */
    public static final AppUser GLOBEX_CUSTOMER_USER =
            UserFixtures.scenarioGlobexCustomerUser(CustomerFixtures.globex().build()).build();

    /**
     * Marker representing an unauthenticated caller.
     * Use when a test must assert that an anonymous request is rejected.
     * Value is {@code null} — callers should check for null before using.
     */
    public static final AppUser UNAUTHENTICATED = null;

    private Personas() {}

    /**
     * Wraps the given {@link AppUser} in a fully-authenticated
     * {@link UsernamePasswordAuthenticationToken} using the user's own authorities.
     *
     * <p>Suitable for setting on {@link org.springframework.security.core.context.SecurityContextHolder}
     * directly in service unit tests, or for constructing a mock principal in
     * {@code @WebMvcTest} slices where the JWT filter is bypassed.</p>
     */
    public static Authentication authenticationFor(AppUser user) {
        return UsernamePasswordAuthenticationToken.authenticated(
                user, null, user.getAuthorities());
    }

    /**
     * Returns the MANAGER's authentication token.
     * Convenience wrapper for the most common manager-scoped test setup.
     */
    public static Authentication managerAuth() {
        return authenticationFor(MANAGER);
    }

    /**
     * Returns the TECHNICIAN's authentication token.
     * Used in tests asserting that the technician's own-jobs scope is enforced.
     */
    public static Authentication technicianAuth() {
        return authenticationFor(TECHNICIAN);
    }

    /**
     * Returns the ACME CUSTOMER user's authentication token.
     * Used in cross-tenant tests asserting ACME data does not leak to GLOBEX.
     */
    public static Authentication acmeCustomerAuth() {
        return authenticationFor(ACME_CUSTOMER_USER);
    }

    /**
     * Returns the GLOBEX CUSTOMER user's authentication token.
     * Counterpart to {@link #acmeCustomerAuth()} for the cross-tenant assertion pair.
     */
    public static Authentication globexCustomerAuth() {
        return authenticationFor(GLOBEX_CUSTOMER_USER);
    }
}
