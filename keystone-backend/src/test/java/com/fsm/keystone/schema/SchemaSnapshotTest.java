package com.fsm.keystone.schema;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Validates that the JPA entity model still produces the DDL committed in
 * {@code docs/baseline/schema-snapshot.sql}. If the schema has changed
 * intentionally, re-run with {@code -Dschema.snapshot.update=true} to
 * regenerate the committed baseline.
 *
 * <p>Normalization rules applied before comparison:
 * <ul>
 *   <li>Comment lines stripped.</li>
 *   <li>Hibernate's {@code IF EXISTS} qualifiers removed.</li>
 *   <li>{@code CONSTRAINT <name>} tokens removed (hash-based names are nondeterministic across Hibernate releases).</li>
 *   <li>Whitespace collapsed to single spaces.</li>
 *   <li>All text lowercased.</li>
 *   <li>Statements sorted alphabetically for stable comparison.</li>
 * </ul>
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=none",
    "spring.jpa.properties.jakarta.persistence.schema-generation.database.action=create",
    "spring.jpa.properties.jakarta.persistence.schema-generation.scripts.action=create",
    "spring.jpa.properties.jakarta.persistence.schema-generation.scripts.create-target=target/test-schema-export.sql",
    "spring.jpa.properties.jakarta.persistence.schema-generation.create-source=metadata"
})
class SchemaSnapshotTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    private static final Path EXPORT_PATH = Path.of("target/test-schema-export.sql");
    private static final Path SNAPSHOT_PATH = Path.of("../docs/baseline/schema-snapshot.sql");
    private static final boolean UPDATE_SNAPSHOT =
            Boolean.parseBoolean(System.getProperty("schema.snapshot.update", "false"));

    @Test
    void schemaMatchesCommittedSnapshot() throws IOException {
        assertTrue(Files.exists(EXPORT_PATH),
                "Hibernate did not write the schema export file to " + EXPORT_PATH.toAbsolutePath()
                + ".\nVerify that spring.jpa.properties.jakarta.persistence.schema-generation"
                + ".scripts.create-target is set and the 'target/' directory exists.");

        String normalized = normalize(Files.readString(EXPORT_PATH));

        if (UPDATE_SNAPSHOT) {
            Files.createDirectories(SNAPSHOT_PATH.getParent());
            Files.writeString(SNAPSHOT_PATH, normalized);
            System.out.println("[SchemaSnapshotTest] Snapshot updated: "
                    + SNAPSHOT_PATH.toAbsolutePath());
            return;
        }

        assertTrue(Files.exists(SNAPSHOT_PATH),
                "Committed snapshot not found at " + SNAPSHOT_PATH.toAbsolutePath()
                + ".\nBootstrap it by running:\n"
                + "  cd keystone-backend && mvn test -Dtest=SchemaSnapshotTest -Dschema.snapshot.update=true\n"
                + "Then commit docs/baseline/schema-snapshot.sql.");

        String committed = Files.readString(SNAPSHOT_PATH);

        assertEquals(committed, normalized,
                "JPA schema has drifted from the committed snapshot.\n"
                + "If the change is intentional, regenerate the snapshot:\n"
                + "  cd keystone-backend && mvn test -Dtest=SchemaSnapshotTest -Dschema.snapshot.update=true\n"
                + "Then review the diff, commit docs/baseline/schema-snapshot.sql, and record\n"
                + "the change in docs/baseline/schema-drift-report.md.\n");
    }

    /**
     * Normalizes Hibernate-generated DDL into a stable, deterministic form.
     * The output is suitable for line-by-line diffing and version control.
     */
    static String normalize(String raw) {
        return Arrays.stream(raw.split(";"))
                .map(stmt -> stmt.lines()
                        .map(String::trim)
                        .filter(line -> !line.startsWith("--") && !line.isEmpty())
                        .collect(Collectors.joining(" ")))
                .map(stmt -> {
                    String s = stmt;
                    s = s.replaceAll("(?i)\\bif\\s+exists\\b", "");
                    s = s.replaceAll("(?i)\\bconstraint\\s+\\S+\\s+", "");
                    s = s.replaceAll("\\s+", " ").trim();
                    return s.toLowerCase(Locale.ROOT);
                })
                .filter(s -> !s.isEmpty())
                .sorted()
                .collect(Collectors.joining(";\n", "", ";\n"));
    }
}
