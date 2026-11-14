package com.businessmanager.ui.reports;

import com.businessmanager.dao.BranchDAO;
import com.businessmanager.dao.CategoryDAO;
import com.businessmanager.dao.CustomerDAO;
import com.businessmanager.dao.SupplierDAO;
import com.businessmanager.enums.PaymentStatus;
import com.businessmanager.model.Branch;
import com.businessmanager.model.Category;
import com.businessmanager.model.Customer;
import com.businessmanager.model.ReportFilterDTO;
import com.businessmanager.model.Supplier;
import com.businessmanager.security.UserSession;
import com.businessmanager.service.ReportService;
import com.businessmanager.theme.Colors;
import com.businessmanager.theme.Fonts;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Date;
import java.sql.SQLException;
import java.util.List;

public class ReportsPanel extends JPanel {

    private final ReportService reportService = new ReportService();
    private final CategoryDAO categoryDAO = new CategoryDAO();
    private final SupplierDAO supplierDAO = new SupplierDAO();
    private final CustomerDAO customerDAO = new CustomerDAO();
    private final BranchDAO branchDAO = new BranchDAO();

    private JComboBox<String> reportTypeCombo;
    private JSpinner startDateSpinner;
    private JSpinner endDateSpinner;
    private JCheckBox enableDateFilterCheck;

    private JComboBox<Category> categoryFilterCombo;
    private JComboBox<Supplier> supplierFilterCombo;
    private JComboBox<Customer> customerFilterCombo;
    private JComboBox<Branch> branchFilterCombo;
    private JComboBox<Object> paymentStatusCombo;

    private DefaultTableModel tableModel;
    private JTable table;
    private JLabel summaryLabel;

    public ReportsPanel() {
        setLayout(new BorderLayout());
        setBackground(Colors.BACKGROUND);
        setBorder(new EmptyBorder(20, 24, 20, 24));

        buildUI();
        loadDropdownData();
        loadReport();
    }

    private void buildUI() {
        JPanel content = new JPanel(new BorderLayout(0, 16));
        content.setOpaque(false);

        // Header & Selector
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);

        JPanel headerText = new JPanel();
        headerText.setLayout(new BoxLayout(headerText, BoxLayout.Y_AXIS));
        headerText.setOpaque(false);

        JLabel title = new JLabel("Comprehensive Business Reports");
        title.setFont(Fonts.DISPLAY_LG);
        title.setForeground(Colors.ON_SURFACE);

        JLabel sub = new JLabel("Generate real-time audit reports for Sales, Purchases, Inventory Valuation, Expenses, and Profit/Loss");
        sub.setFont(Fonts.BODY);
        sub.setForeground(Colors.ON_SURFACE_VARIANT);

        headerText.add(title);
        headerText.add(Box.createVerticalStrut(4));
        headerText.add(sub);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        actions.setOpaque(false);

        reportTypeCombo = new JComboBox<>(new String[]{
                "Sales Report",
                "Purchase Report",
                "Inventory Valuation Report",
                "Expense Breakdown Report",
                "Profit / Loss Report",
                "Executive Summary"
        });
        reportTypeCombo.setPreferredSize(new Dimension(240, 36));
        reportTypeCombo.addActionListener(e -> loadReport());

        JButton exportBtn = new JButton("📄  Export View");
        exportBtn.setFont(Fonts.BODY_SMALL);
        exportBtn.addActionListener(e -> exportCurrentReport());

        actions.add(new JLabel("Select Report:"));
        actions.add(reportTypeCombo);
        actions.add(exportBtn);

        topPanel.add(headerText, BorderLayout.WEST);
        topPanel.add(actions, BorderLayout.EAST);

        content.add(topPanel, BorderLayout.NORTH);

        // Filter & Table Container
        JPanel mainCenter = new JPanel(new BorderLayout(0, 16));
        mainCenter.setOpaque(false);
        mainCenter.add(createFilterBarPanel(), BorderLayout.NORTH);

        // Report Card
        JPanel reportCard = new JPanel(new BorderLayout(0, 12));
        reportCard.setBackground(Colors.SURFACE_CONTAINER);
        reportCard.setBorder(new EmptyBorder(16, 16, 16, 16));

