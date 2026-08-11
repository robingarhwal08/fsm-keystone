package com.fsm.keystone.security;

import com.fsm.keystone.config.JwtProperties;
import com.fsm.keystone.fixtures.JwtTestKeys;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link JwtService} covering single-key and dual-key rotation scenarios.
 */
@Tag("unit")
class JwtServiceTest {

    private static final String SUBJECT = "user@example.test";
    private static final long EXPIRATION_MS = 3_600_000L;

    private SimpleMeterRegistry meterRegistry;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        userDetails = User.withUsername(SUBJECT).password("ignored").authorities(Collections.emptyList()).build();
    }

    private JwtService buildService(String current, String previous) {
        JwtProperties props = new JwtProperties();
        props.setSecret(current);
        props.setPreviousSecret(previous);
        props.setExpirationMs(EXPIRATION_MS);
        ObjectProvider<io.micrometer.core.instrument.MeterRegistry> provider =
                new SingletonObjectProvider<>(meterRegistry);
        return new JwtService(props, provider);
    }

    private JwtService buildSingleKeyService(String current) {
        return buildService(current, null);
    }

    private String rawTokenSignedWith(String key, String subject, long offsetMs) {
        SecretKey secretKey = Keys.hmacShaKeyFor(key.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(subject)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + offsetMs))
                .signWith(secretKey)
                .compact();
    }

    private String rawExpiredTokenSignedWith(String key, String subject) {
        SecretKey secretKey = Keys.hmacShaKeyFor(key.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis() - 7_200_000L))
                .expiration(new Date(System.currentTimeMillis() - 3_600_000L))
                .signWith(secretKey)
                .compact();
    }

    @Nested
    class SingleKeyMode {

        @Test
        void generateAndVerify_withCurrentKey() {
            JwtService service = buildSingleKeyService(JwtTestKeys.KEY_A);
            String token = service.generateToken(userDetails);
            assertThat(service.extractUsername(token)).isEqualTo(SUBJECT);
            assertThat(service.isTokenValid(token, userDetails)).isTrue();
        }

        @Test
        void generatedToken_carriesKidHeader() {
            JwtService service = buildSingleKeyService(JwtTestKeys.KEY_A);
            String token = service.generateToken(userDetails);
            String kid = JwtService.extractKidUnsafe(token);
            assertThat(kid).isNotBlank();
            assertThat(kid).isEqualTo(JwtService.deriveKeyId(JwtTestKeys.KEY_A));
        }

        @Test
        void tamperedToken_isRejected() {
            JwtService service = buildSingleKeyService(JwtTestKeys.KEY_A);
            String token = service.generateToken(userDetails);
            // Corrupt the signature
            String tampered = token.substring(0, token.lastIndexOf('.') + 1) + "invalidsig";
            assertThatThrownBy(() -> service.extractUsername(tampered))
                    .isInstanceOf(JwtException.class);
        }

        @Test
        void expiredToken_throwsExpiredJwtException() {
            JwtService service = buildSingleKeyService(JwtTestKeys.KEY_A);
            String expired = rawExpiredTokenSignedWith(JwtTestKeys.KEY_A, SUBJECT);
            assertThatThrownBy(() -> service.extractUsername(expired))
                    .isInstanceOf(ExpiredJwtException.class);
        }

        @Test
        void tokenSignedWithUnrelatedKey_isRejected() {
            JwtService service = buildSingleKeyService(JwtTestKeys.KEY_A);
            // KEY_B is the unrelated key
            String token = rawTokenSignedWith(JwtTestKeys.KEY_B, SUBJECT, EXPIRATION_MS);
            assertThatThrownBy(() -> service.extractUsername(token))
                    .isInstanceOf(JwtException.class);
        }

        @Test
        void legacyTokenWithoutKid_verifiedWithCurrentKey() {
            JwtService service = buildSingleKeyService(JwtTestKeys.KEY_A);
            // Generate a legacy token (no kid header) signed with KEY_A
            String legacyToken = rawTokenSignedWith(JwtTestKeys.KEY_A, SUBJECT, EXPIRATION_MS);
            assertThat(JwtService.extractKidUnsafe(legacyToken)).isNull();
            assertThat(service.extractUsername(legacyToken)).isEqualTo(SUBJECT);
        }
    }

    @Nested
    class DualKeyMode {

        @Test
        void currentKeyToken_verifiedWithCurrentKey_noFallback() {
            JwtService service = buildService(JwtTestKeys.KEY_B, JwtTestKeys.KEY_A);
            String token = service.generateToken(userDetails);
            assertThat(service.isTokenValid(token, userDetails)).isTrue();
            double fallbacks = fallbackCount(service);
            assertThat(fallbacks).isZero();
        }

        @Test
        void previousKeyToken_withKid_verifiedWithPreviousKey_andFallbackRecorded() {
            // Service with KEY_B current and KEY_A previous
            JwtService service = buildService(JwtTestKeys.KEY_B, JwtTestKeys.KEY_A);
            // Simulate a token signed with KEY_A (previous) that carries its kid
            String previousKid = JwtService.deriveKeyId(JwtTestKeys.KEY_A);
            SecretKey keyA = Keys.hmacShaKeyFor(JwtTestKeys.KEY_A.getBytes(StandardCharsets.UTF_8));
            String prevToken = Jwts.builder()
                    .header().keyId(previousKid).and()
                    .subject(SUBJECT)
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                    .signWith(keyA)
                    .compact();

            assertThat(service.extractUsername(prevToken)).isEqualTo(SUBJECT);
            assertThat(fallbackCount(service)).isEqualTo(1.0);
        }

        @Test
        void legacyPreviousKeyToken_noKid_verifiedWithFallback() {
            JwtService service = buildService(JwtTestKeys.KEY_B, JwtTestKeys.KEY_A);
            // Legacy token: no kid header, signed with KEY_A (the previous key)
            String legacyToken = rawTokenSignedWith(JwtTestKeys.KEY_A, SUBJECT, EXPIRATION_MS);
            assertThat(JwtService.extractKidUnsafe(legacyToken)).isNull();
            assertThat(service.extractUsername(legacyToken)).isEqualTo(SUBJECT);
            assertThat(fallbackCount(service)).isEqualTo(1.0);
        }

        @Test
        void previousKeyAbsent_legacyToken_isRejected() {
            JwtService service = buildSingleKeyService(JwtTestKeys.KEY_B);
            // Token signed with KEY_A (not configured)
            String token = rawTokenSignedWith(JwtTestKeys.KEY_A, SUBJECT, EXPIRATION_MS);
            assertThatThrownBy(() -> service.extractUsername(token))
                    .isInstanceOf(JwtException.class);
        }

        @Test
        void unknownKidToken_isRejected() {
            JwtService service = buildService(JwtTestKeys.KEY_B, JwtTestKeys.KEY_A);
            // Token with a kid that matches neither current nor previous
            String unknownKid = JwtService.deriveKeyId("some-completely-different-secret-of-at-least-64-chars-00000000");
            SecretKey someKey = Keys.hmacShaKeyFor(
                    "some-completely-different-secret-of-at-least-64-chars-00000000"
                            .getBytes(StandardCharsets.UTF_8));
            String token = Jwts.builder()
                    .header().keyId(unknownKid).and()
                    .subject(SUBJECT)
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                    .signWith(someKey)
                    .compact();
            assertThatThrownBy(() -> service.extractUsername(token))
                    .isInstanceOf(JwtException.class)
                    .hasMessageContaining("Unknown key identifier");
        }

        @Test
        void expiredPreviousKeyToken_throwsExpiredJwtException_notSignatureException() {
            JwtService service = buildService(JwtTestKeys.KEY_B, JwtTestKeys.KEY_A);
            String expiredToken = rawExpiredTokenSignedWith(JwtTestKeys.KEY_A, SUBJECT);
            // Must fail as expired, not as signature failure
            assertThatThrownBy(() -> service.extractUsername(expiredToken))
                    .isInstanceOf(ExpiredJwtException.class);
        }

        @Test
        void identicalCurrentAndPreviousSecret_treatedAsSingleKey() {
            // Same key in both slots → JwtService logs WARN and operates as single-key
            JwtService service = buildService(JwtTestKeys.KEY_A, JwtTestKeys.KEY_A);
            String token = service.generateToken(userDetails);
            assertThat(service.isTokenValid(token, userDetails)).isTrue();
            // No previous key registered, so a legacy KEY_B token must fail
            String otherToken = rawTokenSignedWith(JwtTestKeys.KEY_B, SUBJECT, EXPIRATION_MS);
            assertThatThrownBy(() -> service.extractUsername(otherToken))
                    .isInstanceOf(JwtException.class);
        }

        @Test
        void fallbackCounter_incrementsOncePerFallbackVerification() {
            JwtService service = buildService(JwtTestKeys.KEY_B, JwtTestKeys.KEY_A);
            String legacyToken = rawTokenSignedWith(JwtTestKeys.KEY_A, SUBJECT, EXPIRATION_MS);

            service.extractUsername(legacyToken);
            service.extractUsername(legacyToken);
            service.extractUsername(legacyToken);

            assertThat(fallbackCount(service)).isEqualTo(3.0);
        }
    }

    @Nested
    class HelperMethods {

        @Test
        void deriveKeyId_returnsSameValueForSameInput() {
            String id1 = JwtService.deriveKeyId(JwtTestKeys.KEY_A);
            String id2 = JwtService.deriveKeyId(JwtTestKeys.KEY_A);
            assertThat(id1).isEqualTo(id2).hasSize(8);
        }

        @Test
        void deriveKeyId_returnsDifferentValuesForDifferentKeys() {
            String idA = JwtService.deriveKeyId(JwtTestKeys.KEY_A);
            String idB = JwtService.deriveKeyId(JwtTestKeys.KEY_B);
            assertThat(idA).isNotEqualTo(idB);
        }

        @Test
        void extractKidUnsafe_returnsNullForTokenWithoutKid() {
            String token = rawTokenSignedWith(JwtTestKeys.KEY_A, SUBJECT, EXPIRATION_MS);
            assertThat(JwtService.extractKidUnsafe(token)).isNull();
        }

        @Test
        void extractKidUnsafe_returnsKidForTokenWithKid() {
            JwtService service = buildSingleKeyService(JwtTestKeys.KEY_A);
            String token = service.generateToken(userDetails);
            assertThat(JwtService.extractKidUnsafe(token))
                    .isEqualTo(JwtService.deriveKeyId(JwtTestKeys.KEY_A));
        }

        @Test
        void extractKidUnsafe_returnsNullForMalformedToken() {
            assertThat(JwtService.extractKidUnsafe("not.a.jwt")).isNull();
            assertThat(JwtService.extractKidUnsafe("")).isNull();
        }
    }

    private double fallbackCount(JwtService service) {
        return meterRegistry.find(JwtService.FALLBACK_COUNTER_NAME)
                .counters().stream()
                .mapToDouble(Counter::count)
                .sum();
    }

    /** Minimal ObjectProvider that always returns a single instance. */
    private static class SingletonObjectProvider<T> implements ObjectProvider<T> {
        private final T instance;

        SingletonObjectProvider(T instance) { this.instance = instance; }

        @Override public T getObject() { return instance; }
        @Override public T getObject(Object... args) { return instance; }
        @Override public T getIfAvailable() { return instance; }
        @Override public T getIfUnique() { return instance; }
    }
}
