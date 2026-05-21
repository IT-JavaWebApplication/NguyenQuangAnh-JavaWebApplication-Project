package com.smartacademic.service;

import com.smartacademic.dto.EvaluationDTO;
import com.smartacademic.entity.BorrowingRecord;

import java.util.List;

public interface BorrowingService {
    void evaluateAndAssignEquipment(Long lecturerId, EvaluationDTO dto);
    void confirmDispatch(Long borrowingRecordId, Long adminId);
    List<BorrowingRecord> getPendingDispatch();
    BorrowingRecord getById(Long id);
}