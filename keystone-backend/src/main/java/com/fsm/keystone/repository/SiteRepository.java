package com.fsm.keystone.repository;

import com.fsm.keystone.entity.Site;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SiteRepository extends JpaRepository<Site, Long> {
    java.util.List<Site> findByCustomerId(Long customerId);
}
