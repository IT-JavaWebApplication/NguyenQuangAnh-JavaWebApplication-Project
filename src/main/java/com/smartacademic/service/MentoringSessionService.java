package com.smartacademic.service;

import com.smartacademic.dto.AcademicHistoryDTO;
import com.smartacademic.dto.BookingRequestDTO;
import com.smartacademic.entity.MentoringSession;

import java.util.List;

public interface MentoringSessionService {
    MentoringSession book(Long studentId, BookingRequestDTO dto);       // CORE-05
    void cancel(Long sessionId, Long studentId, String reason);         // CORE-09
    List<MentoringSession> getSessionsByStudent(Long studentId);
    List<MentoringSession> getPendingSessionsByLecturer(Long lecturerId);
    List<MentoringSession> getAllSessionsByLecturer(Long lecturerId);
    MentoringSession getSessionById(Long sessionId);
    List<AcademicHistoryDTO> getAcademicHistory(Long studentId);        // CORE-07
}