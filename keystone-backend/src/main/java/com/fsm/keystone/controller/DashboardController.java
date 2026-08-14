package com.fsm.keystone.controller;

import com.fsm.keystone.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping({"/dashboard/summary", "/reports/summary"})
    @PreAuthorize("hasAnyRole('MANAGER','DISPATCHER')")
    public Map<String, Object> summary() {
        return dashboardService.getDashboardSummary();
    }
}
