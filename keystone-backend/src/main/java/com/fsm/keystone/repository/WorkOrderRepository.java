package com.fsm.keystone.repository;

import com.fsm.keystone.entity.WorkOrder;
import com.fsm.keystone.enums.SlaStatus;
import com.fsm.keystone.enums.WorkOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long> {
    long countByStatus(WorkOrderStatus status);
    long countByPriority(com.fsm.keystone.enums.Priority priority);
    long countBySlaStatus(SlaStatus slaStatus);
    List<WorkOrder> findTop8ByOrderByCreatedAtDesc();
    List<WorkOrder> findByAssignedTechnician_Id(Long technicianId);
    List<WorkOrder> findByCustomer_Id(Long customerId);
    List<WorkOrder> findByCreatedBy_Id(Long userId);

    @Query("""
            select w from WorkOrder w
            left join fetch w.createdBy
            left join fetch w.customer
            left join fetch w.assignedTechnician
            where w.id = :id
            """)
    Optional<WorkOrder> findByIdWithDetails(@Param("id") Long id);
    List<WorkOrder> findByStatusIn(Collection<WorkOrderStatus> statuses);
    List<WorkOrder> findBySlaDueAtNotNullAndStatusNotIn(Collection<WorkOrderStatus> statuses);
    long countByWorkOrderNumberStartingWith(String prefix);
}
