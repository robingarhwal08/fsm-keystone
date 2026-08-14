package com.fsm.keystone.repository;

import com.fsm.keystone.entity.Part;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartRepository extends JpaRepository<Part, Long> {
    long countByStockQuantityLessThanEqual(Integer stockQuantity);

    long countByPartNumberStartingWith(String prefix);
}
