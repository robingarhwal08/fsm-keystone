package com.fsm.keystone.support;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure unit smoke test — no Spring context, no Docker required.
 * Proves the Surefire unit-tag wiring works.
 */
@Tag("unit")
class SmokeTest {

    @Test
    void unitSmokeTestPasses() {
        assertTrue(true, "Unit smoke test must always pass");
    }
}
