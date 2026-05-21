package com.smartacademic.service.impl;

import com.smartacademic.dto.AcademicHistoryDTO;
import com.smartacademic.dto.BookingRequestDTO;
import com.smartacademic.entity.*;
import com.smartacademic.enums.SessionStatus;
import com.smartacademic.repository.MentoringSessionRepository;
import com.smartacademic.service.MentoringSessionService;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class MentoringSessionServiceImpl implements MentoringSessionService {

    @Autowired
    private MentoringSessionRepository sessionRepository;

    @Autowired
    private SessionFactory sessionFactory;

    // CORE-05: Đặt lịch cố vấn & chống xung đột
    @Override
    public MentoringSession book(Long studentId, BookingRequestDTO dto) {
        // Validate: không cho đặt lịch trong quá khứ
        LocalDate today = LocalDate.now();
        if (dto.getSessionDate().isBefore(today) ||
                (dto.getSessionDate().equals(today) && dto.getStartTime().isBefore(java.time.LocalTime.now()))) {
            throw new IllegalArgumentException("Không thể đặt lịch vào ngày/giờ đã qua");
        }

        // Validate: giờ kết thúc phải sau giờ bắt đầu
        if (!dto.getEndTime().isAfter(dto.getStartTime())) {
            throw new IllegalArgumentException("Giờ kết thúc phải sau giờ bắt đầu");
        }

        // CORE-05: Kiểm tra xung đột khung giờ (1 giảng viên không bị đặt 2 lần cùng khung giờ)
        if (sessionRepository.isSlotTaken(dto.getLecturerId(), dto.getSessionDate(), dto.getStartTime())) {
            throw new IllegalStateException("Khung giờ này đã được đặt. Vui lòng chọn khung giờ khác");
        }

        User student = sessionFactory.getCurrentSession().get(User.class, studentId);
        User lecturer = sessionFactory.getCurrentSession().get(User.class, dto.getLecturerId());

        if (student == null || lecturer == null) {
            throw new RuntimeException("Không tìm thấy thông tin người dùng");
        }

        MentoringSession session = new MentoringSession();
        session.setStudent(student);
        session.setLecturer(lecturer);
        session.setSessionDate(dto.getSessionDate());
        session.setStartTime(dto.getStartTime());
        session.setEndTime(dto.getEndTime());
        session.setNote(dto.getNote());
        session.setStatus(SessionStatus.PENDING);

        return sessionRepository.save(session);
    }

    // CORE-09: Hủy lịch & giải phóng slot
    @Override
    public void cancel(Long sessionId, Long studentId, String reason) {
        MentoringSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch hẹn"));

        // Kiểm tra quyền hủy
        if (!session.getStudent().getId().equals(studentId)) {
            throw new SecurityException("Bạn không có quyền hủy lịch này");
        }

        // Chỉ được hủy lịch ở trạng thái PENDING hoặc CONFIRMED
        if (session.getStatus() != SessionStatus.PENDING && session.getStatus() != SessionStatus.CONFIRMED) {
            throw new IllegalStateException("Không thể hủy lịch ở trạng thái hiện tại");
        }

        // Kiểm tra thời gian: phải trước giờ hẹn ít nhất 24 tiếng
        LocalDateTime sessionDateTime = session.getSessionDate().atTime(session.getStartTime());
        LocalDateTime cutoffTime = LocalDateTime.now().plusHours(24);
        if (sessionDateTime.isBefore(cutoffTime)) {
            throw new IllegalStateException("Chỉ được hủy lịch trước giờ hẹn ít nhất 24 giờ");
        }

        // CORE-09: Cập nhật trạng thái hủy - slot tự động được giải phóng (không còn trong trạng thái PENDING/CONFIRMED)
        session.setStatus(SessionStatus.CANCELLED);
        session.setCancelReason(reason);
        sessionRepository.update(session);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MentoringSession> getSessionsByStudent(Long studentId) {
        return sessionRepository.findByStudentId(studentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MentoringSession> getPendingSessionsByLecturer(Long lecturerId) {
        return sessionRepository.findByLecturerIdAndStatus(lecturerId, SessionStatus.PENDING);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MentoringSession> getAllSessionsByLecturer(Long lecturerId) {
        return sessionRepository.findByLecturerId(lecturerId);
    }

    @Override
    @Transactional(readOnly = true)
    public MentoringSession getSessionById(Long sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch hẹn"));
    }

    /**
     * CORE-07: Tra cứu Hồ sơ Học thuật Liên kết (JOIN phức tạp nhiều bảng)
     * Trả về thông tin đầy đủ: giảng viên, đánh giá, thiết bị mượn
     */
    @Override
    @Transactional(readOnly = true)
    public List<AcademicHistoryDTO> getAcademicHistory(Long studentId) {
        // Query JOIN phức tạp: mentoring_sessions + academic_evaluations + borrowing_records + borrowing_details + equipments
        List<Object[]> results = sessionFactory.getCurrentSession()
                .createQuery(
                        "SELECT ms, ae, br " +
                                "FROM MentoringSession ms " +
                                "LEFT JOIN FETCH ms.lecturer l " +
                                "LEFT JOIN FETCH l.profile lp " +
                                "LEFT JOIN FETCH l.lecturerInfo li " +
                                "LEFT JOIN FETCH li.department d " +
                                "LEFT JOIN ms.evaluation ae " +
                                "LEFT JOIN ms.borrowingRecord br " +
                                "WHERE ms.student.id = :studentId " +
                                "AND ms.status IN ('COMPLETED', 'PENDING', 'CONFIRMED') " +
                                "ORDER BY ms.sessionDate DESC", Object[].class)
                .setParameter("studentId", studentId)
                .list();

        List<AcademicHistoryDTO> history = new ArrayList<>();

        for (Object[] row : results) {
            MentoringSession ms = (MentoringSession) row[0];
            AcademicEvaluation ae = (AcademicEvaluation) row[1];
            BorrowingRecord br = (BorrowingRecord) row[2];

            AcademicHistoryDTO dto = new AcademicHistoryDTO();
            dto.setSessionId(ms.getId());
            dto.setSessionDate(ms.getSessionDate());
            dto.setStartTime(ms.getStartTime());
            dto.setEndTime(ms.getEndTime());
            dto.setStatus(ms.getStatus());
            dto.setNote(ms.getNote());

            // Thông tin giảng viên
            User lecturer = ms.getLecturer();
            dto.setLecturerId(lecturer.getId());
            if (lecturer.getProfile() != null) {
                dto.setLecturerName(lecturer.getProfile().getFullName());
            }
            if (lecturer.getLecturerInfo() != null) {
                dto.setLecturerCode(lecturer.getLecturerInfo().getLecturerCode());
                dto.setSpecialization(lecturer.getLecturerInfo().getSpecialization());
                if (lecturer.getLecturerInfo().getDepartment() != null) {
                    dto.setDepartment(lecturer.getLecturerInfo().getDepartment().getName());
                }
            }

            // Đánh giá năng lực
            if (ae != null) {
                dto.setSkillScore(ae.getSkillScore());
                dto.setAttitudeScore(ae.getAttitudeScore());
                dto.setComments(ae.getComments());
                dto.setRecommendations(ae.getRecommendations());
            }

            // Danh sách thiết bị mượn
            if (br != null) {
                dto.setBorrowingStatus(br.getStatus().getDisplayName());
                // Load details (cần eager hoặc trong session)
                List<BorrowingDetail> details = sessionFactory.getCurrentSession()
                        .createQuery(
                                "SELECT bd FROM BorrowingDetail bd " +
                                        "JOIN FETCH bd.equipment " +
                                        "WHERE bd.borrowingRecord.id = :brId", BorrowingDetail.class)
                        .setParameter("brId", br.getId())
                        .list();

                List<AcademicHistoryDTO.BorrowedEquipmentDTO> equipDTOs = details.stream().map(d -> {
                    AcademicHistoryDTO.BorrowedEquipmentDTO eDto = new AcademicHistoryDTO.BorrowedEquipmentDTO();
                    eDto.setEquipmentCode(d.getEquipment().getCode());
                    eDto.setEquipmentName(d.getEquipment().getName());
                    eDto.setQuantity(d.getQuantity());
                    eDto.setUnit(d.getEquipment().getUnit());
                    return eDto;
                }).collect(Collectors.toList());
                dto.setBorrowedEquipments(equipDTOs);
            }

            history.add(dto);
        }
        return history;
    }
}