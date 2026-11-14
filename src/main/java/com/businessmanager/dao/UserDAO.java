package com.businessmanager.dao;

import com.businessmanager.database.DBConnection;
import com.businessmanager.enums.UserRole;
import com.businessmanager.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    public User findById(int id) throws SQLException {
        String sql = "SELECT u.*, b.name as branch_name FROM users u LEFT JOIN branches b ON u.branch_id = b.id WHERE u.id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        }
        return null;
    }

    public User findByUsernameOrEmail(String identifier) throws SQLException {
        String sql = "SELECT u.*, b.name as branch_name FROM users u LEFT JOIN branches b ON u.branch_id = b.id WHERE u.username = ? OR u.email = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, identifier);
            pstmt.setString(2, identifier);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        }
        return null;
    }

    public boolean existsByEmail(String email) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE email = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean existsByUsername(int businessId, String username) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE business_id = ? AND username = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, businessId);
            pstmt.setString(2, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public int create(User user) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return create(conn, user);
        }
    }

    /** Creates a user using a caller-owned transaction connection. */
    public int create(Connection conn, User user) throws SQLException {
        String sql = "INSERT INTO users (business_id, branch_id, full_name, username, email, phone, password_hash, salt, role, active) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, user.getBusinessId());
            if (user.getBranchId() != null && user.getBranchId() > 0) {
                pstmt.setInt(2, user.getBranchId());
            } else {
                pstmt.setNull(2, Types.INTEGER);
            }
            pstmt.setString(3, user.getFullName());
            pstmt.setString(4, user.getUsername());
            pstmt.setString(5, user.getEmail());
            pstmt.setString(6, user.getPhone());
            pstmt.setString(7, user.getPasswordHash());
            pstmt.setString(8, user.getSalt());
            pstmt.setString(9, user.getRole().name());
            pstmt.setBoolean(10, user.isActive());

            int affected = pstmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        int id = rs.getInt(1);
                        user.setId(id);
                        return id;
                    }
                }
            }
        }
        return 0;
    }

    public boolean updatePassword(int userId, String newHash, String newSalt) throws SQLException {
        String sql = "UPDATE users SET password_hash = ?, salt = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newHash);
            pstmt.setString(2, newSalt);
            pstmt.setInt(3, userId);
            return pstmt.executeUpdate() > 0;
        }
    }

    public List<User> findByBusiness(int businessId) throws SQLException {
        List<User> list = new ArrayList<>();
        String sql = "SELECT u.*, b.name as branch_name FROM users u LEFT JOIN branches b ON u.branch_id = b.id WHERE u.business_id = ? ORDER BY u.full_name";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, businessId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToUser(rs));
                }
            }
        }
        return list;
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setBusinessId(rs.getInt("business_id"));
        int branchId = rs.getInt("branch_id");
        u.setBranchId(rs.wasNull() ? null : branchId);
        u.setFullName(rs.getString("full_name"));
        u.setUsername(rs.getString("username"));
        u.setEmail(rs.getString("email"));
        u.setPhone(rs.getString("phone"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setSalt(rs.getString("salt"));
        u.setRole(UserRole.valueOf(rs.getString("role")));
        u.setActive(rs.getBoolean("active"));
        u.setCreatedAt(rs.getTimestamp("created_at"));
        u.setUpdatedAt(rs.getTimestamp("updated_at"));
        try {
            u.setBranchName(rs.getString("branch_name"));
        } catch (SQLException ignored) {
        }
        return u;
    }
}
