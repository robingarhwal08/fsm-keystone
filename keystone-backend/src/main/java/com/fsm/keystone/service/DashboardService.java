package com.fsm.keystone.service;

import com.fsm.keystone.entity.WorkOrder;
import com.fsm.keystone.enums.Priority;
import com.fsm.keystone.enums.Role;
import com.fsm.keystone.enums.SlaStatus;
import com.fsm.keystone.enums.WorkOrderStatus;
import com.fsm.keystone.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final WorkOrderRepository workRepo;
    private final UserRepository userRepo;
    private final PartRepository partRepo;
    private final CustomerRepository customerRepo;
    private final SiteRepository siteRepo;

    public Map<String, Object> getDashboardSummary() {

        Map<String, Object> data = new LinkedHashMap<>();
        List<WorkOrder> all = workRepo.findAll();
        LocalDateTime now = LocalDateTime.now();

        long overdue = all.stream()
                .filter(w -> w.getSlaDueAt() != null && w.getStatus() != null && w.getStatus().isOpen())
                .filter(w -> !now.isBefore(w.getSlaDueAt()))
                .count();

        long slaTracked = all.stream()
                .filter(w -> w.getSlaDueAt() != null)
                .count();
        long slaOk = all.stream()
                .filter(w -> w.getSlaDueAt() != null && w.getSlaStatus() != SlaStatus.BREACHED)
                .count();
        double compliance = slaTracked == 0 ? 100.0 : (slaOk * 100.0 / slaTracked);

        data.put("totalCustomers",
                customerRepo.count());

        data.put("totalSites",
                siteRepo.count());

        data.put("totalWorkOrders",
                workRepo.count());

        data.put("createdWorkOrders",
                workRepo.countByStatus(WorkOrderStatus.CREATED)
                        + workRepo.countByStatus(WorkOrderStatus.NEW));

        data.put("assignedWorkOrders",
                workRepo.countByStatus(WorkOrderStatus.ASSIGNED));

        data.put("inProgressWorkOrders",
                workRepo.countByStatus(WorkOrderStatus.IN_PROGRESS));

        data.put("onHoldWorkOrders",
                workRepo.countByStatus(WorkOrderStatus.ON_HOLD));

        data.put("completedWorkOrders",
                workRepo.countByStatus(WorkOrderStatus.COMPLETED));

        data.put("closedWorkOrders",
                workRepo.countByStatus(WorkOrderStatus.CLOSED));

        data.put("cancelledWorkOrders",
                workRepo.countByStatus(WorkOrderStatus.CANCELLED));

        data.put("criticalWorkOrders",
                workRepo.countByPriority(Priority.CRITICAL));

        data.put("overdueWorkOrders", overdue);
        data.put("atRiskWorkOrders", workRepo.countBySlaStatus(SlaStatus.AT_RISK));
        data.put("breachedWorkOrders", workRepo.countBySlaStatus(SlaStatus.BREACHED));
        data.put("slaCompliancePercent", Math.round(compliance * 10.0) / 10.0);

        data.put("byTechnician", all.stream()
                .filter(w -> w.getAssignedTechnician() != null)
                .collect(Collectors.groupingBy(
                        w -> w.getAssignedTechnician().getFullName(),
                        Collectors.counting())));

        data.put("bySite", all.stream()
                .filter(w -> w.getSite() != null)
                .collect(Collectors.groupingBy(
                        w -> w.getSite().getSiteName(),
                        Collectors.counting())));

        data.put("availableTechnicians",
                userRepo.countByRoleAndActive(
                        Role.TECHNICIAN,
                        true
                ));

        data.put("lowStockParts",
                partRepo.countByStockQuantityLessThanEqual(5));

        data.put("recentWorkOrders",
                workRepo.findTop8ByOrderByCreatedAtDesc());

        return data;
    }
}
