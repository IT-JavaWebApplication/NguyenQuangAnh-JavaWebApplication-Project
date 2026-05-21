package com.smartacademic.service.impl;

import com.smartacademic.dto.EvaluationDTO;
import com.smartacademic.entity.*;
import com.smartacademic.enums.BorrowingStatus;
import com.smartacademic.enums.SessionStatus;
import com.smartacademic.repository.BorrowingRepository;
import com.smartacademic.repository.EquipmentRepository;
import com.smartacademic.repository.MentoringSessionRepository;
import com.smartacademic.service.BorrowingService;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private SessionFactory sessionFactory;

  
    @Override
    @Transactional(rollbackFor = Exception.class)
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
            sessionFactory.getCurrentSession().persist(borrowingRecord);

            List<BorrowingDetail> details = new ArrayList<>();
            for (int i = 0; i < dto.getEquipmentIds().size(); i++) {
                Long equipId = dto.getEquipmentIds().get(i);
                Integer qty = (dto.getEquipmentQuantities() != null && i < dto.getEquipmentQuantities().size())
                        ? dto.getEquipmentQuantities().get(i) : 1;

                Equipment equipment = equipmentRepository.findById(equipId)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy thiết bị ID: " + equipId));

                BorrowingDetail detail = new BorrowingDetail(borrowingRecord, equipment, qty);
                sessionFactory.getCurrentSession().persist(detail);
                details.add(detail);
            }
            borrowingRecord.setDetails(details);
        }
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmDispatch(Long borrowingRecordId, Long adminId) {
        BorrowingRecord record = borrowingRepository.findById(borrowingRecordId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu mượn"));

        if (record.getStatus() != BorrowingStatus.PENDING_DISPATCH) {
            throw new IllegalStateException("Phiếu mượn không ở trạng thái chờ cấp phát");
        }

        // Load details
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
            if (eq.getAvailable() < detail.getQuantity()) {
                errorMsg.append(String.format(
                        "Thiết bị [%s] không đủ tồn kho. Cần: %d, Còn lại: %d\n",
                        eq.getName(), detail.getQuantity(), eq.getAvailable()
                ));
            }
        }

        // Nếu bất kỳ thiết bị nào không đủ
        if (errorMsg.length() > 0) {
            throw new IllegalStateException("Không thể xuất kho:\n" + errorMsg);
        }

        // Đủ tồn kho
        for (BorrowingDetail detail : details) {
            Equipment eq = detail.getEquipment();
            eq.setAvailable(eq.getAvailable() - detail.getQuantity());
            equipmentRepository.update(eq);
        }

        // Cập nhật phiếu mượn
        record.setStatus(BorrowingStatus.DISPATCHED);
        record.setDispatchedAt(LocalDateTime.now());
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
        return borrowingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu mượn"));
    }
}