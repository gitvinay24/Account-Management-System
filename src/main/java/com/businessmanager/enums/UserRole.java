package com.businessmanager.enums;

public enum UserRole {
    ADMIN("Administrator"),
    OWNER("Business Owner"),
    MANAGER("Branch / Store Manager"),
    CASHIER("Cashier / POS Operator"),
    INVENTORY_MANAGER("Inventory Specialist"),
    GUEST("Guest sample access");

    private final String displayName;

    UserRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
