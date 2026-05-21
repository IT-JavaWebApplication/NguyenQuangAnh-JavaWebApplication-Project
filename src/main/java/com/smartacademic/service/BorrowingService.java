package com.smartacademic.service;

import com.smartacademic.dto.EvaluationDTO;
import com.smartacademic.entity.BorrowingRecord;

import java.util.List;

public interface BorrowingService {
    /** @return id phiếu mượn nếu có chỉ định thiết bị, null nếu chỉ lưu đánh giá */
    Long evaluateAndAssignEquipment(Long lecturerId, EvaluationDTO dto);
    void confirmDispatch(Long borrowingRecordId, Long adminId);
    List<BorrowingRecord> getPendingDispatch();
    BorrowingRecord getById(Long id);
}