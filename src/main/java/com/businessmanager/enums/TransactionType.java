package com.businessmanager.enums;

public enum TransactionType {
    SALE("Sale"),
    PURCHASE("Purchase"),
    ADJUSTMENT("Stock Adjustment"),
    ADJUSTMENT_ADD("Stock Added"),
    ADJUSTMENT_REMOVE("Stock Removed");

    private final String displayName;

    TransactionType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
