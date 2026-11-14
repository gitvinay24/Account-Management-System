package com.businessmanager.model;

import com.businessmanager.enums.ExpenseCategory;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;

public class Expense {
    private int id;
    private int businessId;
    private Integer branchId;
    private int userId;
    private ExpenseCategory category = ExpenseCategory.OTHER;
    private String title;
    private BigDecimal amount = BigDecimal.ZERO;
    private String description;
    private Date expenseDate;
    private Timestamp createdAt;

    // Display fields
    private String userName;
    private String branchName;

    public Expense() {
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getBusinessId() { return businessId; }
    public void setBusinessId(int businessId) { this.businessId = businessId; }

    public Integer getBranchId() { return branchId; }
    public void setBranchId(Integer branchId) { this.branchId = branchId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public ExpenseCategory getCategory() { return category; }
    public void setCategory(ExpenseCategory category) { this.category = category; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Date getExpenseDate() { return expenseDate; }
    public void setExpenseDate(Date expenseDate) { this.expenseDate = expenseDate; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getBranchName() { return branchName; }
    public void setBranchName(String branchName) { this.branchName = branchName; }
}
