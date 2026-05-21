package com.smartacademic.service.impl;

import com.smartacademic.config.HibernateSessionProvider;
import com.smartacademic.dto.EvaluationDTO;
import com.smartacademic.entity.*;
import com.smartacademic.enums.BorrowingStatus;
import com.smartacademic.enums.SessionStatus;
import com.smartacademic.repository.BorrowingRepository;
import com.smartacademic.repository.EquipmentRepository;
import com.smartacademic.repository.MentoringSessionRepository;
import com.smartacademic.service.BorrowingService;
import com.smartacademic.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class BorrowingServiceImpl implements BorrowingService {

    @Autowired
    private MentoringSessionRepository sessionRepository;

    @Autowired
    private BorrowingRepository borrowingRepository;

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private HibernateSessionProvider sessionFactory;

    @Autowired
    private EmailService emailService;

    /**
     * Giảng viên đánh giá buổi tư vấn + (tuỳ chọn) tạo phiếu mượn thiết bị.
     * Toàn bộ thao tác (update session, persist evaluation, persist phiếu mượn)
     * nằm trong 1 transaction — lỗi giữa chừng sẽ rollback hết.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void evaluateAndAssignEquipment(Long lecturerId, EvaluationDTO dto) {

        MentoringSession session = sessionRepository.findById(dto.getSessionId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch hẹn"));

        if (!session.getLecturer().getId().equals(lecturerId)) {
            throw new SecurityException("Bạn không có quyền đánh giá buổi tư vấn này");
        }
        if (session.getStatus() != SessionStatus.PENDING && session.getStatus() != SessionStatus.CONFIRMED) {
            throw new IllegalStateException("Buổi tư vấn này không ở trạng thái có thể đánh giá");
        }

        session.setStatus(SessionStatus.COMPLETED);
        sessionRepository.update(session);

        AcademicEvaluation evaluation = new AcademicEvaluation();
        evaluation.setSession(session);
        evaluation.setLecturer(session.getLecturer());
        evaluation.setStudent(session.getStudent());
        evaluation.setSkillScore(dto.getSkillScore());
        evaluation.setAttitudeScore(dto.getAttitudeScore());
        evaluation.setComments(dto.getComments());
        evaluation.setRecommendations(dto.getRecommendations());
        sessionFactory.getCurrentSession().persist(evaluation);

        if (dto.getEquipmentIds() != null && !dto.getEquipmentIds().isEmpty()) {
            BorrowingRecord borrowingRecord = new BorrowingRecord();
            borrowingRecord.setSession(session);
            borrowingRecord.setStudent(session.getStudent());
            borrowingRecord.setStatus(BorrowingStatus.PENDING_DISPATCH);
            borrowingRecord.setDueDate(LocalDate.now().plusDays(7));
            sessionFactory.getCurrentSession().persist(borrowingRecord);

            List<BorrowingDetail> details = new ArrayList<>();
            for (int i = 0; i < dto.getEquipmentIds().size(); i++) {
                Long equipId = dto.getEquipmentIds().get(i);
                if (equipId == null) continue;
                Integer qty = (dto.getEquipmentQuantities() != null && i < dto.getEquipmentQuantities().size())
                        ? dto.getEquipmentQuantities().get(i) : 1;
                if (qty == null || qty <= 0) qty = 1;

                Equipment equipment = equipmentRepository.findById(equipId)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy thiết bị ID: " + equipId));

                BorrowingDetail detail = new BorrowingDetail(borrowingRecord, equipment, qty);
                sessionFactory.getCurrentSession().persist(detail);
                details.add(detail);
            }
            borrowingRecord.setDetails(details);
        }

        User student = session.getStudent();
        if (student != null && student.getEmail() != null) {
            String name = student.getProfile() != null
                    ? student.getProfile().getFullName()
                    : student.getUsername();
            emailService.sendEvaluationReady(student.getEmail(), name, session);
        }
    }

    /**
     * Admin xác nhận xuất kho: check toàn bộ tồn kho trước, đủ thì trừ và chuyển
     * status DISPATCHED; thiếu thì throw để rollback và báo lỗi tổng hợp.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void confirmDispatch(Long borrowingRecordId, Long adminId) {
        BorrowingRecord record = borrowingRepository.findById(borrowingRecordId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu mượn"));

        if (record.getStatus() != BorrowingStatus.PENDING_DISPATCH) {
            throw new IllegalStateException("Phiếu mượn không ở trạng thái chờ cấp phát");
        }

        @SuppressWarnings("unchecked")
        List<BorrowingDetail> details = sessionFactory.getCurrentSession()
                .createQuery(
                        "SELECT bd FROM BorrowingDetail bd " +
                                "JOIN FETCH bd.equipment " +
                                "WHERE bd.borrowingRecord.id = :id", BorrowingDetail.class)
                .setParameter("id", borrowingRecordId)
                .list();

        StringBuilder errorMsg = new StringBuilder();
        for (BorrowingDetail detail : details) {
            Equipment eq = detail.getEquipment();
            if (eq.getAvailable() == null || eq.getAvailable() < detail.getQuantity()) {
                errorMsg.append(String.format(
                        "• [%s] %s — Cần: %d, Còn: %d%n",
                        eq.getCode(), eq.getName(), detail.getQuantity(),
                        eq.getAvailable() == null ? 0 : eq.getAvailable()));
            }
        }

        if (errorMsg.length() > 0) {
            throw new IllegalStateException("Không đủ tồn kho:\n" + errorMsg);
        }

        for (BorrowingDetail detail : details) {
            Equipment eq = detail.getEquipment();
            eq.setAvailable(eq.getAvailable() - detail.getQuantity());
            equipmentRepository.update(eq);
        }

        record.setStatus(BorrowingStatus.DISPATCHED);
        record.setDispatchedAt(LocalDateTime.now());
        if (record.getDueDate() == null) {
            record.setDueDate(LocalDate.now().plusDays(7));
        }
        borrowingRepository.update(record);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BorrowingRecord> getPendingDispatch() {
        return borrowingRepository.findPendingDispatch();
    }

    @Override
    @Transactional(readOnly = true)
    public BorrowingRecord getById(Long id) {
        BorrowingRecord br = borrowingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu mượn"));
        // Khởi tạo lazy collection trước khi view render.
        br.getDetails().size();
        if (br.getStudent() != null && br.getStudent().getProfile() != null) {
            br.getStudent().getProfile().getFullName();
        }
        return br;
    }
}
