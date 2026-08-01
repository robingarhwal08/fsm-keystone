package com.fsm.keystone.service;

import com.fsm.keystone.entity.Part;
import com.fsm.keystone.repository.PartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PartService {

    private final PartRepository partRepository;

    public Part createPart(Part part) {
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
        part.setPartNumber(input.getPartNumber());
        part.setDescription(input.getDescription());
        part.setUnitPrice(input.getUnitPrice());
        part.setStockQuantity(input.getStockQuantity());
        part.setActive(input.getActive());

        return partRepository.save(part);
    }

    public void deletePart(Long id) {

        Part part = partRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Part not found with id : " + id));

        partRepository.delete(part);
    }
}