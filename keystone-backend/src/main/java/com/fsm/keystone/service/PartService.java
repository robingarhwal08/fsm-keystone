package com.fsm.keystone.service;

import com.fsm.keystone.entity.Part;
import com.fsm.keystone.repository.PartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Year;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PartService {

    private final PartRepository partRepository;
    private final CascadeDeleteService cascadeDeleteService;

    public Part createPart(Part part) {
        part.setPartNumber(nextPartNumber());
        return partRepository.save(part);
    }

    public List<Part> getAllParts() {
        return partRepository.findAll();
    }

    public Part getPartById(Long id) {
        return partRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Part not found with id : " + id));
    }

    public Part updatePart(Long id, Part input) {

        Part part = partRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Part not found with id : " + id));

        part.setPartName(input.getPartName());
        part.setDescription(input.getDescription());
        part.setUnitPrice(input.getUnitPrice());
        part.setStockQuantity(input.getStockQuantity());
        part.setActive(input.getActive());

        return partRepository.save(part);
    }

    public void deletePart(Long id) {
        partRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Part not found with id : " + id));
        cascadeDeleteService.deletePart(id);
    }

    private String nextPartNumber() {
        String prefix = "PN-" + Year.now().getValue() + "-";
        long seq = partRepository.countByPartNumberStartingWith(prefix) + 1;
        return prefix + String.format("%04d", seq);
    }
}