package com.smartacademic.repository;

import com.smartacademic.entity.Equipment;
import java.util.List;
import java.util.Optional;

public interface EquipmentRepository {
    Equipment save(Equipment equipment);
    Optional<Equipment> findById(Long id);
    Optional<Equipment> findByCode(String code);
    List<Equipment> findAll();
    List<Equipment> findAllActive();
    void update(Equipment equipment);
    void delete(Long id);
    boolean existsByCode(String code);
}