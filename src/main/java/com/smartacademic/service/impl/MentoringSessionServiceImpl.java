package com.smartacademic.service.impl;

import com.smartacademic.config.HibernateSessionProvider;
import com.smartacademic.dto.AcademicHistoryDTO;
import com.smartacademic.dto.BookingRequestDTO;
import com.smartacademic.entity.*;
import com.smartacademic.enums.SessionStatus;
import com.smartacademic.repository.MentoringSessionRepository;
import com.smartacademic.service.EmailService;
import com.smartacademic.service.MentoringSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class MentoringSessionServiceImpl implements MentoringSessionService {

    @Autowired
    private MentoringSessionRepository sessionRepository;

    @Autowired
    private HibernateSessionProvider sessionFactory;

    @Autowired
    private EmailService emailService;

    @Override
    public MentoringSession book(Long studentId, BookingRequestDTO dto) {
        LocalDate today = LocalDate.now();
        if (dto.getSessionDate().isBefore(today) ||
                (dto.getSessionDate().equals(today) && dto.getStartTime().isBefore(LocalTime.now()))) {
            throw new IllegalArgumentException("Không thể đặt lịch vào ngày/giờ đã qua");
        }
        if (!dto.getEndTime().isAfter(dto.getStartTime())) {
            throw new IllegalArgumentException("Giờ kết thúc phải sau giờ bắt đầu");
        }

        if (sessionRepository.isSlotTaken(dto.getLecturerId(), dto.getSessionDate(), dto.getStartTime())) {
            throw new IllegalStateException("Khung giờ này đã được đặt. Vui lòng chọn khung giờ khác");
        }

        User student  = sessionFactory.getCurrentSession().get(User.class, studentId);
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

        //Nếu lecturer có session_fee > 0 → trạng thái PENDING_PAYMENT (chờ thanh toán)
        //ngược lại miễn phí → chuyển luôn sang PENDING (chờ giảng viên xác nhận).
        java.math.BigDecimal fee = java.math.BigDecimal.ZERO;
        if (lecturer.getLecturerInfo() != null && lecturer.getLecturerInfo().getSessionFee() != null) {
            fee = lecturer.getLecturerInfo().getSessionFee();
        }
        if (fee.compareTo(java.math.BigDecimal.ZERO) > 0) {
            session.setStatus(SessionStatus.PENDING_PAYMENT);
        } else {
            session.setStatus(SessionStatus.PENDING);
        }

        MentoringSession saved = sessionRepository.save(session);

        // Buổi miễn phí thì gửi email xác nhận luôn; buổi có phí thì
        // PaymentService sẽ gửi email sau khi thanh toán SUCCESS.
        if (saved.getStatus() == SessionStatus.PENDING && student.getEmail() != null) {
            String name = student.getProfile() != null
                    ? student.getProfile().getFullName()
                    : student.getUsername();
            emailService.sendBookingConfirmation(student.getEmail(), name, saved);
        }
        return saved;
    }

    @Override
    public void cancel(Long sessionId, Long studentId, String reason) {
        MentoringSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch hẹn"));

        if (!session.getStudent().getId().equals(studentId)) {
            throw new SecurityException("Bạn không có quyền hủy lịch này");
        }
        if (session.getStatus() != SessionStatus.PENDING_PAYMENT
                && session.getStatus() != SessionStatus.PENDING
                && session.getStatus() != SessionStatus.CONFIRMED) {
            throw new IllegalStateException("Không thể hủy lịch ở trạng thái hiện tại");
        }
        // Lịch chưa thanh toán → cho hủy ngay, bỏ qua rule 24h.
        boolean isWaitingPayment = session.getStatus() == SessionStatus.PENDING_PAYMENT;

        if (!isWaitingPayment) {
            LocalDateTime sessionDateTime = session.getSessionDate().atTime(session.getStartTime());
            LocalDateTime cutoffTime = LocalDateTime.now().plusHours(24);
            if (sessionDateTime.isBefore(cutoffTime)) {
                throw new IllegalStateException("Chỉ được hủy lịch trước giờ hẹn ít nhất 24 giờ");
            }
        }

        session.setStatus(SessionStatus.CANCELLED);
        session.setCancelReason(reason);
        sessionRepository.update(session);

        User student = session.getStudent();
        if (student != null && student.getEmail() != null) {
            String name = student.getProfile() != null
                    ? student.getProfile().getFullName()
                    : student.getUsername();
            emailService.sendCancellationNotice(student.getEmail(), name, session, reason);
        }
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
        MentoringSession ms = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch hẹn"));
        // Khởi tạo lazy association trước khi rời transaction.
        if (ms.getStudent() != null && ms.getStudent().getProfile() != null) {
            ms.getStudent().getProfile().getFullName();
        }
        if (ms.getLecturer() != null && ms.getLecturer().getProfile() != null) {
            ms.getLecturer().getProfile().getFullName();
        }
        return ms;
    }

    //Hồ sơ học thuật của sinh viên: JOIN sessions + evaluation + borrowingRecord.
    @Override
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<AcademicHistoryDTO> getAcademicHistory(Long studentId) {
        List<Object[]> results = sessionFactory.getCurrentSession()
                .createQuery(
                        "SELECT ms, ae, br " +
                                "FROM MentoringSession ms " +
                                "LEFT JOIN FETCH ms.lecturer l " +
                                "LEFT JOIN FETCH l.profile " +
                                "LEFT JOIN FETCH l.lecturerInfo li " +
                                "LEFT JOIN FETCH li.department " +
                                "LEFT JOIN ms.evaluation ae " +
                                "LEFT JOIN ms.borrowingRecord br " +
                                "WHERE ms.student.id = :studentId " +
                                "ORDER BY ms.sessionDate DESC, ms.startTime DESC", Object[].class)
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

            if (ae != null) {
                dto.setSkillScore(ae.getSkillScore());
                dto.setAttitudeScore(ae.getAttitudeScore());
                dto.setComments(ae.getComments());
                dto.setRecommendations(ae.getRecommendations());
            }

            if (br != null) {
                dto.setBorrowingStatus(br.getStatus().getDisplayName());
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
