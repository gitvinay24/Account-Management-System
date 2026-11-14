package com.businessmanager.dao;

import com.businessmanager.database.DBConnection;
import com.businessmanager.enums.BusinessType;
import com.businessmanager.model.Business;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BusinessDAO {

    public Business findById(int id) throws SQLException {
        String sql = "SELECT * FROM businesses WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToBusiness(rs);
                }
            }
        }
        return null;
    }

    public Business findByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM businesses WHERE email = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToBusiness(rs);
                }
            }
        }
        return null;
    }

    public int create(Business business) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return create(conn, business);
        }
    }

    /** Creates a business using a caller-owned transaction connection. */
    public int create(Connection conn, Business business) throws SQLException {
        String sql = "INSERT INTO businesses (name, type, registration_number, tax_id, email, phone, address, city, state, postal_code, country) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, business.getName());
            pstmt.setString(2, business.getType().name());
            pstmt.setString(3, business.getRegistrationNumber());
            pstmt.setString(4, business.getTaxId());
            pstmt.setString(5, business.getEmail());
            pstmt.setString(6, business.getPhone());
            pstmt.setString(7, business.getAddress());
            pstmt.setString(8, business.getCity());
            pstmt.setString(9, business.getState());
            pstmt.setString(10, business.getPostalCode());
            pstmt.setString(11, business.getCountry() != null ? business.getCountry() : "India");

            int affected = pstmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        int generatedId = rs.getInt(1);
                        business.setId(generatedId);
                        return generatedId;
                    }
                }
            }
        }
        return 0;
    }

    public boolean update(Business business) throws SQLException {
        String sql = "UPDATE businesses SET name = ?, type = ?, registration_number = ?, tax_id = ?, email = ?, " +
                "phone = ?, address = ?, city = ?, state = ?, postal_code = ?, country = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, business.getName());
            pstmt.setString(2, business.getType().name());
            pstmt.setString(3, business.getRegistrationNumber());
            pstmt.setString(4, business.getTaxId());
            pstmt.setString(5, business.getEmail());
            pstmt.setString(6, business.getPhone());
            pstmt.setString(7, business.getAddress());
            pstmt.setString(8, business.getCity());
            pstmt.setString(9, business.getState());
            pstmt.setString(10, business.getPostalCode());
            pstmt.setString(11, business.getCountry());
            pstmt.setInt(12, business.getId());
            return pstmt.executeUpdate() > 0;
        }
    }

    private Business mapResultSetToBusiness(ResultSet rs) throws SQLException {
        Business b = new Business();
        b.setId(rs.getInt("id"));
        b.setName(rs.getString("name"));
        b.setType(BusinessType.valueOf(rs.getString("type")));
        b.setRegistrationNumber(rs.getString("registration_number"));
        b.setTaxId(rs.getString("tax_id"));
        b.setEmail(rs.getString("email"));
        b.setPhone(rs.getString("phone"));
        b.setAddress(rs.getString("address"));
        b.setCity(rs.getString("city"));
        b.setState(rs.getString("state"));
        b.setPostalCode(rs.getString("postal_code"));
        b.setCountry(rs.getString("country"));
        b.setCreatedAt(rs.getTimestamp("created_at"));
        b.setUpdatedAt(rs.getTimestamp("updated_at"));
        return b;
    }
}
