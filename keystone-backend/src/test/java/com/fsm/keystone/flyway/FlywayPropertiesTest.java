package com.fsm.keystone.flyway;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests asserting all required Flyway properties are present in
 * src/main/resources/application.properties.  No Spring context is loaded.
 *
 * Maven test execution CWD = module root (keystone-backend/), so the path
 * src/main/resources/application.properties is a stable relative reference.
 */
@Tag("unit")
class FlywayPropertiesTest {

    private static final Path MAIN_PROPS_PATH =
            Paths.get("src/main/resources/application.properties");

    private Properties loadMainProperties() throws IOException {
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(MAIN_PROPS_PATH)) {
            props.load(in);
        }
        return props;
    }

    @Test
    void flyway_enabled_isTrue() throws IOException {
        assertEquals("true", loadMainProperties().getProperty("spring.flyway.enabled"),
                "spring.flyway.enabled must be true in application.properties");
    }

    @Test
    void flyway_locations_isDbMigration() throws IOException {
        assertEquals("classpath:db/migration",
                loadMainProperties().getProperty("spring.flyway.locations"),
                "spring.flyway.locations must point to classpath:db/migration");
    }

    @Test
    void flyway_validateOnMigrate_isTrue() throws IOException {
        assertEquals("true",
                loadMainProperties().getProperty("spring.flyway.validate-on-migrate"),
                "spring.flyway.validate-on-migrate must be true");
    }

    @Test
    void flyway_cleanDisabled_isTrue() throws IOException {
        assertEquals("true",
                loadMainProperties().getProperty("spring.flyway.clean-disabled"),
                "spring.flyway.clean-disabled must be true in every profile");
    }

    @Test
    void flyway_outOfOrder_isFalse() throws IOException {
        assertEquals("false",
                loadMainProperties().getProperty("spring.flyway.out-of-order"),
                "spring.flyway.out-of-order must be false");
    }

    @Test
    void flyway_noUrlOrPasswordPropertyPresent() throws IOException {
        Properties props = loadMainProperties();
        assertNull(props.getProperty("spring.flyway.url"),
                "spring.flyway.url must not be set; Flyway reuses the primary DataSource");
        assertNull(props.getProperty("spring.flyway.password"),
                "spring.flyway.password must not be set; Flyway reuses the primary DataSource");
    }

    @Test
    void devProfile_baselineOnMigrate_isTrue() throws IOException {
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(
                Paths.get("src/main/resources/application-dev.properties"))) {
            props.load(in);
        }
        assertEquals("true", props.getProperty("spring.flyway.baseline-on-migrate"),
                "application-dev.properties must set baseline-on-migrate=true");
    }

    @Test
    void prodProfile_baselineOnMigrate_isFalse() throws IOException {
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(
                Paths.get("src/main/resources/application-prod.properties"))) {
            props.load(in);
        }
        assertEquals("false", props.getProperty("spring.flyway.baseline-on-migrate"),
                "application-prod.properties must set baseline-on-migrate=false");
    }

    @Test
    void migrationDirectory_readmePlaceholderExists() {
        Path readme = Paths.get("src/main/resources/db/migration/README.md");
        assertTrue(Files.exists(readme),
                "db/migration/README.md placeholder must exist so the empty directory is git-tracked");
    }
}
