package com.smartacademic.enums;

public enum BorrowingStatus {
    PENDING_DISPATCH("Chờ cấp phát"),
    DISPATCHED("Đã xuất kho"),
    RETURNED("Đã trả"),
    OVERDUE("Quá hạn"),
    CANCELLED("Đã hủy");

    private final String displayName;

    BorrowingStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
