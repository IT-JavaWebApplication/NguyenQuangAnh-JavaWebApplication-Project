package com.smartacademic.enums;

public enum Role {
    STUDENT("Sinh viên"),
    LECTURER("Giảng viên"),
    ADMIN("Quản trị viên");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}