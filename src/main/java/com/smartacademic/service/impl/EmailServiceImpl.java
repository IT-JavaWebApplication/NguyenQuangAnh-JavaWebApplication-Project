package com.smartacademic.service.impl;

import com.smartacademic.entity.MentoringSession;
import com.smartacademic.service.EmailService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Triển khai EmailService:
 * - Khi app.mail.dev-mode = true: chỉ log nội dung email (không bắn SMTP thật)
 *   để demo được mà không cần cấu hình mail server.
 * - Khi dev-mode = false và SMTP cấu hình hợp lệ: gửi email thật qua JavaMailSender.
 *
 * Tất cả phương thức gắn @Async("asyncTaskExecutor") nên gọi đến đâu là chạy ngầm
 * trên thread khác - không block luồng HTTP.
 */
@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${app.mail.dev-mode:true}")
    private boolean devMode;

    @Value("${app.mail.from:noreply@smartacademic.edu.vn}")
    private String fromAddress;

    @PostConstruct
    public void init() {
        log.info("[EmailService] dev-mode = {} (true = chỉ log, false = gửi SMTP thật)", devMode);
    }

    @Override
    @Async("asyncTaskExecutor")
    public CompletableFuture<Boolean> sendBookingConfirmation(String toEmail, String studentName, MentoringSession s) {
        String subject = "[Smart Academic] Xác nhận đặt lịch tư vấn";
        String body = String.format(
                "Chào %s,%n%n" +
                        "Bạn đã đặt lịch tư vấn thành công.%n" +
                        " - Ngày: %s%n" +
                        " - Giờ: %s - %s%n" +
                        " - Mã buổi: #%d%n%n" +
                        "Vui lòng đăng nhập hệ thống để theo dõi trạng thái.%n%n" +
                        "Trân trọng,%nSmart Academic & Lab Support Platform",
                studentName, s.getSessionDate(), s.getStartTime(), s.getEndTime(), s.getId());
        return doSend(toEmail, subject, body);
    }

    @Override
    @Async("asyncTaskExecutor")
    public CompletableFuture<Boolean> sendCancellationNotice(String toEmail, String studentName, MentoringSession s, String reason) {
        String subject = "[Smart Academic] Lịch tư vấn đã được hủy";
        String body = String.format(
                "Chào %s,%n%n" +
                        "Lịch tư vấn #%d ngày %s (%s - %s) đã được hủy.%n" +
                        "Lý do: %s%n%n" +
                        "Khung giờ đã được giải phóng.",
                studentName, s.getId(), s.getSessionDate(), s.getStartTime(), s.getEndTime(),
                (reason == null || reason.isBlank()) ? "Không có" : reason);
        return doSend(toEmail, subject, body);
    }

    @Override
    @Async("asyncTaskExecutor")
    public CompletableFuture<Boolean> sendEvaluationReady(String toEmail, String studentName, MentoringSession s) {
        String subject = "[Smart Academic] Đánh giá buổi tư vấn đã sẵn sàng";
        String body = String.format(
                "Chào %s,%n%n" +
                        "Giảng viên đã hoàn tất đánh giá cho buổi tư vấn #%d ngày %s.%n" +
                        "Vui lòng đăng nhập để xem chi tiết và danh sách thiết bị/tài liệu được cấp.",
                studentName, s.getId(), s.getSessionDate());
        return doSend(toEmail, subject, body);
    }

    @Override
    @Async("asyncTaskExecutor")
    public CompletableFuture<Boolean> sendOverdueAlert(String toEmail, String studentName, Long borrowingRecordId) {
        String subject = "[Smart Academic] Cảnh báo: Thiết bị mượn quá hạn";
        String body = String.format(
                "Chào %s,%n%n" +
                        "Phiếu mượn #%d của bạn đã quá hạn trả thiết bị/tài liệu.%n" +
                        "Vui lòng liên hệ phòng Lab để hoàn trả nhằm tránh bị phạt.",
                studentName, borrowingRecordId);
        return doSend(toEmail, subject, body);
    }

    private CompletableFuture<Boolean> doSend(String to, String subject, String body) {
        try {
            if (devMode || mailSender == null) {
                log.info("\n========= [DEV-MODE EMAIL] =========\nTO: {}\nSUBJECT: {}\n{}\n=====================================",
                        to, subject, body);
                return CompletableFuture.completedFuture(true);
            }
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromAddress);
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(body);
            mailSender.send(msg);
            log.info("[EmailService] Đã gửi email tới {} - {}", to, subject);
            return CompletableFuture.completedFuture(true);
        } catch (Exception e) {
            log.error("[EmailService] Lỗi gửi email tới {}: {}", to, e.getMessage());
            return CompletableFuture.completedFuture(false);
        }
    }
}
