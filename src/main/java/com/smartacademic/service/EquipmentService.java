package com.smartacademic.service;

import com.smartacademic.dto.EquipmentDTO;
import com.smartacademic.entity.Equipment;

import java.util.List;

public interface EquipmentService {
    Equipment create(EquipmentDTO dto);      // CORE-04
    Equipment update(Long id, EquipmentDTO dto);  // CORE-04
    void delete(Long id);                    // CORE-04
    Equipment getById(Long id);
    List<Equipment> getAll();
    List<Equipment> getAllActive();
}