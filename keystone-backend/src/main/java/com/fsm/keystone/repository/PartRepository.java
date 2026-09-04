package com.fsm.keystone.repository;

import com.fsm.keystone.entity.Part;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PartRepository extends JpaRepository<Part, Long> {
    long countByPartNumberStartingWith(String prefix);

    @Query("""
            select count(p) from Part p
            where p.active = true
              and coalesce(p.stockQuantity, 0) <= coalesce(p.reorderLevel, :defaultLevel)
            """)
    long countLowStockParts(@Param("defaultLevel") int defaultLevel);

    @Query("""
            select p from Part p
            where p.active = true
              and coalesce(p.stockQuantity, 0) <= coalesce(p.reorderLevel, :defaultLevel)
            order by p.stockQuantity asc, p.partName asc
            """)
    List<Part> findLowStockParts(@Param("defaultLevel") int defaultLevel);
}
