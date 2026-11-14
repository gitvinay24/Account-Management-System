package com.businessmanager.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class RecentActivityDTO {
    private Timestamp timestamp;
    private String type; // "SALE", "PURCHASE", "INVENTORY"
    private String title;
    private String description;
    private BigDecimal amount;
    private Integer referenceId;

    public RecentActivityDTO() {}

    public RecentActivityDTO(Timestamp timestamp, String type, String title, String description, BigDecimal amount, Integer referenceId) {
        this.timestamp = timestamp;
        this.type = type;
        this.title = title;
        this.description = description;
        this.amount = amount;
        this.referenceId = referenceId;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Integer getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(Integer referenceId) {
        this.referenceId = referenceId;
    }
}
