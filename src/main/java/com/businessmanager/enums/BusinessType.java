package com.businessmanager.enums;

public enum BusinessType {
    SMALL("Small Business"),
    LARGE("Large / Enterprise Business");

    private final String displayName;

    BusinessType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
