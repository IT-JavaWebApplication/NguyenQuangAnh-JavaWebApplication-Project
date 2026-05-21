package com.smartacademic.repository;

import com.smartacademic.entity.MentoringSession;
import com.smartacademic.enums.SessionStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface MentoringSessionRepository {
    MentoringSession save(MentoringSession session);
    Optional<MentoringSession> findById(Long id);
    List<MentoringSession> findByStudentId(Long studentId);
    List<MentoringSession> findByLecturerId(Long lecturerId);
    List<MentoringSession> findByLecturerIdAndStatus(Long lecturerId, SessionStatus status);
    boolean isSlotTaken(Long lecturerId, LocalDate date, LocalTime startTime);
    void update(MentoringSession session);
    List<MentoringSession> findAllPending();
}