package com.fsm.keystone.controller;

import com.fsm.keystone.dto.TimeLogRequest;
import com.fsm.keystone.dto.TimeLogResponse;
import com.fsm.keystone.entity.TimeLog;
import com.fsm.keystone.service.WorkOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/time-logs")
@RequiredArgsConstructor
public class TimeLogController {

    private final WorkOrderService workOrderService;

    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER','DISPATCHER','TECHNICIAN')")
    public List<TimeLogResponse> all() {
        return workOrderService.listTimeLogs();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER','DISPATCHER','TECHNICIAN')")
    public TimeLog createTimeLog(
            @Valid @RequestBody TimeLogRequest request
    ) {
        return workOrderService.addTimeLog(
                request.workOrderId(),
                request
        );
    }
}
