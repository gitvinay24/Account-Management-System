package com.businessmanager.dao;

import com.businessmanager.database.DBConnection;
import com.businessmanager.config.DemoModeConfig;
import com.businessmanager.demo.DemoDataStore;
import com.businessmanager.model.Supplier;
import com.businessmanager.security.AccessControl;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SupplierDAO {

    public List<Supplier> findByBusiness(int businessId) throws SQLException {
        if (DemoModeConfig.isEnabled()) return DemoDataStore.suppliers(businessId);
        List<Supplier> list = new ArrayList<>();
        String sql = "SELECT s.*, " +
                "COALESCE((SELECT SUM(p.total) FROM purchases p WHERE p.supplier_id = s.id), 0.00) as total_purchased, " +
                "COALESCE((SELECT SUM(p.paid_amount) FROM purchases p WHERE p.supplier_id = s.id), 0.00) as total_paid " +
                "FROM suppliers s WHERE s.business_id = ? ORDER BY s.name ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, businessId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Supplier sup = mapResultSet(rs);
                    BigDecimal purchased = rs.getBigDecimal("total_purchased");
                    BigDecimal paid = rs.getBigDecimal("total_paid");
                    sup.setTotalPurchased(purchased);
                    sup.setTotalPaid(paid);
                    sup.setOutstandingBalance(purchased.subtract(paid));
                    list.add(sup);
                }
            }
        }
        return list;
    }

    public Supplier findById(int id) throws SQLException {
        if (DemoModeConfig.isEnabled()) return DemoDataStore.supplier(id);
        String sql = "SELECT s.*, " +
                "COALESCE((SELECT SUM(p.total) FROM purchases p WHERE p.supplier_id = s.id), 0.00) as total_purchased, " +
                "COALESCE((SELECT SUM(p.paid_amount) FROM purchases p WHERE p.supplier_id = s.id), 0.00) as total_paid " +
                "FROM suppliers s WHERE s.id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Supplier sup = mapResultSet(rs);
                    BigDecimal purchased = rs.getBigDecimal("total_purchased");
                    BigDecimal paid = rs.getBigDecimal("total_paid");
                    sup.setTotalPurchased(purchased);
                    sup.setTotalPaid(paid);
                    sup.setOutstandingBalance(purchased.subtract(paid));
                    return sup;
                }
            }
        }
        return null;
    }

    public int create(Supplier s) throws SQLException {
        AccessControl.requireWrite("SUPPLIERS");
        if (DemoModeConfig.isEnabled()) return DemoDataStore.save(s);
        String sql = "INSERT INTO suppliers (business_id, name, contact_person, email, phone, address, city, tax_id, active) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, s.getBusinessId());
            pstmt.setString(2, s.getName());
            pstmt.setString(3, s.getContactPerson());
            pstmt.setString(4, s.getEmail());
            pstmt.setString(5, s.getPhone());
            pstmt.setString(6, s.getAddress());
            pstmt.setString(7, s.getCity());
            pstmt.setString(8, s.getTaxId());
            pstmt.setBoolean(9, s.isActive());

            int affected = pstmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        int id = rs.getInt(1);
                        s.setId(id);
                        return id;
                    }
                }
            }
        }
        return 0;
    }

    public boolean update(Supplier s) throws SQLException {
        AccessControl.requireWrite("SUPPLIERS");
        if (DemoModeConfig.isEnabled()) return DemoDataStore.supplier(s.getId()) != null && DemoDataStore.save(s) > 0;
        String sql = "UPDATE suppliers SET name = ?, contact_person = ?, email = ?, phone = ?, " +
                "address = ?, city = ?, tax_id = ?, active = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, s.getName());
            pstmt.setString(2, s.getContactPerson());
            pstmt.setString(3, s.getEmail());
            pstmt.setString(4, s.getPhone());
            pstmt.setString(5, s.getAddress());
            pstmt.setString(6, s.getCity());
            pstmt.setString(7, s.getTaxId());
            pstmt.setBoolean(8, s.isActive());
            pstmt.setInt(9, s.getId());
            return pstmt.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        AccessControl.requireWrite("SUPPLIERS");
        if (DemoModeConfig.isEnabled()) return DemoDataStore.deleteSupplier(id);
        String sql = "DELETE FROM suppliers WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    private Supplier mapResultSet(ResultSet rs) throws SQLException {
        Supplier s = new Supplier();
        s.setId(rs.getInt("id"));
        s.setBusinessId(rs.getInt("business_id"));
        s.setName(rs.getString("name"));
        s.setContactPerson(rs.getString("contact_person"));
        s.setEmail(rs.getString("email"));
        s.setPhone(rs.getString("phone"));
        s.setAddress(rs.getString("address"));
        s.setCity(rs.getString("city"));
        s.setTaxId(rs.getString("tax_id"));
        s.setActive(rs.getBoolean("active"));
        s.setCreatedAt(rs.getTimestamp("created_at"));
        s.setUpdatedAt(rs.getTimestamp("updated_at"));
        return s;
    }
}
