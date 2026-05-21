package com.smartacademic.service.impl;

import com.smartacademic.config.VNPayConfig;
import com.smartacademic.entity.MentoringSession;
import com.smartacademic.entity.Payment;
import com.smartacademic.entity.User;
import com.smartacademic.enums.PaymentMethod;
import com.smartacademic.enums.PaymentStatus;
import com.smartacademic.enums.PaymentType;
import com.smartacademic.enums.SessionStatus;
import com.smartacademic.repository.MentoringSessionRepository;
import com.smartacademic.repository.PaymentRepository;
import com.smartacademic.service.EmailService;
import com.smartacademic.service.PaymentService;
import com.smartacademic.util.VNPayUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    @Autowired private PaymentRepository paymentRepository;
    @Autowired private MentoringSessionRepository sessionRepository;
    @Autowired private VNPayConfig vnpayConfig;
    @Autowired private EmailService emailService;

    @Override
    public String createSessionPayment(Long sessionId, Long payerId, HttpServletRequest request) {
        MentoringSession ms = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy buổi tư vấn"));

        if (!ms.getStudent().getId().equals(payerId)) {
            throw new SecurityException("Bạn không có quyền thanh toán buổi tư vấn này");
        }
        if (ms.getStatus() != SessionStatus.PENDING_PAYMENT) {
            throw new IllegalStateException(
                    "Buổi tư vấn không ở trạng thái chờ thanh toán (hiện tại: "
                            + ms.getStatus().getDisplayName() + ")");
        }

        BigDecimal fee = BigDecimal.ZERO;
        if (ms.getLecturer() != null && ms.getLecturer().getLecturerInfo() != null) {
            fee = Optional.ofNullable(ms.getLecturer().getLecturerInfo().getSessionFee())
                    .orElse(BigDecimal.ZERO);
        }
        if (fee.compareTo(BigDecimal.ZERO) <= 0) {
            log.info("[Payment] Buổi #{} miễn phí, bỏ qua thanh toán", sessionId);
            ms.setStatus(SessionStatus.PENDING);
            sessionRepository.update(ms);
            return "/student/dashboard?paid=free";
        }

        // Tái sử dụng phiên PENDING gần nhất để tránh sinh rác Payment.
        Payment payment = paymentRepository.findLatestPendingBySessionId(sessionId)
                .orElseGet(() -> {
                    Payment p = new Payment();
                    p.setTxnRef(VNPayUtils.generateTxnRef());
                    p.setPayer(ms.getStudent());
                    p.setSession(ms);
                    p.setType(PaymentType.SESSION_FEE);
                    p.setMethod(PaymentMethod.VNPAY);
                    p.setStatus(PaymentStatus.PENDING);
                    p.setAmount(ms.getLecturer().getLecturerInfo().getSessionFee());
                    // VNPay yêu cầu vnp_OrderInfo: tiếng Việt không dấu, không có ký tự đặc biệt.
                    p.setDescription("Thanh toan phi tu van buoi so " + ms.getId());
                    p.setClientIp(VNPayUtils.getClientIp(request));
                    return paymentRepository.save(p);
                });

        if (vnpayConfig.isSimulateMode()) {
            return "/payment/simulate/" + payment.getTxnRef();
        }

        return buildVnpayUrl(payment, request);
    }

    /** Build URL thanh toán VNPay theo chuẩn API 2.1.0. */
    private String buildVnpayUrl(Payment payment, HttpServletRequest request) {
        VNPayConfig.VnpaySettings v = vnpayConfig.getVnpay();

        Map<String, String> p = new TreeMap<>();
        p.put("vnp_Version", v.getVersion());
        p.put("vnp_Command", "pay");
        p.put("vnp_TmnCode", v.getTmnCode());
        p.put("vnp_Amount", payment.getAmount().multiply(BigDecimal.valueOf(100)).toBigInteger().toString());
        p.put("vnp_CurrCode", "VND");
        p.put("vnp_TxnRef", payment.getTxnRef());
        p.put("vnp_OrderInfo", payment.getDescription());
        p.put("vnp_OrderType", "other");
        p.put("vnp_Locale", "vn");
        p.put("vnp_ReturnUrl", v.getReturnUrl());
        p.put("vnp_IpAddr", payment.getClientIp() == null ? "127.0.0.1" : payment.getClientIp());

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        ZoneId vnTz = ZoneId.of("Asia/Ho_Chi_Minh");
        LocalDateTime now = LocalDateTime.now(vnTz);
        p.put("vnp_CreateDate", now.format(fmt));
        p.put("vnp_ExpireDate", now.plusMinutes(vnpayConfig.getExpireMinutes()).format(fmt));

        String hashData = VNPayUtils.buildHashData(p);
        String secureHash = VNPayUtils.hmacSHA512(v.getHashSecret(), hashData);

        String url = v.getPayUrl() + "?" + hashData + "&vnp_SecureHash=" + secureHash;
        log.debug("[VNPay] Created URL for txnRef={} → {}", payment.getTxnRef(), url);
        return url;
    }

    /** Xử lý callback từ VNPay (return URL). */
    @Override
    public Payment handleVnpayReturn(Map<String, String> params) {
        String secret = vnpayConfig.getVnpay().getHashSecret();
        boolean validSig = VNPayUtils.verifySignature(params, secret);

        String txnRef = params.get("vnp_TxnRef");
        Payment payment = paymentRepository.findByTxnRef(txnRef)
                .orElseThrow(() -> new IllegalStateException(
                        "Không tìm thấy giao dịch txnRef=" + txnRef));

        // Idempotent: callback lại trên giao dịch đã SUCCESS thì bỏ qua.
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            log.warn("[VNPay] Callback lại trên giao dịch đã SUCCESS: {}", txnRef);
            return payment;
        }

        payment.setGatewaySignature(params.get("vnp_SecureHash"));
        payment.setGatewayTxnId(params.get("vnp_TransactionNo"));
        payment.setResponseCode(params.get("vnp_ResponseCode"));

        if (!validSig) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setResponseMessage("Chữ ký không hợp lệ - khả năng bị giả mạo");
            paymentRepository.update(payment);
            log.error("[VNPay] CHỮ KÝ KHÔNG HỢP LỆ - txnRef={}", txnRef);
            return payment;
        }

        // Đối chiếu số tiền để chống tampering từ phía client.
        try {
            long amountFromGateway = Long.parseLong(params.getOrDefault("vnp_Amount", "0"));
            long expected = payment.getAmount().multiply(BigDecimal.valueOf(100)).longValueExact();
            if (amountFromGateway != expected) {
                payment.setStatus(PaymentStatus.FAILED);
                payment.setResponseMessage("Số tiền không khớp - tampering detected");
                paymentRepository.update(payment);
                log.error("[VNPay] Amount mismatch on {}: gateway={}, expected={}",
                        txnRef, amountFromGateway, expected);
                return payment;
            }
        } catch (NumberFormatException ignore) {
        }

        String code = params.get("vnp_ResponseCode");
        if ("00".equals(code)) {
            return markSuccess(payment, "Thanh toán thành công qua VNPay");
        } else {
            return markFailed(payment, "VNPay từ chối, mã lỗi: " + code);
        }
    }

    /** Mô phỏng gateway VNPay khi chạy demo (không cần internet). */
    @Override
    public Payment simulatePaymentResult(String txnRef, boolean success) {
        Payment payment = paymentRepository.findByTxnRef(txnRef)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy giao dịch " + txnRef));

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return payment;
        }

        payment.setMethod(PaymentMethod.SIMULATED);
        payment.setGatewayTxnId("SIM-" + System.currentTimeMillis());
        payment.setResponseCode(success ? "00" : "24");

        return success
                ? markSuccess(payment, "Thanh toán mô phỏng thành công")
                : markFailed(payment, "Người dùng hủy thanh toán mô phỏng");
    }

    private Payment markSuccess(Payment payment, String message) {
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setResponseMessage(message);
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.update(payment);

        // Thanh toán thành công → đẩy session từ PENDING_PAYMENT sang PENDING (chờ xác nhận).
        if (payment.getSession() != null
                && payment.getSession().getStatus() == SessionStatus.PENDING_PAYMENT) {
            MentoringSession ms = payment.getSession();
            ms.setStatus(SessionStatus.PENDING);
            sessionRepository.update(ms);

            User student = ms.getStudent();
            if (student != null && student.getEmail() != null) {
                String name = student.getProfile() != null
                        ? student.getProfile().getFullName()
                        : student.getUsername();
                emailService.sendBookingConfirmation(student.getEmail(), name, ms);
            }
        }
        log.info("[Payment] SUCCESS txnRef={}", payment.getTxnRef());
        return payment;
    }

    private Payment markFailed(Payment payment, String message) {
        payment.setStatus(PaymentStatus.FAILED);
        payment.setResponseMessage(message);
        paymentRepository.update(payment);
        log.warn("[Payment] FAILED txnRef={} - {}", payment.getTxnRef(), message);
        return payment;
    }

    @Override
    @Transactional(readOnly = true)
    public Payment getByTxnRef(String txnRef) {
        return paymentRepository.findByTxnRef(txnRef)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giao dịch"));
    }

    @Override
    @Transactional(readOnly = true)
    public Payment getById(Long id) {
        Payment p = paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giao dịch"));
        // Khởi tạo lazy association trước khi Hibernate đóng session.
        if (p.getSession() != null) p.getSession().getId();
        return p;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Payment> getHistoryOfUser(Long userId) {
        return paymentRepository.findByPayerId(userId);
    }
}
