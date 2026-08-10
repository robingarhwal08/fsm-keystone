package com.fsm.keystone.security;

import com.fsm.keystone.controller.AuthController;
import com.fsm.keystone.controller.CustomerController;
import com.fsm.keystone.controller.DashboardController;
import com.fsm.keystone.controller.PartController;
import com.fsm.keystone.controller.PartUsageController;
import com.fsm.keystone.controller.SiteController;
import com.fsm.keystone.controller.TimeLogController;
import com.fsm.keystone.controller.UserController;
import com.fsm.keystone.controller.WorkOrderController;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Endpoint discovery guard — fails with a descriptive message if any handler in
 * {@code com.fsm.keystone.controller} is absent from {@code authorization-matrix.csv}.
 *
 * <p>This test uses only Java reflection (no Spring context) so it runs as a fast
 * unit test. It scans all known controller classes, extracts their HTTP handler
 * methods, and compares the discovered set against the keys loaded from the CSV.</p>
 *
 * <p>All un-matrixed handlers are reported in a SINGLE failure message so the
 * developer can update the CSV in one pass.</p>
 *
 * <p>Side-effect: regenerates {@code docs/baseline/authorization-matrix.md} and
 * {@code docs/baseline/authorization-matrix.json} from the CSV after the guard
 * check passes.</p>
 */
class EndpointDiscoveryGuardTest {

    /** All controller classes that must be covered by the matrix. */
    private static final List<Class<?>> CONTROLLERS = List.of(
            AuthController.class,
            CustomerController.class,
            DashboardController.class,
            PartController.class,
            PartUsageController.class,
            SiteController.class,
            TimeLogController.class,
            UserController.class,
            WorkOrderController.class
    );

    // ─── Guard test ──────────────────────────────────────────────────────────

    @Test
    void allHandlersAreInMatrix() {
        Set<String> matrixKeys = MatrixLoader.stream()
                .map(MatrixRow::matrixKey)
                .collect(Collectors.toSet());

        List<String> missing = new ArrayList<>();

        for (Class<?> controller : CONTROLLERS) {
            String basePath = extractBasePath(controller);
            for (Method method : controller.getDeclaredMethods()) {
                HttpHandlerInfo info = extractHandlerInfo(method, basePath);
                if (info == null) continue; // not a handler method
                String key = info.httpMethod() + " " + info.path();
                if (!matrixKeys.contains(key)) {
                    missing.add(String.format("  %-8s %-55s [%s.%s()]",
                            info.httpMethod(), info.path(),
                            controller.getSimpleName(), method.getName()));
                }
            }
        }

        if (!missing.isEmpty()) {
            fail("The following controller handlers are MISSING from authorization-matrix.csv.\n"
                    + "Add a row for each endpoint, then re-run this test:\n\n"
                    + String.join("\n", missing) + "\n");
        }
    }

    // ─── Artifact generation ─────────────────────────────────────────────────

    @Test
    void generateMatrixArtifacts() throws IOException {
        List<MatrixRow> rows = MatrixLoader.rows();
        Path baselineDir = resolveBaselineDir();
        Files.createDirectories(baselineDir);
        writeMarkdown(rows, baselineDir.resolve("authorization-matrix.md"));
        writeJson(rows, baselineDir.resolve("authorization-matrix.json"));
    }

    // ─── Reflection helpers ──────────────────────────────────────────────────

    private static String extractBasePath(Class<?> controller) {
        RequestMapping rm = controller.getAnnotation(RequestMapping.class);
        if (rm != null && rm.value().length > 0) return rm.value()[0];
        return "";
    }

    private record HttpHandlerInfo(String httpMethod, String path) {}

