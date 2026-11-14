package com.businessmanager.dao;

import com.businessmanager.database.DBConnection;
import com.businessmanager.config.DemoModeConfig;
import com.businessmanager.demo.DemoDataStore;
import com.businessmanager.enums.PaymentMethod;
import com.businessmanager.enums.PaymentStatus;
import com.businessmanager.enums.TransactionType;
import com.businessmanager.model.Sale;
import com.businessmanager.model.SaleItem;
import com.businessmanager.security.AccessControl;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SaleDAO {

    public List<Sale> findByBusiness(int businessId) throws SQLException {
        if (DemoModeConfig.isEnabled()) return DemoDataStore.sales(businessId);
        List<Sale> list = new ArrayList<>();
        String sql = "SELECT s.*, c.name as customer_name, u.full_name as user_name, b.name as branch_name " +
                "FROM sales s " +
                "LEFT JOIN customers c ON s.customer_id = c.id " +
                "LEFT JOIN users u ON s.user_id = u.id " +
                "LEFT JOIN branches b ON s.branch_id = b.id " +
                "WHERE s.business_id = ? ORDER BY s.sale_date DESC";
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

    public List<Sale> findRecentSales(int businessId, int limit) throws SQLException {
        if (DemoModeConfig.isEnabled()) return DemoDataStore.sales(businessId).stream().limit(limit).toList();
        List<Sale> list = new ArrayList<>();
        String sql = "SELECT s.*, c.name as customer_name, u.full_name as user_name, b.name as branch_name " +
                "FROM sales s " +
                "LEFT JOIN customers c ON s.customer_id = c.id " +
                "LEFT JOIN users u ON s.user_id = u.id " +
                "LEFT JOIN branches b ON s.branch_id = b.id " +
                "WHERE s.business_id = ? ORDER BY s.sale_date DESC LIMIT ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, businessId);
            pstmt.setInt(2, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }
        }
        return list;
    }

    public BigDecimal getTotalSalesRevenue(int businessId) throws SQLException {
        if (DemoModeConfig.isEnabled()) return DemoDataStore.totalSales();
        String sql = "SELECT COALESCE(SUM(total), 0.00) FROM sales WHERE business_id = ?";
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

    public Sale findById(int saleId) throws SQLException {
        if (DemoModeConfig.isEnabled()) return DemoDataStore.sale(saleId);
        String sql = "SELECT s.*, c.name as customer_name, u.full_name as user_name, b.name as branch_name " +
                "FROM sales s " +
                "LEFT JOIN customers c ON s.customer_id = c.id " +
                "LEFT JOIN users u ON s.user_id = u.id " +
                "LEFT JOIN branches b ON s.branch_id = b.id " +
                "WHERE s.id = ?";
        Sale sale = null;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, saleId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    sale = mapResultSet(rs);
                }
            }
        }
        if (sale != null) {
            sale.setItems(findSaleItems(saleId));
        }
        return sale;
    }

    public List<SaleItem> findSaleItems(int saleId) throws SQLException {
        if (DemoModeConfig.isEnabled()) { Sale sale = DemoDataStore.sale(saleId); return sale == null ? new ArrayList<>() : new ArrayList<>(sale.getItems()); }
        List<SaleItem> items = new ArrayList<>();
        String sql = "SELECT si.*, p.name as product_name, p.sku as product_sku " +
                "FROM sale_items si " +
                "JOIN products p ON si.product_id = p.id " +
                "WHERE si.sale_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, saleId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    SaleItem item = new SaleItem();
                    item.setId(rs.getInt("id"));
                    item.setSaleId(rs.getInt("sale_id"));
                    item.setProductId(rs.getInt("product_id"));
                    item.setUnitPrice(rs.getBigDecimal("unit_price"));
                    item.setQuantity(rs.getInt("quantity"));
                    item.setDiscount(rs.getBigDecimal("discount"));
                    item.setTax(rs.getBigDecimal("tax"));
                    item.setLineTotal(rs.getBigDecimal("line_total"));
                    item.setProductName(rs.getString("product_name"));
                    item.setProductSku(rs.getString("product_sku"));
                    items.add(item);
                }
            }
        }
        return items;
    }

    /**
     * Completes a POS Sale atomically in a database transaction.
     * Decreases product inventory and logs inventory transactions.
     */
    public boolean createSale(Sale sale) throws SQLException {
        AccessControl.requireWrite("POS");
        if (DemoModeConfig.isEnabled()) {
            try { return DemoDataStore.createSale(sale); } catch (IllegalArgumentException ex) { throw new SQLException(ex.getMessage(), ex); }
        }
        validateAndNormaliseSale(sale);
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            // 1. Insert Sales Record
            String saleSql = "INSERT INTO sales (business_id, branch_id, user_id, customer_id, invoice_number, " +
                    "subtotal, discount, tax, total, paid_amount, change_amount, payment_method, payment_status, notes, sale_date) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())";

            int saleId = 0;
            try (PreparedStatement pstmt = conn.prepareStatement(saleSql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setInt(1, sale.getBusinessId());
                setNullableInt(pstmt, 2, sale.getBranchId());
                pstmt.setInt(3, sale.getUserId());
                setNullableInt(pstmt, 4, sale.getCustomerId());
                String invNum = sale.getInvoiceNumber();
                if (invNum == null || invNum.trim().isEmpty()) {
                    invNum = "INV-" + System.currentTimeMillis();
                    sale.setInvoiceNumber(invNum);
                }
                pstmt.setString(5, invNum);
                pstmt.setBigDecimal(6, sale.getSubtotal());
                pstmt.setBigDecimal(7, sale.getDiscount());
                pstmt.setBigDecimal(8, sale.getTax());
                pstmt.setBigDecimal(9, sale.getTotal());
                pstmt.setBigDecimal(10, sale.getPaidAmount());
                pstmt.setBigDecimal(11, sale.getChangeAmount());
                pstmt.setString(12, sale.getPaymentMethod().name());
                pstmt.setString(13, sale.getPaymentStatus().name());
                pstmt.setString(14, sale.getNotes());

                pstmt.executeUpdate();
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        saleId = rs.getInt(1);
                        sale.setId(saleId);
                    }
                }
            }

            if (saleId == 0) {
                conn.rollback();
                return false;
            }

            // 2. Insert Sale Items, update product stock, and log inventory transaction
            String itemSql = "INSERT INTO sale_items (sale_id, product_id, unit_price, quantity, discount, tax, line_total) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";
            String updateStockSql = "UPDATE products SET stock_quantity = stock_quantity - ? WHERE id = ? AND business_id = ? AND stock_quantity >= ?";
            String updateBranchStockSql = "UPDATE branch_inventory SET quantity = quantity - ? WHERE branch_id = ? AND product_id = ? AND quantity >= ?";
            String logInvSql = "INSERT INTO inventory_transactions (business_id, branch_id, product_id, user_id, transaction_type, quantity, reference_type, reference_id, notes) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

            for (SaleItem item : sale.getItems()) {
                item.setSaleId(saleId);
                try (PreparedStatement itemStmt = conn.prepareStatement(itemSql)) {
                    itemStmt.setInt(1, saleId);
                    itemStmt.setInt(2, item.getProductId());
                    itemStmt.setBigDecimal(3, item.getUnitPrice());
                    itemStmt.setInt(4, item.getQuantity());
                    itemStmt.setBigDecimal(5, item.getDiscount());
                    itemStmt.setBigDecimal(6, item.getTax());
                    itemStmt.setBigDecimal(7, item.getLineTotal());
                    itemStmt.executeUpdate();
                }

                if (sale.getBranchId() != null && sale.getBranchId() > 0) {
                    try (PreparedStatement branchStock = conn.prepareStatement(updateBranchStockSql)) {
                        branchStock.setInt(1, item.getQuantity());
                        branchStock.setInt(2, sale.getBranchId());
                        branchStock.setInt(3, item.getProductId());
                        branchStock.setInt(4, item.getQuantity());
                        if (branchStock.executeUpdate() == 0) {
                            conn.rollback();
                            String pName = getProductName(conn, item);
                            int avail = getAvailableBranchStock(conn, item.getProductId(), sale.getBranchId());
                            throw new SQLException("Insufficient stock for " + pName + ". Available: " + avail);
                        }
                    }
                }

                // Maintain product stock as the business-wide aggregate.
                try (PreparedStatement stockStmt = conn.prepareStatement(updateStockSql)) {
                    stockStmt.setInt(1, item.getQuantity());
                    stockStmt.setInt(2, item.getProductId());
                    stockStmt.setInt(3, sale.getBusinessId());
                    stockStmt.setInt(4, item.getQuantity());
                    int updated = stockStmt.executeUpdate();
                    if (updated == 0) {
                        conn.rollback();
                        String pName = getProductName(conn, item);
                        int avail = getAvailableBranchStock(conn, item.getProductId(), sale.getBranchId());
                        throw new SQLException("Insufficient stock for " + pName + ". Available: " + avail);
                    }
                }

                // Log inventory transaction
                try (PreparedStatement invStmt = conn.prepareStatement(logInvSql)) {
                    invStmt.setInt(1, sale.getBusinessId());
                    setNullableInt(invStmt, 2, sale.getBranchId());
                    invStmt.setInt(3, item.getProductId());
                    invStmt.setInt(4, sale.getUserId());
                    invStmt.setString(5, TransactionType.SALE.name());
                    invStmt.setInt(6, item.getQuantity());
                    invStmt.setString(7, "SALE");
                    invStmt.setInt(8, saleId);
                    invStmt.setString(9, "POS Sale Invoice " + sale.getInvoiceNumber());
                    invStmt.executeUpdate();
                }
            }

            // A customer payment is part of the same sale transaction. Walk-in cash sales have
            // no customer ledger account, so they remain represented by the sale's paid amount.
            if (sale.getCustomerId() != null && sale.getCustomerId() > 0
                    && sale.getPaidAmount() != null && sale.getPaidAmount().signum() > 0) {
                insertPayment(conn, sale, saleId);
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

    /** Keeps sale totals, tender, and change internally consistent before the JDBC transaction begins. */
    private void validateAndNormaliseSale(Sale sale) throws SQLException {
        if (sale == null) throw new SQLException("Sale details are required.");
        if (sale.getBusinessId() <= 0 || sale.getUserId() <= 0) throw new SQLException("A valid business and cashier are required.");
        if (sale.getItems() == null || sale.getItems().isEmpty()) throw new SQLException("Add at least one item before completing the sale.");
        BigDecimal subtotal = BigDecimal.ZERO;
        for (SaleItem item : sale.getItems()) {
            if (item == null || item.getProductId() <= 0) throw new SQLException("Each sale item must reference a product.");
            if (item.getQuantity() <= 0) throw new SQLException("Sale quantity must be greater than zero.");
            if (item.getUnitPrice() == null || item.getUnitPrice().signum() < 0) throw new SQLException("Sale item price must be a valid non-negative amount.");
            if (item.getDiscount() == null) item.setDiscount(BigDecimal.ZERO);
            if (item.getTax() == null) item.setTax(BigDecimal.ZERO);
            BigDecimal lineTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())).subtract(item.getDiscount()).add(item.getTax());
            if (lineTotal.signum() < 0) throw new SQLException("Sale item total cannot be negative.");
            item.setLineTotal(lineTotal);
            subtotal = subtotal.add(lineTotal);
        }
        BigDecimal discount = sale.getDiscount() == null ? BigDecimal.ZERO : sale.getDiscount();
        BigDecimal tax = sale.getTax() == null ? BigDecimal.ZERO : sale.getTax();
        if (discount.signum() < 0 || tax.signum() < 0) throw new SQLException("Discount and tax must be non-negative.");
        BigDecimal total = subtotal.subtract(discount).add(tax);
        if (total.signum() < 0) throw new SQLException("Sale total cannot be negative.");
        BigDecimal tendered = sale.getPaidAmount() == null ? BigDecimal.ZERO : sale.getPaidAmount();
        if (tendered.signum() < 0) throw new SQLException("Payment amount cannot be negative.");
        BigDecimal applied = tendered.min(total);
        sale.setSubtotal(subtotal);
        sale.setDiscount(discount);
        sale.setTax(tax);
        sale.setTotal(total);
        sale.setPaidAmount(applied);
        sale.setChangeAmount(tendered.subtract(total).max(BigDecimal.ZERO));
        sale.setPaymentStatus(applied.compareTo(total) == 0 ? PaymentStatus.PAID : applied.signum() == 0 ? PaymentStatus.UNPAID : PaymentStatus.PARTIAL);
        if (sale.getPaymentMethod() == null) sale.setPaymentMethod(PaymentMethod.CASH);
    }

    private void insertPayment(Connection conn, Sale sale, int saleId) throws SQLException {
        String sql = "INSERT INTO payments (business_id, party_type, party_id, reference_type, reference_id, amount, payment_method, notes) "
                + "VALUES (?, 'CUSTOMER', ?, 'SALE', ?, ?, ?, ?)";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, sale.getBusinessId());
            statement.setInt(2, sale.getCustomerId());
            statement.setInt(3, saleId);
            statement.setBigDecimal(4, sale.getPaidAmount());
            statement.setString(5, sale.getPaymentMethod().name());
            statement.setString(6, "Payment received for invoice " + sale.getInvoiceNumber());
            statement.executeUpdate();
        }
    }

    private Sale mapResultSet(ResultSet rs) throws SQLException {
        Sale s = new Sale();
        s.setId(rs.getInt("id"));
        s.setBusinessId(rs.getInt("business_id"));
        int branchId = rs.getInt("branch_id");
        s.setBranchId(rs.wasNull() ? null : branchId);
        s.setUserId(rs.getInt("user_id"));
        int custId = rs.getInt("customer_id");
        s.setCustomerId(rs.wasNull() ? null : custId);
        s.setInvoiceNumber(rs.getString("invoice_number"));
        s.setSubtotal(rs.getBigDecimal("subtotal"));
        s.setDiscount(rs.getBigDecimal("discount"));
        s.setTax(rs.getBigDecimal("tax"));
        s.setTotal(rs.getBigDecimal("total"));
        s.setPaidAmount(rs.getBigDecimal("paid_amount"));
        s.setChangeAmount(rs.getBigDecimal("change_amount"));
        s.setPaymentMethod(PaymentMethod.valueOf(rs.getString("payment_method")));
        s.setPaymentStatus(PaymentStatus.valueOf(rs.getString("payment_status")));
        s.setNotes(rs.getString("notes"));
        s.setSaleDate(rs.getTimestamp("sale_date"));
        s.setCreatedAt(rs.getTimestamp("created_at"));
        try {
            s.setCustomerName(rs.getString("customer_name"));
            s.setUserName(rs.getString("user_name"));
            s.setBranchName(rs.getString("branch_name"));
        } catch (SQLException ignored) {
        }
        return s;
    }

    private String getProductName(Connection conn, SaleItem item) {
        String pName = item.getProductName();
        if (pName != null && !pName.trim().isEmpty()) return pName;
        String sql = "SELECT name FROM products WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, item.getProductId());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getString("name");
            }
        } catch (Exception ignored) {}
        return "Product #" + item.getProductId();
    }

    private int getAvailableBranchStock(Connection conn, int productId, Integer branchId) {
        if (branchId == null || branchId <= 0) return 0;
        String sql = "SELECT COALESCE(quantity, 0) FROM branch_inventory WHERE product_id = ? AND branch_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, productId);
            pstmt.setInt(2, branchId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (Exception ignored) {}
        return 0;
    }
}
