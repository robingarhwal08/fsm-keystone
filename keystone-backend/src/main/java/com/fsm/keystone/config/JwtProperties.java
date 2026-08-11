package com.fsm.keystone.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Typed configuration properties for JWT signing and verification.
 *
 * <p>Registered via {@code @EnableConfigurationProperties(JwtProperties.class)} on
 * {@code SecurityConfig} so it is available in both the full application context and
 * lightweight {@code @WebMvcTest} slices that import {@code SecurityConfig}.</p>
 *
 * <h2>Dual-Key Rotation Window</h2>
 * <p>Set {@code app.jwt.previous-secret} to the old signing key during a rotation.
 * JwtService will accept tokens signed by either key during the window.
 * Once all outstanding old-key tokens have expired, remove the property to close the window.
 * Monitor the {@code jwt.verification.fallback} Micrometer counter to confirm old-key
 * traffic has drained to zero before removing the property.</p>
 *
 * <p>{@code app.jwt.previous-secret} is subject to the same minimum-length requirement
 * as the current secret when it is set, to prevent a short secret entering via
 * operator error during a high-pressure rotation event.</p>
 */
@ConfigurationProperties("app.jwt")
@Validated
public class JwtProperties {

    /**
     * Current JWT signing key. HS256 requires at least 256 bits (32 bytes); the
     * project convention uses 64-hex-char strings (64 bytes) for HS256.
     */
    @NotBlank
    @Size(min = 64, message = "app.jwt.secret must be at least 64 characters")
    private String secret;

    /**
     * Previous signing key — present only during a rotation window.
     * Null or blank means single-key mode (no fallback verification).
     * Must be ≥ 64 characters when set.
     * Must differ from {@code secret}; identical values are treated as absent.
     */
    private String previousSecret;

    /** Token lifetime in milliseconds. Default 24 h (86400000). */
    @Positive
    private long expirationMs = 86_400_000L;

    /** Bean Validation assertion: previousSecret must be long enough when present. */
    @AssertTrue(message = "app.jwt.previous-secret must be at least 64 characters when set")
    public boolean isPreviousSecretValidLength() {
        return previousSecret == null
                || previousSecret.isBlank()
                || previousSecret.length() >= 64;
    }

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }

    public String getPreviousSecret() { return previousSecret; }
    public void setPreviousSecret(String previousSecret) { this.previousSecret = previousSecret; }

    public long getExpirationMs() { return expirationMs; }
    public void setExpirationMs(long expirationMs) { this.expirationMs = expirationMs; }
}
