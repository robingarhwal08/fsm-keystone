package com.fsm.keystone.repository;

import com.fsm.keystone.entity.PartUsage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartUsageRepository
        extends JpaRepository<PartUsage, Long> {
}