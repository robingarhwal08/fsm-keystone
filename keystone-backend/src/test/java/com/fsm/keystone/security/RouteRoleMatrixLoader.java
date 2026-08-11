package com.fsm.keystone.security;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * Parses {@code /security/route-role-matrix.csv} from the test classpath into
 * {@link RouteRoleMatrixRow} records and loads request body fixtures from
 * {@code /security/bodies/{key}.json}.
 *
 * <p>The loader eagerly parses on construction and caches all rows. Malformed
 * lines are rejected with a descriptive exception naming the offending line number.
 * Unknown role columns and blank path values are also rejected.</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * RouteRoleMatrixLoader loader = new RouteRoleMatrixLoader();
 * List<RouteRoleMatrixRow> rows = loader.rows();
 * String body = loader.loadBody("work-order-create");
 * }</pre>
 */
public class RouteRoleMatrixLoader {

    static final String CSV_PATH = "/security/route-role-matrix.csv";
    static final String BODY_PATH_TEMPLATE = "/security/bodies/%s.json";

    // Expected column count in the CSV (excluding header)
    static final int EXPECTED_COLUMN_COUNT = 9;

    private final List<RouteRoleMatrixRow> rows;

    /** Constructs a loader and eagerly parses the CSV from the classpath. */
    public RouteRoleMatrixLoader() {
        this.rows = Collections.unmodifiableList(parse(CSV_PATH));
    }

    /** Returns all matrix rows (immutable). */
    public List<RouteRoleMatrixRow> rows() {
        return rows;
    }

    /** Returns a stream over all matrix rows. */
    public Stream<RouteRoleMatrixRow> stream() {
        return rows.stream();
    }

    /**
     * Finds the row for the given method and path template.
     *
     * @throws IllegalArgumentException if no row matches
     */
    public RouteRoleMatrixRow findRow(String method, String pathTemplate) {
        return rows.stream()
                .filter(r -> r.method().equalsIgnoreCase(method)
                        && r.pathTemplate().equals(pathTemplate))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No matrix row found for: " + method + " " + pathTemplate));
    }

    /**
     * Loads a request body fixture JSON from
     * {@code /security/bodies/{key}.json} on the classpath.
     *
     * @param key the {@code bodyFixtureKey} from the matrix row
     * @return the JSON string (trimmed)
     * @throws IllegalArgumentException if the resource is not found or is blank
     */
    public String loadBody(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("bodyFixtureKey must not be blank");
        }
        String resourcePath = String.format(BODY_PATH_TEMPLATE, key);
        try (InputStream is = RouteRoleMatrixLoader.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalArgumentException(
                        "Body fixture not found on classpath: " + resourcePath
                        + " — create the file or fix the bodyFixtureKey in route-role-matrix.csv");
            }
            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8).trim();
            if (json.isBlank()) {
                throw new IllegalArgumentException("Body fixture is empty: " + resourcePath);
            }
            return json;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read body fixture: " + resourcePath, e);
        }
    }

    // ── parsing ──────────────────────────────────────────────────────────────

    private static List<RouteRoleMatrixRow> parse(String resourcePath) {
        try (InputStream is = RouteRoleMatrixLoader.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalStateException(
                        "route-role-matrix.csv not found on classpath at: " + resourcePath);
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8))) {

                List<RouteRoleMatrixRow> result = new ArrayList<>();
                String headerLine = reader.readLine();
                if (headerLine == null) {
                    throw new IllegalStateException(CSV_PATH + " is empty");
                }
                // validate header columns exist (don't enforce exact names — they describe themselves)
                String[] headers = headerLine.split(",", -1);
                if (headers.length != EXPECTED_COLUMN_COUNT) {
                    throw new IllegalStateException(
                            CSV_PATH + " header has " + headers.length + " columns, expected "
                            + EXPECTED_COLUMN_COUNT);
                }

                String line;
                int lineNum = 1; // header was line 1
                while ((line = reader.readLine()) != null) {
                    lineNum++;
                    if (line.isBlank()) continue;
                    result.add(parseRow(line, lineNum));
                }
                if (result.isEmpty()) {
                    throw new IllegalStateException(CSV_PATH + " contains no data rows");
                }
                return result;
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse " + CSV_PATH, e);
        }
    }

    private static RouteRoleMatrixRow parseRow(String line, int lineNum) {
        String[] cols = line.split(",", -1);
        if (cols.length != EXPECTED_COLUMN_COUNT) {
            throw new IllegalArgumentException(
                    CSV_PATH + " line " + lineNum + " has " + cols.length + " columns, expected "
                    + EXPECTED_COLUMN_COUNT + ": [" + line + "]");
        }

        String method       = requireNonBlank(cols[0].trim(), "method",       lineNum);
        String pathTemplate = requireNonBlank(cols[1].trim(), "pathTemplate", lineNum);
        boolean requiresBody = parseBoolean(cols[2].trim(), "requiresBody", lineNum);
        String bodyFixtureKey = cols[3].trim(); // blank is valid when requiresBody=false

        if (requiresBody && bodyFixtureKey.isBlank()) {
            throw new IllegalArgumentException(
                    CSV_PATH + " line " + lineNum + ": requiresBody=true but bodyFixtureKey is blank");
        }

        int expectedAnonymous   = parseInt(cols[4].trim(), "expectedAnonymous",   lineNum);
        int expectedManager     = parseInt(cols[5].trim(), "expectedManager",     lineNum);
        int expectedDispatcher  = parseInt(cols[6].trim(), "expectedDispatcher",  lineNum);
        int expectedTechnician  = parseInt(cols[7].trim(), "expectedTechnician",  lineNum);
        int expectedCustomer    = parseInt(cols[8].trim(), "expectedCustomer",    lineNum);

        return new RouteRoleMatrixRow(
                method, pathTemplate, requiresBody, bodyFixtureKey,
                expectedAnonymous, expectedManager, expectedDispatcher,
                expectedTechnician, expectedCustomer);
    }

    private static String requireNonBlank(String value, String column, int lineNum) {
        if (value.isBlank()) {
            throw new IllegalArgumentException(
                    CSV_PATH + " line " + lineNum + ": column '" + column + "' must not be blank");
        }
        return value;
    }

    private static boolean parseBoolean(String value, String column, int lineNum) {
        if ("true".equalsIgnoreCase(value)) return true;
        if ("false".equalsIgnoreCase(value)) return false;
        throw new IllegalArgumentException(
                CSV_PATH + " line " + lineNum + ": column '" + column
                + "' must be 'true' or 'false', got: '" + value + "'");
    }

    private static int parseInt(String value, String column, int lineNum) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    CSV_PATH + " line " + lineNum + ": column '" + column
                    + "' is not a valid integer: '" + value + "'", e);
        }
    }
}
