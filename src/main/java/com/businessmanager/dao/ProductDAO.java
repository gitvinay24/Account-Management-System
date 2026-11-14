package com.businessmanager.dao;

import com.businessmanager.database.DBConnection;
import com.businessmanager.config.DemoModeConfig;
import com.businessmanager.demo.DemoDataStore;
import com.businessmanager.model.Product;
import com.businessmanager.security.AccessControl;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductDAO {

    public List<Product> findByBusiness(int businessId) throws SQLException {
        if (DemoModeConfig.isEnabled()) return DemoDataStore.products(businessId);
        List<Product> list = new ArrayList<>();
        String sql = "SELECT p.*, c.name as category_name, s.name as supplier_name " +
                "FROM products p " +
                "LEFT JOIN categories c ON p.category_id = c.id " +
                "LEFT JOIN suppliers s ON p.supplier_id = s.id " +
                "WHERE p.business_id = ? ORDER BY p.name ASC";
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

    /** Returns branch-available quantities while preserving product metadata for the POS. */
    public List<Product> findByBusinessAndBranch(int businessId, int branchId) throws SQLException {
        if (DemoModeConfig.isEnabled()) return DemoDataStore.products(businessId);
        List<Product> list = new ArrayList<>();
        String sql = "SELECT p.*, c.name AS category_name, s.name AS supplier_name, COALESCE(bi.quantity, 0) AS branch_quantity "
                + "FROM products p LEFT JOIN categories c ON p.category_id = c.id "
                + "LEFT JOIN suppliers s ON p.supplier_id = s.id "
                + "LEFT JOIN branch_inventory bi ON bi.product_id = p.id AND bi.branch_id = ? "
                + "WHERE p.business_id = ? AND p.active = 1 ORDER BY p.name";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, branchId);
            statement.setInt(2, businessId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    Product product = mapResultSet(result);
                    product.setStockQuantity(result.getInt("branch_quantity"));
                    list.add(product);
                }
            }
        }
        return list;
    }

    public List<Product> findLowStock(int businessId) throws SQLException {
        if (DemoModeConfig.isEnabled()) return DemoDataStore.lowStock();
        List<Product> list = new ArrayList<>();
        String sql = "SELECT p.*, c.name as category_name, s.name as supplier_name " +
                "FROM products p " +
                "LEFT JOIN categories c ON p.category_id = c.id " +
                "LEFT JOIN suppliers s ON p.supplier_id = s.id " +
                "WHERE p.business_id = ? AND p.stock_quantity <= p.min_stock AND p.active = 1 " +
                "ORDER BY p.stock_quantity ASC";
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

    public int countLowStock(int businessId) throws SQLException {
        if (DemoModeConfig.isEnabled()) return DemoDataStore.lowStock().size();
        String sql = "SELECT COUNT(*) FROM products WHERE business_id = ? AND stock_quantity <= min_stock AND active = 1";
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

    public Product findById(int id) throws SQLException {
        if (DemoModeConfig.isEnabled()) return DemoDataStore.product(id);
        String sql = "SELECT p.*, c.name as category_name, s.name as supplier_name " +
                "FROM products p " +
                "LEFT JOIN categories c ON p.category_id = c.id " +
                "LEFT JOIN suppliers s ON p.supplier_id = s.id " +
                "WHERE p.id = ?";
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

    public boolean existsBySku(int businessId, String sku, Integer excludeId) throws SQLException {
        if (DemoModeConfig.isEnabled()) return DemoDataStore.products(businessId).stream().anyMatch(p -> p.getSku() != null && p.getSku().equalsIgnoreCase(sku) && (excludeId == null || p.getId() != excludeId));
        String sql = "SELECT 1 FROM products WHERE business_id = ? AND sku = ?" + (excludeId != null ? " AND id != ?" : "");
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, businessId);
            pstmt.setString(2, sku);
            if (excludeId != null) {
                pstmt.setInt(3, excludeId);
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public int create(Product p) throws SQLException {
        AccessControl.requireWrite("PRODUCTS");
        if (DemoModeConfig.isEnabled()) return DemoDataStore.save(p);
        String sql = "INSERT INTO products (business_id, category_id, supplier_id, name, sku, barcode, description, " +
                "purchase_price, selling_price, stock_quantity, min_stock, unit, tax_rate, active) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, p.getBusinessId());
            setNullableInt(pstmt, 2, p.getCategoryId());
            setNullableInt(pstmt, 3, p.getSupplierId());
            pstmt.setString(4, p.getName());
            pstmt.setString(5, p.getSku());
            pstmt.setString(6, p.getBarcode());
            pstmt.setString(7, p.getDescription());
            pstmt.setBigDecimal(8, p.getPurchasePrice());
            pstmt.setBigDecimal(9, p.getSellingPrice());
            pstmt.setInt(10, p.getStockQuantity());
            pstmt.setInt(11, p.getMinStock());
            pstmt.setString(12, p.getUnit() != null ? p.getUnit() : "pcs");
            pstmt.setBigDecimal(13, p.getTaxRate());
            pstmt.setBoolean(14, p.isActive());

            int affected = pstmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        int generatedId = rs.getInt(1);
                        p.setId(generatedId);
                        ensureBranchInventoryForProduct(conn, p.getBusinessId(), generatedId, p.getStockQuantity(), p.getMinStock());
                        return generatedId;
                    }
                }
            }
        }
        return 0;
    }

    public boolean update(Product p) throws SQLException {
        AccessControl.requireWrite("PRODUCTS");
        if (DemoModeConfig.isEnabled()) return DemoDataStore.product(p.getId()) != null && DemoDataStore.save(p) > 0;
        String sql = "UPDATE products SET category_id = ?, supplier_id = ?, name = ?, sku = ?, barcode = ?, " +
                "description = ?, purchase_price = ?, selling_price = ?, stock_quantity = ?, min_stock = ?, " +
                "unit = ?, tax_rate = ?, active = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            setNullableInt(pstmt, 1, p.getCategoryId());
            setNullableInt(pstmt, 2, p.getSupplierId());
            pstmt.setString(3, p.getName());
            pstmt.setString(4, p.getSku());
            pstmt.setString(5, p.getBarcode());
            pstmt.setString(6, p.getDescription());
            pstmt.setBigDecimal(7, p.getPurchasePrice());
            pstmt.setBigDecimal(8, p.getSellingPrice());
            pstmt.setInt(9, p.getStockQuantity());
            pstmt.setInt(10, p.getMinStock());
            pstmt.setString(11, p.getUnit());
            pstmt.setBigDecimal(12, p.getTaxRate());
            pstmt.setBoolean(13, p.isActive());
            pstmt.setInt(14, p.getId());
            boolean success = pstmt.executeUpdate() > 0;
            if (success) {
                ensureBranchInventoryForProduct(conn, p.getBusinessId(), p.getId(), p.getStockQuantity(), p.getMinStock());
            }
            return success;
        }
    }

    public boolean save(Product p) throws SQLException {
        if (p.getId() > 0) {
            return update(p);
        } else {
            return create(p) > 0;
        }
    }

    public boolean updateStock(Connection conn, int productId, int newQuantity) throws SQLException {
        String sql = "UPDATE products SET stock_quantity = ? WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, newQuantity);
            pstmt.setInt(2, productId);
            return pstmt.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        AccessControl.requireWrite("PRODUCTS");
        if (DemoModeConfig.isEnabled()) return DemoDataStore.deleteProduct(id);
        String sql = "DELETE FROM products WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    private void ensureBranchInventoryForProduct(Connection conn, int businessId, int productId, int stockQuantity, int minStock) throws SQLException {
        String findBranchesSql = "SELECT id FROM branches WHERE business_id = ?";
        List<Integer> branchIds = new ArrayList<>();
        try (PreparedStatement pstmt = conn.prepareStatement(findBranchesSql)) {
            pstmt.setInt(1, businessId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    branchIds.add(rs.getInt("id"));
                }
            }
        }
        if (branchIds.isEmpty()) {
            String createBranchSql = "INSERT INTO branches (business_id, name, code, is_main, active) VALUES (?, 'Main Branch', 'BR-MAIN', 1, 1)";
            try (PreparedStatement pstmt = conn.prepareStatement(createBranchSql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setInt(1, businessId);
                pstmt.executeUpdate();
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) branchIds.add(rs.getInt(1));
                }
            }
        }

        String upsertInvSql = "INSERT INTO branch_inventory (branch_id, product_id, quantity, min_stock) VALUES (?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE quantity = VALUES(quantity), min_stock = VALUES(min_stock)";
        for (int branchId : branchIds) {
            try (PreparedStatement pstmt = conn.prepareStatement(upsertInvSql)) {
                pstmt.setInt(1, branchId);
                pstmt.setInt(2, productId);
                pstmt.setInt(3, stockQuantity);
                pstmt.setInt(4, minStock);
                pstmt.executeUpdate();
            }
        }
    }

    private void setNullableInt(PreparedStatement pstmt, int index, Integer value) throws SQLException {
        if (value != null && value > 0) {
            pstmt.setInt(index, value);
        } else {
            pstmt.setNull(index, Types.INTEGER);
        }
    }

    private Product mapResultSet(ResultSet rs) throws SQLException {
        Product p = new Product();
        p.setId(rs.getInt("id"));
        p.setBusinessId(rs.getInt("business_id"));
        int catId = rs.getInt("category_id");
        p.setCategoryId(rs.wasNull() ? null : catId);
        int supId = rs.getInt("supplier_id");
        p.setSupplierId(rs.wasNull() ? null : supId);
        p.setName(rs.getString("name"));
        p.setSku(rs.getString("sku"));
        p.setBarcode(rs.getString("barcode"));
        p.setDescription(rs.getString("description"));
        p.setPurchasePrice(rs.getBigDecimal("purchase_price"));
        p.setSellingPrice(rs.getBigDecimal("selling_price"));
        p.setStockQuantity(rs.getInt("stock_quantity"));
        p.setMinStock(rs.getInt("min_stock"));
        p.setUnit(rs.getString("unit"));
        p.setTaxRate(rs.getBigDecimal("tax_rate"));
        p.setActive(rs.getBoolean("active"));
        p.setCreatedAt(rs.getTimestamp("created_at"));
        p.setUpdatedAt(rs.getTimestamp("updated_at"));
        try {
            p.setCategoryName(rs.getString("category_name"));
            p.setSupplierName(rs.getString("supplier_name"));
        } catch (SQLException ignored) {
        }
        return p;
    }
}
