package com.fsm.keystone.controller;

import com.fsm.keystone.entity.Part;
import com.fsm.keystone.service.PartService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/parts")
@RequiredArgsConstructor
public class PartController {

    private final PartService partService;

    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    public Part create(@RequestBody Part part) {

        return partService.createPart(part);
    }

    @GetMapping
    public List<Part> all() {

        return partService.getAllParts();
    }

    @GetMapping("/{id}")
    public Part getById(@PathVariable Long id) {

        return partService.getPartById(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public Part update(
            @PathVariable Long id,
            @RequestBody Part part) {

        return partService.updatePart(id, part);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public void delete(@PathVariable Long id) {

        partService.deletePart(id);
    }
}