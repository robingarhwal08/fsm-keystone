package com.fsm.keystone.dto;
import com.fsm.keystone.enums.WorkOrderStatus;
public record StatusUpdateRequest(WorkOrderStatus status,
                                  Long changedByUserId,
                                  String comment) {}