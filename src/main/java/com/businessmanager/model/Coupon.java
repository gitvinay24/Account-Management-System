package com.businessmanager.model;

import java.math.BigDecimal;

public class Coupon {

    public enum DiscountType {
        PERCENTAGE,
        FLAT
    }

    private String code;
    private String description;
    private DiscountType type;
    private BigDecimal value;
    private BigDecimal minPurchase;
    private boolean active;

    public Coupon() {
    }

    public Coupon(String code, String description, DiscountType type, BigDecimal value, BigDecimal minPurchase) {
        this.code = code;
        this.description = description;
        this.type = type;
        this.value = value;
        this.minPurchase = minPurchase != null ? minPurchase : BigDecimal.ZERO;
        this.active = true;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public DiscountType getType() { return type; }
    public void setType(DiscountType type) { this.type = type; }

    public BigDecimal getValue() { return value; }
    public void setValue(BigDecimal value) { this.value = value; }

    public BigDecimal getMinPurchase() { return minPurchase; }
    public void setMinPurchase(BigDecimal minPurchase) { this.minPurchase = minPurchase; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
