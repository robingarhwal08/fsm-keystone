package com.fsm.keystone.security;

import com.fsm.keystone.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT signing and verification service with support for a dual-key rotation window.
 *
 * <h2>Dual-Key Rotation Window</h2>
 * <p>Set {@code app.jwt.previous-secret} to the old signing key during a rotation.
 * New tokens are always signed with the current key and carry a {@code kid} header
 * identifying the signing key. Verification selects the key deterministically from
 * the {@code kid} header, or falls back to current-then-previous when the header is
 * absent (tokens issued before this change carry no {@code kid}).</p>
 *
 * <p>Each fallback verification (token valid only via the previous key) emits one
 * {@code WARN} log line and increments the {@code jwt.verification.fallback} counter.
 * Monitor the counter to confirm old-key traffic has drained to zero before removing
 * {@code app.jwt.previous-secret} to close the window.</p>
 *
 * <p>Key identifiers are derived as the first 8 hex characters of SHA-256 of the
 * secret, so no secret material is ever exposed in the {@code kid} header.</p>
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    static final String FALLBACK_COUNTER_NAME = "jwt.verification.fallback";

    private final SecretKey currentKey;
    private final String currentKeyId;
    private final SecretKey previousKey;
    private final String previousKeyId;
    private final long expirationMs;
    private final MeterRegistry meterRegistry;

    public JwtService(JwtProperties props, ObjectProvider<MeterRegistry> registryProvider) {
        this.currentKey = Keys.hmacShaKeyFor(props.getSecret().getBytes(StandardCharsets.UTF_8));
        this.currentKeyId = deriveKeyId(props.getSecret());
        this.expirationMs = props.getExpirationMs();
        this.meterRegistry = registryProvider.getIfAvailable();

        String prev = props.getPreviousSecret();
        if (prev != null && !prev.isBlank() && !prev.equals(props.getSecret())) {
            this.previousKey = Keys.hmacShaKeyFor(prev.getBytes(StandardCharsets.UTF_8));
            this.previousKeyId = deriveKeyId(prev);
        } else {
            if (prev != null && !prev.isBlank()) {
                log.warn("app.jwt.previous-secret is identical to current secret; treating as absent");
            }
            this.previousKey = null;
            this.previousKeyId = null;
        }
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        return resolver.apply(extractAllClaims(token));
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Jwts.builder()
                .header().keyId(currentKeyId).and()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(currentKey)
                .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        return extractUsername(token).equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private Claims extractAllClaims(String token) {
        String kid = extractKidUnsafe(token);

        if (kid != null) {
            if (kid.equals(currentKeyId)) {
                return Jwts.parser().verifyWith(currentKey).build()
                        .parseSignedClaims(token).getPayload();
            } else if (kid.equals(previousKeyId) && previousKey != null) {
                Claims claims = Jwts.parser().verifyWith(previousKey).build()
                        .parseSignedClaims(token).getPayload();
                recordFallback(claims.getSubject(), previousKeyId);
                return claims;
            } else {
                throw new JwtException("Unknown key identifier: " + kid);
            }
        }

        // No kid header — legacy token issued before dual-key support.
        // Try current key first; fall back to previous key if present.
        try {
            return Jwts.parser().verifyWith(currentKey).build()
                    .parseSignedClaims(token).getPayload();
        } catch (ExpiredJwtException e) {
            // Signed with current key but expired — propagate immediately.
            throw e;
        } catch (JwtException e) {
            if (previousKey != null) {
                // May throw ExpiredJwtException (correct: token is expired regardless of key).
                Claims claims = Jwts.parser().verifyWith(previousKey).build()
                        .parseSignedClaims(token).getPayload();
                recordFallback(claims.getSubject(), previousKeyId);
                return claims;
            }
            throw e;
        }
    }

    private void recordFallback(String subject, String keyId) {
        log.warn("JWT verified via previous key — subject={} keyId={}", subject, keyId);
        if (meterRegistry != null) {
            Counter.builder(FALLBACK_COUNTER_NAME)
                    .tag("keyId", keyId)
                    .description("JWT verifications that succeeded only via the previous key")
                    .register(meterRegistry)
                    .increment();
        }
    }

    /**
     * Derives a non-revealing key identifier from a secret.
     * Returns the first 8 hex characters of SHA-256(secret), e.g. {@code "a1b2c3d4"}.
     */
    static String deriveKeyId(String secret) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(secret.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, 8);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    /**
     * Extracts the {@code kid} header from a JWT without verifying the signature.
     * Returns {@code null} if the header is absent, malformed, or has no {@code kid}.
     */
    static String extractKidUnsafe(String token) {
        try {
            String[] parts = token.split("\\.", -1);
            if (parts.length < 2) return null;
            String headerB64 = parts[0];
            int pad = (4 - headerB64.length() % 4) % 4;
            String padded = headerB64 + "=".repeat(pad);
            String decoded = new String(Base64.getUrlDecoder().decode(padded), StandardCharsets.UTF_8);
            int kidIdx = decoded.indexOf("\"kid\"");
            if (kidIdx < 0) return null;
            int colon = decoded.indexOf(':', kidIdx + 5);
            if (colon < 0) return null;
            int quoteStart = decoded.indexOf('"', colon + 1);
            if (quoteStart < 0) return null;
            int quoteEnd = decoded.indexOf('"', quoteStart + 1);
            if (quoteEnd < 0) return null;
            return decoded.substring(quoteStart + 1, quoteEnd);
        } catch (Exception e) {
            return null;
        }
    }
}
