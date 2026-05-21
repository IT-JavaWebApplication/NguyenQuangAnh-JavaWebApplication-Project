package com.smartacademic.enums;

public enum SessionStatus {
    PENDING("Chờ xác nhận"),
    CONFIRMED("Đã xác nhận"),
    COMPLETED("Đã hoàn thành"),
    CANCELLED("Đã hủy");

    private final String displayName;

    SessionStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}