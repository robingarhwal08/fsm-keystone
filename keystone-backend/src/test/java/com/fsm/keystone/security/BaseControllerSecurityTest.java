package com.fsm.keystone.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.Tag;
import org.mockito.Answers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

/**
 * Abstract base for all @WebMvcTest security baseline slices.
 *
 * <p>Provides:</p>
 * <ul>
 *   <li>Common {@link MockBean}s: JwtAuthenticationFilter (chains through) and AuthenticationProvider</li>
 *   <li>Helper to build a {@link MockHttpServletRequestBuilder} from method/path/role</li>
 *   <li>Static request bodies for endpoints that require @Valid input to return 200</li>
 * </ul>
 *
 * <p>Subclasses must call {@link #setupJwtFilterPassthrough()} from their own
 * {@code @BeforeEach} method (or override it), and must declare their own
 * service {@link MockBean}s since they differ per controller.</p>
 */
@Tag("slice")
public abstract class BaseControllerSecurityTest {

    /** Path variable placeholder replacement map used to resolve path templates. */
    private static final Map<String, String> PATH_VAR_VALUES = Map.of(
            "{id}", "1",
            "{customerId}", "1"
    );

    /**
     * Minimal valid JSON bodies for endpoints annotated with {@code @Valid @RequestBody}.
     * Keyed by "METHOD PATH" (e.g. "POST /api/auth/signup").
     */
    static final Map<String, String> VALID_BODIES = Map.of(
            "POST /api/auth/signup",
            "{\"fullName\":\"Test User\",\"email\":\"test@example.test\",\"password\":\"pass1234\",\"role\":\"CUSTOMER\"}",
            "POST /api/auth/login",
            "{\"email\":\"test@example.test\",\"password\":\"pass1234\"}",
            "POST /api/time-logs",
            "{\"workOrderId\":1,\"technicianId\":1,"
                    + "\"startTime\":\"2025-01-01T08:00:00\",\"endTime\":\"2025-01-01T10:00:00\"}",
            "PUT /api/users/{id}",
            "{\"fullName\":\"Test User\",\"role\":\"MANAGER\",\"active\":true}"
    );

    @Autowired
    protected MockMvc mockMvc;

    @MockBean
    protected JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    protected AuthenticationProvider authenticationProvider;

    /**
     * Configures the mocked {@link JwtAuthenticationFilter} to simply pass the request
     * through to the next filter in the chain, without attempting JWT validation.
     *
     * <p>This must be called in each concrete test's {@code @BeforeEach}.</p>
     */
    protected void setupJwtFilterPassthrough() throws Exception {
        doAnswer(invocation -> {
            ServletRequest req = invocation.getArgument(0);
            ServletResponse resp = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(req, resp);
            return null;
        }).when(jwtAuthenticationFilter)
                .doFilter(any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));
    }

    /**
     * Builds a {@link MockHttpServletRequestBuilder} for the given HTTP method, path template,
     * and actor role.
     *
     * <ul>
     *   <li>Path template variables ({@code {id}}, {@code {customerId}}) are replaced with "1".</li>
     *   <li>POST/PUT/PATCH requests receive {@code Content-Type: application/json} and an
     *       appropriate JSON body (valid where @Valid is present, {@code {}} otherwise).</li>
     *   <li>When {@code role} is {@code "ANONYMOUS"}, no authentication post-processor is added.</li>
     * </ul>
     */
    protected MockHttpServletRequestBuilder buildRequest(String method, String path, String role) {
        String resolvedPath = resolvePathVars(path);
        String key = method + " " + path; // use template path for body lookup
        String body = VALID_BODIES.getOrDefault(key, "{}");

        MockHttpServletRequestBuilder builder = switch (method.toUpperCase()) {
            case "GET"    -> MockMvcRequestBuilders.get(resolvedPath);
            case "POST"   -> MockMvcRequestBuilders.post(resolvedPath)
                    .contentType(MediaType.APPLICATION_JSON).content(body);
            case "PUT"    -> MockMvcRequestBuilders.put(resolvedPath)
                    .contentType(MediaType.APPLICATION_JSON).content(body);
            case "PATCH"  -> MockMvcRequestBuilders.patch(resolvedPath)
                    .contentType(MediaType.APPLICATION_JSON).content(body);
            case "DELETE" -> MockMvcRequestBuilders.delete(resolvedPath);
            default -> throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        };

        if (!"ANONYMOUS".equalsIgnoreCase(role)) {
            builder = builder.with(user("test@example.test").roles(role));
        }
        return builder;
    }

    /** Replaces all known path variable placeholders with the value "1". */
    private static String resolvePathVars(String path) {
        String resolved = path;
        for (Map.Entry<String, String> entry : PATH_VAR_VALUES.entrySet()) {
            resolved = resolved.replace(entry.getKey(), entry.getValue());
        }
        return resolved;
    }
}