        summaryLabel = new JLabel("Report Summary");
        summaryLabel.setFont(Fonts.HEADLINE_MD);
        summaryLabel.setForeground(Colors.PRIMARY);
        reportCard.add(summaryLabel, BorderLayout.NORTH);

        tableModel = new DefaultTableModel();
        table = new JTable(tableModel);
        table.setBackground(Colors.SURFACE_CONTAINER);
        table.setForeground(Colors.ON_SURFACE);
        table.setGridColor(Colors.OUTLINE_VARIANT);
        table.setRowHeight(36);
        table.getTableHeader().setBackground(Colors.SURFACE_CONTAINER_HIGH);
        table.getTableHeader().setForeground(Colors.ON_SURFACE_VARIANT);
        table.getTableHeader().setFont(Fonts.LABEL_CAPS);
        table.setFont(Fonts.BODY);

        reportCard.add(new JScrollPane(table), BorderLayout.CENTER);
        mainCenter.add(reportCard, BorderLayout.CENTER);

        content.add(mainCenter, BorderLayout.CENTER);
        add(content, BorderLayout.CENTER);
    }

    private JPanel createFilterBarPanel() {
        JPanel filterCard = new JPanel(new GridBagLayout());
        filterCard.setBackground(Colors.SURFACE_CONTAINER_LOW);
        filterCard.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Colors.OUTLINE_VARIANT),
                "Report Filters", 0, 0, Fonts.LABEL_CAPS, Colors.ON_SURFACE_VARIANT));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // Date Range Controls
        enableDateFilterCheck = new JCheckBox("Date Range:");
        enableDateFilterCheck.setFont(Fonts.BODY_SMALL);
        enableDateFilterCheck.setOpaque(false);
        enableDateFilterCheck.setForeground(Colors.ON_SURFACE);

        long now = System.currentTimeMillis();
        long monthAgo = now - (30L * 24 * 3600 * 1000);
        startDateSpinner = new JSpinner(new SpinnerDateModel(new java.util.Date(monthAgo), null, null, java.util.Calendar.DAY_OF_MONTH));
        startDateSpinner.setEditor(new JSpinner.DateEditor(startDateSpinner, "yyyy-MM-dd"));
        startDateSpinner.setPreferredSize(new Dimension(115, 32));

        endDateSpinner = new JSpinner(new SpinnerDateModel(new java.util.Date(now), null, null, java.util.Calendar.DAY_OF_MONTH));
        endDateSpinner.setEditor(new JSpinner.DateEditor(endDateSpinner, "yyyy-MM-dd"));
        endDateSpinner.setPreferredSize(new Dimension(115, 32));

        // Dropdowns
        categoryFilterCombo = new JComboBox<>();
        categoryFilterCombo.setPreferredSize(new Dimension(140, 32));

        customerFilterCombo = new JComboBox<>();
        customerFilterCombo.setPreferredSize(new Dimension(140, 32));

        supplierFilterCombo = new JComboBox<>();
        supplierFilterCombo.setPreferredSize(new Dimension(140, 32));

        branchFilterCombo = new JComboBox<>();
        branchFilterCombo.setPreferredSize(new Dimension(140, 32));

        paymentStatusCombo = new JComboBox<>();
        paymentStatusCombo.addItem("All Statuses");
        for (PaymentStatus status : PaymentStatus.values()) {
            paymentStatusCombo.addItem(status);
        }
        paymentStatusCombo.setPreferredSize(new Dimension(130, 32));

        JButton applyFilterBtn = new JButton("🔍  Apply Filters");
        applyFilterBtn.setFont(Fonts.BODY_SMALL);
        applyFilterBtn.setForeground(Colors.ON_PRIMARY);
        applyFilterBtn.setBackground(Colors.PRIMARY);
        applyFilterBtn.addActionListener(e -> loadReport());

        JButton resetFilterBtn = new JButton("🔄  Reset");
        resetFilterBtn.setFont(Fonts.BODY_SMALL);
        resetFilterBtn.addActionListener(e -> resetFilters());

        // Row 1: Date Range, Category, Customer
        int col = 0;
        gbc.gridy = 0;

        gbc.gridx = col++; filterCard.add(enableDateFilterCheck, gbc);
        gbc.gridx = col++; filterCard.add(startDateSpinner, gbc);
        gbc.gridx = col++; filterCard.add(new JLabel("to"), gbc);
        gbc.gridx = col++; filterCard.add(endDateSpinner, gbc);

        gbc.gridx = col++; filterCard.add(new JLabel("Category:"), gbc);
        gbc.gridx = col++; filterCard.add(categoryFilterCombo, gbc);

        gbc.gridx = col++; filterCard.add(new JLabel("Customer:"), gbc);
        gbc.gridx = col++; filterCard.add(customerFilterCombo, gbc);

        // Row 2: Supplier, Branch, Status, Action Buttons
        col = 0;
        gbc.gridy = 1;

        gbc.gridx = col++; filterCard.add(new JLabel("Supplier:"), gbc);
        gbc.gridx = col++; filterCard.add(supplierFilterCombo, gbc);

        gbc.gridx = col++; filterCard.add(new JLabel("Branch:"), gbc);
        gbc.gridx = col++; filterCard.add(branchFilterCombo, gbc);

        gbc.gridx = col++; filterCard.add(new JLabel("Status:"), gbc);
        gbc.gridx = col++; filterCard.add(paymentStatusCombo, gbc);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        btnPanel.setOpaque(false);
        btnPanel.add(applyFilterBtn);
        btnPanel.add(resetFilterBtn);

        gbc.gridx = col++; gbc.gridwidth = 2; filterCard.add(btnPanel, gbc);

        return filterCard;
    }

    private void resetFilters() {
        enableDateFilterCheck.setSelected(false);
        if (categoryFilterCombo.getItemCount() > 0) categoryFilterCombo.setSelectedIndex(0);
        if (customerFilterCombo.getItemCount() > 0) customerFilterCombo.setSelectedIndex(0);
        if (supplierFilterCombo.getItemCount() > 0) supplierFilterCombo.setSelectedIndex(0);
        if (branchFilterCombo.getItemCount() > 0) branchFilterCombo.setSelectedIndex(0);
        if (paymentStatusCombo.getItemCount() > 0) paymentStatusCombo.setSelectedIndex(0);
        loadReport();
    }

    private void loadDropdownData() {
        int bId = UserSession.getBusinessId();
        if (!UserSession.isLoggedIn()) return;

        try {
            categoryFilterCombo.removeAllItems();
            categoryFilterCombo.addItem(new Category() {{ setId(0); setName("All Categories"); }});
            for (Category c : categoryDAO.findByBusiness(bId)) categoryFilterCombo.addItem(c);

            customerFilterCombo.removeAllItems();
            customerFilterCombo.addItem(new Customer() {{ setId(0); setName("All Customers"); }});
            for (Customer cust : customerDAO.findByBusiness(bId)) customerFilterCombo.addItem(cust);

            supplierFilterCombo.removeAllItems();
            supplierFilterCombo.addItem(new Supplier() {{ setId(0); setName("All Suppliers"); }});
            for (Supplier sup : supplierDAO.findByBusiness(bId)) supplierFilterCombo.addItem(sup);

            branchFilterCombo.removeAllItems();
            branchFilterCombo.addItem(new Branch() {{ setId(0); setName("All Branches"); }});
            for (Branch br : branchDAO.findByBusiness(bId)) branchFilterCombo.addItem(br);
        } catch (SQLException e) {
            System.err.println("Report dropdown loading warning: " + e.getMessage());
        }
    }

    private void loadReport() {
        int bId = UserSession.getBusinessId();
        if (!UserSession.isLoggedIn()) return;

        String selectedType = (String) reportTypeCombo.getSelectedItem();
        tableModel.setRowCount(0);
        tableModel.setColumnCount(0);

        ReportFilterDTO filter = new ReportFilterDTO();
        filter.setReportType(selectedType);

        if (enableDateFilterCheck.isSelected()) {
            filter.setStartDate(new Date(((java.util.Date) startDateSpinner.getValue()).getTime()));
            filter.setEndDate(new Date(((java.util.Date) endDateSpinner.getValue()).getTime()));
        }

        Category cat = (Category) categoryFilterCombo.getSelectedItem();
        if (cat != null && cat.getId() > 0) filter.setCategoryId(cat.getId());

        Customer cust = (Customer) customerFilterCombo.getSelectedItem();
        if (cust != null && cust.getId() > 0) filter.setCustomerId(cust.getId());

        Supplier sup = (Supplier) supplierFilterCombo.getSelectedItem();
        if (sup != null && sup.getId() > 0) filter.setSupplierId(sup.getId());

        Branch br = (Branch) branchFilterCombo.getSelectedItem();
        if (br != null && br.getId() > 0) filter.setBranchId(br.getId());

        Object statusObj = paymentStatusCombo.getSelectedItem();
        if (statusObj instanceof PaymentStatus ps) filter.setPaymentStatus(ps);

        try {
            configureTableColumns(selectedType);
            List<Object[]> reportData = reportService.generateReport(bId, filter);
            for (Object[] row : reportData) {
                tableModel.addRow(row);
            }
        } catch (SQLException e) {
            System.err.println("Could not generate report: " + e.getMessage());
        }
    }

    private void configureTableColumns(String type) {
        if ("Sales Report".equals(type)) {
            summaryLabel.setText("Sales & Revenue Performance Report");
            tableModel.addColumn("Invoice #");
            tableModel.addColumn("Customer");
            tableModel.addColumn("Branch");
            tableModel.addColumn("Payment Method");
            tableModel.addColumn("Status");
            tableModel.addColumn("Total Amount");
            tableModel.addColumn("Date");
        } else if ("Purchase Report".equals(type)) {
            summaryLabel.setText("Supplier Wholesale Purchase Orders Report");
            tableModel.addColumn("PO #");
            tableModel.addColumn("Supplier");
            tableModel.addColumn("Branch");
            tableModel.addColumn("Payment Method");
            tableModel.addColumn("Status");
            tableModel.addColumn("Total Cost");
            tableModel.addColumn("Date");
        } else if ("Inventory Valuation Report".equals(type)) {
            summaryLabel.setText("Asset Stock Valuation & Inventory Audit");
            tableModel.addColumn("SKU");
            tableModel.addColumn("Product Name");
            tableModel.addColumn("Category");
            tableModel.addColumn("Branch");
            tableModel.addColumn("Stock Qty");
            tableModel.addColumn("Unit Cost");
            tableModel.addColumn("Total Inventory Value");
        } else if ("Expense Breakdown Report".equals(type)) {
            summaryLabel.setText("Categorized Operating Expense Audit");
            tableModel.addColumn("Expense ID");
            tableModel.addColumn("Title");
            tableModel.addColumn("Category");
            tableModel.addColumn("Branch");
            tableModel.addColumn("Amount");
            tableModel.addColumn("Date");
        } else if ("Profit / Loss Report".equals(type)) {
            summaryLabel.setText("Profit & Loss Financial Audit Statement");
            tableModel.addColumn("Metric Category");
            tableModel.addColumn("Valuation");
            tableModel.addColumn("Description / Formula");
        } else {
            summaryLabel.setText("Executive Financial & Asset Summary Report");
            tableModel.addColumn("Metric Category");
            tableModel.addColumn("Valuation");
        }
    }

    private void exportCurrentReport() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export report as CSV");
        chooser.setSelectedFile(new File("business-manager-report.csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".csv")) file = new File(file.getAbsolutePath() + ".csv");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            for (int column = 0; column < tableModel.getColumnCount(); column++) {
                if (column > 0) writer.write(',');
                writer.write(csv(tableModel.getColumnName(column)));
            }
            writer.newLine();
            for (int row = 0; row < tableModel.getRowCount(); row++) {
                for (int column = 0; column < tableModel.getColumnCount(); column++) {
                    if (column > 0) writer.write(',');
                    Object value = tableModel.getValueAt(row, column);
                    writer.write(csv(value == null ? "" : value.toString()));
                }
                writer.newLine();
            }
            JOptionPane.showMessageDialog(this, "Report exported to " + file.getAbsolutePath(), "Report Export", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Could not export report: " + e.getMessage(), "Report Export", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String csv(String value) {
        return '"' + value.replace("\"", "\"\"") + '"';
    }
}
