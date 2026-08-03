package com.fsm.keystone.service;

import com.fsm.keystone.enums.Priority;
import com.fsm.keystone.enums.Role;
import com.fsm.keystone.enums.WorkOrderStatus;
import com.fsm.keystone.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

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

        data.put("totalCustomers",
                customerRepo.count());

        data.put("totalSites",
                siteRepo.count());

        data.put("totalWorkOrders",
                workRepo.count());

        data.put("createdWorkOrders",
                workRepo.countByStatus(WorkOrderStatus.CREATED));

        data.put("assignedWorkOrders",
                workRepo.countByStatus(WorkOrderStatus.ASSIGNED));

        data.put("inProgressWorkOrders",
                workRepo.countByStatus(WorkOrderStatus.IN_PROGRESS));

        data.put("completedWorkOrders",
                workRepo.countByStatus(WorkOrderStatus.COMPLETED));

        data.put("criticalWorkOrders",
                workRepo.countByPriority(Priority.CRITICAL));

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