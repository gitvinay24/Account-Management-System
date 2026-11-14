package com.businessmanager.enums;

public enum PaymentStatus {
    PAID("Paid"),
    PARTIAL("Partial Payment"),
    UNPAID("Unpaid");

    private final String displayName;

    PaymentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
