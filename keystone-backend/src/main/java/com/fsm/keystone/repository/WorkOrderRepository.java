package com.fsm.keystone.repository;

import com.fsm.keystone.entity.WorkOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long> {
    long countByStatus(com.fsm.keystone.enums.WorkOrderStatus status);
    long countByPriority(com.fsm.keystone.enums.Priority priority);
    java.util.List<WorkOrder> findTop8ByOrderByCreatedAtDesc();
}
