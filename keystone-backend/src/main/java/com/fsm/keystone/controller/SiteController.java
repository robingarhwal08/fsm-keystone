package com.fsm.keystone.controller;

import com.fsm.keystone.entity.Site;
import com.fsm.keystone.service.SiteService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sites")
@RequiredArgsConstructor
public class SiteController {

    private final SiteService siteService;

    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER','DISPATCHER')")
    public Site create(@RequestBody Site site) {
        return siteService.createSite(site);
    }

    @GetMapping
    public List<Site> all() {
        return siteService.getAllSites();
    }

    @GetMapping("/customer/{customerId}")
    public List<Site> byCustomer(
            @PathVariable Long customerId) {

        return siteService.getSitesByCustomerId(
                customerId);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER','DISPATCHER')")
    public Site update(
            @PathVariable Long id,
            @RequestBody Site site) {

        return siteService.updateSite(
                id,
                site);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER','DISPATCHER')")
    public void delete(
            @PathVariable Long id) {

        siteService.deleteSite(id);
    }
}