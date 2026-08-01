package com.fsm.keystone.repository;

import com.fsm.keystone.entity.TimeLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TimeLogRepository extends JpaRepository<TimeLog, Long> {

    List<TimeLog> findByTechnicianId(Long technicianId);

    List<TimeLog> findByWorkOrderId(Long workOrderId);
}