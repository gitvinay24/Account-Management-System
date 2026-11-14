package com.businessmanager.model;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;

public class Employee {
    private int id;
    private int businessId;
    private Integer branchId;
    private Integer userId;
    private String employeeCode;
    private String fullName;
    private String email;
    private String phone;
    private String position;
    private String department;
    private BigDecimal salary = BigDecimal.ZERO;
    private Date hireDate;
    private boolean active = true;
    private Timestamp createdAt;

    // Display fields
    private String branchName;
    private String userRole;

    public Employee() {
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getBusinessId() { return businessId; }
    public void setBusinessId(int businessId) { this.businessId = businessId; }

    public Integer getBranchId() { return branchId; }
    public void setBranchId(Integer branchId) { this.branchId = branchId; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getEmployeeCode() { return employeeCode; }
    public void setEmployeeCode(String employeeCode) { this.employeeCode = employeeCode; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public BigDecimal getSalary() { return salary; }
    public void setSalary(BigDecimal salary) { this.salary = salary; }

    public Date getHireDate() { return hireDate; }
    public void setHireDate(Date hireDate) { this.hireDate = hireDate; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public String getBranchName() { return branchName; }
    public void setBranchName(String branchName) { this.branchName = branchName; }

    public String getUserRole() { return userRole; }
    public void setUserRole(String userRole) { this.userRole = userRole; }

    @Override
    public String toString() {
        return fullName + " (" + employeeCode + ")";
    }
}
