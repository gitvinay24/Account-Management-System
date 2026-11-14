package com.businessmanager.model;

import java.sql.Timestamp;

public class Category {
    private int id;
    private int businessId;
    private String name;
    private String description;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public Category() {
    }

    public Category(int id, int businessId, String name, String description) {
        this.id = id;
        this.businessId = businessId;
        this.name = name;
        this.description = description;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getBusinessId() { return businessId; }
    public void setBusinessId(int businessId) { this.businessId = businessId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return name;
    }
}
