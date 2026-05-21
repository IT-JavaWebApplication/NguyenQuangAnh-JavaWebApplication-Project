package com.smartacademic.service;

import com.smartacademic.dto.EquipmentDTO;
import com.smartacademic.entity.Equipment;

import java.util.List;

public interface EquipmentService {
    Equipment create(EquipmentDTO dto);
    Equipment update(Long id, EquipmentDTO dto);
    void delete(Long id);
    Equipment getById(Long id);
    List<Equipment> getAll();
    List<Equipment> getAllActive();
}