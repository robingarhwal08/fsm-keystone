package com.fsm.keystone.exception;

import com.fsm.keystone.dto.ApiErrorResponse;
import com.fsm.keystone.dto.FieldErrorDetail;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Single exception-translation component for the fsm-keystone API.
 *
 * <p>Every handled branch returns an {@link ApiErrorResponse} with the {@code X-Correlation-Id}
 * response header set to the same correlationId value as the body. The correlationId is sourced
 * from MDC key {@code correlationId} (populated by the correlation filter in WO-023) and falls
 * back to a freshly generated UUID when absent.</p>
 *
 * <p>5xx branches log at ERROR with the full throwable so stacks reach the log pipeline.
 * 4xx branches log at WARN with structured key/value context only — no stack traces.</p>
 *
 * <p>This is the only {@code @ControllerAdvice} in the backend. Spring Security
 * filter-chain-level 401/403 responses are handled by the {@code AuthenticationEntryPoint}
 * and {@code AccessDeniedHandler} beans configured in {@code SecurityConfig}.</p>
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final int MAX_FIELD_ERRORS = 50;
    private static final Set<String> SENSITIVE_FIELDS = Set.of("password", "secret", "token");
    private static final String MASKED_MESSAGE = "Value must satisfy constraints";

    // ── ApiException hierarchy (WO-012) ──────────────────────────────────────

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorResponse> handleApiException(
            ApiException ex, HttpServletRequest request) {
        String correlationId = resolveCorrelationId();
        HttpStatus status = ex.getStatus();
        if (status.is5xxServerError()) {
            log.error("ApiException [{} {}] method={} path={}",
                    status.value(), ex.getCode(), request.getMethod(), request.getRequestURI(), ex);
        } else {
            log.warn("ApiException [{} {}] method={} path={} message={}",
                    status.value(), ex.getCode(), request.getMethod(), request.getRequestURI(),
                    ex.getMessage());
        }
        return respond(
                ApiErrorResponse.of(status.value(), ex.getCode().name(), ex.getMessage(),
                        correlationId, request.getRequestURI(), List.of()),
                status, correlationId);
    }

    // ── Bean Validation 400 ───────────────────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        String correlationId = resolveCorrelationId();
        int totalErrors = ex.getBindingResult().getFieldErrors().size();
        List<FieldErrorDetail> fieldErrors = buildFieldErrors(ex.getBindingResult().getFieldErrors());
        String message = totalErrors > MAX_FIELD_ERRORS
                ? "Validation failed (" + MAX_FIELD_ERRORS + " of " + totalErrors + " errors shown)"
                : "Validation failed";
        log.warn("Validation failed [400 {}] method={} path={} errorCount={}",
                ErrorCode.INVALID_REQUEST_DATA.name(), request.getMethod(), request.getRequestURI(),
                totalErrors);
        return respond(
                ApiErrorResponse.of(400, ErrorCode.INVALID_REQUEST_DATA.name(), message,
                        correlationId, request.getRequestURI(), fieldErrors),
                HttpStatus.BAD_REQUEST, correlationId);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {
        String correlationId = resolveCorrelationId();
        List<FieldErrorDetail> fieldErrors = ex.getConstraintViolations().stream()
                .limit(MAX_FIELD_ERRORS)
                .map(v -> {
                    String rawPath = v.getPropertyPath().toString();
                    // Strip method-name prefix: "methodName.paramName" -> "paramName"
                    int dot = rawPath.lastIndexOf('.');
                    String field = dot >= 0 ? rawPath.substring(dot + 1) : rawPath;
                    String msg = isSensitiveField(field) ? MASKED_MESSAGE : v.getMessage();
                    return new FieldErrorDetail(field, msg);
                })
                .collect(Collectors.toList());
        log.warn("ConstraintViolation [400 {}] method={} path={} errorCount={}",
                ErrorCode.INVALID_REQUEST_DATA.name(), request.getMethod(), request.getRequestURI(),
                fieldErrors.size());
        return respond(
                ApiErrorResponse.of(400, ErrorCode.INVALID_REQUEST_DATA.name(), "Validation failed",
                        correlationId, request.getRequestURI(), fieldErrors),
                HttpStatus.BAD_REQUEST, correlationId);
    }

    // ── Malformed input 400 ───────────────────────────────────────────────────

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleMessageNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        String correlationId = resolveCorrelationId();
        // Sanitise: ex.getMessage() may echo the raw request payload — never reflect it.
        log.warn("Malformed request body [400 {}] method={} path={}",
                ErrorCode.INVALID_REQUEST_DATA.name(), request.getMethod(), request.getRequestURI());
        return respond(
                ApiErrorResponse.of(400, ErrorCode.INVALID_REQUEST_DATA.name(), "Malformed request body",
                        correlationId, request.getRequestURI(), List.of()),
                HttpStatus.BAD_REQUEST, correlationId);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String correlationId = resolveCorrelationId();
        String message;
        Class<?> requiredType = ex.getRequiredType();
        if (requiredType != null && requiredType.isEnum()) {
            // List permitted values; do NOT include the Java class name.
            String permitted = Arrays.stream(requiredType.getEnumConstants())
                    .map(Object::toString)
                    .collect(Collectors.joining(", "));
            message = "Invalid value for parameter '" + ex.getName() + "'. Permitted values: " + permitted;
        } else {
            message = "Invalid value for parameter '" + ex.getName() + "'";
        }
        log.warn("TypeMismatch [400 INVALID_ENUM_VALUE] method={} path={} param={}",
                request.getMethod(), request.getRequestURI(), ex.getName());
        return respond(
                ApiErrorResponse.of(400, "INVALID_ENUM_VALUE", message,
                        correlationId, request.getRequestURI(), List.of()),
                HttpStatus.BAD_REQUEST, correlationId);
    }

    // ── Not Found 404 ─────────────────────────────────────────────────────────

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoResourceFound(
            NoResourceFoundException ex, HttpServletRequest request) {
        String correlationId = resolveCorrelationId();
        log.warn("NoResourceFound [404 {}] method={} path={}",
                ErrorCode.RESOURCE_NOT_FOUND.name(), request.getMethod(), request.getRequestURI());
        return respond(
                ApiErrorResponse.of(404, ErrorCode.RESOURCE_NOT_FOUND.name(),
                        "The requested resource was not found",
                        correlationId, request.getRequestURI(), List.of()),
                HttpStatus.NOT_FOUND, correlationId);
    }

    // ── Method Not Allowed 405 ────────────────────────────────────────────────

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        String correlationId = resolveCorrelationId();
        log.warn("MethodNotSupported [405 METHOD_NOT_ALLOWED] method={} path={}",
                request.getMethod(), request.getRequestURI());
        return respond(
                ApiErrorResponse.of(405, "METHOD_NOT_ALLOWED",
                        "HTTP method '" + ex.getMethod() + "' is not supported for this endpoint",
                        correlationId, request.getRequestURI(), List.of()),
                HttpStatus.METHOD_NOT_ALLOWED, correlationId);
    }

    // ── Access Denied 403 / Unauthenticated 401 ───────────────────────────────

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        String correlationId = resolveCorrelationId();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAnonymous = auth == null
                || auth instanceof AnonymousAuthenticationToken
                || !auth.isAuthenticated();
        if (isAnonymous) {
            log.warn("AccessDenied (anonymous→401) [401 {}] method={} path={}",
                    ErrorCode.AUTHENTICATION_FAILED.name(), request.getMethod(), request.getRequestURI());
            return respond(
                    ApiErrorResponse.of(401, ErrorCode.AUTHENTICATION_FAILED.name(),
                            "Authentication is required to access this resource",
                            correlationId, request.getRequestURI(), List.of()),
                    HttpStatus.UNAUTHORIZED, correlationId);
        }
        log.warn("AccessDenied [403 ACCESS_DENIED] method={} path={}",
                request.getMethod(), request.getRequestURI());
        return respond(
                ApiErrorResponse.of(403, "ACCESS_DENIED",
                        "You do not have permission to perform this action",
                        correlationId, request.getRequestURI(), List.of()),
                HttpStatus.FORBIDDEN, correlationId);
    }

    // ── Spring Security AuthenticationException 401 ───────────────────────────

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthenticationException(
            AuthenticationException ex, HttpServletRequest request) {
        String correlationId = resolveCorrelationId();
        log.warn("AuthenticationException [401 {}] method={} path={} message={}",
                ErrorCode.AUTHENTICATION_FAILED.name(), request.getMethod(), request.getRequestURI(),
                ex.getMessage());
        return respond(
                ApiErrorResponse.of(401, ErrorCode.AUTHENTICATION_FAILED.name(),
                        "Authentication failed",
                        correlationId, request.getRequestURI(), List.of()),
                HttpStatus.UNAUTHORIZED, correlationId);
    }

    // ── Data Integrity Violation 409 ──────────────────────────────────────────

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        String correlationId = resolveCorrelationId();
        // ex.getMessage() may contain SQL constraint names — never include in response.
        log.warn("DataIntegrityViolation [409 DATA_INTEGRITY_VIOLATION] method={} path={}",
                request.getMethod(), request.getRequestURI());
        return respond(
                ApiErrorResponse.of(409, "DATA_INTEGRITY_VIOLATION",
                        "The request conflicts with an existing resource",
                        correlationId, request.getRequestURI(), List.of()),
                HttpStatus.CONFLICT, correlationId);
    }

    // ── Catch-all 500 ─────────────────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(
            Exception ex, HttpServletRequest request) {
        String correlationId = resolveCorrelationId();
        // Log with full stack trace so operators can locate the cause.
        log.error("Unexpected exception [500 INTERNAL_ERROR] method={} path={}",
                request.getMethod(), request.getRequestURI(), ex);
        // Response body must never contain class names, SQL, file paths or stack traces.
        return respond(
                ApiErrorResponse.of(500, "INTERNAL_ERROR", "An unexpected error occurred",
                        correlationId, request.getRequestURI(), List.of()),
                HttpStatus.INTERNAL_SERVER_ERROR, correlationId);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private String resolveCorrelationId() {
        String id = MDC.get("correlationId");
        return (id != null && !id.isBlank()) ? id : UUID.randomUUID().toString();
    }

    private <T> ResponseEntity<T> respond(T body, HttpStatus status, String correlationId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Correlation-Id", correlationId);
        return ResponseEntity.status(status).headers(headers).body(body);
    }

    private List<FieldErrorDetail> buildFieldErrors(List<FieldError> fieldErrors) {
        return fieldErrors.stream()
                .limit(MAX_FIELD_ERRORS)
                .map(fe -> {
                    String msg = isSensitiveField(fe.getField())
                            ? MASKED_MESSAGE
                            : fe.getDefaultMessage();
                    return new FieldErrorDetail(fe.getField(), msg);
                })
                .collect(Collectors.toList());
    }

    private boolean isSensitiveField(String fieldName) {
        if (fieldName == null) return false;
        String lower = fieldName.toLowerCase();
        return SENSITIVE_FIELDS.stream().anyMatch(lower::contains);
    }
}
