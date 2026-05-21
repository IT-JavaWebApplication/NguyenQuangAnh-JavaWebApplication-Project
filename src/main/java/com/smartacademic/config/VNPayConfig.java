package com.smartacademic.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Bind cấu hình VNPay từ application.properties (tiền tố: app.payment).
 */
@Configuration
@ConfigurationProperties(prefix = "app.payment")
@Data
public class VNPayConfig {

    /**
     * Khi true: bỏ qua hoàn toàn việc gọi VNPay sandbox, dùng trang giả lập tích hợp
     * trong app để demo nhanh. Khi false: redirect ra URL thật của VNPay sandbox.
     */
    private boolean simulateMode = true;

    /** Phút trước khi đơn thanh toán hết hạn. */
    private int expireMinutes = 15;

    private VnpaySettings vnpay = new VnpaySettings();

    @Data
    public static class VnpaySettings {
        /** Mã website do VNPay cấp. */
        private String tmnCode;
        /** Khóa bí mật (HMAC-SHA512). */
        private String hashSecret;
        /** URL của VNPay để redirect người dùng. */
        private String payUrl;
        /** URL về app sau khi thanh toán xong. */
        private String returnUrl;
        /** Phiên bản API. */
        private String version = "2.1.0";
    }
}
