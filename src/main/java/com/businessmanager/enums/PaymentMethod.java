package com.businessmanager.enums;

public enum PaymentMethod {
    CASH("Cash"),
    CARD("Card"),
    UPI("UPI / QR Code"),
    BANK_TRANSFER("Bank Transfer"),
    OTHER("Other");

    private final String displayName;

    PaymentMethod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
