package com.businessmanager.dao;

import com.businessmanager.database.DBConnection;
import com.businessmanager.config.DemoModeConfig;
import com.businessmanager.demo.DemoDataStore;
import com.businessmanager.enums.PaymentMethod;
import com.businessmanager.enums.PaymentStatus;
import com.businessmanager.enums.TransactionType;
import com.businessmanager.model.Purchase;
import com.businessmanager.model.PurchaseItem;
import com.businessmanager.security.AccessControl;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PurchaseDAO {

    public List<Purchase> findByBusiness(int businessId) throws SQLException {
        if (DemoModeConfig.isEnabled()) return DemoDataStore.purchases(businessId);
        List<Purchase> list = new ArrayList<>();
        String sql = "SELECT p.*, sup.name as supplier_name, u.full_name as user_name, b.name as branch_name " +
                "FROM purchases p " +
                "LEFT JOIN suppliers sup ON p.supplier_id = sup.id " +
                "LEFT JOIN users u ON p.user_id = u.id " +
                "LEFT JOIN branches b ON p.branch_id = b.id " +
                "WHERE p.business_id = ? ORDER BY p.purchase_date DESC";
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

    /**
     * Creates a purchase order, increases product inventory stock, and logs inventory transactions atomically.
     */
    public boolean createPurchase(Purchase p) throws SQLException {
        AccessControl.requireWrite("PURCHASES");
        if (DemoModeConfig.isEnabled()) {
            try { return DemoDataStore.createPurchase(p); } catch (IllegalArgumentException ex) { throw new SQLException(ex.getMessage(), ex); }
        }
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            // 1. Insert Purchase
            String pSql = "INSERT INTO purchases (business_id, branch_id, supplier_id, user_id, purchase_number, " +
                    "subtotal, discount, tax, total, paid_amount, payment_method, payment_status, notes, purchase_date) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())";

            int purchaseId = 0;
            try (PreparedStatement pstmt = conn.prepareStatement(pSql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setInt(1, p.getBusinessId());
                setNullableInt(pstmt, 2, p.getBranchId());
                pstmt.setInt(3, p.getSupplierId());
                pstmt.setInt(4, p.getUserId());
                pstmt.setString(5, p.getPurchaseNumber());
                pstmt.setBigDecimal(6, p.getSubtotal());
                pstmt.setBigDecimal(7, p.getDiscount());
                pstmt.setBigDecimal(8, p.getTax());
                pstmt.setBigDecimal(9, p.getTotal());
                pstmt.setBigDecimal(10, p.getPaidAmount());
                pstmt.setString(11, p.getPaymentMethod().name());
                pstmt.setString(12, p.getPaymentStatus().name());
                pstmt.setString(13, p.getNotes());

                pstmt.executeUpdate();
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        purchaseId = rs.getInt(1);
                        p.setId(purchaseId);
                    }
                }
            }

            if (purchaseId == 0) {
                conn.rollback();
                return false;
            }

            // 2. Insert Purchase Items, INCREASE product stock, log inventory transaction
            String itemSql = "INSERT INTO purchase_items (purchase_id, product_id, unit_price, quantity, line_total) VALUES (?, ?, ?, ?, ?)";
            String updateStockSql = "UPDATE products SET stock_quantity = stock_quantity + ? WHERE id = ? AND business_id = ?";
            String updateBranchStockSql = "INSERT INTO branch_inventory (branch_id, product_id, quantity, min_stock) VALUES (?, ?, ?, 5) "
                    + "ON DUPLICATE KEY UPDATE quantity = quantity + VALUES(quantity)";
            String logInvSql = "INSERT INTO inventory_transactions (business_id, branch_id, product_id, user_id, transaction_type, quantity, reference_type, reference_id, notes) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

            for (PurchaseItem item : p.getItems()) {
                item.setPurchaseId(purchaseId);
                try (PreparedStatement itemStmt = conn.prepareStatement(itemSql)) {
                    itemStmt.setInt(1, purchaseId);
                    itemStmt.setInt(2, item.getProductId());
                    itemStmt.setBigDecimal(3, item.getUnitPrice());
                    itemStmt.setInt(4, item.getQuantity());
                    itemStmt.setBigDecimal(5, item.getLineTotal());
                    itemStmt.executeUpdate();
                }

                if (p.getBranchId() != null && p.getBranchId() > 0) {
                    try (PreparedStatement branchStock = conn.prepareStatement(updateBranchStockSql)) {
                        branchStock.setInt(1, p.getBranchId());
                        branchStock.setInt(2, item.getProductId());
                        branchStock.setInt(3, item.getQuantity());
                        branchStock.executeUpdate();
                    }
                }

                // Maintain product stock as the business-wide aggregate.
                try (PreparedStatement stockStmt = conn.prepareStatement(updateStockSql)) {
                    stockStmt.setInt(1, item.getQuantity());
                    stockStmt.setInt(2, item.getProductId());
                    stockStmt.setInt(3, p.getBusinessId());
                    if (stockStmt.executeUpdate() == 0) throw new SQLException("Product does not belong to this business: " + item.getProductId());
                }

                // Log transaction
                try (PreparedStatement invStmt = conn.prepareStatement(logInvSql)) {
                    invStmt.setInt(1, p.getBusinessId());
                    setNullableInt(invStmt, 2, p.getBranchId());
                    invStmt.setInt(3, item.getProductId());
                    invStmt.setInt(4, p.getUserId());
                    invStmt.setString(5, TransactionType.PURCHASE.name());
                    invStmt.setInt(6, item.getQuantity());
                    invStmt.setString(7, "PURCHASE");
                    invStmt.setInt(8, purchaseId);
                    invStmt.setString(9, "Supplier Purchase Order " + p.getPurchaseNumber());
                    invStmt.executeUpdate();
                }
            }

            if (p.getPaidAmount() != null && p.getPaidAmount().signum() > 0) {
                insertPayment(conn, p, purchaseId);
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            throw e;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
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

    private void insertPayment(Connection conn, Purchase purchase, int purchaseId) throws SQLException {
        String sql = "INSERT INTO payments (business_id, party_type, party_id, reference_type, reference_id, amount, payment_method, notes) "
                + "VALUES (?, 'SUPPLIER', ?, 'PURCHASE', ?, ?, ?, ?)";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, purchase.getBusinessId());
            statement.setInt(2, purchase.getSupplierId());
            statement.setInt(3, purchaseId);
            statement.setBigDecimal(4, purchase.getPaidAmount());
            statement.setString(5, purchase.getPaymentMethod().name());
            statement.setString(6, "Payment made for purchase " + purchase.getPurchaseNumber());
            statement.executeUpdate();
        }
    }

    private Purchase mapResultSet(ResultSet rs) throws SQLException {
        Purchase p = new Purchase();
        p.setId(rs.getInt("id"));
        p.setBusinessId(rs.getInt("business_id"));
        int branchId = rs.getInt("branch_id");
        p.setBranchId(rs.wasNull() ? null : branchId);
        p.setSupplierId(rs.getInt("supplier_id"));
        p.setUserId(rs.getInt("user_id"));
        p.setPurchaseNumber(rs.getString("purchase_number"));
        p.setSubtotal(rs.getBigDecimal("subtotal"));
        p.setDiscount(rs.getBigDecimal("discount"));
        p.setTax(rs.getBigDecimal("tax"));
        p.setTotal(rs.getBigDecimal("total"));
        p.setPaidAmount(rs.getBigDecimal("paid_amount"));
        p.setPaymentMethod(PaymentMethod.valueOf(rs.getString("payment_method")));
        p.setPaymentStatus(PaymentStatus.valueOf(rs.getString("payment_status")));
        p.setNotes(rs.getString("notes"));
        p.setPurchaseDate(rs.getTimestamp("purchase_date"));
        p.setCreatedAt(rs.getTimestamp("created_at"));
        try {
            p.setSupplierName(rs.getString("supplier_name"));
            p.setUserName(rs.getString("user_name"));
            p.setBranchName(rs.getString("branch_name"));
        } catch (SQLException ignored) {
        }
        return p;
    }
}
