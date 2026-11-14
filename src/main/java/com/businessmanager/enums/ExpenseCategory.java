package com.businessmanager.enums;

public enum ExpenseCategory {
    RENT("Rent & Lease"),
    UTILITIES("Utilities & Bills"),
    SALARIES("Salaries & Payroll"),
    SUPPLIES("Office Supplies"),
    MARKETING("Marketing & Ads"),
    MAINTENANCE("Maintenance & Repairs"),
    OTHER("Other Expense");

    private final String displayName;

    ExpenseCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
