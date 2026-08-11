package com.fsm.keystone.fixtures;

import java.time.LocalDateTime;

/**
 * Deterministic fixed timestamp shared by all fixture builders.
 *
 * <p>Use {@link #NOW} for any fixture field that needs a LocalDateTime default.
 * Derived timestamps (e.g. +1 hour) are documented on each field so ordering
 * and duration assertions are reproducible regardless of wall-clock time.</p>
 *
 * <p>Note: entities that assign timestamps in {@code @PrePersist} callbacks
 * (AppUser, Customer, WorkOrder, StatusHistory, PartUsage) will overwrite
 * these values on persist. Integration test assertions must compare ranges or
 * ignore those fields; unit assertions may use these constants directly.</p>
 */
public final class FixtureClock {

    /** Fixed reference point: 2026-01-15 09:00:00. */
    public static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 15, 9, 0, 0);

    /** One hour after NOW — used for scheduledEnd / endTime defaults. */
    public static final LocalDateTime PLUS_ONE_HOUR = NOW.plusHours(1);

    /** Eight hours after NOW — used for scheduledEnd on full-day jobs. */
    public static final LocalDateTime PLUS_EIGHT_HOURS = NOW.plusHours(8);

    private FixtureClock() {}
}
