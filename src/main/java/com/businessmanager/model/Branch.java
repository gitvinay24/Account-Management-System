package com.businessmanager.model;

import java.sql.Timestamp;

public class Branch {
    private int id;
    private int businessId;
    private String name;
    private String code;
    private String phone;
    private String email;
    private String address;
    private String city;
    private String state;
    private boolean isMain;
    private boolean active;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public Branch() {
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getBusinessId() { return businessId; }
    public void setBusinessId(int businessId) { this.businessId = businessId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public boolean isMain() { return isMain; }
    public void setMain(boolean main) { isMain = main; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return name + (isMain ? " (Main HQ)" : "");
    }
}
