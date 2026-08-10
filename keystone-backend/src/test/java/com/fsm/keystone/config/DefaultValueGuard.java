package com.fsm.keystone.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility that detects {@code ${VARIABLE:literal}} fallback patterns in a
 * {@link Properties} object and reports violations for variables that are
 * declared as required-no-default in the manifest.
 *
 * <p><strong>Phase 0 usage:</strong> the caller asserts that the known violation
 * count equals the expected baseline count (2). This lets the test pass today while
 * preventing <em>new</em> fallbacks from being silently added. When fallbacks are
 * removed in Phase 2, update the assertion to {@code == 0}.</p>
 *
 * <p><strong>Safety:</strong> violation messages intentionally omit the literal
 * default value so this class can never print a secret to logs or test output.</p>
 */
public final class DefaultValueGuard {

    /** Regex that matches {@code ${VARNAME:any-literal}} — the unsafe pattern. */
    private static final Pattern LITERAL_FALLBACK = Pattern.compile(
            "\\$\\{([A-Za-z0-9_]+):([^}]+)\\}");

    private DefaultValueGuard() {}

    /**
     * Scans all entries in {@code props} for the {@code ${VARNAME:literal}} pattern.
     * Only entries whose environment variable name appears in {@code requiredNoDefault}
     * produce a violation.
     *
     * @param props            the properties to inspect
     * @param requiredNoDefault set of environment variable names that must NOT have
     *                         a literal fallback (e.g. {@code APP_JWT_SECRET})
     * @return a list of human-readable violation messages; empty means no violations
     */
    public static List<String> findViolations(Properties props,
                                              Set<String> requiredNoDefault) {
        List<String> violations = new ArrayList<>();
        for (String propertyKey : props.stringPropertyNames()) {
            String value = props.getProperty(propertyKey);
            if (value == null) continue;
            Matcher m = LITERAL_FALLBACK.matcher(value);
            while (m.find()) {
                String envVarName = m.group(1);
                // group(2) is the literal — NEVER include it in the message
                if (requiredNoDefault.contains(envVarName)) {
                    violations.add(String.format(
                            "Property '%s' maps environment variable '%s' with a literal "
                                    + "fallback at position %d–%d. "
                                    + "This variable is declared required-no-default in the "
                                    + "required-properties manifest.",
                            propertyKey, envVarName, m.start(), m.end()));
                }
            }
        }
        return violations;
    }

    /**
     * Loads a {@link Properties} object from a classpath resource path.
     *
     * @param resourcePath classpath-relative path, e.g. {@code /guard/violating.properties}
     * @return loaded properties
     * @throws IOException if the resource cannot be read
     */
    public static Properties loadFromClasspath(String resourcePath) throws IOException {
        InputStream is = DefaultValueGuard.class.getResourceAsStream(resourcePath);
        if (is == null) {
            throw new IOException("Classpath resource not found: " + resourcePath);
        }
        Properties props = new Properties();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8))) {
            props.load(reader);
        }
        return props;
    }

    /**
     * Loads a {@link Properties} object from a filesystem path (relative to the
     * Maven module directory — i.e. the working directory when Maven runs tests).
     *
     * @param relativePath path relative to the Maven module (e.g.
     *                     {@code src/main/resources/application.properties})
     * @return loaded properties
     * @throws IOException if the file cannot be read
     */
    public static Properties loadFromFile(String relativePath) throws IOException {
        java.nio.file.Path path = java.nio.file.Paths.get(relativePath);
        Properties props = new Properties();
        try (InputStream is = java.nio.file.Files.newInputStream(path)) {
            props.load(is);
        }
        return props;
    }

    /**
     * Formats a violation report suitable for a test failure message.
     * Does not echo any literal values.
     *
     * @param violations the list returned by {@link #findViolations}
     * @return formatted multi-line string
     */
    public static String formatReport(List<String> violations) {
        if (violations.isEmpty()) {
            return "No violations found.";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Default-value guard found ").append(violations.size())
          .append(" violation(s):\n");
        for (int i = 0; i < violations.size(); i++) {
            sb.append("  [").append(i + 1).append("] ").append(violations.get(i)).append('\n');
        }
        sb.append("\nTo resolve: remove the literal fallback from the property value, ")
          .append("ensure the environment supplies the variable, and update the known-violation ")
          .append("count in DefaultValueGuardTest.");
        return sb.toString();
    }
}
