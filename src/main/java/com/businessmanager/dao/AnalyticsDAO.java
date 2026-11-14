package com.businessmanager.dao;

import com.businessmanager.database.DBConnection;
import com.businessmanager.config.DemoModeConfig;
import com.businessmanager.demo.DemoDataStore;
import com.businessmanager.model.*;
import org.jfree.data.category.DefaultCategoryDataset;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.util.*;

public class AnalyticsDAO {

    private void add(DefaultCategoryDataset dataset, BigDecimal amount, String series, String key) {
        if (series == null) series = "General";
        if (key == null || key.trim().isEmpty()) key = "Unspecified";
        if (amount == null) amount = BigDecimal.ZERO;
        Number current = dataset.getRowKeys().contains(series) && dataset.getColumnKeys().contains(key) ? dataset.getValue(series, key) : null;
        dataset.addValue((current == null ? BigDecimal.ZERO : new BigDecimal(current.toString())).add(amount), series, key);
    }

    public DefaultCategoryDataset getTopProductsDataset(int businessId, Integer branchId) throws SQLException {
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        if (DemoModeConfig.isEnabled()) {
            for (Sale sale : DemoDataStore.sales(businessId)) {
                for (SaleItem item : sale.getItems()) {
                    String pName = item.getProductName();
                    if (pName == null || pName.trim().isEmpty()) {
                        Product p = DemoDataStore.product(item.getProductId());
                        pName = p != null ? p.getName() : "Product #" + item.getProductId();
                    }
                    add(ds, item.getLineTotal(), "Sales", pName);
                }
            }
            return ds;
        }
        StringBuilder sql = new StringBuilder(
                "SELECT COALESCE(p.name, 'Unknown Product') AS product_name, SUM(si.line_total) AS total_sales " +
                        "FROM sale_items si " +
                        "JOIN sales s ON si.sale_id = s.id " +
                        "LEFT JOIN products p ON si.product_id = p.id " +
                        "WHERE s.business_id = ? "
        );
        if (branchId != null && branchId > 0) {
            sql.append("AND s.branch_id = ? ");
        }
        sql.append("GROUP BY p.id, p.name ORDER BY total_sales DESC LIMIT 5");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            pstmt.setInt(1, businessId);
            if (branchId != null && branchId > 0) pstmt.setInt(2, branchId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String pName = rs.getString("product_name");
                    BigDecimal totalSales = rs.getBigDecimal("total_sales");
                    if (pName != null && totalSales != null) {
                        ds.addValue(totalSales, "Sales", pName);
                    }
                }
            }
        }
        return ds;
    }

    public DefaultCategoryDataset getCategoryRevenueDataset(int businessId, Integer branchId) throws SQLException {
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        if (DemoModeConfig.isEnabled()) {
            for (Sale sale : DemoDataStore.sales(businessId)) {
                for (SaleItem item : sale.getItems()) {
                    Product p = DemoDataStore.product(item.getProductId());
                    add(ds, item.getLineTotal(), "Category", p == null || p.getCategoryName() == null ? "Uncategorized" : p.getCategoryName());
                }
            }
            return ds;
        }
        StringBuilder sql = new StringBuilder(
                "SELECT COALESCE(c.name, 'Uncategorized') AS category_name, SUM(si.line_total) AS cat_sales " +
                        "FROM sale_items si " +
                        "JOIN sales s ON si.sale_id = s.id " +
                        "JOIN products p ON si.product_id = p.id " +
                        "LEFT JOIN categories c ON p.category_id = c.id " +
                        "WHERE s.business_id = ? "
        );
        if (branchId != null && branchId > 0) {
            sql.append("AND s.branch_id = ? ");
        }
        sql.append("GROUP BY c.id, c.name ORDER BY cat_sales DESC LIMIT 6");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            pstmt.setInt(1, businessId);
            if (branchId != null && branchId > 0) pstmt.setInt(2, branchId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String catName = rs.getString("category_name");
                    BigDecimal catSales = rs.getBigDecimal("cat_sales");
                    if (catName != null && catSales != null) {
                        ds.addValue(catSales, "Category", catName);
                    }
                }
            }
        }
        return ds;
    }

    public DefaultCategoryDataset getBranchComparisonDataset(int businessId) throws SQLException {
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        if (DemoModeConfig.isEnabled()) {
            for (Sale sale : DemoDataStore.sales(businessId)) {
                String n = sale.getBranchName() != null ? sale.getBranchName() : "Main Branch";
                add(ds, sale.getTotal(), "Total Revenue", n);
                add(ds, sale.getPaidAmount(), "Collections", n);
            }
            return ds;
        }
        String sql = "SELECT COALESCE(b.name, 'Main Branch') AS branch_name, " +
                "SUM(s.total) AS total_revenue, " +
                "COALESCE(SUM(s.paid_amount), 0) AS total_collected " +
                "FROM sales s " +
                "LEFT JOIN branches b ON s.branch_id = b.id " +
                "WHERE s.business_id = ? " +
                "GROUP BY s.branch_id, b.name";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, businessId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String branch = rs.getString("branch_name");
                    BigDecimal totalRev = rs.getBigDecimal("total_revenue");
                    BigDecimal totalColl = rs.getBigDecimal("total_collected");
                    if (branch != null) {
                        ds.addValue(totalRev != null ? totalRev : BigDecimal.ZERO, "Total Revenue", branch);
                        ds.addValue(totalColl != null ? totalColl : BigDecimal.ZERO, "Collections", branch);
                    }
                }
            }
        }
        return ds;
    }

    public DefaultCategoryDataset getMonthlyRevenueTrendDataset(int businessId, Integer branchId) throws SQLException {
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        if (DemoModeConfig.isEnabled()) {
            for (Sale sale : DemoDataStore.sales(businessId)) {
                add(ds, sale.getTotal(), "Monthly Revenue", "Current month");
            }
            return ds;
        }
        StringBuilder sql = new StringBuilder(
                "SELECT DATE_FORMAT(sale_date, '%Y-%m') AS sale_month, SUM(total) AS monthly_total " +
                        "FROM sales WHERE business_id = ? "
        );
        if (branchId != null && branchId > 0) {
            sql.append("AND branch_id = ? ");
        }
        sql.append("GROUP BY sale_month ORDER BY sale_month ASC LIMIT 12");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            pstmt.setInt(1, businessId);
            if (branchId != null && branchId > 0) pstmt.setInt(2, branchId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String month = rs.getString("sale_month");
                    BigDecimal total = rs.getBigDecimal("monthly_total");
                    if (month != null && total != null) {
                        ds.addValue(total, "Monthly Revenue", month);
                    }
                }
            }
        }
        return ds;
    }

    public DefaultCategoryDataset getExpenseCategoryBreakdownDataset(int businessId, Integer branchId) throws SQLException {
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        if (DemoModeConfig.isEnabled()) {
            for (Expense expense : DemoDataStore.expenses(businessId)) {
                String catName = expense.getCategory() != null ? expense.getCategory().getDisplayName() : "General";
                add(ds, expense.getAmount(), "Expense", catName);
            }
            return ds;
        }
        StringBuilder sql = new StringBuilder(
                "SELECT category, SUM(amount) AS cat_expense FROM expenses WHERE business_id = ? "
        );
        if (branchId != null && branchId > 0) {
            sql.append("AND branch_id = ? ");
        }
        sql.append("GROUP BY category ORDER BY cat_expense DESC");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            pstmt.setInt(1, businessId);
            if (branchId != null && branchId > 0) pstmt.setInt(2, branchId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String cat = rs.getString("category");
                    BigDecimal catExpense = rs.getBigDecimal("cat_expense");
                    if (cat != null && catExpense != null) {
                        ds.addValue(catExpense, "Expense", cat);
                    }
                }
            }
        }
        return ds;
    }

    public DefaultCategoryDataset getTopCustomersDataset(int businessId, Integer branchId) throws SQLException {
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        if (DemoModeConfig.isEnabled()) {
            for (Sale sale : DemoDataStore.sales(businessId)) {
                String cName = sale.getCustomerName() != null ? sale.getCustomerName() : "Walk-in";
                add(ds, sale.getTotal(), "Spend (₹)", cName);
            }
            return ds;
        }
        StringBuilder sql = new StringBuilder(
                "SELECT COALESCE(c.name, 'Walk-in') AS customer_name, SUM(s.total) AS total_spend " +
                        "FROM sales s " +
                        "LEFT JOIN customers c ON s.customer_id = c.id " +
                        "WHERE s.business_id = ? "
        );
        if (branchId != null && branchId > 0) {
            sql.append("AND s.branch_id = ? ");
        }
        sql.append("GROUP BY s.customer_id, c.name ORDER BY total_spend DESC LIMIT 5");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            pstmt.setInt(1, businessId);
            if (branchId != null && branchId > 0) pstmt.setInt(2, branchId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String cName = rs.getString("customer_name");
                    BigDecimal totalSpend = rs.getBigDecimal("total_spend");
                    if (cName != null && totalSpend != null) {
                        ds.addValue(totalSpend, "Spend (₹)", cName);
                    }
                }
            }
        }
        return ds;
    }

    public DashboardMetricsDTO getDashboardMetrics(int businessId, Integer branchId, String dateRangeFilter, Timestamp customStart, Timestamp customEnd) throws SQLException {
        DashboardMetricsDTO dto = new DashboardMetricsDTO();
        if (DemoModeConfig.isEnabled()) {
            dto.setTotalSales(new BigDecimal("1245000.00"));
            dto.setSalesChangePercent(12.5);
            dto.setGrossProfit(new BigDecimal("482500.00"));
            dto.setProfitChangePercent(8.2);
            dto.setTotalPurchases(new BigDecimal("762500.00"));
            dto.setPurchasesChangePercent(5.1);
            dto.setInventoryValue(new BigDecimal("3100000.00"));
            dto.setOutstandingPayments(new BigDecimal("84200.00"));
            dto.setTotalCustomers(148);
            return dto;
        }

        Timestamp[] bounds = getDateBounds(dateRangeFilter, customStart, customEnd);
        Timestamp start = bounds[0];
        Timestamp end = bounds[1];

        Timestamp[] prevBounds = getPreviousDateBounds(dateRangeFilter, start, end);
        Timestamp prevStart = prevBounds[0];
        Timestamp prevEnd = prevBounds[1];

        try (Connection conn = DBConnection.getConnection()) {
            // 1. Total Sales (Current & Prev)
            BigDecimal currentSales = querySum(conn,
                    "SELECT COALESCE(SUM(total), 0) FROM sales WHERE business_id = ? " +
                            (branchId != null && branchId > 0 ? "AND branch_id = ? " : "") +
                            "AND sale_date >= ? AND sale_date <= ?", businessId, branchId, start, end);
            BigDecimal prevSales = querySum(conn,
                    "SELECT COALESCE(SUM(total), 0) FROM sales WHERE business_id = ? " +
                            (branchId != null && branchId > 0 ? "AND branch_id = ? " : "") +
                            "AND sale_date >= ? AND sale_date <= ?", businessId, branchId, prevStart, prevEnd);

            dto.setTotalSales(currentSales);
            dto.setSalesChangePercent(calculatePercentChange(currentSales, prevSales));

            // 2. COGS & Gross Profit
            BigDecimal cogs = querySum(conn,
                    "SELECT COALESCE(SUM(si.quantity * COALESCE(p.purchase_price, 0)), 0) FROM sale_items si " +
                            "JOIN sales s ON si.sale_id = s.id LEFT JOIN products p ON si.product_id = p.id " +
                            "WHERE s.business_id = ? " +
                            (branchId != null && branchId > 0 ? "AND s.branch_id = ? " : "") +
                            "AND s.sale_date >= ? AND s.sale_date <= ?", businessId, branchId, start, end);
            BigDecimal prevCogs = querySum(conn,
                    "SELECT COALESCE(SUM(si.quantity * COALESCE(p.purchase_price, 0)), 0) FROM sale_items si " +
                            "JOIN sales s ON si.sale_id = s.id LEFT JOIN products p ON si.product_id = p.id " +
                            "WHERE s.business_id = ? " +
                            (branchId != null && branchId > 0 ? "AND s.branch_id = ? " : "") +
                            "AND s.sale_date >= ? AND s.sale_date <= ?", businessId, branchId, prevStart, prevEnd);

            BigDecimal grossProfit = currentSales.subtract(cogs);
            BigDecimal prevGrossProfit = prevSales.subtract(prevCogs);
            dto.setGrossProfit(grossProfit);
            dto.setProfitChangePercent(calculatePercentChange(grossProfit, prevGrossProfit));

            // 3. Total Purchases
            BigDecimal currentPurchases = querySum(conn,
                    "SELECT COALESCE(SUM(total), 0) FROM purchases WHERE business_id = ? " +
                            (branchId != null && branchId > 0 ? "AND branch_id = ? " : "") +
                            "AND purchase_date >= ? AND purchase_date <= ?", businessId, branchId, start, end);
            BigDecimal prevPurchases = querySum(conn,
                    "SELECT COALESCE(SUM(total), 0) FROM purchases WHERE business_id = ? " +
                            (branchId != null && branchId > 0 ? "AND branch_id = ? " : "") +
                            "AND purchase_date >= ? AND purchase_date <= ?", businessId, branchId, prevStart, prevEnd);

            dto.setTotalPurchases(currentPurchases);
            dto.setPurchasesChangePercent(calculatePercentChange(currentPurchases, prevPurchases));

            // 4. Inventory Value
            BigDecimal invValue;
            if (branchId != null && branchId > 0) {
                invValue = querySum(conn,
                        "SELECT COALESCE(SUM(bi.quantity * p.purchase_price), 0) FROM branch_inventory bi " +
                                "JOIN products p ON bi.product_id = p.id WHERE p.business_id = ? AND bi.branch_id = ? AND p.active = 1",
                        businessId, branchId, null, null);
            } else {
                invValue = querySum(conn,
                        "SELECT COALESCE(SUM(stock_quantity * purchase_price), 0) FROM products WHERE business_id = ? AND active = 1",
                        businessId, null, null, null);
            }
            dto.setInventoryValue(invValue);

            // 5. Outstanding Payments (Unpaid Sales + Unpaid Purchases)
            BigDecimal unpaidSales = querySum(conn,
                    "SELECT COALESCE(SUM(total - paid_amount), 0) FROM sales WHERE business_id = ? " +
                            (branchId != null && branchId > 0 ? "AND branch_id = ? " : "") +
                            "AND payment_status != 'PAID'", businessId, branchId, null, null);
            BigDecimal unpaidPurchases = querySum(conn,
                    "SELECT COALESCE(SUM(total - paid_amount), 0) FROM purchases WHERE business_id = ? " +
                            (branchId != null && branchId > 0 ? "AND branch_id = ? " : "") +
                            "AND payment_status != 'PAID'", businessId, branchId, null, null);
            dto.setOutstandingPayments(unpaidSales.add(unpaidPurchases));

            // 6. Total Customers
            String custSql = "SELECT COUNT(*) FROM customers WHERE business_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(custSql)) {
                pstmt.setInt(1, businessId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        dto.setTotalCustomers(rs.getInt(1));
                    }
                }
            }
        }

        return dto;
    }

    public DefaultCategoryDataset getSalesVsPurchasesTrendDataset(int businessId, Integer branchId, String dateRangeFilter, Timestamp customStart, Timestamp customEnd) throws SQLException {
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        if (DemoModeConfig.isEnabled()) {
            ds.addValue(12000, "Sales", "Mon");
            ds.addValue(15000, "Sales", "Tue");
            ds.addValue(18000, "Sales", "Wed");
            ds.addValue(14000, "Sales", "Thu");
            ds.addValue(22000, "Sales", "Fri");
            ds.addValue(28000, "Sales", "Sat");
            ds.addValue(25000, "Sales", "Sun");

            ds.addValue(8000, "Purchases", "Mon");
            ds.addValue(5000, "Purchases", "Tue");
            ds.addValue(12000, "Purchases", "Wed");
            ds.addValue(6000, "Purchases", "Thu");
            ds.addValue(15000, "Purchases", "Fri");
            ds.addValue(10000, "Purchases", "Sat");
            ds.addValue(4000, "Purchases", "Sun");
            return ds;
        }

        Timestamp[] bounds = getDateBounds(dateRangeFilter, customStart, customEnd);
        Timestamp start = bounds[0];
        Timestamp end = bounds[1];

        String dateFormat = "%Y-%m-%d";
        if ("Today".equalsIgnoreCase(dateRangeFilter)) {
            dateFormat = "%H:00";
        } else if ("Year".equalsIgnoreCase(dateRangeFilter)) {
            dateFormat = "%Y-%m";
        }

        Map<String, BigDecimal> salesMap = new LinkedHashMap<>();
        Map<String, BigDecimal> purchaseMap = new LinkedHashMap<>();

        // Fetch Sales trend
        StringBuilder salesSql = new StringBuilder(
                "SELECT DATE_FORMAT(sale_date, '" + dateFormat + "') AS time_label, SUM(total) AS total_val " +
                        "FROM sales WHERE business_id = ? "
        );
        if (branchId != null && branchId > 0) salesSql.append("AND branch_id = ? ");
        salesSql.append("AND sale_date >= ? AND sale_date <= ? GROUP BY time_label ORDER BY time_label ASC");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(salesSql.toString())) {
            int idx = 1;
            pstmt.setInt(idx++, businessId);
            if (branchId != null && branchId > 0) pstmt.setInt(idx++, branchId);
            pstmt.setTimestamp(idx++, start);
            pstmt.setTimestamp(idx++, end);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    salesMap.put(rs.getString("time_label"), rs.getBigDecimal("total_val"));
                }
            }
        }

        // Fetch Purchase trend
        StringBuilder purSql = new StringBuilder(
                "SELECT DATE_FORMAT(purchase_date, '" + dateFormat + "') AS time_label, SUM(total) AS total_val " +
                        "FROM purchases WHERE business_id = ? "
        );
        if (branchId != null && branchId > 0) purSql.append("AND branch_id = ? ");
        purSql.append("AND purchase_date >= ? AND purchase_date <= ? GROUP BY time_label ORDER BY time_label ASC");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(purSql.toString())) {
            int idx = 1;
            pstmt.setInt(idx++, businessId);
            if (branchId != null && branchId > 0) pstmt.setInt(idx++, branchId);
            pstmt.setTimestamp(idx++, start);
            pstmt.setTimestamp(idx++, end);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    purchaseMap.put(rs.getString("time_label"), rs.getBigDecimal("total_val"));
                }
            }
        }

        Set<String> allLabels = new TreeSet<>(salesMap.keySet());
        allLabels.addAll(purchaseMap.keySet());

        if (allLabels.isEmpty()) {
            allLabels.add("No Data");
        }

        for (String label : allLabels) {
            BigDecimal sVal = salesMap.getOrDefault(label, BigDecimal.ZERO);
            BigDecimal pVal = purchaseMap.getOrDefault(label, BigDecimal.ZERO);
            ds.addValue(sVal, "Sales", label);
            ds.addValue(pVal, "Purchases", label);
        }

        return ds;
    }

    public List<TopProductDTO> getTopSellingProductsList(int businessId, Integer branchId, String dateRangeFilter, Timestamp customStart, Timestamp customEnd, int limit) throws SQLException {
        List<TopProductDTO> list = new ArrayList<>();
        if (DemoModeConfig.isEnabled()) {
            list.add(new TopProductDTO(1, 101, "Wireless Ergonomic Mouse", "Electronics", 142, new BigDecimal("70858.00")));
            list.add(new TopProductDTO(2, 102, "Mechanical RGB Keyboard", "Electronics", 98, new BigDecimal("146902.00")));
            list.add(new TopProductDTO(3, 103, "USB-C Fast Charger 65W", "Accessories", 84, new BigDecimal("25116.00")));
            list.add(new TopProductDTO(4, 104, "4K UHD Monitor 27\"", "Electronics", 32, new BigDecimal("479968.00")));
            list.add(new TopProductDTO(5, 105, "Noise Cancelling Headphones", "Audio", 28, new BigDecimal("139972.00")));
            return list;
        }

        Timestamp[] bounds = getDateBounds(dateRangeFilter, customStart, customEnd);
        Timestamp start = bounds[0];
        Timestamp end = bounds[1];

        StringBuilder sql = new StringBuilder(
                "SELECT p.id AS product_id, COALESCE(p.name, 'Unknown Product') AS product_name, " +
                        "COALESCE(c.name, 'Uncategorized') AS category_name, " +
                        "SUM(si.quantity) AS total_qty, SUM(si.line_total) AS total_rev " +
                        "FROM sale_items si " +
                        "JOIN sales s ON si.sale_id = s.id " +
                        "LEFT JOIN products p ON si.product_id = p.id " +
                        "LEFT JOIN categories c ON p.category_id = c.id " +
                        "WHERE s.business_id = ? "
        );
        if (branchId != null && branchId > 0) sql.append("AND s.branch_id = ? ");
        sql.append("AND s.sale_date >= ? AND s.sale_date <= ? GROUP BY p.id, p.name, c.name ORDER BY total_rev DESC LIMIT ?");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            pstmt.setInt(idx++, businessId);
            if (branchId != null && branchId > 0) pstmt.setInt(idx++, branchId);
            pstmt.setTimestamp(idx++, start);
            pstmt.setTimestamp(idx++, end);
            pstmt.setInt(idx++, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                int rank = 1;
                while (rs.next()) {
                    list.add(new TopProductDTO(
                            rank++,
                            rs.getInt("product_id"),
                            rs.getString("product_name"),
                            rs.getString("category_name"),
                            rs.getInt("total_qty"),
                            rs.getBigDecimal("total_rev")
                    ));
                }
            }
        }
        return list;
    }

    public DefaultCategoryDataset getSalesByCategoryDataset(int businessId, Integer branchId, String dateRangeFilter, Timestamp customStart, Timestamp customEnd) throws SQLException {
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        if (DemoModeConfig.isEnabled()) {
            ds.addValue(450000, "Sales", "Electronics");
            ds.addValue(230000, "Sales", "Accessories");
            ds.addValue(180000, "Sales", "Audio");
            ds.addValue(120000, "Sales", "Office Supplies");
            return ds;
        }

        Timestamp[] bounds = getDateBounds(dateRangeFilter, customStart, customEnd);
        Timestamp start = bounds[0];
        Timestamp end = bounds[1];

        StringBuilder sql = new StringBuilder(
                "SELECT COALESCE(c.name, 'Uncategorized') AS cat_name, SUM(si.line_total) AS cat_sales " +
                        "FROM sale_items si " +
                        "JOIN sales s ON si.sale_id = s.id " +
                        "LEFT JOIN products p ON si.product_id = p.id " +
                        "LEFT JOIN categories c ON p.category_id = c.id " +
                        "WHERE s.business_id = ? "
        );
        if (branchId != null && branchId > 0) sql.append("AND s.branch_id = ? ");
        sql.append("AND s.sale_date >= ? AND s.sale_date <= ? GROUP BY c.id, c.name ORDER BY cat_sales DESC LIMIT 6");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            pstmt.setInt(idx++, businessId);
            if (branchId != null && branchId > 0) pstmt.setInt(idx++, branchId);
            pstmt.setTimestamp(idx++, start);
            pstmt.setTimestamp(idx++, end);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString("cat_name");
                    BigDecimal val = rs.getBigDecimal("cat_sales");
                    if (name != null && val != null) {
                        ds.addValue(val, "Sales", name);
                    }
                }
            }
        }
        return ds;
    }

    public List<RecentSaleDTO> getRecentSalesList(int businessId, Integer branchId, int limit) throws SQLException {
        List<RecentSaleDTO> list = new ArrayList<>();
        if (DemoModeConfig.isEnabled()) {
            list.add(new RecentSaleDTO(1001, "INV-2026-001", new Timestamp(System.currentTimeMillis()), "Acme Corp", 4, new BigDecimal("12450.00"), "PAID"));
            list.add(new RecentSaleDTO(1002, "INV-2026-002", new Timestamp(System.currentTimeMillis() - 3600000), "TechSolutions Ltd", 2, new BigDecimal("8900.00"), "PARTIAL"));
            list.add(new RecentSaleDTO(1003, "INV-2026-003", new Timestamp(System.currentTimeMillis() - 7200000), "Walk-in Customer", 1, new BigDecimal("1500.00"), "PAID"));
            return list;
        }

        StringBuilder sql = new StringBuilder(
                "SELECT s.id AS sale_id, s.invoice_number, s.sale_date, COALESCE(c.name, 'Walk-in Customer') AS customer_name, " +
                        "(SELECT COUNT(*) FROM sale_items si WHERE si.sale_id = s.id) AS item_count, " +
                        "s.total, s.payment_status " +
                        "FROM sales s " +
                        "LEFT JOIN customers c ON s.customer_id = c.id " +
                        "WHERE s.business_id = ? "
        );
        if (branchId != null && branchId > 0) sql.append("AND s.branch_id = ? ");
        sql.append("ORDER BY s.sale_date DESC LIMIT ?");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            pstmt.setInt(idx++, businessId);
            if (branchId != null && branchId > 0) pstmt.setInt(idx++, branchId);
            pstmt.setInt(idx++, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new RecentSaleDTO(
                            rs.getInt("sale_id"),
                            rs.getString("invoice_number"),
                            rs.getTimestamp("sale_date"),
                            rs.getString("customer_name"),
                            rs.getInt("item_count"),
                            rs.getBigDecimal("total"),
                            rs.getString("payment_status")
                    ));
                }
            }
        }
        return list;
    }

    public List<LowStockProductDTO> getLowStockProducts(int businessId, Integer branchId) throws SQLException {
        List<LowStockProductDTO> list = new ArrayList<>();
        if (DemoModeConfig.isEnabled()) {
            list.add(new LowStockProductDTO(201, "Wireless Gaming Headset", "WGH-001", "Audio", 2, 10, "Critical"));
            list.add(new LowStockProductDTO(202, "USB Flash Drive 128GB", "UFD-128", "Accessories", 4, 15, "Low Stock"));
            return list;
        }

        StringBuilder sql = new StringBuilder();
        if (branchId != null && branchId > 0) {
            sql.append("SELECT p.id, p.name, p.sku, COALESCE(c.name, 'General') AS category_name, " +
                    "COALESCE(bi.quantity, 0) AS stock_qty, p.min_stock " +
                    "FROM products p " +
                    "LEFT JOIN categories c ON p.category_id = c.id " +
                    "LEFT JOIN branch_inventory bi ON bi.product_id = p.id AND bi.branch_id = ? " +
                    "WHERE p.business_id = ? AND COALESCE(bi.quantity, 0) <= p.min_stock AND p.active = 1 " +
                    "ORDER BY stock_qty ASC LIMIT 10");
        } else {
            sql.append("SELECT p.id, p.name, p.sku, COALESCE(c.name, 'General') AS category_name, " +
                    "p.stock_quantity AS stock_qty, p.min_stock " +
                    "FROM products p " +
                    "LEFT JOIN categories c ON p.category_id = c.id " +
                    "WHERE p.business_id = ? AND p.stock_quantity <= p.min_stock AND p.active = 1 " +
                    "ORDER BY p.stock_quantity ASC LIMIT 10");
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            if (branchId != null && branchId > 0) pstmt.setInt(idx++, branchId);
            pstmt.setInt(idx++, businessId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int qty = rs.getInt("stock_qty");
                    int min = rs.getInt("min_stock");
                    String status = qty <= 0 ? "Out of Stock" : (qty <= min / 2 ? "Critical" : "Low Stock");
                    list.add(new LowStockProductDTO(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("sku"),
                            rs.getString("category_name"),
                            qty,
                            min,
                            status
                    ));
                }
            }
        }
        return list;
    }

    public List<RecentActivityDTO> getRecentActivitiesList(int businessId, Integer branchId, int limit) throws SQLException {
        List<RecentActivityDTO> list = new ArrayList<>();
        if (DemoModeConfig.isEnabled()) {
            long now = System.currentTimeMillis();
            list.add(new RecentActivityDTO(new Timestamp(now), "SALE", "New Sale Completed", "Invoice #INV-2026-001 generated for Acme Corp", new BigDecimal("12450.00"), 1001));
            list.add(new RecentActivityDTO(new Timestamp(now - 1800000), "PURCHASE", "Purchase Order Received", "PO #PO-2026-042 received from TechSupplies", new BigDecimal("45000.00"), 501));
            list.add(new RecentActivityDTO(new Timestamp(now - 3600000), "INVENTORY", "Stock Restocked", "Wireless Ergonomic Mouse stock updated (+50 units)", null, 101));
            return list;
        }

        try (Connection conn = DBConnection.getConnection()) {
            // Recent Sales
            StringBuilder sSql = new StringBuilder(
                    "SELECT s.id, s.invoice_number, s.sale_date, s.total, COALESCE(c.name, 'Walk-in') AS cust_name " +
                            "FROM sales s LEFT JOIN customers c ON s.customer_id = c.id WHERE s.business_id = ? "
            );
            if (branchId != null && branchId > 0) sSql.append("AND s.branch_id = ? ");
            sSql.append("ORDER BY s.sale_date DESC LIMIT ?");

            try (PreparedStatement pstmt = conn.prepareStatement(sSql.toString())) {
                int idx = 1;
                pstmt.setInt(idx++, businessId);
                if (branchId != null && branchId > 0) pstmt.setInt(idx++, branchId);
                pstmt.setInt(idx++, limit);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        list.add(new RecentActivityDTO(
                                rs.getTimestamp("sale_date"),
                                "SALE",
                                "Sale #" + rs.getString("invoice_number"),
                                "Customer: " + rs.getString("cust_name"),
                                rs.getBigDecimal("total"),
                                rs.getInt("id")
                        ));
                    }
                }
            }

            // Recent Purchases
            StringBuilder pSql = new StringBuilder(
                    "SELECT p.id, p.purchase_number, p.purchase_date, p.total, COALESCE(sup.name, 'Supplier') AS sup_name " +
                            "FROM purchases p LEFT JOIN suppliers sup ON p.supplier_id = sup.id WHERE p.business_id = ? "
            );
            if (branchId != null && branchId > 0) pSql.append("AND p.branch_id = ? ");
            pSql.append("ORDER BY p.purchase_date DESC LIMIT ?");

            try (PreparedStatement pstmt = conn.prepareStatement(pSql.toString())) {
                int idx = 1;
                pstmt.setInt(idx++, businessId);
                if (branchId != null && branchId > 0) pstmt.setInt(idx++, branchId);
                pstmt.setInt(idx++, limit);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        list.add(new RecentActivityDTO(
                                rs.getTimestamp("purchase_date"),
                                "PURCHASE",
                                "PO #" + rs.getString("purchase_number"),
                                "Supplier: " + rs.getString("sup_name"),
                                rs.getBigDecimal("total"),
                                rs.getInt("id")
                        ));
                    }
                }
            }
        }

        list.sort((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));
        if (list.size() > limit) {
            list = list.subList(0, limit);
        }
        return list;
    }

    // Helper methods for SQL execution and date calculation
    private BigDecimal querySum(Connection conn, String sql, int businessId, Integer branchId, Timestamp start, Timestamp end) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            int idx = 1;
            pstmt.setInt(idx++, businessId);
            if (branchId != null && branchId > 0) pstmt.setInt(idx++, branchId);
            if (start != null) pstmt.setTimestamp(idx++, start);
            if (end != null) pstmt.setTimestamp(idx++, end);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    BigDecimal val = rs.getBigDecimal(1);
                    return val != null ? val : BigDecimal.ZERO;
                }
            }
        }
        return BigDecimal.ZERO;
    }

    private double calculatePercentChange(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return current != null && current.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0;
        }
        if (current == null) current = BigDecimal.ZERO;
        BigDecimal diff = current.subtract(previous);
        return diff.divide(previous, 4, RoundingMode.HALF_UP).doubleValue() * 100.0;
    }

    private Timestamp[] getDateBounds(String filter, Timestamp customStart, Timestamp customEnd) {
        long now = System.currentTimeMillis();
        long endOfToday = now + 86400000L; // 24 hour future buffer for current day transactions
        Calendar cal = Calendar.getInstance();

        if ("Today".equalsIgnoreCase(filter)) {
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            return new Timestamp[]{new Timestamp(cal.getTimeInMillis()), new Timestamp(endOfToday)};
        } else if ("Week".equalsIgnoreCase(filter)) {
            cal.add(Calendar.DAY_OF_YEAR, -7);
            return new Timestamp[]{new Timestamp(cal.getTimeInMillis()), new Timestamp(endOfToday)};
        } else if ("Year".equalsIgnoreCase(filter)) {
            cal.add(Calendar.YEAR, -1);
            return new Timestamp[]{new Timestamp(cal.getTimeInMillis()), new Timestamp(endOfToday)};
        } else if ("Custom".equalsIgnoreCase(filter) && customStart != null && customEnd != null) {
            return new Timestamp[]{customStart, customEnd};
        } else { // Default to Month
            cal.add(Calendar.DAY_OF_YEAR, -30);
            return new Timestamp[]{new Timestamp(cal.getTimeInMillis()), new Timestamp(endOfToday)};
        }
    }

    private Timestamp[] getPreviousDateBounds(String filter, Timestamp currentStart, Timestamp currentEnd) {
        long duration = currentEnd.getTime() - currentStart.getTime();
        long prevEnd = currentStart.getTime() - 1;
        long prevStart = prevEnd - duration;
        return new Timestamp[]{new Timestamp(prevStart), new Timestamp(prevEnd)};
    }
}
