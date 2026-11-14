package com.businessmanager.dao;

import com.businessmanager.database.DBConnection;
import com.businessmanager.config.DemoModeConfig;
import com.businessmanager.demo.DemoDataStore;
import com.businessmanager.enums.ExpenseCategory;
import com.businessmanager.model.Expense;
import com.businessmanager.security.AccessControl;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ExpenseDAO {

    public List<Expense> findByBusiness(int businessId) throws SQLException {
        if (DemoModeConfig.isEnabled()) return DemoDataStore.expenses(businessId);
        List<Expense> list = new ArrayList<>();
        String sql = "SELECT e.*, u.full_name as user_name, b.name as branch_name " +
                "FROM expenses e " +
                "LEFT JOIN users u ON e.user_id = u.id " +
                "LEFT JOIN branches b ON e.branch_id = b.id " +
                "WHERE e.business_id = ? ORDER BY e.expense_date DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, businessId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }
        }
        return list;
    }

    public BigDecimal getTotalExpenses(int businessId) throws SQLException {
        if (DemoModeConfig.isEnabled()) return DemoDataStore.totalExpenses();
        String sql = "SELECT COALESCE(SUM(amount), 0.00) FROM expenses WHERE business_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, businessId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal(1);
                }
            }
        }
        return BigDecimal.ZERO;
    }

    public int create(Expense e) throws SQLException {
        AccessControl.requireWrite("EXPENSES");
        if (DemoModeConfig.isEnabled()) return DemoDataStore.save(e);
        String sql = "INSERT INTO expenses (business_id, branch_id, user_id, category, title, amount, description, expense_date) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, e.getBusinessId());
            if (e.getBranchId() != null && e.getBranchId() > 0) {
                pstmt.setInt(2, e.getBranchId());
            } else {
                pstmt.setNull(2, Types.INTEGER);
            }
            pstmt.setInt(3, e.getUserId());
            pstmt.setString(4, e.getCategory().name());
            pstmt.setString(5, e.getTitle());
            pstmt.setBigDecimal(6, e.getAmount());
            pstmt.setString(7, e.getDescription());
            pstmt.setDate(8, e.getExpenseDate());

            int affected = pstmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        int id = rs.getInt(1);
                        e.setId(id);
                        return id;
                    }
                }
            }
        }
        return 0;
    }

    public boolean update(Expense e) throws SQLException {
        AccessControl.requireWrite("EXPENSES");
        if (DemoModeConfig.isEnabled()) return DemoDataStore.save(e) > 0;
        String sql = "UPDATE expenses SET branch_id = ?, category = ?, title = ?, amount = ?, description = ?, expense_date = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (e.getBranchId() != null && e.getBranchId() > 0) {
                pstmt.setInt(1, e.getBranchId());
            } else {
                pstmt.setNull(1, Types.INTEGER);
            }
            pstmt.setString(2, e.getCategory().name());
            pstmt.setString(3, e.getTitle());
            pstmt.setBigDecimal(4, e.getAmount());
            pstmt.setString(5, e.getDescription());
            pstmt.setDate(6, e.getExpenseDate());
            pstmt.setInt(7, e.getId());
            return pstmt.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        AccessControl.requireWrite("EXPENSES");
        if (DemoModeConfig.isEnabled()) return DemoDataStore.deleteExpense(id);
        String sql = "DELETE FROM expenses WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    private Expense mapResultSet(ResultSet rs) throws SQLException {
        Expense e = new Expense();
        e.setId(rs.getInt("id"));
        e.setBusinessId(rs.getInt("business_id"));
        int branchId = rs.getInt("branch_id");
        e.setBranchId(rs.wasNull() ? null : branchId);
        e.setUserId(rs.getInt("user_id"));
        e.setCategory(ExpenseCategory.valueOf(rs.getString("category")));
        e.setTitle(rs.getString("title"));
        e.setAmount(rs.getBigDecimal("amount"));
        e.setDescription(rs.getString("description"));
        e.setExpenseDate(rs.getDate("expense_date"));
        e.setCreatedAt(rs.getTimestamp("created_at"));
        try {
            e.setUserName(rs.getString("user_name"));
            e.setBranchName(rs.getString("branch_name"));
        } catch (SQLException ignored) {
        }
        return e;
    }
}
