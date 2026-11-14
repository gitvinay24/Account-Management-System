package com.businessmanager.ui.analytics;

import com.businessmanager.dao.BranchDAO;
import com.businessmanager.model.Branch;
import com.businessmanager.security.UserSession;
import com.businessmanager.service.AnalyticsService;
import com.businessmanager.theme.Colors;
import com.businessmanager.theme.Fonts;
import com.businessmanager.theme.StitchChartTheme;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.SQLException;

public class AnalyticsPanel extends JPanel {

    private final AnalyticsService analyticsService = new AnalyticsService();
    private final BranchDAO branchDAO = new BranchDAO();

    private ChartPanel chartPanel1;
    private ChartPanel chartPanel2;
    private ChartPanel chartPanel3;
    private ChartPanel chartPanel4;
    private ChartPanel chartPanel5;
    private ChartPanel chartPanel6;

    private JComboBox<Branch> branchFilterCombo;

    public AnalyticsPanel() {
        setLayout(new BorderLayout());
        setBackground(Colors.BACKGROUND);
        setBorder(new EmptyBorder(20, 24, 20, 24));

        buildUI();
        loadBranchDropdownData();
        refreshData();
    }

    private void buildUI() {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);

        // Header
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);

        JPanel headerText = new JPanel();
        headerText.setLayout(new BoxLayout(headerText, BoxLayout.Y_AXIS));
        headerText.setOpaque(false);

        JLabel title = new JLabel("Business Analytics & Intelligence");
        title.setFont(Fonts.DISPLAY_LG);
        title.setForeground(Colors.ON_SURFACE);

        JLabel sub = new JLabel("Data-driven insights into top-selling products, category distribution, branch performance, and revenue trends");
        sub.setFont(Fonts.BODY);
        sub.setForeground(Colors.ON_SURFACE_VARIANT);

        headerText.add(title);
        headerText.add(Box.createVerticalStrut(4));
        headerText.add(sub);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        actions.setOpaque(false);

        branchFilterCombo = new JComboBox<>();
        branchFilterCombo.setPreferredSize(new Dimension(160, 36));
        branchFilterCombo.addActionListener(e -> refreshData());

        JButton refreshBtn = new JButton("🔄  Refresh Charts");
        refreshBtn.setFont(Fonts.BODY_SMALL);
        refreshBtn.addActionListener(e -> refreshData());

        actions.add(new JLabel("Branch:"));
        actions.add(branchFilterCombo);
        actions.add(refreshBtn);

        topPanel.add(headerText, BorderLayout.WEST);
        topPanel.add(actions, BorderLayout.EAST);

        content.add(topPanel);
        content.add(Box.createVerticalStrut(20));

        // Charts Grid (2 columns x 3 rows)
        JPanel grid = new JPanel(new GridLayout(3, 2, 20, 20));
        grid.setOpaque(false);
        grid.setPreferredSize(new Dimension(1000, 1100));

        // Chart 1: Top Selling Products
        chartPanel1 = createChartCard(grid, "Top Selling Products Ranking", "Product", "Sales (₹)");

        // Chart 2: Category Revenue Breakdown
        chartPanel2 = createChartCard(grid, "Category Revenue Breakdown", "Category", "Revenue (₹)");

        // Chart 3: Branch Comparison
        chartPanel3 = createChartCard(grid, "Branch Performance & Comparison", "Branch", "Revenue / Collections (₹)");

        // Chart 4: Monthly Revenue Trends
        chartPanel4 = createChartCard(grid, "Monthly Revenue Trends", "Month", "Revenue (₹)");

        // Chart 5: Expense Breakdown by Category
        chartPanel5 = createChartCard(grid, "Operating Expense Breakdown", "Category", "Expense (₹)");

        // Chart 6: Top Customers by Spend
        chartPanel6 = createChartCard(grid, "Top Customer Spending Accounts", "Customer", "Total Spend (₹)");

        content.add(grid);

        JScrollPane scroll = new JScrollPane(content);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);

        add(scroll, BorderLayout.CENTER);
    }

    private ChartPanel createChartCard(JPanel gridPanel, String cardTitle, String xAxisLabel, String yAxisLabel) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Colors.SURFACE_CONTAINER);
        card.setBorder(new EmptyBorder(14, 14, 14, 14));

        JLabel title = new JLabel(cardTitle);
        title.setFont(Fonts.TITLE_SM);
        title.setForeground(Colors.ON_SURFACE);
        card.add(title, BorderLayout.NORTH);

        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        JFreeChart chart = ChartFactory.createBarChart(null, xAxisLabel, yAxisLabel, ds, PlotOrientation.VERTICAL, true, true, false);
        StitchChartTheme.applyTheme(chart);

        ChartPanel cp = new ChartPanel(chart);
        cp.setOpaque(false);
        card.add(cp, BorderLayout.CENTER);

        gridPanel.add(card);
        return cp;
    }

    private void loadBranchDropdownData() {
        int bId = UserSession.getBusinessId();
        if (!UserSession.isLoggedIn()) return;

        try {
            branchFilterCombo.removeAllItems();
            branchFilterCombo.addItem(new Branch() {{ setId(0); setName("All Branches"); }});
            for (Branch br : branchDAO.findByBusiness(bId)) {
                branchFilterCombo.addItem(br);
            }
        } catch (SQLException e) {
            System.err.println("Analytics branch dropdown warning: " + e.getMessage());
        }
    }

    public void refreshData() {
        int bId = UserSession.getBusinessId();
        if (!UserSession.isLoggedIn()) return;

        Branch selBranch = (Branch) branchFilterCombo.getSelectedItem();
        Integer branchId = selBranch != null && selBranch.getId() > 0 ? selBranch.getId() : null;

        try {
            updateChart(chartPanel1, "Top Selling Products", "Product", "Sales (₹)", analyticsService.getTopProductsDataset(bId, branchId), false);
            updateChart(chartPanel2, "Category Revenue Breakdown", "Category", "Revenue (₹)", analyticsService.getCategoryRevenueDataset(bId, branchId), false);
            updateChart(chartPanel3, "Branch Comparison", "Branch", "Revenue / Collections (₹)", analyticsService.getBranchComparisonDataset(bId), true);
            updateChart(chartPanel4, "Monthly Revenue Trends", "Month", "Revenue (₹)", analyticsService.getMonthlyRevenueTrendDataset(bId, branchId), false);
            updateChart(chartPanel5, "Operating Expense Breakdown", "Category", "Expense (₹)", analyticsService.getExpenseCategoryBreakdownDataset(bId, branchId), false);
            updateChart(chartPanel6, "Top Customer Spending Accounts", "Customer", "Spend (₹)", analyticsService.getTopCustomersDataset(bId, branchId), false);
        } catch (SQLException e) {
            System.err.println("Analytics data refresh error: " + e.getMessage());
        }
    }

    private void updateChart(ChartPanel panel, String title, String x, String y, DefaultCategoryDataset ds, boolean legend) {
        JFreeChart chart = ChartFactory.createBarChart(null, x, y, ds, PlotOrientation.VERTICAL, legend, true, false);
        StitchChartTheme.applyTheme(chart);
        panel.setChart(chart);
    }
}
