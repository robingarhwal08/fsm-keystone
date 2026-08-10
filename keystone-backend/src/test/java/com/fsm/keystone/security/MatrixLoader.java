package com.fsm.keystone.security;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Parses {@code security/authorization-matrix.csv} from the test classpath and
 * exposes rows as {@link MatrixRow} instances.
 *
 * <p>The CSV must have the header:
 * {@code controller,method,path,filterChainRule,hasPreAuthorize,preAuthorizeExpr,
 * expectedStatusAnon,expectedStatusManager,expectedStatusDispatcher,
 * expectedStatusTechnician,expectedStatusCustomer}</p>
 *
 * <p>Loading is eager and cached; the CSV is small enough that re-parsing per
 * test run is cheap but the list is static so JVM-level caching happens
 * naturally via class initialization.</p>
 */
public final class MatrixLoader {

    private static final String CSV_PATH = "/security/authorization-matrix.csv";

    /** All rows loaded from the CSV, excluding the header line. */
    private static final List<MatrixRow> ALL_ROWS = loadAll();

    private MatrixLoader() {}

    /** Returns all rows in the matrix. */
    public static List<MatrixRow> rows() {
        return ALL_ROWS;
    }

    /** Returns a stream of all rows. */
    public static Stream<MatrixRow> stream() {
        return ALL_ROWS.stream();
    }

    /**
     * Returns rows whose {@code controller} column equals {@code controllerSimpleName}.
     *
     * @param controllerSimpleName e.g. {@code "WorkOrderController"}
     */
    public static Stream<MatrixRow> forController(String controllerSimpleName) {
        return ALL_ROWS.stream()
                .filter(r -> r.controller().equals(controllerSimpleName));
    }

    // ─── Private ────────────────────────────────────────────────────────────

    private static List<MatrixRow> loadAll() {
        InputStream is = MatrixLoader.class.getResourceAsStream(CSV_PATH);
        if (is == null) {
            throw new IllegalStateException(
                    "Cannot find authorization-matrix.csv at classpath:" + CSV_PATH);
        }
        List<MatrixRow> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            boolean firstLine = true;
            while ((line = reader.readLine()) != null) {
                if (firstLine) { firstLine = false; continue; } // skip header
                line = line.trim();
                if (line.isEmpty()) continue;
                rows.add(parseLine(line));
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load " + CSV_PATH, e);
        }
        return List.copyOf(rows);
    }

    /**
     * Parses a single CSV line, handling quoted fields that contain commas.
     *
     * <p>Expected 11 columns exactly.</p>
     */
    private static MatrixRow parseLine(String line) {
        String[] cols = splitCsv(line);
        if (cols.length != 11) {
            throw new IllegalArgumentException(
                    "Expected 11 CSV columns but got " + cols.length + " in: [" + line + "]");
        }
        return new MatrixRow(
                cols[0].trim(),                    // controller
                cols[1].trim(),                    // method
                cols[2].trim(),                    // path
                cols[3].trim(),                    // filterChainRule
                Boolean.parseBoolean(cols[4].trim()), // hasPreAuthorize
                cols[5].trim(),                    // preAuthorizeExpr
                Integer.parseInt(cols[6].trim()),  // expectedStatusAnon
                Integer.parseInt(cols[7].trim()),  // expectedStatusManager
                Integer.parseInt(cols[8].trim()),  // expectedStatusDispatcher
                Integer.parseInt(cols[9].trim()),  // expectedStatusTechnician
                Integer.parseInt(cols[10].trim())  // expectedStatusCustomer
        );
    }

    /** Splits a CSV line respecting double-quoted fields. */
    private static String[] splitCsv(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                tokens.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        tokens.add(current.toString());
        return tokens.toArray(new String[0]);
    }
}
