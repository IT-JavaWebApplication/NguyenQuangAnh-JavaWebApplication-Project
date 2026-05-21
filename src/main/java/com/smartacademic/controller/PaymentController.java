package com.smartacademic.controller;

import com.smartacademic.entity.Payment;
import com.smartacademic.entity.User;
import com.smartacademic.enums.PaymentStatus;
import com.smartacademic.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/payment")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    @Autowired private PaymentService paymentService;

    private User currentUser(HttpSession s) {
        return (User) s.getAttribute("currentUser");
    }

    /* ----------------------------------------------------------
     * 1) Sinh viên bấm "Thanh toán" cho 1 buổi tư vấn → redirect
     *    sang VNPay (hoặc trang simulate)
     * ---------------------------------------------------------- */
    @PostMapping("/session/{sessionId}/checkout")
    public String checkoutSession(@PathVariable Long sessionId,
                                  HttpSession httpSession,
                                  HttpServletRequest request,
                                  RedirectAttributes ra) {
        User user = currentUser(httpSession);
        if (user == null) return "redirect:/auth/login";

        try {
            String url = paymentService.createSessionPayment(sessionId, user.getId(), request);
            if (url.startsWith("/")) {
                // Internal route (simulate hoặc miễn phí)
                if (url.contains("paid=free")) {
                    ra.addFlashAttribute("successMsg",
                            "Buổi tư vấn miễn phí - lịch đã chuyển sang chờ xác nhận.");
                }
                return "redirect:" + url;
            }
            return "redirect:" + url;
        } catch (Exception e) {
            log.error("Lỗi khởi tạo thanh toán: {}", e.getMessage(), e);
            ra.addFlashAttribute("errorMsg", "Không thể khởi tạo thanh toán: " + e.getMessage());
            return "redirect:/student/dashboard";
        }
    }

    /* ----------------------------------------------------------
     * 2) Trang giả lập VNPay (chế độ simulate-mode = true)
     *    Hiển thị thông tin giao dịch, 2 nút: "Thanh toán thành công" / "Hủy"
     * ---------------------------------------------------------- */
    @GetMapping("/simulate/{txnRef}")
    public String simulatePage(@PathVariable String txnRef, HttpSession httpSession, Model model) {
        User user = currentUser(httpSession);
        if (user == null) return "redirect:/auth/login";

        Payment payment = paymentService.getByTxnRef(txnRef);
        if (!payment.getPayer().getId().equals(user.getId())) {
            return "redirect:/auth/access-denied";
        }
        model.addAttribute("payment", payment);
        return "payment/simulate";
    }

    @PostMapping("/simulate/{txnRef}/confirm")
    public String simulateConfirm(@PathVariable String txnRef,
                                  @RequestParam("result") String result,
                                  RedirectAttributes ra) {
        boolean ok = "success".equalsIgnoreCase(result);
        Payment payment = paymentService.simulatePaymentResult(txnRef, ok);
        return "redirect:/payment/result/" + payment.getTxnRef();
    }

    /* ----------------------------------------------------------
     * 3) Callback từ VNPay (Return URL)
     *    VNPay redirect kèm tất cả vnp_* params + vnp_SecureHash.
     *    Service verify chữ ký rồi update Payment & Session.
     * ---------------------------------------------------------- */
    @GetMapping("/vnpay-return")
    public String vnpayReturn(@RequestParam Map<String, String> allParams,
                              RedirectAttributes ra) {
        try {
            Map<String, String> vnpParams = new HashMap<>();
            allParams.forEach((k, v) -> {
                if (k != null && k.startsWith("vnp_")) vnpParams.put(k, v);
            });
            Payment payment = paymentService.handleVnpayReturn(vnpParams);
            return "redirect:/payment/result/" + payment.getTxnRef();
        } catch (Exception e) {
            log.error("Lỗi xử lý callback VNPay: {}", e.getMessage(), e);
            ra.addFlashAttribute("errorMsg", "Xử lý kết quả thanh toán thất bại: " + e.getMessage());
            return "redirect:/student/dashboard";
        }
    }

    /* ----------------------------------------------------------
     * 4) Trang kết quả thanh toán (thành công / thất bại)
     * ---------------------------------------------------------- */
    @GetMapping("/result/{txnRef}")
    public String result(@PathVariable String txnRef, HttpSession httpSession, Model model) {
        User user = currentUser(httpSession);
        if (user == null) return "redirect:/auth/login";

        Payment payment = paymentService.getByTxnRef(txnRef);
        if (!payment.getPayer().getId().equals(user.getId())) {
            return "redirect:/auth/access-denied";
        }
        model.addAttribute("payment", payment);
        model.addAttribute("isSuccess", payment.getStatus() == PaymentStatus.SUCCESS);
        return "payment/result";
    }

    /* ----------------------------------------------------------
     * 5) Lịch sử giao dịch của sinh viên
     * ---------------------------------------------------------- */
    @GetMapping("/history")
    public String history(HttpSession httpSession, Model model) {
        User user = currentUser(httpSession);
        if (user == null) return "redirect:/auth/login";

        List<Payment> payments = paymentService.getHistoryOfUser(user.getId());
        model.addAttribute("user", user);
        model.addAttribute("payments", payments);
        model.addAttribute("activePage", "payments");
        return "payment/history";
    }
}
