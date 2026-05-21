package com.smartacademic.entity;

import com.smartacademic.enums.PaymentMethod;
import com.smartacademic.enums.PaymentStatus;
import com.smartacademic.enums.PaymentType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Một lần khởi tạo thanh toán (sinh viên đặt lịch → tạo Payment với status = PENDING).
 *
 * Sau khi gateway redirect callback về thì cập nhật status thành SUCCESS/FAILED,
 * đồng thời ghi lại transactionId, responseCode, signature... để có dấu vết kiểm tra.
 */
@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_payment_txn_ref", columnList = "txn_ref", unique = true),
        @Index(name = "idx_payment_session", columnList = "session_id"),
        @Index(name = "idx_payment_status", columnList = "status")
})
@Data
@NoArgsConstructor
@ToString(exclude = {"session", "borrowingRecord", "payer"})
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Mã giao dịch nội bộ (truyền vào gateway field vnp_TxnRef) - duy nhất. */
    @Column(name = "txn_ref", nullable = false, unique = true, length = 64)
    private String txnRef;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payer_id", nullable = false)
    private User payer;

    /** Phí buổi tư vấn: gắn vào MentoringSession. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private MentoringSession session;

    /** Phí đặt cọc thiết bị: gắn vào BorrowingRecord. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "borrowing_record_id")
    private BorrowingRecord borrowingRecord;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod method = PaymentMethod.VNPAY;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(nullable = false, precision = 12, scale = 0)
    private BigDecimal amount;

    @Column(length = 255)
    private String description;

    /** Mã giao dịch trả về từ gateway (vnp_TransactionNo). */
    @Column(name = "gateway_txn_id", length = 100)
    private String gatewayTxnId;

    /** Response code từ gateway: 00 = thành công. */
    @Column(name = "response_code", length = 10)
    private String responseCode;

    /** Thông điệp giải thích kết quả. */
    @Column(name = "response_message", length = 500)
    private String responseMessage;

    /** Chữ ký HMAC do gateway trả về (lưu để audit). */
    @Column(name = "gateway_signature", length = 500)
    private String gatewaySignature;

    /** IP của client lúc khởi tạo (yêu cầu của VNPay). */
    @Column(name = "client_ip", length = 64)
    private String clientIp;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @PrePersist
    protected void onCreate() {
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
