package com.fsm.keystone.enums;

public enum WorkOrderStatus {
    CREATED,
    NEW,
    ASSIGNED,
    IN_PROGRESS,
    ON_HOLD,
    COMPLETED,
    CLOSED,
    CANCELLED;

    public WorkOrderStatus canonical() {
        return this == CREATED ? NEW : this;
    }

    public boolean isTerminal() {
        WorkOrderStatus c = canonical();
        return c == CLOSED || c == CANCELLED;
    }

    public boolean isOpen() {
        return !isTerminal();
    }
}
