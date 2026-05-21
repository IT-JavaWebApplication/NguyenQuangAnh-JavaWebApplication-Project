package com.smartacademic.repository;

import com.smartacademic.entity.BorrowingRecord;
import java.util.List;
import java.util.Optional;

public interface BorrowingRepository {
    BorrowingRecord save(BorrowingRecord record);
    Optional<BorrowingRecord> findById(Long id);
    Optional<BorrowingRecord> findBySessionId(Long sessionId);
    List<BorrowingRecord> findPendingDispatch();
    List<BorrowingRecord> findByStudentId(Long studentId);
    void update(BorrowingRecord record);
}