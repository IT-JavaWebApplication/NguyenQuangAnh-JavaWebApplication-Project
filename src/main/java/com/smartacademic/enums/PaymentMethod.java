package com.smartacademic.enums;

public enum PaymentMethod {
    VNPAY("VNPay"),
    MOMO("Momo"),
    SIMULATED("Mô phỏng (Sandbox)");

    private final String displayName;

    PaymentMethod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
