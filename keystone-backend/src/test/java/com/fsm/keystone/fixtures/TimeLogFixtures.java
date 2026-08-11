package com.fsm.keystone.fixtures;

import com.fsm.keystone.entity.AppUser;
import com.fsm.keystone.entity.TimeLog;
import com.fsm.keystone.entity.WorkOrder;

import java.math.BigDecimal;

/**
 * Object-mother factory for {@link TimeLog} entities.
 *
 * <p>Minimum valid shape: {@code workOrder} and {@code technician} are required
 * ({@code optional = false} on FKs). {@code hoursSpent} is computed server-side
 * from {@code startTime} and {@code endTime} in production; the fixture pre-populates
 * the computed value so unit tests can assert it without executing service logic.</p>
 */
public final class TimeLogFixtures {

    private TimeLogFixtures() {}

    /** Returns a builder for a 1-hour time log starting at {@link FixtureClock#NOW}. */
    public static TimeLog.TimeLogBuilder aTimeLog(WorkOrder workOrder, AppUser technician) {
        return TimeLog.builder()
                .startTime(FixtureClock.NOW)
                .endTime(FixtureClock.PLUS_ONE_HOUR)
                .hoursSpent(new BigDecimal("1.00"))
                .workDescription("General maintenance work")
                .workOrder(workOrder)
                .technician(technician);
    }

    /** Returns a builder for an 8-hour full-day time log starting at {@link FixtureClock#NOW}. */
    public static TimeLog.TimeLogBuilder aFullDayTimeLog(WorkOrder workOrder, AppUser technician) {
        return TimeLog.builder()
                .startTime(FixtureClock.NOW)
                .endTime(FixtureClock.PLUS_EIGHT_HOURS)
                .hoursSpent(new BigDecimal("8.00"))
                .workDescription("Full day on-site service")
                .workOrder(workOrder)
                .technician(technician);
    }
}
