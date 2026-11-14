package com.businessmanager.dao;

import com.businessmanager.database.DBConnection;
import com.businessmanager.config.DemoModeConfig;
import com.businessmanager.model.Employee;
import com.businessmanager.security.AccessControl;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EmployeeDAO {

    public List<Employee> findByBusiness(int businessId) throws SQLException {
        if (DemoModeConfig.isEnabled()) return new ArrayList<>();
        List<Employee> list = new ArrayList<>();
        String sql = "SELECT e.*, b.name as branch_name, u.role as user_role " +
                "FROM employees e " +
                "LEFT JOIN branches b ON e.branch_id = b.id " +
                "LEFT JOIN users u ON e.user_id = u.id " +
                "WHERE e.business_id = ? ORDER BY e.full_name ASC";
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

    public int countEmployees(int businessId) throws SQLException {
        if (DemoModeConfig.isEnabled()) return 4;
        String sql = "SELECT COUNT(*) FROM employees WHERE business_id = ? AND active = 1";
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

    public Employee findById(int id) throws SQLException {
        if (DemoModeConfig.isEnabled()) return null;
        String sql = "SELECT e.*, b.name as branch_name, u.role as user_role " +
                "FROM employees e " +
                "LEFT JOIN branches b ON e.branch_id = b.id " +
                "LEFT JOIN users u ON e.user_id = u.id " +
                "WHERE e.id = ?";
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

    public int create(Employee e) throws SQLException {
        AccessControl.requireWrite("EMPLOYEES");
        String sql = "INSERT INTO employees (business_id, branch_id, user_id, employee_code, full_name, email, phone, position, department, salary, hire_date, active) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, e.getBusinessId());
            setNullableInt(pstmt, 2, e.getBranchId());
            setNullableInt(pstmt, 3, e.getUserId());
            pstmt.setString(4, e.getEmployeeCode());
            pstmt.setString(5, e.getFullName());
            pstmt.setString(6, e.getEmail());
            pstmt.setString(7, e.getPhone());
            pstmt.setString(8, e.getPosition());
            pstmt.setString(9, e.getDepartment());
            pstmt.setBigDecimal(10, e.getSalary());
            pstmt.setDate(11, e.getHireDate());
            pstmt.setBoolean(12, e.isActive());

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

    public boolean update(Employee e) throws SQLException {
        AccessControl.requireWrite("EMPLOYEES");
        String sql = "UPDATE employees SET branch_id = ?, user_id = ?, employee_code = ?, full_name = ?, email = ?, " +
                "phone = ?, position = ?, department = ?, salary = ?, hire_date = ?, active = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            setNullableInt(pstmt, 1, e.getBranchId());
            setNullableInt(pstmt, 2, e.getUserId());
            pstmt.setString(3, e.getEmployeeCode());
            pstmt.setString(4, e.getFullName());
            pstmt.setString(5, e.getEmail());
            pstmt.setString(6, e.getPhone());
            pstmt.setString(7, e.getPosition());
            pstmt.setString(8, e.getDepartment());
            pstmt.setBigDecimal(9, e.getSalary());
            pstmt.setDate(10, e.getHireDate());
            pstmt.setBoolean(11, e.isActive());
            pstmt.setInt(12, e.getId());
            return pstmt.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        AccessControl.requireWrite("EMPLOYEES");
        String sql = "DELETE FROM employees WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    private void setNullableInt(PreparedStatement pstmt, int index, Integer value) throws SQLException {
        if (value != null && value > 0) {
            pstmt.setInt(index, value);
        } else {
            pstmt.setNull(index, Types.INTEGER);
        }
    }

    private Employee mapResultSet(ResultSet rs) throws SQLException {
        Employee e = new Employee();
        e.setId(rs.getInt("id"));
        e.setBusinessId(rs.getInt("business_id"));
        int branchId = rs.getInt("branch_id");
        e.setBranchId(rs.wasNull() ? null : branchId);
        int userId = rs.getInt("user_id");
        e.setUserId(rs.wasNull() ? null : userId);
        e.setEmployeeCode(rs.getString("employee_code"));
        e.setFullName(rs.getString("full_name"));
        e.setEmail(rs.getString("email"));
        e.setPhone(rs.getString("phone"));
        e.setPosition(rs.getString("position"));
        e.setDepartment(rs.getString("department"));
        e.setSalary(rs.getBigDecimal("salary"));
        e.setHireDate(rs.getDate("hire_date"));
        e.setActive(rs.getBoolean("active"));
        e.setCreatedAt(rs.getTimestamp("created_at"));
        try {
            e.setBranchName(rs.getString("branch_name"));
            e.setUserRole(rs.getString("user_role"));
        } catch (SQLException ignored) {
        }
        return e;
    }
}
