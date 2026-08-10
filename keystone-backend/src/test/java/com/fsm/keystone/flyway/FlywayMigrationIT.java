package com.fsm.keystone.flyway;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test: boots the full Spring context against a Testcontainers PostgreSQL 16
 * instance and asserts that Flyway initialises correctly on an empty database.
 *
 * <p>Uses {@code @ServiceConnection} so Testcontainers auto-configures the datasource
 * without a {@code @DynamicPropertySource} workaround. The "dev" profile enables
 * {@code baseline-on-migrate=true} so a legacy-shaped database does not block startup.</p>
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("dev")
@Testcontainers
class FlywayMigrationIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private Flyway flyway;

    @Autowired
    private DataSource dataSource;

    @Test
    void flyway_schemaHistoryTable_isCreated() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData meta = connection.getMetaData();
            try (ResultSet rs = meta.getTables(null, null, "flyway_schema_history",
                    new String[]{"TABLE"})) {
                assertTrue(rs.next(),
                        "flyway_schema_history table must be created by Flyway during startup");
            }
        }
    }

    @Test
    void flyway_clean_isDisabled() {
        assertTrue(flyway.getConfiguration().isCleanDisabled(),
                "Flyway clean must be disabled in all profiles to prevent accidental schema drops");
    }

    @Test
    void flyway_validateOnMigrate_isEnabled() {
        assertTrue(flyway.getConfiguration().isValidateOnMigrate(),
                "Flyway validate-on-migrate must be enabled");
    }

    @Test
    void flyway_outOfOrder_isDisabled() {
        assertFalse(flyway.getConfiguration().isOutOfOrder(),
                "Flyway out-of-order must be disabled");
    }

    @Test
    void flyway_noMigrations_applied_onEmptyDirectory() {
        // Empty migration directory → Flyway creates schema history but applies nothing.
        assertEquals(0, flyway.info().applied().length,
                "No migrations should be applied when the migration directory is empty");
    }
}
