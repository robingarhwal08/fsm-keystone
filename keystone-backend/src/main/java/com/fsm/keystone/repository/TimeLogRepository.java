package com.fsm.keystone.repository;

import com.fsm.keystone.entity.TimeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TimeLogRepository extends JpaRepository<TimeLog, Long> {

    List<TimeLog> findByTechnicianId(Long technicianId);

    List<TimeLog> findByWorkOrderId(Long workOrderId);

    @Query("""
            select t from TimeLog t
            left join fetch t.workOrder
            left join fetch t.technician
            order by t.startTime desc
            """)
    List<TimeLog> findAllWithDetails();

    @Query("""
            select t from TimeLog t
            left join fetch t.workOrder
            left join fetch t.technician
            where t.technician.id = :technicianId
            order by t.startTime desc
            """)
    List<TimeLog> findByTechnicianIdWithDetails(@Param("technicianId") Long technicianId);
}
