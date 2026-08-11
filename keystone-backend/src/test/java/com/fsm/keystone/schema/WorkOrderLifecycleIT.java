package com.fsm.keystone.schema;

import com.fsm.keystone.dto.AssignTechnicianRequest;
import com.fsm.keystone.dto.StatusUpdateRequest;
import com.fsm.keystone.entity.AppUser;
import com.fsm.keystone.entity.Customer;
import com.fsm.keystone.entity.Part;
import com.fsm.keystone.entity.PartUsage;
import com.fsm.keystone.entity.Site;
import com.fsm.keystone.entity.StatusHistory;
import com.fsm.keystone.entity.TimeLog;
import com.fsm.keystone.entity.WorkOrder;
import com.fsm.keystone.enums.Priority;
import com.fsm.keystone.enums.Role;
import com.fsm.keystone.enums.WorkOrderStatus;
import com.fsm.keystone.repository.CustomerRepository;
import com.fsm.keystone.repository.PartRepository;
import com.fsm.keystone.repository.PartUsageRepository;
import com.fsm.keystone.repository.SiteRepository;
import com.fsm.keystone.repository.StatusHistoryRepository;
import com.fsm.keystone.repository.TimeLogRepository;
import com.fsm.keystone.repository.UserRepository;
import com.fsm.keystone.repository.WorkOrderRepository;
import com.fsm.keystone.service.WorkOrderService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @SpringBootTest integration test exercising the full work-order lifecycle on the
 * Flyway-built V1 schema.
 *
 * <p>Sequence: create customer → create site → create manager + technician users →
 * create part → create work order → assign technician → update status → add time log →
 * add part usage → assert status_history rows and parts.stock_quantity decrement.</p>
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.flyway.enabled=true",
        "spring.flyway.baseline-on-migrate=false",
        "app.cors.allowed-origin=http://localhost:5173"
})
class WorkOrderLifecycleIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired CustomerRepository    customerRepo;
    @Autowired SiteRepository        siteRepo;
    @Autowired UserRepository        userRepo;
    @Autowired WorkOrderRepository   workOrderRepo;
    @Autowired PartRepository        partRepo;
    @Autowired TimeLogRepository     timeLogRepo;
    @Autowired PartUsageRepository   partUsageRepo;
    @Autowired StatusHistoryRepository historyRepo;
    @Autowired WorkOrderService      workOrderService;

    @Test
    void workOrder_fullLifecycle_persistsAllEntitiesCorrectly() {
        // ── Step 1: Create customer and site ─────────────────────────────────
        Customer customer = customerRepo.save(
                Customer.builder()
                        .name("Lifecycle Corp")
                        .email("lifecycle@example.test")
                        .build());
        assertThat(customer.getId()).isNotNull();
        assertThat(customer.getCreatedAt()).isNotNull();

        Site site = siteRepo.save(
                Site.builder()
                        .siteName("Lifecycle HQ")
                        .address("1 Lifecycle Lane")
                        .city("Springfield")
                        .state("IL")
                        .customer(customer)
                        .build());
        assertThat(site.getId()).isNotNull();

        // ── Step 2: Create manager and technician users ───────────────────────
        AppUser manager = userRepo.save(
                AppUser.builder()
                        .fullName("LC Manager")
                        .email("lc.manager@example.test")
                        .password("placeholder-hash")
                        .role(Role.MANAGER)
                        .build());
        AppUser technician = userRepo.save(
                AppUser.builder()
                        .fullName("LC Technician")
                        .email("lc.technician@example.test")
                        .password("placeholder-hash")
                        .role(Role.TECHNICIAN)
                        .build());
        assertThat(manager.getCreatedAt()).isNotNull();
        assertThat(technician.getActive()).isTrue();

        // ── Step 3: Create a part ─────────────────────────────────────────────
        Part bearing = partRepo.save(
                Part.builder()
                        .partName("LC Bearing")
                        .partNumber("LC-BEARING-001")
                        .unitPrice(new BigDecimal("49.95"))
                        .stockQuantity(20)
                        .build());
        assertThat(bearing.getStockQuantity()).isEqualTo(20);
        assertThat(bearing.getActive()).isTrue();

        // ── Step 4: Create work order ─────────────────────────────────────────
        WorkOrder wo = workOrderRepo.save(
                WorkOrder.builder()
                        .title("LC HVAC Service")
                        .description("Annual HVAC service for lifecycle test")
                        .priority(Priority.HIGH)
                        .customer(customer)
                        .site(site)
                        .createdBy(manager)
                        .build());
        assertThat(wo.getId()).isNotNull();
        assertThat(wo.getStatus()).isEqualTo(WorkOrderStatus.CREATED);
        assertThat(wo.getWorkOrderNumber()).isNotBlank();
        assertThat(wo.getCreatedAt()).isNotNull();
        assertThat(wo.getUpdatedAt()).isNotNull();

        // ── Step 5: Assign technician → creates StatusHistory row ─────────────
        long historyCountBefore = historyRepo.count();
        WorkOrder assigned = workOrderService.assignTechnician(
                wo.getId(), new AssignTechnicianRequest(technician.getId()));
        assertThat(assigned.getStatus()).isEqualTo(WorkOrderStatus.ASSIGNED);
        assertThat(assigned.getAssignedTechnician().getId()).isEqualTo(technician.getId());
        assertThat(historyRepo.count()).isEqualTo(historyCountBefore + 1);

        StatusHistory assignHistory = historyRepo.findAll().stream()
                .filter(h -> h.getWorkOrder().getId().equals(wo.getId()))
                .filter(h -> h.getNewStatus() == WorkOrderStatus.ASSIGNED)
                .findFirst().orElseThrow();
        assertThat(assignHistory.getOldStatus()).isEqualTo(WorkOrderStatus.CREATED);
        assertThat(assignHistory.getChangedAt()).isNotNull();

        // ── Step 6: Update status to IN_PROGRESS → creates another StatusHistory ─
        WorkOrder inProgress = workOrderService.updateStatus(
                wo.getId(), new StatusUpdateRequest(
                        WorkOrderStatus.IN_PROGRESS, manager.getId(), "Starting service"));
        assertThat(inProgress.getStatus()).isEqualTo(WorkOrderStatus.IN_PROGRESS);
        assertThat(historyRepo.count()).isEqualTo(historyCountBefore + 2);

        // ── Step 7: Add time log ──────────────────────────────────────────────
        TimeLog timeLog = timeLogRepo.save(
                TimeLog.builder()
                        .workOrder(inProgress)
                        .technician(technician)
                        .startTime(java.time.LocalDateTime.now().minusHours(2))
                        .endTime(java.time.LocalDateTime.now())
                        .hoursSpent(new BigDecimal("2.00"))
                        .workDescription("Performed HVAC filter replacement and pressure test")
                        .build());
        assertThat(timeLog.getId()).isNotNull();
        assertThat(timeLogRepo.findByWorkOrderId(wo.getId())).hasSize(1);
        assertThat(timeLogRepo.findByTechnicianId(technician.getId())).hasSize(1);

        // ── Step 8: Add part usage and decrement stock ────────────────────────
        int stockBefore = bearing.getStockQuantity();
        int quantityUsed = 3;
        BigDecimal priceAtUse = bearing.getUnitPrice();
        BigDecimal totalCost = priceAtUse.multiply(BigDecimal.valueOf(quantityUsed));

        PartUsage partUsage = partUsageRepo.save(
                PartUsage.builder()
                        .workOrder(inProgress)
                        .part(bearing)
                        .usedBy(technician)
                        .quantityUsed(quantityUsed)
                        .unitPriceAtUsage(priceAtUse)
                        .totalCost(totalCost)
                        .build());
        assertThat(partUsage.getId()).isNotNull();
        assertThat(partUsage.getUsedAt()).isNotNull();

        bearing.setStockQuantity(stockBefore - quantityUsed);
        Part updatedBearing = partRepo.save(bearing);
        assertThat(updatedBearing.getStockQuantity()).isEqualTo(stockBefore - quantityUsed);

        // ── Step 9: Verify final state ────────────────────────────────────────
        WorkOrder finalWo = workOrderRepo.findById(wo.getId()).orElseThrow();
        assertThat(finalWo.getStatus()).isEqualTo(WorkOrderStatus.IN_PROGRESS);
        assertThat(historyRepo.count()).isEqualTo(historyCountBefore + 2);
        assertThat(partRepo.countByStockQuantityLessThanEqual(20))
                .isGreaterThanOrEqualTo(1L);
    }
}
