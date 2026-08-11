package com.fsm.keystone.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fsm.keystone.dto.ApiErrorResponse;
import com.fsm.keystone.exception.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import java.util.UUID;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    private final AuthenticationProvider authenticationProvider;

    /** Jackson mapper for serialising {@link ApiErrorResponse} inside the filter chain. */
    private final ObjectMapper objectMapper;

    @Value("${app.cors.allowed-origin}")
    private String allowedOrigin;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth

                        // Actuator health probes — must be reachable without auth so that
                        // container orchestration healthchecks succeed before any JWT is issued.
                        // /actuator/prometheus stays authenticated (falls through to anyRequest).
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()

                        // Public APIs
                        .requestMatchers("/api/auth/**").permitAll()

                        // User Management — currently permitAll (filter-chain gap).
                        // Role enforcement is handled per-method via @PreAuthorize on UserController.
                        // Removing this permitAll is tracked in the authorization remediation epic; see ADR-0001.
                        .requestMatchers("/api/users/**").permitAll()

                        // Customer APIs
                        .requestMatchers("/api/customers/**")
                        .permitAll()

                        // Time Logs APIs
                        .requestMatchers("/api/time-logs/**")
                        .permitAll()

                        // Part Usage APIs
                        .requestMatchers("/api/part-usage/**")
                        .permitAll()

                        // All other APIs require authentication
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        // Unauthenticated requests that hit an authenticated endpoint.
                        .authenticationEntryPoint((request, response, authException) -> {
                            if (response.isCommitted()) return;
                            String correlationId = resolveCorrelationId();
                            ApiErrorResponse body = ApiErrorResponse.of(
                                    HttpStatus.UNAUTHORIZED.value(),
                                    ErrorCode.AUTHENTICATION_FAILED.name(),
                                    "Authentication is required to access this resource",
                                    correlationId, request.getRequestURI(), List.of());
                            writeError(response, HttpStatus.UNAUTHORIZED, body, correlationId);
                        })
                        // Authenticated requests that lack the required role.
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            if (response.isCommitted()) return;
                            String correlationId = resolveCorrelationId();
                            ApiErrorResponse body = ApiErrorResponse.of(
                                    HttpStatus.FORBIDDEN.value(),
                                    "ACCESS_DENIED",
                                    "You do not have permission to perform this action",
                                    correlationId, request.getRequestURI(), List.of());
                            writeError(response, HttpStatus.FORBIDDEN, body, correlationId);
                        })
                )
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(
                        jwtAuthFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration cfg = new CorsConfiguration();

        cfg.setAllowedOrigins(List.of(allowedOrigin));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        cfg.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);

        return source;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void writeError(HttpServletResponse response, HttpStatus status,
                            ApiErrorResponse body, String correlationId) throws Exception {
        response.setStatus(status.value());
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("X-Correlation-Id", correlationId);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    private static String resolveCorrelationId() {
        String id = MDC.get("correlationId");
        return (id != null && !id.isBlank()) ? id : UUID.randomUUID().toString();
    }
}
