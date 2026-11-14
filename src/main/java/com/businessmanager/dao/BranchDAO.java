package com.businessmanager.dao;

import com.businessmanager.database.DBConnection;
import com.businessmanager.config.DemoModeConfig;
import com.businessmanager.demo.DemoDataStore;
import com.businessmanager.model.Branch;
import com.businessmanager.security.AccessControl;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BranchDAO {

    public List<Branch> findByBusiness(int businessId) throws SQLException {
        if (DemoModeConfig.isEnabled()) return DemoDataStore.branches(businessId);
        List<Branch> list = new ArrayList<>();
        String sql = "SELECT * FROM branches WHERE business_id = ? ORDER BY is_main DESC, name ASC";
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

    public Branch findMainBranch(int businessId) throws SQLException {
        List<Branch> branches = findByBusiness(businessId);
        return branches.isEmpty() ? null : branches.get(0);
    }

    public int countBranches(int businessId) throws SQLException {
        if (DemoModeConfig.isEnabled()) return DemoDataStore.branches(businessId).size();
        String sql = "SELECT COUNT(*) FROM branches WHERE business_id = ? AND active = 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, businessId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    public Branch findById(int id) throws SQLException {
        if (DemoModeConfig.isEnabled()) return DemoDataStore.branch(id);
        String sql = "SELECT * FROM branches WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs);
                }
            }
        }
        return null;
    }

    public int create(Branch b) throws SQLException {
        AccessControl.requireWrite("BRANCHES");
        if (DemoModeConfig.isEnabled()) return DemoDataStore.save(b);
        String sql = "INSERT INTO branches (business_id, name, code, phone, email, address, city, state, is_main, active) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, b.getBusinessId());
            pstmt.setString(2, b.getName());
            pstmt.setString(3, b.getCode());
            pstmt.setString(4, b.getPhone());
            pstmt.setString(5, b.getEmail());
            pstmt.setString(6, b.getAddress());
            pstmt.setString(7, b.getCity());
            pstmt.setString(8, b.getState());
            pstmt.setBoolean(9, b.isMain());
            pstmt.setBoolean(10, b.isActive());

            int affected = pstmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        int id = rs.getInt(1);
                        b.setId(id);
                        return id;
                    }
                }
            }
        }
        return 0;
    }

    public boolean update(Branch b) throws SQLException {
        AccessControl.requireWrite("BRANCHES");
        if (DemoModeConfig.isEnabled()) return DemoDataStore.branch(b.getId()) != null && DemoDataStore.save(b) > 0;
        String sql = "UPDATE branches SET name = ?, code = ?, phone = ?, email = ?, address = ?, city = ?, state = ?, is_main = ?, active = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, b.getName());
            pstmt.setString(2, b.getCode());
            pstmt.setString(3, b.getPhone());
            pstmt.setString(4, b.getEmail());
            pstmt.setString(5, b.getAddress());
            pstmt.setString(6, b.getCity());
            pstmt.setString(7, b.getState());
            pstmt.setBoolean(8, b.isMain());
            pstmt.setBoolean(9, b.isActive());
            pstmt.setInt(10, b.getId());
            return pstmt.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        AccessControl.requireWrite("BRANCHES");
        if (DemoModeConfig.isEnabled()) return DemoDataStore.deleteBranch(id);
        String sql = "DELETE FROM branches WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    private Branch mapResultSet(ResultSet rs) throws SQLException {
        Branch b = new Branch();
        b.setId(rs.getInt("id"));
        b.setBusinessId(rs.getInt("business_id"));
        b.setName(rs.getString("name"));
        b.setCode(rs.getString("code"));
        b.setPhone(rs.getString("phone"));
        b.setEmail(rs.getString("email"));
        b.setAddress(rs.getString("address"));
        b.setCity(rs.getString("city"));
        b.setState(rs.getString("state"));
        b.setMain(rs.getBoolean("is_main"));
        b.setActive(rs.getBoolean("active"));
        b.setCreatedAt(rs.getTimestamp("created_at"));
        b.setUpdatedAt(rs.getTimestamp("updated_at"));
        return b;
    }
}
