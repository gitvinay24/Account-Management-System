package com.businessmanager.model;

import com.businessmanager.enums.UserRole;
import java.sql.Timestamp;

public class User {
    private int id;
    private int businessId;
    private Integer branchId;
    private String fullName;
    private String username;
    private String email;
    private String phone;
    private String passwordHash;
    private String salt;
    /**
     * Short-lived registration secret used only while the multi-screen sign-up wizard is open.
     * It is deliberately separate from {@code passwordHash} so a raw password can never be
     * mistaken for a value that is safe to persist.
     */
    private transient char[] registrationPassword;
    private UserRole role;
    private boolean active;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Additional display field
    private String branchName;

    public User() {
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getBusinessId() { return businessId; }
    public void setBusinessId(int businessId) { this.businessId = businessId; }

    public Integer getBranchId() { return branchId; }
    public void setBranchId(Integer branchId) { this.branchId = branchId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getSalt() { return salt; }
    public void setSalt(String salt) { this.salt = salt; }

    public void setRegistrationPassword(char[] password) {
        clearRegistrationPassword();
        registrationPassword = password == null ? null : password.clone();
    }

    public char[] getRegistrationPassword() {
        return registrationPassword == null ? null : registrationPassword.clone();
    }

    public void clearRegistrationPassword() {
        if (registrationPassword != null) {
            java.util.Arrays.fill(registrationPassword, '\0');
            registrationPassword = null;
        }
    }

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    public String getBranchName() { return branchName; }
    public void setBranchName(String branchName) { this.branchName = branchName; }

    @Override
    public String toString() {
        return fullName + " (" + (role != null ? role.getDisplayName() : "") + ")";
    }
}