    private static HttpHandlerInfo extractHandlerInfo(Method method, String basePath) {
        if (method.isAnnotationPresent(GetMapping.class)) {
            return new HttpHandlerInfo("GET", joinPath(basePath, getFirst(method.getAnnotation(GetMapping.class).value())));
        }
        if (method.isAnnotationPresent(PostMapping.class)) {
            return new HttpHandlerInfo("POST", joinPath(basePath, getFirst(method.getAnnotation(PostMapping.class).value())));
        }
        if (method.isAnnotationPresent(PutMapping.class)) {
            return new HttpHandlerInfo("PUT", joinPath(basePath, getFirst(method.getAnnotation(PutMapping.class).value())));
        }
        if (method.isAnnotationPresent(PatchMapping.class)) {
            return new HttpHandlerInfo("PATCH", joinPath(basePath, getFirst(method.getAnnotation(PatchMapping.class).value())));
        }
        if (method.isAnnotationPresent(DeleteMapping.class)) {
            return new HttpHandlerInfo("DELETE", joinPath(basePath, getFirst(method.getAnnotation(DeleteMapping.class).value())));
        }
        if (method.isAnnotationPresent(RequestMapping.class)) {
            RequestMapping rm = method.getAnnotation(RequestMapping.class);
            if (rm.method().length > 0) {
                return new HttpHandlerInfo(
                        rm.method()[0].name(),
                        joinPath(basePath, getFirst(rm.value())));
            }
        }
        return null;
    }

    private static String getFirst(String[] values) {
        return values.length > 0 ? values[0] : "";
    }

    private static String joinPath(String base, String sub) {
        if (sub == null || sub.isEmpty()) return base;
        if (base.endsWith("/") || sub.startsWith("/")) return base + sub;
        return base + "/" + sub;
    }

    // ─── Markdown generation ─────────────────────────────────────────────────

    private static void writeMarkdown(List<MatrixRow> rows, Path output) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("# Authorization Baseline Matrix\n\n");
        sb.append("_Generated from `authorization-matrix.csv`. Do not edit by hand._\n\n");
        sb.append("> **Generated:** ").append(Instant.now()).append("\n\n");

        sb.append("## Route × Role Observed Status Codes\n\n");
        sb.append("| Controller | Method | Path | Filter Chain Rule | @PreAuthorize | ANON | MGR | DISP | TECH | CUST |\n");
        sb.append("|---|---|---|---|---|---|---|---|---|---|\n");
        for (MatrixRow r : rows) {
            String preAuth = r.isAnnotationCommentedOut()
                    ? "⚠ **COMMENTED OUT**"
                    : (r.hasPreAuthorize() ? "`" + r.preAuthorizeExpr() + "`" : "—");
            sb.append("| ").append(r.controller())
              .append(" | ").append(r.method())
              .append(" | `").append(r.path()).append("`")
              .append(" | ").append(r.filterChainRule())
              .append(" | ").append(preAuth)
              .append(" | ").append(r.expectedStatusAnon())
              .append(" | ").append(r.expectedStatusManager())
              .append(" | ").append(r.expectedStatusDispatcher())
              .append(" | ").append(r.expectedStatusTechnician())
              .append(" | ").append(r.expectedStatusCustomer())
              .append(" |\n");
        }

        sb.append("\n## Key Findings\n\n");
        sb.append("### Endpoints Accessible to Anonymous Users (Blast-Radius List for Default-Deny)\n\n");
        sb.append("The following endpoints return HTTP 200 for unauthenticated callers.\n");
        sb.append("The SPA reaches these paths without an Authorization header via:\n");
        sb.append("`frontend/src/services/commonService.js` + `frontend/src/api/axiosConfig.js`\n");
        sb.append("(axios attaches `Bearer <token>` only when `localStorage.getItem(\"token\")` is non-null).\n\n");
        sb.append("| Method | Path | Controller | PreAuthorize | SPA Function |\n");
        sb.append("|---|---|---|---|---|\n");

