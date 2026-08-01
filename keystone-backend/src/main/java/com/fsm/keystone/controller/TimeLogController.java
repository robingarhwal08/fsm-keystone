package com.fsm.keystone.controller;

import com.fsm.keystone.dto.TimeLogRequest;
import com.fsm.keystone.entity.TimeLog;
import com.fsm.keystone.service.WorkOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/time-logs")
@RequiredArgsConstructor
public class TimeLogController {

    private final WorkOrderService workOrderService;

    @PostMapping
    public TimeLog createTimeLog(
            @Valid @RequestBody TimeLogRequest request
    ) {

        return workOrderService.addTimeLog(
                request.workOrderId(),
                request
        );
    }
}