package com.smartacademic.enums;

public enum PaymentStatus {
    PENDING("Chờ thanh toán"),
    PROCESSING("Đang xử lý"),
    SUCCESS("Thanh toán thành công"),
    FAILED("Thanh toán thất bại"),
    CANCELLED("Đã hủy"),
    REFUNDED("Đã hoàn tiền");

    private final String displayName;

    PaymentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