        // Known SPA anonymous calls (derived from commonService.js / axiosConfig.js analysis)
        List<String[]> spaCalls = List.of(
            new String[]{"POST", "/api/auth/signup",        "AuthController",     "—",                   "signup()"},
            new String[]{"POST", "/api/auth/login",         "AuthController",     "—",                   "login()"},
            new String[]{"GET",  "/api/users",              "UserController",     "—",                   "getUsers()"},
            new String[]{"GET",  "/api/users/technicians",  "UserController",     "—",                   "getTechnicians()"},
            new String[]{"PUT",  "/api/users/{id}",         "UserController",     "—",                   "updateUser()"},
            new String[]{"DELETE","/api/users/{id}",        "UserController",     "—",                   "deleteUser()"},
            new String[]{"GET",  "/api/customers",          "CustomerController", "—",                   "getCustomers()"},
            new String[]{"DELETE","/api/customers/{id}",   "CustomerController", "⚠ COMMENTED OUT",     "deleteCustomer()"},
            new String[]{"POST", "/api/time-logs",          "TimeLogController",  "—",                   "createTimeLog()"},
            new String[]{"POST", "/api/part-usage",         "PartUsageController","—",                   "createPartUsage()"}
        );
        for (String[] call : spaCalls) {
            sb.append("| ").append(call[0]).append(" | `").append(call[1]).append("` | ")
              .append(call[2]).append(" | ").append(call[3]).append(" | `").append(call[4]).append("` |\n");
        }

        sb.append("\n### CustomerController.delete — Commented-Out Annotation\n\n");
        sb.append("```java\n// @PreAuthorize(\"hasRole('MANAGER')\")\npublic void delete(@PathVariable Long id) { ... }\n```\n");
        sb.append("This endpoint is **publicly writable** by any actor including anonymous. ");
        sb.append("The commented-out annotation must be uncommented in the default-deny phase.\n");

        sb.append("\n### Unbounded List Endpoints\n\n");
        sb.append("The following list endpoints return the entire table with no pagination filter.\n");
        sb.append("Payload sizes are recorded in `payload-baseline.md`.\n\n");
        sb.append("| Endpoint | findAll() call | Client-side filter? |\n");
        sb.append("|---|---|---|\n");
        sb.append("| GET /api/work-orders | `workRepo.findAll()` | YES — Dashboard.jsx, CustomerRequests.jsx |\n");
        sb.append("| GET /api/sites       | `siteRepo.findAll()` | YES — CustomerRequests.jsx |\n");
        sb.append("| GET /api/users       | `userRepo.findAll()` | NO  |\n");
        sb.append("| GET /api/customers   | `customerRepo.findAll()` | NO |\n");

        Files.writeString(output, sb.toString(), StandardCharsets.UTF_8);
    }

    // ─── JSON generation ─────────────────────────────────────────────────────

    private static void writeJson(List<MatrixRow> rows, Path output) throws IOException {
        List<Map<String, Object>> list = new ArrayList<>();
        for (MatrixRow r : rows) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("controller", r.controller());
            entry.put("method", r.method());
            entry.put("path", r.path());
            entry.put("filterChainRule", r.filterChainRule());
            entry.put("hasPreAuthorize", r.hasPreAuthorize());
            entry.put("preAuthorizeExpr", r.preAuthorizeExpr());
            entry.put("annotationCommentedOut", r.isAnnotationCommentedOut());
            Map<String, Integer> statuses = new LinkedHashMap<>();
            statuses.put("ANONYMOUS",  r.expectedStatusAnon());
            statuses.put("MANAGER",    r.expectedStatusManager());
            statuses.put("DISPATCHER", r.expectedStatusDispatcher());
            statuses.put("TECHNICIAN", r.expectedStatusTechnician());
            statuses.put("CUSTOMER",   r.expectedStatusCustomer());
            entry.put("observedStatusByRole", statuses);
            list.add(entry);
        }
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("generatedAt", Instant.now().toString());
        root.put("totalEndpoints", rows.size());
        root.put("endpoints", list);

        ObjectMapper mapper = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT);
        Files.writeString(output, mapper.writeValueAsString(root), StandardCharsets.UTF_8);
    }

    // ─── Path resolution ─────────────────────────────────────────────────────

    /**
     * Resolves {@code docs/baseline/} relative to the Maven project root.
     * Tests run with working directory = {@code keystone-backend/}, so we go up one level.
     */
    private static Path resolveBaselineDir() {
        Path cwd = Paths.get("").toAbsolutePath();
        // If running from keystone-backend/, the parent is the repo root.
        Path candidate = cwd.resolveSibling("docs/baseline");
        if (cwd.getFileName() != null
                && cwd.getFileName().toString().equals("keystone-backend")) {
            return candidate;
        }
        // Fallback: look for docs/baseline from cwd itself
        return cwd.resolve("docs/baseline");
    }
}
