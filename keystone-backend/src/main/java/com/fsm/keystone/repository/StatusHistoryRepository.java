package com.fsm.keystone.repository;

import com.fsm.keystone.entity.StatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StatusHistoryRepository extends JpaRepository<StatusHistory, Long> {

    @Query("""
            select h from StatusHistory h
            left join fetch h.changedBy
            where h.workOrder.id = :workOrderId
            order by h.changedAt asc
            """)
    List<StatusHistory> findByWorkOrderIdOrderByChangedAtAsc(@Param("workOrderId") Long workOrderId);
}
