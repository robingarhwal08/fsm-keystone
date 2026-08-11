package com.fsm.keystone.fixtures;

import com.fsm.keystone.entity.AppUser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Issues signed JWT tokens for test personas without requiring a Spring context.
 *
 * <p>Uses the same jjwt API as the production {@code JwtService} so token shape
 * (claims, subject, expiry, algorithm) stays in lockstep with production signing.
 * If {@code JwtService.generateToken()} changes its claim structure, update this
 * class at the same time.</p>
 *
 * <p>{@link #TEST_SECRET} must match {@code app.jwt.secret} in
 * {@code src/test/resources/application-test.properties} so tokens produced
 * here validate when the Spring context uses that profile.</p>
 *
 * <p><strong>Security note</strong>: {@link #TEST_SECRET} is a documented test-only
 * placeholder value. It must never appear in production configuration.
 * The production secret is supplied via Docker/Compose secrets (see ADR-0004).</p>
 */
public final class TestJwtFactory {

    /**
     * Test-only signing secret — 64 characters, HS256-compatible.
     * Must match {@code app.jwt.secret} in {@code application-test.properties}.
     * Never use in production.
     */
    public static final String TEST_SECRET =
            "test-fixture-jwt-secret-key-must-be-at-least-64-chars-00000000";

    /**
     * Token expiry for test tokens: 1 hour.
     * Long enough that tokens do not expire mid-test on any reasonable machine.
     */
    public static final long TEST_EXPIRATION_MS = 3_600_000L;

    private TestJwtFactory() {}

    /**
     * Issues a signed JWT for the given {@link AppUser} using {@link #TEST_SECRET}.
     * The token subject is the user's email (matching {@code JwtService.generateToken}).
     */
    public static String issueFor(AppUser user) {
        return issueFor(user, new HashMap<>());
    }

    /**
     * Issues a signed JWT for the given user with additional claims merged into the payload.
     * Claim keys from {@code extraClaims} override the defaults.
     */
    public static String issueFor(AppUser user, Map<String, Object> extraClaims) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(user.getEmail())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + TEST_EXPIRATION_MS))
                .signWith(signingKey())
                .compact();
    }

    /**
     * Issues a bearer header value ({@code "Bearer <token>"}) ready to set on
     * {@code MockMvc} requests or {@code RestTemplate} headers.
     */
    public static String bearerHeaderFor(AppUser user) {
        return "Bearer " + issueFor(user);
    }

    /** Returns the HMAC signing key derived from {@link #TEST_SECRET}. */
    private static SecretKey signingKey() {
        return Keys.hmacShaKeyFor(TEST_SECRET.getBytes());
    }
}
