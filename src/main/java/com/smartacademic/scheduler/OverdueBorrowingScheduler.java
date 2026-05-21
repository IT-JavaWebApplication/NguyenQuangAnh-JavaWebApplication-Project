package com.smartacademic.scheduler;

import com.smartacademic.config.HibernateSessionProvider;
import com.smartacademic.entity.BorrowingRecord;
import com.smartacademic.entity.User;
import com.smartacademic.enums.BorrowingStatus;
import com.smartacademic.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;


@Component
public class OverdueBorrowingScheduler {

    private static final Logger log = LoggerFactory.getLogger(OverdueBorrowingScheduler.class);

    @Autowired
    private HibernateSessionProvider sessionFactory;

    @Autowired
    private EmailService emailService;

    @Value("${app.scheduling.enabled:true}")
    private boolean schedulingEnabled;

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void scanOverdue() {
        if (!schedulingEnabled) {
            log.debug("[OverdueScheduler] Bị tắt - bỏ qua.");
            return;
        }

        var session = sessionFactory.getCurrentSession();
        LocalDate today = LocalDate.now();

        List<BorrowingRecord> overdueList = session.createQuery(
                "SELECT DISTINCT br FROM BorrowingRecord br " +
                        "LEFT JOIN FETCH br.student s " +
                        "LEFT JOIN FETCH s.profile " +
                        "WHERE br.status = :dispatched " +
                        "  AND br.dueDate IS NOT NULL " +
                        "  AND br.dueDate < :today", BorrowingRecord.class)
                .setParameter("dispatched", BorrowingStatus.DISPATCHED)
                .setParameter("today", today)
                .list();

        if (overdueList.isEmpty()) {
            log.info("[OverdueScheduler] Không có phiếu mượn nào quá hạn vào {}.", today);
            return;
        }

        log.warn("[OverdueScheduler] Phát hiện {} phiếu mượn quá hạn vào {}.", overdueList.size(), today);
        for (BorrowingRecord br : overdueList) {
            br.setStatus(BorrowingStatus.OVERDUE);
            session.merge(br);

            User student = br.getStudent();
            if (student != null && student.getEmail() != null) {
                String name = student.getProfile() != null
                        ? student.getProfile().getFullName()
                        : student.getUsername();
                emailService.sendOverdueAlert(student.getEmail(), name, br.getId());
            }
        }
    }

    //Mỗi 30 phút log số phiếu chờ cấp phát để admin nắm tình hình.
    @Scheduled(fixedDelay = 30 * 60 * 1000, initialDelay = 60 * 1000)
    @Transactional(readOnly = true)
    public void monitorPending() {
        if (!schedulingEnabled) return;
        Long pendingCount = sessionFactory.getCurrentSession()
                .createQuery("SELECT COUNT(br) FROM BorrowingRecord br WHERE br.status = 'PENDING_DISPATCH'", Long.class)
                .uniqueResult();
        if (pendingCount != null && pendingCount > 0) {
            log.info("[OverdueScheduler.monitor] Hiện có {} phiếu mượn chờ cấp phát.", pendingCount);
        }
    }
}
