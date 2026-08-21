package com.fsm.keystone.repository;

import com.fsm.keystone.entity.PartUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PartUsageRepository extends JpaRepository<PartUsage, Long> {

    @Query("""
            select p from PartUsage p
            left join fetch p.workOrder w
            left join fetch w.site
            left join fetch w.assignedTechnician
            left join fetch p.part
            left join fetch p.usedBy
            order by p.usedAt desc
            """)
    List<PartUsage> findAllWithDetails();

    @Query("""
            select p from PartUsage p
            left join fetch p.workOrder w
            left join fetch w.site
            left join fetch w.assignedTechnician
            left join fetch p.part
            left join fetch p.usedBy
            where p.usedBy.id = :userId
            order by p.usedAt desc
            """)
    List<PartUsage> findByUsedByIdWithDetails(@Param("userId") Long userId);
}
