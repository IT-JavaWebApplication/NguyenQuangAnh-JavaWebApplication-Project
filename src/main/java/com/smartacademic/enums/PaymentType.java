package com.smartacademic.enums;

public enum PaymentType {
    SESSION_FEE("Phí buổi tư vấn"),
    EQUIPMENT_DEPOSIT("Phí đặt cọc thiết bị");

    private final String displayName;

    PaymentType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
