package com.fsm.keystone.fixtures;

/**
 * Throwaway JWT signing keys for tests that exercise dual-key rotation logic.
 *
 * <p>Both keys are exactly 64 ASCII characters (≥ 64-byte minimum for HS256).
 * They are deliberately distinct so tests can distinguish which key signed a token.
 * Neither value must ever appear in production configuration.</p>
 *
 * <p>{@link #KEY_A} represents the <em>old</em> signing key (demoted to
 * {@code app.jwt.previous-secret} during a rotation window).
 * {@link #KEY_B} represents the <em>new</em> current signing key
 * ({@code app.jwt.secret}) after rotation.</p>
 *
 * <p>Note: {@link TestJwtFactory#TEST_SECRET} is intentionally a third distinct
 * key used by all existing single-key tests so rotation tests do not interfere.</p>
 */
public final class JwtTestKeys {

    /** Old key — used as previous-secret during rotation window tests. 64 characters. */
    public static final String KEY_A =
            "jwt-rotation-test-key-A-must-be-at-least-64-chars-AAAAAAAAAAAAA1";

    /** New key — used as current secret during rotation window tests. 64 characters. */
    public static final String KEY_B =
            "jwt-rotation-test-key-B-must-be-at-least-64-chars-BBBBBBBBBBBBB2";

    private JwtTestKeys() {}
}
