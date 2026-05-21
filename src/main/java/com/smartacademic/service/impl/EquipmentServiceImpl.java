package com.smartacademic.service.impl;

import com.smartacademic.dto.EquipmentDTO;
import com.smartacademic.entity.Equipment;
import com.smartacademic.repository.EquipmentRepository;
import com.smartacademic.service.EquipmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class EquipmentServiceImpl implements EquipmentService {

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Override
    public Equipment create(EquipmentDTO dto) {
        if (equipmentRepository.existsByCode(dto.getCode())) {
            throw new IllegalArgumentException("Mã thiết bị đã tồn tại: " + dto.getCode());
        }
        Equipment equipment = new Equipment();
        mapDtoToEntity(dto, equipment);
        if (dto.getAvailable() == null) {
            equipment.setAvailable(dto.getQuantity());
        }
        return equipmentRepository.save(equipment);
    }

    @Override
    public Equipment update(Long id, EquipmentDTO dto) {
        Equipment equipment = equipmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thiết bị"));

        // Kiểm tra code trùng (ngoại trừ chính nó)
        equipmentRepository.findByCode(dto.getCode()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new IllegalArgumentException("Mã thiết bị đã tồn tại: " + dto.getCode());
            }
        });

        mapDtoToEntity(dto, equipment);
        equipmentRepository.update(equipment);
        return equipment;
    }

    @Override
    public void delete(Long id) {
        equipmentRepository.delete(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Equipment getById(Long id) {
        return equipmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thiết bị"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Equipment> getAll() {
        return equipmentRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Equipment> getAllActive() {
        return equipmentRepository.findAllActive();
    }

    private void mapDtoToEntity(EquipmentDTO dto, Equipment equipment) {
        equipment.setCode(dto.getCode().trim().toUpperCase());
        equipment.setName(dto.getName().trim());
        equipment.setDescription(dto.getDescription());
        equipment.setQuantity(dto.getQuantity());
        equipment.setUnit(dto.getUnit() != null ? dto.getUnit() : "Cái");
        equipment.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        equipment.setDepositAmount(dto.getDepositAmount() != null
                ? dto.getDepositAmount() : java.math.BigDecimal.ZERO);
        if (dto.getAvailable() != null) {
            equipment.setAvailable(dto.getAvailable());
        }
    }
}