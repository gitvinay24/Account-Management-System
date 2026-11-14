package com.businessmanager.dao;

import com.businessmanager.database.DBConnection;
import com.businessmanager.config.DemoModeConfig;
import com.businessmanager.demo.DemoDataStore;
import com.businessmanager.model.Customer;
import com.businessmanager.security.AccessControl;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CustomerDAO {

    public List<Customer> findByBusiness(int businessId) throws SQLException {
        if (DemoModeConfig.isEnabled()) return DemoDataStore.customers(businessId);
        List<Customer> list = new ArrayList<>();
        String sql = "SELECT c.*, " +
                "COALESCE((SELECT SUM(s.total) FROM sales s WHERE s.customer_id = c.id), 0.00) as total_purchased, " +
                "COALESCE((SELECT SUM(s.paid_amount) FROM sales s WHERE s.customer_id = c.id), 0.00) as total_paid " +
                "FROM customers c WHERE c.business_id = ? ORDER BY c.name ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, businessId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Customer cust = mapResultSet(rs);
                    BigDecimal purchased = rs.getBigDecimal("total_purchased");
                    BigDecimal paid = rs.getBigDecimal("total_paid");
                    cust.setTotalPurchased(purchased);
                    cust.setTotalPaid(paid);
                    cust.setOutstandingBalance(purchased.subtract(paid));
                    list.add(cust);
                }
            }
        }
        return list;
    }

    public Customer findById(int id) throws SQLException {
        if (DemoModeConfig.isEnabled()) return DemoDataStore.customer(id);
        String sql = "SELECT c.*, " +
                "COALESCE((SELECT SUM(s.total) FROM sales s WHERE s.customer_id = c.id), 0.00) as total_purchased, " +
                "COALESCE((SELECT SUM(s.paid_amount) FROM sales s WHERE s.customer_id = c.id), 0.00) as total_paid " +
                "FROM customers c WHERE c.id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Customer cust = mapResultSet(rs);
                    BigDecimal purchased = rs.getBigDecimal("total_purchased");
                    BigDecimal paid = rs.getBigDecimal("total_paid");
                    cust.setTotalPurchased(purchased);
                    cust.setTotalPaid(paid);
                    cust.setOutstandingBalance(purchased.subtract(paid));
                    return cust;
                }
            }
        }
        return null;
    }

    public int create(Customer c) throws SQLException {
        AccessControl.requireWrite("CUSTOMERS");
        if (DemoModeConfig.isEnabled()) return DemoDataStore.save(c);
        String sql = "INSERT INTO customers (business_id, name, email, phone, address, city, tax_id, active) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, c.getBusinessId());
            pstmt.setString(2, c.getName());
            pstmt.setString(3, c.getEmail());
            pstmt.setString(4, c.getPhone());
            pstmt.setString(5, c.getAddress());
            pstmt.setString(6, c.getCity());
            pstmt.setString(7, c.getTaxId());
            pstmt.setBoolean(8, c.isActive());

            int affected = pstmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        int id = rs.getInt(1);
                        c.setId(id);
                        return id;
                    }
                }
            }
        }
        return 0;
    }

    public boolean update(Customer c) throws SQLException {
        AccessControl.requireWrite("CUSTOMERS");
        if (DemoModeConfig.isEnabled()) return DemoDataStore.customer(c.getId()) != null && DemoDataStore.save(c) > 0;
        String sql = "UPDATE customers SET name = ?, email = ?, phone = ?, address = ?, city = ?, tax_id = ?, active = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, c.getName());
            pstmt.setString(2, c.getEmail());
            pstmt.setString(3, c.getPhone());
            pstmt.setString(4, c.getAddress());
            pstmt.setString(5, c.getCity());
            pstmt.setString(6, c.getTaxId());
            pstmt.setBoolean(7, c.isActive());
            pstmt.setInt(8, c.getId());
            return pstmt.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        AccessControl.requireWrite("CUSTOMERS");
        if (DemoModeConfig.isEnabled()) return DemoDataStore.deleteCustomer(id);
        String sql = "DELETE FROM customers WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    private Customer mapResultSet(ResultSet rs) throws SQLException {
        Customer c = new Customer();
        c.setId(rs.getInt("id"));
        c.setBusinessId(rs.getInt("business_id"));
        c.setName(rs.getString("name"));
        c.setEmail(rs.getString("email"));
        c.setPhone(rs.getString("phone"));
        c.setAddress(rs.getString("address"));
        c.setCity(rs.getString("city"));
        c.setTaxId(rs.getString("tax_id"));
        c.setActive(rs.getBoolean("active"));
        c.setCreatedAt(rs.getTimestamp("created_at"));
        c.setUpdatedAt(rs.getTimestamp("updated_at"));
        return c;
    }
}
