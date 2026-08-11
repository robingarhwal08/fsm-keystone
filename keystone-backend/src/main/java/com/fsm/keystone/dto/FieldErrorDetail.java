package com.fsm.keystone.dto;

/**
 * Per-field validation error detail returned inside {@link ApiErrorResponse#fieldErrors()}.
 *
 * <p>The {@code message} carries only the constraint message text — the rejected value
 * is never included to avoid echoing sensitive input back to the caller.</p>
 */
public record FieldErrorDetail(String field, String message) {}
