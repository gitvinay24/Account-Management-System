package com.businessmanager.dao;

import com.businessmanager.database.DBConnection;
import com.businessmanager.config.DemoModeConfig;
import com.businessmanager.demo.DemoDataStore;
import com.businessmanager.model.Category;
import com.businessmanager.security.AccessControl;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CategoryDAO {

    public List<Category> findByBusiness(int businessId) throws SQLException {
        if (DemoModeConfig.isEnabled()) return DemoDataStore.categories(businessId);
        List<Category> list = new ArrayList<>();
        String sql = "SELECT * FROM categories WHERE business_id = ? ORDER BY name ASC";
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

    public Category findById(int id) throws SQLException {
        if (DemoModeConfig.isEnabled()) return DemoDataStore.category(id);
        String sql = "SELECT * FROM categories WHERE id = ?";
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

    public int create(Category c) throws SQLException {
        AccessControl.requireWrite("CATEGORIES");
        if (DemoModeConfig.isEnabled()) return DemoDataStore.save(c);
        String sql = "INSERT INTO categories (business_id, name, description) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, c.getBusinessId());
            pstmt.setString(2, c.getName());
            pstmt.setString(3, c.getDescription());
            int affected = pstmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        int generatedId = rs.getInt(1);
                        c.setId(generatedId);
                        return generatedId;
                    }
                }
            }
        }
        return 0;
    }

    public boolean update(Category c) throws SQLException {
        AccessControl.requireWrite("CATEGORIES");
        if (DemoModeConfig.isEnabled()) return DemoDataStore.category(c.getId()) != null && DemoDataStore.save(c) > 0;
        String sql = "UPDATE categories SET name = ?, description = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, c.getName());
            pstmt.setString(2, c.getDescription());
            pstmt.setInt(3, c.getId());
            return pstmt.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        AccessControl.requireWrite("CATEGORIES");
        if (DemoModeConfig.isEnabled()) return DemoDataStore.deleteCategory(id);
        String sql = "DELETE FROM categories WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    public boolean hasAssociatedProducts(int categoryId) throws SQLException {
        if (DemoModeConfig.isEnabled()) return DemoDataStore.categoryInUse(categoryId);
        String sql = "SELECT 1 FROM products WHERE category_id = ? LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, categoryId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private Category mapResultSet(ResultSet rs) throws SQLException {
        Category c = new Category();
        c.setId(rs.getInt("id"));
        c.setBusinessId(rs.getInt("business_id"));
        c.setName(rs.getString("name"));
        c.setDescription(rs.getString("description"));
        c.setCreatedAt(rs.getTimestamp("created_at"));
        c.setUpdatedAt(rs.getTimestamp("updated_at"));
        return c;
    }
}
