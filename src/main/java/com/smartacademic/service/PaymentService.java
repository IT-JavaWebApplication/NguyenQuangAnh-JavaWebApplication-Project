package com.smartacademic.service;

import com.smartacademic.entity.Payment;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Map;

/**
 * Quản lý vòng đời thanh toán cho phí buổi tư vấn / phí đặt cọc thiết bị.
 */
public interface PaymentService {

    /**
     * Tạo Payment cho 1 buổi tư vấn (status = PENDING) và sinh URL để redirect
     * sang VNPay sandbox (hoặc trang giả lập nếu simulate-mode = true).
     */
    String createSessionPayment(Long sessionId, Long payerId, HttpServletRequest request);

    /**
     * Xác minh & xử lý callback từ VNPay sandbox.
     * @return Payment đã được cập nhật trạng thái
     */
    Payment handleVnpayReturn(Map<String, String> params);

    /**
     * Dành cho chế độ giả lập: đánh dấu Payment thành SUCCESS / FAILED rồi
     * đi tiếp luồng nghiệp vụ (chuyển session sang PENDING).
     */
    Payment simulatePaymentResult(String txnRef, boolean success);

    Payment getByTxnRef(String txnRef);
    Payment getById(Long id);
    List<Payment> getHistoryOfUser(Long userId);
}
