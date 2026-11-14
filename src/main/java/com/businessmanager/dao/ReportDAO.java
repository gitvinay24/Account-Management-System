package com.businessmanager.dao;

import com.businessmanager.database.DBConnection;
import com.businessmanager.config.DemoModeConfig;
import com.businessmanager.demo.DemoDataStore;
import com.businessmanager.model.ReportFilterDTO;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ReportDAO {

    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

    public List<Object[]> generateReport(int businessId, ReportFilterDTO filter) throws SQLException {
        if (DemoModeConfig.isEnabled()) return DemoDataStore.reportRows(filter);
        List<Object[]> rows = new ArrayList<>();
        String type = filter.getReportType() != null ? filter.getReportType() : "Sales Report";

        try (Connection conn = DBConnection.getConnection()) {
            switch (type) {
                case "Sales Report" -> generateSalesReport(conn, businessId, filter, rows);
                case "Purchase Report" -> generatePurchaseReport(conn, businessId, filter, rows);
                case "Inventory Valuation Report" -> generateInventoryReport(conn, businessId, filter, rows);
                case "Expense Breakdown Report" -> generateExpenseReport(conn, businessId, filter, rows);
                case "Profit / Loss Report" -> generateProfitLossReport(conn, businessId, filter, rows);
                case "Executive Summary" -> generateExecutiveSummaryReport(conn, businessId, filter, rows);
                default -> generateSalesReport(conn, businessId, filter, rows);
            }
        }
        return rows;
    }

    private void generateSalesReport(Connection conn, int businessId, ReportFilterDTO filter, List<Object[]> rows) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT s.invoice_number, COALESCE(c.name, 'Walk-in') AS customer_name, COALESCE(b.name, 'Main') AS branch_name, " +
                "s.payment_method, s.payment_status, s.total, s.sale_date " +
                "FROM sales s " +
                "LEFT JOIN customers c ON s.customer_id = c.id " +
                "LEFT JOIN branches b ON s.branch_id = b.id " +
                "WHERE s.business_id = ? "
        );

        List<Object> params = new ArrayList<>();
        params.add(businessId);

        if (filter.getStartDate() != null) {
            sql.append("AND s.sale_date >= ? ");
            params.add(filter.getStartDate());
        }
        if (filter.getEndDate() != null) {
            sql.append("AND s.sale_date <= ? ");
            params.add(filter.getEndDate());
        }
        if (filter.getCustomerId() != null && filter.getCustomerId() > 0) {
            sql.append("AND s.customer_id = ? ");
            params.add(filter.getCustomerId());
        }
        if (filter.getBranchId() != null && filter.getBranchId() > 0) {
            sql.append("AND s.branch_id = ? ");
            params.add(filter.getBranchId());
        }
        if (filter.getPaymentStatus() != null) {
            sql.append("AND s.payment_status = ? ");
            params.add(filter.getPaymentStatus().name());
        }
        if (filter.getCategoryId() != null && filter.getCategoryId() > 0) {
            sql.append("AND s.id IN (SELECT si.sale_id FROM sale_items si JOIN products p ON si.product_id = p.id WHERE p.category_id = ?) ");
            params.add(filter.getCategoryId());
        }

        sql.append("ORDER BY s.sale_date DESC");

        try (PreparedStatement statement = conn.prepareStatement(sql.toString())) {
            setParams(statement, params);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    rows.add(new Object[]{
                            rs.getString("invoice_number"),
                            rs.getString("customer_name"),
                            rs.getString("branch_name"),
                            rs.getString("payment_method"),
                            rs.getString("payment_status"),
                            CURRENCY_FORMAT.format(rs.getBigDecimal("total")),
                            rs.getTimestamp("sale_date").toString().substring(0, 16)
                    });
                }
            }
        }
    }

    private void generatePurchaseReport(Connection conn, int businessId, ReportFilterDTO filter, List<Object[]> rows) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT p.purchase_number, COALESCE(sup.name, 'N/A') AS supplier_name, COALESCE(b.name, 'Main') AS branch_name, " +
                "p.payment_method, p.payment_status, p.total, p.purchase_date " +
                "FROM purchases p " +
                "LEFT JOIN suppliers sup ON p.supplier_id = sup.id " +
                "LEFT JOIN branches b ON p.branch_id = b.id " +
                "WHERE p.business_id = ? "
        );

        List<Object> params = new ArrayList<>();
        params.add(businessId);

        if (filter.getStartDate() != null) {
            sql.append("AND p.purchase_date >= ? ");
            params.add(filter.getStartDate());
        }
        if (filter.getEndDate() != null) {
            sql.append("AND p.purchase_date <= ? ");
            params.add(filter.getEndDate());
        }
        if (filter.getSupplierId() != null && filter.getSupplierId() > 0) {
            sql.append("AND p.supplier_id = ? ");
            params.add(filter.getSupplierId());
        }
        if (filter.getBranchId() != null && filter.getBranchId() > 0) {
            sql.append("AND p.branch_id = ? ");
            params.add(filter.getBranchId());
        }
        if (filter.getPaymentStatus() != null) {
            sql.append("AND p.payment_status = ? ");
            params.add(filter.getPaymentStatus().name());
        }

        sql.append("ORDER BY p.purchase_date DESC");

        try (PreparedStatement statement = conn.prepareStatement(sql.toString())) {
            setParams(statement, params);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    rows.add(new Object[]{
                            rs.getString("purchase_number"),
                            rs.getString("supplier_name"),
                            rs.getString("branch_name"),
                            rs.getString("payment_method"),
                            rs.getString("payment_status"),
                            CURRENCY_FORMAT.format(rs.getBigDecimal("total")),
                            rs.getTimestamp("purchase_date").toString().substring(0, 16)
                    });
                }
            }
        }
    }

    private void generateInventoryReport(Connection conn, int businessId, ReportFilterDTO filter, List<Object[]> rows) throws SQLException {
        boolean branchFilter = filter.getBranchId() != null && filter.getBranchId() > 0;
        StringBuilder sql = new StringBuilder();

        if (branchFilter) {
            sql.append(
                    "SELECT p.sku, p.name, COALESCE(c.name, 'Uncategorized') AS category_name, COALESCE(b.name, 'Main') AS branch_name, " +
                    "COALESCE(bi.quantity, 0) AS qty, p.purchase_price, (COALESCE(bi.quantity, 0) * p.purchase_price) AS total_val " +
                    "FROM products p " +
                    "LEFT JOIN categories c ON p.category_id = c.id " +
                    "LEFT JOIN branch_inventory bi ON bi.product_id = p.id AND bi.branch_id = ? " +
                    "LEFT JOIN branches b ON b.id = ? " +
                    "WHERE p.business_id = ? "
            );
        } else {
            sql.append(
                    "SELECT p.sku, p.name, COALESCE(c.name, 'Uncategorized') AS category_name, 'All Branches' AS branch_name, " +
                    "p.stock_quantity AS qty, p.purchase_price, (p.stock_quantity * p.purchase_price) AS total_val " +
                    "FROM products p " +
                    "LEFT JOIN categories c ON p.category_id = c.id " +
                    "WHERE p.business_id = ? "
            );
        }

        List<Object> params = new ArrayList<>();
        if (branchFilter) {
            params.add(filter.getBranchId());
            params.add(filter.getBranchId());
        }
        params.add(businessId);

        if (filter.getCategoryId() != null && filter.getCategoryId() > 0) {
            sql.append("AND p.category_id = ? ");
            params.add(filter.getCategoryId());
        }
        if (filter.getSupplierId() != null && filter.getSupplierId() > 0) {
            sql.append("AND p.supplier_id = ? ");
            params.add(filter.getSupplierId());
        }

        sql.append("ORDER BY p.name ASC");

        try (PreparedStatement statement = conn.prepareStatement(sql.toString())) {
            setParams(statement, params);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    rows.add(new Object[]{
                            rs.getString("sku"),
                            rs.getString("name"),
                            rs.getString("category_name"),
                            rs.getString("branch_name"),
                            rs.getInt("qty"),
                            CURRENCY_FORMAT.format(rs.getBigDecimal("purchase_price")),
                            CURRENCY_FORMAT.format(rs.getBigDecimal("total_val"))
                    });
                }
            }
        }
    }

    private void generateExpenseReport(Connection conn, int businessId, ReportFilterDTO filter, List<Object[]> rows) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT e.id, e.title, e.category, COALESCE(b.name, 'Main') AS branch_name, e.amount, e.expense_date " +
                "FROM expenses e " +
                "LEFT JOIN branches b ON e.branch_id = b.id " +
                "WHERE e.business_id = ? "
        );

        List<Object> params = new ArrayList<>();
        params.add(businessId);

        if (filter.getStartDate() != null) {
            sql.append("AND e.expense_date >= ? ");
            params.add(filter.getStartDate());
        }
        if (filter.getEndDate() != null) {
            sql.append("AND e.expense_date <= ? ");
            params.add(filter.getEndDate());
        }
        if (filter.getBranchId() != null && filter.getBranchId() > 0) {
            sql.append("AND e.branch_id = ? ");
            params.add(filter.getBranchId());
        }

        sql.append("ORDER BY e.expense_date DESC");

        try (PreparedStatement statement = conn.prepareStatement(sql.toString())) {
            setParams(statement, params);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    rows.add(new Object[]{
                            "EXP-" + rs.getInt("id"),
                            rs.getString("title"),
                            rs.getString("category"),
                            rs.getString("branch_name"),
                            CURRENCY_FORMAT.format(rs.getBigDecimal("amount")),
                            rs.getDate("expense_date").toString()
                    });
                }
            }
        }
    }

    private void generateProfitLossReport(Connection conn, int businessId, ReportFilterDTO filter, List<Object[]> rows) throws SQLException {
        BigDecimal salesRevenue = getSum(conn, "SELECT COALESCE(SUM(total), 0) FROM sales WHERE business_id = ?", businessId, filter, "sale_date");
        BigDecimal purchaseCost = getSum(conn, "SELECT COALESCE(SUM(pi.unit_price * pi.quantity), 0) FROM purchase_items pi JOIN purchases p ON p.id = pi.purchase_id WHERE p.business_id = ?", businessId, filter, "purchase_date");
        BigDecimal operatingExpenses = getSum(conn, "SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE business_id = ?", businessId, filter, "expense_date");

        BigDecimal grossProfit = salesRevenue.subtract(purchaseCost);
        BigDecimal netProfit = grossProfit.subtract(operatingExpenses);

        rows.add(new Object[]{"Sales Revenue", CURRENCY_FORMAT.format(salesRevenue), "Completed sales revenue"});
        rows.add(new Object[]{"Wholesale Purchase Cost", CURRENCY_FORMAT.format(purchaseCost), "Cost of purchased goods"});
        rows.add(new Object[]{"Gross Profit", CURRENCY_FORMAT.format(grossProfit), "Sales Revenue − Purchase Cost"});
        rows.add(new Object[]{"Operating Expenses", CURRENCY_FORMAT.format(operatingExpenses), "Total operating expenses"});
        rows.add(new Object[]{"Net Profit / (Loss)", CURRENCY_FORMAT.format(netProfit), "Gross Profit − Operating Expenses"});
    }

    private void generateExecutiveSummaryReport(Connection conn, int businessId, ReportFilterDTO filter, List<Object[]> rows) throws SQLException {
        BigDecimal salesRevenue = getSum(conn, "SELECT COALESCE(SUM(total), 0) FROM sales WHERE business_id = ?", businessId, filter, "sale_date");
        BigDecimal operatingExpenses = getSum(conn, "SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE business_id = ?", businessId, filter, "expense_date");
        BigDecimal inventoryValue = getSum(conn, "SELECT COALESCE(SUM(stock_quantity * purchase_price), 0) FROM products WHERE business_id = ?", businessId, null, null);
        BigDecimal customerReceivables = getSum(conn, "SELECT COALESCE(SUM(total - paid_amount), 0) FROM sales WHERE business_id = ? AND payment_status != 'PAID'", businessId, filter, "sale_date");
        BigDecimal supplierPayables = getSum(conn, "SELECT COALESCE(SUM(total - paid_amount), 0) FROM purchases WHERE business_id = ? AND payment_status != 'PAID'", businessId, filter, "purchase_date");

        rows.add(new Object[]{"Sales Revenue", CURRENCY_FORMAT.format(salesRevenue)});
        rows.add(new Object[]{"Operating Expenses", CURRENCY_FORMAT.format(operatingExpenses)});
        rows.add(new Object[]{"Net Operating Income", CURRENCY_FORMAT.format(salesRevenue.subtract(operatingExpenses))});
        rows.add(new Object[]{"Total Inventory Cost Value", CURRENCY_FORMAT.format(inventoryValue)});
        rows.add(new Object[]{"Outstanding Customer Receivables", CURRENCY_FORMAT.format(customerReceivables)});
        rows.add(new Object[]{"Outstanding Supplier Payables", CURRENCY_FORMAT.format(supplierPayables)});
    }

    private BigDecimal getSum(Connection conn, String baseSql, int businessId, ReportFilterDTO filter, String dateCol) throws SQLException {
        StringBuilder sql = new StringBuilder(baseSql);
        List<Object> params = new ArrayList<>();
        params.add(businessId);

        if (filter != null) {
            if (dateCol != null && filter.getStartDate() != null) {
                sql.append(" AND ").append(dateCol).append(" >= ?");
                params.add(filter.getStartDate());
            }
            if (dateCol != null && filter.getEndDate() != null) {
                sql.append(" AND ").append(dateCol).append(" <= ?");
                params.add(filter.getEndDate());
            }
            if (filter.getBranchId() != null && filter.getBranchId() > 0) {
                sql.append(" AND branch_id = ?");
                params.add(filter.getBranchId());
            }
        }

        try (PreparedStatement statement = conn.prepareStatement(sql.toString())) {
            setParams(statement, params);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
            }
        }
    }

    private void setParams(PreparedStatement statement, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            Object param = params.get(i);
            if (param instanceof Integer val) {
                statement.setInt(i + 1, val);
            } else if (param instanceof java.sql.Date val) {
                statement.setDate(i + 1, val);
            } else {
                statement.setString(i + 1, param.toString());
            }
        }
    }
}
