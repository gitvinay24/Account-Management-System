package com.businessmanager.model;

import com.businessmanager.enums.BusinessType;
import java.sql.Timestamp;

public class Business {
    private int id;
    private String name;
    private BusinessType type;
    private String registrationNumber;
    private String taxId;
    private String email;
    private String phone;
    private String address;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public Business() {
    }

    public Business(int id, String name, BusinessType type, String email) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.email = email;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BusinessType getType() { return type; }
    public void setType(BusinessType type) { this.type = type; }

    public String getRegistrationNumber() { return registrationNumber; }
    public void setRegistrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; }

    public String getTaxId() { return taxId; }
    public void setTaxId(String taxId) { this.taxId = taxId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return name;
    }
}
