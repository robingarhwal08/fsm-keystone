package com.fsm.keystone.dto;
import com.fsm.keystone.enums.Priority;
import java.time.LocalDateTime;
public record CreateWorkOrderRequest(String title,
                                     String description,
                                     Long customerId,
                                     Long siteId,
                                     Long createdByUserId,
                                     Long assignedTechnicianId,
                                     Priority priority,
                                     LocalDateTime scheduledStart,
                                     LocalDateTime scheduledEnd) {}
