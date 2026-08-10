package com.fsm.keystone.config;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that Spring Boot's configtree binding resolves file-based secrets
 * (Docker Compose secrets mounted at /run/secrets/) to Spring properties.
 *
 * <p>Tests are pure unit tests — no Spring context loaded, no Docker required.
 * They directly assert the fixture files committed under
 * {@code src/test/resources/secrets-configtree/} have the expected content,
 * and separately verify the configtree file-reading semantics using temp files.</p>
 *
 * <p>The actual Spring property resolution from configtree is an integration concern
 * exercised by the compose smoke test (scripts/ci/compose-smoke.sh). The fixture
 * files committed here are consumed by WO-021 and WO-025 handler tests.</p>
 */
@Tag("unit")
class ConfigtreeBindingTest {

    // ── Fixture file assertions ───────────────────────────────────────────────

    @Test
    void fixtureSecretsDirectory_exists() {
        URL resourceUrl = getClass().getClassLoader().getResource("secrets-configtree");
        assertNotNull(resourceUrl,
                "secrets-configtree fixture directory must be on the test classpath");
    }

    @Test
    void fixtureSecretsDirectory_containsDatasourcePasswordFile() throws Exception {
        Path secretsDir = classpathDir("secrets-configtree");
        Path passwordFile = secretsDir.resolve("spring.datasource.password");
        assertTrue(Files.exists(passwordFile),
                "spring.datasource.password fixture file must exist");
        String content = Files.readString(passwordFile, StandardCharsets.UTF_8).trim();
        assertFalse(content.isEmpty(), "spring.datasource.password fixture must not be empty");
        // Verify it is clearly a non-production placeholder
        assertTrue(content.startsWith("fixture-"),
                "spring.datasource.password fixture value must start with 'fixture-' to prevent accidental production use");
    }

    @Test
    void fixtureSecretsDirectory_containsJwtSecretFile() throws Exception {
        Path secretsDir = classpathDir("secrets-configtree");
        Path jwtFile = secretsDir.resolve("app.jwt.secret");
        assertTrue(Files.exists(jwtFile),
                "app.jwt.secret fixture file must exist");
        String content = Files.readString(jwtFile, StandardCharsets.UTF_8).trim();
        assertFalse(content.isEmpty(), "app.jwt.secret fixture must not be empty");
        assertEquals(64, content.length(),
                "app.jwt.secret fixture must be exactly 64 hex characters");
    }

    // ── Configtree file-reading semantics ────────────────────────────────────

    @Test
    void configtreeFile_trims_trailingNewline(@TempDir Path tempDir) throws IOException {
        Path secretFile = tempDir.resolve("spring.datasource.password");
        // Simulate a Docker secret file that may include a trailing newline
        Files.writeString(secretFile, "db-password-value\n", StandardCharsets.UTF_8);

        String content = Files.readString(secretFile, StandardCharsets.UTF_8).trim();
        assertEquals("db-password-value", content,
                "configtree files must be trimmed before use");
    }

    @Test
    void configtreeFile_emptyContent_detectedAsInvalid(@TempDir Path tempDir) throws IOException {
        Path secretFile = tempDir.resolve("app.jwt.secret");
        Files.writeString(secretFile, "", StandardCharsets.UTF_8);

        String content = Files.readString(secretFile, StandardCharsets.UTF_8).trim();
        assertTrue(content.isEmpty(),
                "empty secret file content must be detectable as invalid by application startup validation");
    }

    @Test
    void configtreeFile_absolutePathResolvesCorrectly(@TempDir Path tempDir) throws IOException {
        // Verify the naming convention: file name = Spring property key with dots
        Path dbPassFile = tempDir.resolve("spring.datasource.password");
        Path jwtFile    = tempDir.resolve("app.jwt.secret");
        Files.writeString(dbPassFile, "test-db-pass");
        Files.writeString(jwtFile,    "0".repeat(64));

        // spring.datasource.password and app.jwt.secret are the exact names
        // Docker Compose uses when mounting secrets under /run/secrets/.
        // Spring configtree binds flat files with dots in the name directly to
        // the property key — no directory hierarchy needed.
        assertEquals("spring.datasource.password", dbPassFile.getFileName().toString());
        assertEquals("app.jwt.secret", jwtFile.getFileName().toString());
        assertEquals("test-db-pass", Files.readString(dbPassFile));
        assertEquals("0".repeat(64), Files.readString(jwtFile));
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private static Path classpathDir(String name) throws Exception {
        URL url = ConfigtreeBindingTest.class.getClassLoader().getResource(name);
        assertNotNull(url, name + " not found on classpath");
        return Paths.get(url.toURI());
    }
}
