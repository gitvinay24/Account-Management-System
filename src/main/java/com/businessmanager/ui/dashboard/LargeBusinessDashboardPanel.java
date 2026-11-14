package com.businessmanager.ui.dashboard;

import com.businessmanager.components.StitchIcon;
import com.businessmanager.dao.AnalyticsDAO;
import com.businessmanager.dao.BranchDAO;
import com.businessmanager.dao.BusinessDAO;
import com.businessmanager.dao.SaleDAO;
import com.businessmanager.model.*;
import com.businessmanager.security.UserSession;
import com.businessmanager.theme.Colors;
import com.businessmanager.theme.Fonts;
import com.businessmanager.theme.StitchChartTheme;
import com.businessmanager.ui.MainFrame;
import com.businessmanager.utils.InvoicePrinter;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class LargeBusinessDashboardPanel extends JPanel {

    private final AnalyticsDAO analyticsDAO = new AnalyticsDAO();
    private final BranchDAO branchDAO = new BranchDAO();
    private final BusinessDAO businessDAO = new BusinessDAO();
    private final SaleDAO saleDAO = new SaleDAO();

    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
    private final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    private JLabel greetingLabel;
    private JLabel subtitleLabel;
    private JComboBox<Branch> branchCombo;
    private JComboBox<String> dateRangeCombo;
    private JButton refreshButton;
    private JLabel statusLabel;
    private JButton retryButton;

    // 6 KPI Labels & Badges
    private JLabel totalSalesValLbl;
    private JLabel totalSalesBadgeLbl;
    private JLabel grossProfitValLbl;
    private JLabel grossProfitBadgeLbl;
    private JLabel totalPurchasesValLbl;
    private JLabel totalPurchasesBadgeLbl;
    private JLabel inventoryValLbl;
    private JLabel inventoryBadgeLbl;
    private JLabel outstandingValLbl;
    private JLabel outstandingBadgeLbl;
    private JLabel customersValLbl;
    private JLabel customersBadgeLbl;

    // Charts & Tables
    private ChartPanel trendChartPanel;
    private ChartPanel categoryChartPanel;
    private DefaultTableModel topProductsModel;
    private DefaultTableModel recentSalesModel;
    private DefaultTableModel lowStockModel;
    private JPanel activityFeedContainer;

    private String selectedDateFilter = "Month";
    private Integer selectedBranchId = null;

    public LargeBusinessDashboardPanel() {
        setLayout(new BorderLayout());
        setBackground(Colors.BACKGROUND);
        setBorder(new EmptyBorder(20, 24, 20, 24));

        buildUI();
        loadBranches();
        refreshData();
    }

    private void buildUI() {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);

        // Header & Status
        content.add(createHeaderPanel());
        content.add(Box.createVerticalStrut(16));

        // 6 KPI Cards
        content.add(createKpiGridPanel());
        content.add(Box.createVerticalStrut(20));

        // Main Analytics: Sales vs Purchases Trend (2/3) + Top Products (1/3)
        content.add(createMainAnalyticsPanel());
        content.add(Box.createVerticalStrut(20));

        // Second Analytics: Sales by Category (1/2) + Low Stock Alerts (1/2)
        content.add(createSecondaryAnalyticsPanel());
        content.add(Box.createVerticalStrut(20));

        // Bottom Section: Recent Sales Table + Recent Activity Feed
        content.add(createBottomDataPanel());

        JScrollPane scroll = new JScrollPane(content);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        add(scroll, BorderLayout.CENTER);
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout(16, 8));
        header.setOpaque(false);

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);

        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        String tod = (hour < 12) ? "Good morning" : (hour < 17 ? "Good afternoon" : "Good evening");
        String userName = UserSession.getCurrentUser() != null ? UserSession.getCurrentUser().getFullName() : "Administrator";
        String bizName = UserSession.getCurrentBusiness() != null ? UserSession.getCurrentBusiness().getName() : "Account Management";

        greetingLabel = new JLabel(tod + ", " + userName);
        greetingLabel.setFont(Fonts.DISPLAY_LG);
        greetingLabel.setForeground(Colors.TEXT);

        subtitleLabel = new JLabel("Here's how " + bizName + " is doing today.");
        subtitleLabel.setFont(Fonts.BODY);
        subtitleLabel.setForeground(Colors.SLATE);

        left.add(greetingLabel);
        left.add(Box.createVerticalStrut(4));
        left.add(subtitleLabel);

        JPanel rightControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightControls.setOpaque(false);

        statusLabel = new JLabel("Ready");
        statusLabel.setFont(Fonts.BODY_SMALL);
        statusLabel.setForeground(Colors.ON_SURFACE_VARIANT);

        retryButton = new JButton("🔄 Retry");
        retryButton.setFont(Fonts.BODY_SMALL);
        retryButton.setVisible(false);
        retryButton.addActionListener(e -> refreshData());

        dateRangeCombo = new JComboBox<>(new String[]{"Today", "Week", "Month", "Year"});
        dateRangeCombo.setSelectedItem("Month");
        dateRangeCombo.setPreferredSize(new Dimension(110, 34));
        dateRangeCombo.addActionListener(e -> {
            selectedDateFilter = (String) dateRangeCombo.getSelectedItem();
            refreshData();
        });

        branchCombo = new JComboBox<>();
        branchCombo.setPreferredSize(new Dimension(160, 34));
        branchCombo.addActionListener(e -> {
            Branch b = (Branch) branchCombo.getSelectedItem();
            selectedBranchId = (b != null && b.getId() > 0) ? b.getId() : null;
            refreshData();
        });

        refreshButton = new JButton("🔄 Refresh");
        refreshButton.setFont(Fonts.BODY_SMALL);
        refreshButton.setFocusPainted(false);
        refreshButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        refreshButton.putClientProperty("JButton.buttonType", "roundRect");
        refreshButton.addActionListener(e -> refreshData());

        rightControls.add(statusLabel);
        rightControls.add(retryButton);
        rightControls.add(new JLabel("Period:"));
        rightControls.add(dateRangeCombo);
        rightControls.add(new JLabel("Branch:"));
        rightControls.add(branchCombo);
        rightControls.add(refreshButton);

        header.add(left, BorderLayout.WEST);
        header.add(rightControls, BorderLayout.EAST);
        return header;
    }

    private JPanel createKpiGridPanel() {
        JPanel grid = new JPanel(new GridLayout(1, 6, 12, 0));
        grid.setOpaque(false);
        grid.setMaximumSize(new Dimension(2400, 110));

        totalSalesValLbl = new JLabel("₹0.00");
        totalSalesBadgeLbl = new JLabel("0.0% vs prev");
        grossProfitValLbl = new JLabel("₹0.00");
        grossProfitBadgeLbl = new JLabel("0.0% vs prev");
        totalPurchasesValLbl = new JLabel("₹0.00");
        totalPurchasesBadgeLbl = new JLabel("0.0% vs prev");
        inventoryValLbl = new JLabel("₹0.00");
        inventoryBadgeLbl = new JLabel("Live Valuation");
        outstandingValLbl = new JLabel("₹0.00");
        outstandingBadgeLbl = new JLabel("Unpaid AR/AP");
        customersValLbl = new JLabel("0");
        customersBadgeLbl = new JLabel("Active Accounts");

        grid.add(createKpiCard("TOTAL SALES", totalSalesValLbl, totalSalesBadgeLbl, StitchIcon.Type.PAYMENTS, Colors.PRIMARY));
        grid.add(createKpiCard("GROSS PROFIT", grossProfitValLbl, grossProfitBadgeLbl, StitchIcon.Type.STOREFRONT, Colors.SECONDARY));
        grid.add(createKpiCard("TOTAL PURCHASES", totalPurchasesValLbl, totalPurchasesBadgeLbl, StitchIcon.Type.DESCRIPTION, Colors.TERTIARY));
        grid.add(createKpiCard("INVENTORY VALUE", inventoryValLbl, inventoryBadgeLbl, StitchIcon.Type.STORE, Colors.PRIMARY));
        grid.add(createKpiCard("OUTSTANDING PAY", outstandingValLbl, outstandingBadgeLbl, StitchIcon.Type.PAYMENTS, Colors.TERTIARY));
        grid.add(createKpiCard("TOTAL CUSTOMERS", customersValLbl, customersBadgeLbl, StitchIcon.Type.CATEGORY, Colors.SECONDARY));

        return grid;
    }

    private JPanel createKpiCard(String title, JLabel valLbl, JLabel badgeLbl, StitchIcon.Type iconType, Color iconColor) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Colors.SURFACE_CONTAINER_LOW);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Colors.OUTLINE_VARIANT),
                new EmptyBorder(12, 14, 12, 14)
        ));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JLabel tLbl = new JLabel(title);
        tLbl.setFont(Fonts.LABEL_CAPS);
        tLbl.setForeground(Colors.ON_SURFACE_VARIANT);

        StitchIcon icon = new StitchIcon(iconType);
        icon.setForeground(iconColor);
        JLabel iconLbl = new JLabel(icon);

        top.add(tLbl, BorderLayout.WEST);
        top.add(iconLbl, BorderLayout.EAST);

        valLbl.setFont(Fonts.HEADLINE_MD);
        valLbl.setForeground(Colors.ON_SURFACE);

        badgeLbl.setFont(Fonts.BODY_SMALL);
        badgeLbl.setForeground(Colors.ON_SURFACE_VARIANT);

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);
        center.add(Box.createVerticalStrut(6));
        center.add(valLbl);
        center.add(Box.createVerticalStrut(4));
        center.add(badgeLbl);

        card.add(top, BorderLayout.NORTH);
        card.add(center, BorderLayout.CENTER);
        return card;
    }

    private JPanel createMainAnalyticsPanel() {
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;

        // Sales vs Purchases Chart (2/3)
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.65;
        gbc.insets = new Insets(0, 0, 0, 12);

        JPanel chartCard = new JPanel(new BorderLayout(0, 12));
        chartCard.setBackground(Colors.SURFACE_CONTAINER_LOW);
        chartCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Colors.OUTLINE_VARIANT),
                new EmptyBorder(16, 16, 16, 16)
        ));
        chartCard.setPreferredSize(new Dimension(600, 320));

        JLabel chartTitle = new JLabel("Revenue & Purchase Expense Trend");
        chartTitle.setFont(Fonts.HEADLINE_MD);
        chartTitle.setForeground(Colors.ON_SURFACE);
        chartCard.add(chartTitle, BorderLayout.NORTH);

        JFreeChart lineChart = ChartFactory.createLineChart(
                null, "Period", "Amount (₹)", new DefaultCategoryDataset(),
                PlotOrientation.VERTICAL, true, true, false
        );
        StitchChartTheme.applyTheme(lineChart);
        trendChartPanel = new ChartPanel(lineChart);
        trendChartPanel.setOpaque(false);
        trendChartPanel.setBackground(Colors.SURFACE_CONTAINER_LOW);
        chartCard.add(trendChartPanel, BorderLayout.CENTER);

        mainPanel.add(chartCard, gbc);

        // Top Selling Products Table (1/3)
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 0.35;
        gbc.insets = new Insets(0, 0, 0, 0);

        JPanel topProdCard = new JPanel(new BorderLayout(0, 10));
        topProdCard.setBackground(Colors.SURFACE_CONTAINER_LOW);
        topProdCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Colors.OUTLINE_VARIANT),
                new EmptyBorder(16, 16, 16, 16)
        ));

        JPanel topHeader = new JPanel(new BorderLayout());
        topHeader.setOpaque(false);

        JLabel topTitle = new JLabel("Top Selling Products");
        topTitle.setFont(Fonts.HEADLINE_MD);
        topTitle.setForeground(Colors.ON_SURFACE);

        JButton viewAllProdBtn = new JButton("View All →");
        viewAllProdBtn.setFont(Fonts.BODY_SMALL);
        viewAllProdBtn.setFocusPainted(false);
        viewAllProdBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        viewAllProdBtn.addActionListener(e -> navigateToModule("PRODUCTS"));

        topHeader.add(topTitle, BorderLayout.WEST);
        topHeader.add(viewAllProdBtn, BorderLayout.EAST);
        topProdCard.add(topHeader, BorderLayout.NORTH);

        topProductsModel = new DefaultTableModel(new Object[]{"#", "Product", "Qty", "Revenue"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable topTable = new JTable(topProductsModel);
        topTable.setRowHeight(32);
        topTable.setFont(Fonts.BODY_SMALL);
        topTable.getTableHeader().setFont(Fonts.LABEL_CAPS);
        topTable.setBackground(Colors.SURFACE_CONTAINER_LOW);
        topTable.setForeground(Colors.ON_SURFACE);
        topTable.getColumnModel().getColumn(0).setPreferredWidth(30);
        topTable.getColumnModel().getColumn(1).setPreferredWidth(140);

        topProdCard.add(new JScrollPane(topTable), BorderLayout.CENTER);

        mainPanel.add(topProdCard, gbc);

        return mainPanel;
    }

    private JPanel createSecondaryAnalyticsPanel() {
        JPanel secPanel = new JPanel(new GridLayout(1, 2, 16, 0));
        secPanel.setOpaque(false);
        secPanel.setPreferredSize(new Dimension(1000, 280));

        // Sales by Category Donut/Pie Chart
        JPanel pieCard = new JPanel(new BorderLayout(0, 10));
        pieCard.setBackground(Colors.SURFACE_CONTAINER_LOW);
        pieCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Colors.OUTLINE_VARIANT),
                new EmptyBorder(16, 16, 16, 16)
        ));

        JLabel pieTitle = new JLabel("Sales Distribution by Category");
        pieTitle.setFont(Fonts.HEADLINE_MD);
        pieTitle.setForeground(Colors.ON_SURFACE);
        pieCard.add(pieTitle, BorderLayout.NORTH);

        DefaultPieDataset pieDs = new DefaultPieDataset();
        JFreeChart pieChart = ChartFactory.createPieChart(null, pieDs, true, true, false);
        StitchChartTheme.applyTheme(pieChart);
        if (pieChart.getPlot() instanceof PiePlot plot) {
            plot.setBackgroundPaint(Colors.SURFACE_CONTAINER_LOW);
            plot.setOutlinePaint(null);
            plot.setLabelFont(Fonts.BODY_SMALL);
        }
        categoryChartPanel = new ChartPanel(pieChart);
        categoryChartPanel.setOpaque(false);
        pieCard.add(categoryChartPanel, BorderLayout.CENTER);

        // Low Stock Alerts Card
        JPanel lowStockCard = new JPanel(new BorderLayout(0, 10));
        lowStockCard.setBackground(Colors.SURFACE_CONTAINER_LOW);
        lowStockCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Colors.OUTLINE_VARIANT),
                new EmptyBorder(16, 16, 16, 16)
        ));

        JPanel lowHeader = new JPanel(new BorderLayout());
        lowHeader.setOpaque(false);

        JLabel lowTitle = new JLabel("Low Stock Inventory Alerts");
        lowTitle.setFont(Fonts.HEADLINE_MD);
        lowTitle.setForeground(Colors.ON_SURFACE);

        JButton viewInvBtn = new JButton("Manage Inventory →");
        viewInvBtn.setFont(Fonts.BODY_SMALL);
        viewInvBtn.addActionListener(e -> navigateToModule("INVENTORY"));

        lowHeader.add(lowTitle, BorderLayout.WEST);
        lowHeader.add(viewInvBtn, BorderLayout.EAST);
        lowStockCard.add(lowHeader, BorderLayout.NORTH);

        lowStockModel = new DefaultTableModel(new Object[]{"Product", "SKU", "Category", "Stock", "Min", "Status"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable lowTable = new JTable(lowStockModel);
        lowTable.setRowHeight(32);
        lowTable.setFont(Fonts.BODY_SMALL);
        lowTable.getTableHeader().setFont(Fonts.LABEL_CAPS);
        lowTable.setBackground(Colors.SURFACE_CONTAINER_LOW);
        lowTable.setForeground(Colors.ON_SURFACE);

        lowStockCard.add(new JScrollPane(lowTable), BorderLayout.CENTER);

        secPanel.add(pieCard);
        secPanel.add(lowStockCard);

        return secPanel;
    }

    private JPanel createBottomDataPanel() {
        JPanel botPanel = new JPanel(new GridBagLayout());
        botPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;

        // Recent Sales Table (2/3)
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.65;
        gbc.insets = new Insets(0, 0, 0, 12);

        JPanel salesCard = new JPanel(new BorderLayout(0, 10));
        salesCard.setBackground(Colors.SURFACE_CONTAINER_LOW);
        salesCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Colors.OUTLINE_VARIANT),
                new EmptyBorder(16, 16, 16, 16)
        ));
        salesCard.setPreferredSize(new Dimension(600, 260));

        JPanel salesHeader = new JPanel(new BorderLayout());
        salesHeader.setOpaque(false);

        JLabel salesTitle = new JLabel("Recent Business Sales");
        salesTitle.setFont(Fonts.HEADLINE_MD);
        salesTitle.setForeground(Colors.ON_SURFACE);

        JButton viewSalesBtn = new JButton("All Sales →");
        viewSalesBtn.setFont(Fonts.BODY_SMALL);
        viewSalesBtn.addActionListener(e -> navigateToModule("SALES"));

        salesHeader.add(salesTitle, BorderLayout.WEST);
        salesHeader.add(viewSalesBtn, BorderLayout.EAST);
        salesCard.add(salesHeader, BorderLayout.NORTH);

        recentSalesModel = new DefaultTableModel(new Object[]{"ID", "Invoice #", "Date & Time", "Customer", "Items", "Total", "Status"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable salesTable = new JTable(recentSalesModel);
        salesTable.setRowHeight(34);
        salesTable.setFont(Fonts.BODY_SMALL);
        salesTable.getTableHeader().setFont(Fonts.LABEL_CAPS);
        salesTable.setBackground(Colors.SURFACE_CONTAINER_LOW);
        salesTable.setForeground(Colors.ON_SURFACE);

        // Hide ID column
        salesTable.getColumnModel().getColumn(0).setMinWidth(0);
        salesTable.getColumnModel().getColumn(0).setMaxWidth(0);
        salesTable.getColumnModel().getColumn(0).setWidth(0);

        salesTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = salesTable.getSelectedRow();
                    if (row >= 0) {
                        int saleId = (int) recentSalesModel.getValueAt(row, 0);
                        previewInvoice(saleId);
                    }
                }
            }
        });

        salesCard.add(new JScrollPane(salesTable), BorderLayout.CENTER);
        botPanel.add(salesCard, gbc);

        // Recent Activity Feed (1/3)
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 0.35;
        gbc.insets = new Insets(0, 0, 0, 0);

        JPanel actCard = new JPanel(new BorderLayout(0, 10));
        actCard.setBackground(Colors.SURFACE_CONTAINER_LOW);
        actCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Colors.OUTLINE_VARIANT),
                new EmptyBorder(16, 16, 16, 16)
        ));

        JLabel actTitle = new JLabel("Audit & Activity Feed");
        actTitle.setFont(Fonts.HEADLINE_MD);
        actTitle.setForeground(Colors.ON_SURFACE);
        actCard.add(actTitle, BorderLayout.NORTH);

        activityFeedContainer = new JPanel();
        activityFeedContainer.setLayout(new BoxLayout(activityFeedContainer, BoxLayout.Y_AXIS));
        activityFeedContainer.setOpaque(false);

        JScrollPane actScroll = new JScrollPane(activityFeedContainer);
        actScroll.setOpaque(false);
        actScroll.getViewport().setOpaque(false);
        actScroll.setBorder(null);

        actCard.add(actScroll, BorderLayout.CENTER);
        botPanel.add(actCard, gbc);

        return botPanel;
    }

    private void loadBranches() {
        try {
            int bizId = UserSession.getCurrentBusiness() != null ? UserSession.getCurrentBusiness().getId() : 1;
            List<Branch> branches = branchDAO.findByBusiness(bizId);

            branchCombo.removeAllItems();
            Branch allB = new Branch();
            allB.setId(0);
            allB.setName("All Branches");
            branchCombo.addItem(allB);

            for (Branch b : branches) {
                branchCombo.addItem(b);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void refreshData() {
        statusLabel.setText("Fetching metrics...");
        refreshButton.setEnabled(false);
        retryButton.setVisible(false);

        final int bizId = UserSession.getCurrentBusiness() != null ? UserSession.getCurrentBusiness().getId() : 1;
        final Integer branchId = selectedBranchId;
        final String dateFilter = selectedDateFilter;

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private DashboardMetricsDTO metrics;
            private DefaultCategoryDataset trendDs;
            private List<TopProductDTO> topProducts;
            private DefaultCategoryDataset categoryDs;
            private List<RecentSaleDTO> recentSales;
            private List<LowStockProductDTO> lowStock;
            private List<RecentActivityDTO> activities;
            private Exception error;

            @Override
            protected Void doInBackground() {
                try {
                    metrics = analyticsDAO.getDashboardMetrics(bizId, branchId, dateFilter, null, null);
                    trendDs = analyticsDAO.getSalesVsPurchasesTrendDataset(bizId, branchId, dateFilter, null, null);
                    topProducts = analyticsDAO.getTopSellingProductsList(bizId, branchId, dateFilter, null, null, 5);
                    categoryDs = analyticsDAO.getSalesByCategoryDataset(bizId, branchId, dateFilter, null, null);
                    recentSales = analyticsDAO.getRecentSalesList(bizId, branchId, 8);
                    lowStock = analyticsDAO.getLowStockProducts(bizId, branchId);
                    activities = analyticsDAO.getRecentActivitiesList(bizId, branchId, 6);
                } catch (Exception ex) {
                    error = ex;
                }
                return null;
            }

            @Override
            protected void done() {
                refreshButton.setEnabled(true);
                if (error != null) {
                    statusLabel.setText("Error loading data");
                    retryButton.setVisible(true);
                    error.printStackTrace();
                    return;
                }

                statusLabel.setText("Updated");
                retryButton.setVisible(false);

                // Update 6 KPI Cards
                if (metrics != null) {
                    totalSalesValLbl.setText(currencyFormat.format(metrics.getTotalSales()));
                    totalSalesBadgeLbl.setText(formatBadge(metrics.getSalesChangePercent()));

                    grossProfitValLbl.setText(currencyFormat.format(metrics.getGrossProfit()));
                    grossProfitBadgeLbl.setText(formatBadge(metrics.getProfitChangePercent()));

                    totalPurchasesValLbl.setText(currencyFormat.format(metrics.getTotalPurchases()));
                    totalPurchasesBadgeLbl.setText(formatBadge(metrics.getPurchasesChangePercent()));

                    inventoryValLbl.setText(currencyFormat.format(metrics.getInventoryValue()));
                    outstandingValLbl.setText(currencyFormat.format(metrics.getOutstandingPayments()));
                    customersValLbl.setText(String.valueOf(metrics.getTotalCustomers()));
                }

                // Update Trend Chart
                if (trendDs != null) {
                    JFreeChart lineChart = ChartFactory.createLineChart(
                            null, "Period", "Amount (₹)", trendDs,
                            PlotOrientation.VERTICAL, true, true, false
                    );
                    StitchChartTheme.applyTheme(lineChart);
                    trendChartPanel.setChart(lineChart);
                }

                // Update Top Products Table
                topProductsModel.setRowCount(0);
                if (topProducts != null && !topProducts.isEmpty()) {
                    for (TopProductDTO p : topProducts) {
                        topProductsModel.addRow(new Object[]{
                                p.getRank(),
                                p.getProductName(),
                                p.getQuantitySold(),
                                currencyFormat.format(p.getRevenue())
                        });
                    }
                } else {
                    topProductsModel.addRow(new Object[]{"-", "No products sold during this period.", "-", "-"});
                }

                // Update Sales by Category Chart
                if (categoryDs != null) {
                    DefaultPieDataset pieDs = new DefaultPieDataset();
                    for (int i = 0; i < categoryDs.getColumnCount(); i++) {
                        Comparable key = categoryDs.getColumnKey(i);
                        Number val = categoryDs.getValue(0, i);
                        if (val != null) {
                            pieDs.setValue(key, val);
                        }
                    }
                    JFreeChart pieChart = ChartFactory.createPieChart(null, pieDs, true, true, false);
                    StitchChartTheme.applyTheme(pieChart);
                    if (pieChart.getPlot() instanceof PiePlot plot) {
                        plot.setBackgroundPaint(Colors.SURFACE_CONTAINER_LOW);
                        plot.setOutlinePaint(null);
                    }
                    categoryChartPanel.setChart(pieChart);
                }

                // Update Low Stock Table
                lowStockModel.setRowCount(0);
                if (lowStock != null && !lowStock.isEmpty()) {
                    for (LowStockProductDTO p : lowStock) {
                        lowStockModel.addRow(new Object[]{
                                p.getProductName(),
                                p.getSku(),
                                p.getCategoryName(),
                                p.getStockQuantity(),
                                p.getMinStock(),
                                p.getStatus()
                        });
                    }
                } else {
                    lowStockModel.addRow(new Object[]{"All products are sufficiently stocked.", "-", "-", "-", "-", "OK"});
                }

                // Update Recent Sales Table
                recentSalesModel.setRowCount(0);
                if (recentSales != null) {
                    for (RecentSaleDTO s : recentSales) {
                        recentSalesModel.addRow(new Object[]{
                                s.getSaleId(),
                                s.getInvoiceNumber(),
                                s.getSaleDate() != null ? dateTimeFormat.format(s.getSaleDate()) : "-",
                                s.getCustomerName(),
                                s.getItemCount(),
                                currencyFormat.format(s.getTotalAmount()),
                                s.getPaymentStatus()
                        });
                    }
                }

                // Update Recent Activity Feed
                activityFeedContainer.removeAll();
                if (activities != null && !activities.isEmpty()) {
                    for (RecentActivityDTO act : activities) {
                        activityFeedContainer.add(createActivityItem(act));
                        activityFeedContainer.add(Box.createVerticalStrut(8));
                    }
                } else {
                    JLabel emptyLbl = new JLabel("No recent activity recorded.");
                    emptyLbl.setFont(Fonts.BODY_SMALL);
                    emptyLbl.setForeground(Colors.ON_SURFACE_VARIANT);
                    activityFeedContainer.add(emptyLbl);
                }
                activityFeedContainer.revalidate();
                activityFeedContainer.repaint();
            }
        };

        worker.execute();
    }

    private JPanel createActivityItem(RecentActivityDTO act) {
        JPanel item = new JPanel(new BorderLayout(10, 0));
        item.setOpaque(false);
        item.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Colors.OUTLINE_VARIANT),
                new EmptyBorder(6, 4, 6, 4)
        ));

        StitchIcon.Type iconType = "SALE".equals(act.getType()) ? StitchIcon.Type.PAYMENTS :
                ("PURCHASE".equals(act.getType()) ? StitchIcon.Type.DESCRIPTION : StitchIcon.Type.STORE);
        Color iconColor = "SALE".equals(act.getType()) ? Colors.PRIMARY :
                ("PURCHASE".equals(act.getType()) ? Colors.TERTIARY : Colors.SECONDARY);

        StitchIcon icon = new StitchIcon(iconType);
        icon.setForeground(iconColor);
        JLabel iconLbl = new JLabel(icon);

        JPanel details = new JPanel();
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
        details.setOpaque(false);

        JLabel titleLbl = new JLabel(act.getTitle());
        titleLbl.setFont(Fonts.LABEL_CAPS);
        titleLbl.setForeground(Colors.ON_SURFACE);

        JLabel descLbl = new JLabel(act.getDescription());
        descLbl.setFont(Fonts.BODY_SMALL);
        descLbl.setForeground(Colors.ON_SURFACE_VARIANT);

        details.add(titleLbl);
        details.add(Box.createVerticalStrut(2));
        details.add(descLbl);

        JLabel timeLbl = new JLabel(act.getTimestamp() != null ? dateTimeFormat.format(act.getTimestamp()) : "");
        timeLbl.setFont(Fonts.BODY_SMALL);
        timeLbl.setForeground(Colors.ON_SURFACE_VARIANT);

        item.add(iconLbl, BorderLayout.WEST);
        item.add(details, BorderLayout.CENTER);
        item.add(timeLbl, BorderLayout.EAST);

        return item;
    }

    private String formatBadge(double percent) {
        if (percent > 0) {
            return String.format("↑ %.1f%% vs prev period", percent);
        } else if (percent < 0) {
            return String.format("↓ %.1f%% vs prev period", Math.abs(percent));
        } else {
            return "No comparison data";
        }
    }

    private void previewInvoice(int saleId) {
        try {
            Sale sale = saleDAO.findById(saleId);
            Business business = UserSession.getCurrentBusiness();
            if (business == null && sale != null) {
                business = businessDAO.findById(sale.getBusinessId());
            }
            if (sale != null && business != null) {
                InvoicePrinter.showPreview(this, sale, business);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Could not open invoice preview: " + ex.getMessage(), "Preview Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void navigateToModule(String moduleKey) {
        Window w = SwingUtilities.getWindowAncestor(this);
        if (w instanceof MainFrame mainFrame) {
            try {
                var method = MainFrame.class.getDeclaredMethod("navigateToModule", String.class);
                method.setAccessible(true);
                method.invoke(mainFrame, moduleKey);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
