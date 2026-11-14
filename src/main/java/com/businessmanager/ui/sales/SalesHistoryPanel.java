package com.businessmanager.ui.sales;

import com.businessmanager.dao.SaleDAO;
import com.businessmanager.model.Sale;
import com.businessmanager.model.SaleItem;
import com.businessmanager.security.UserSession;
import com.businessmanager.theme.Colors;
import com.businessmanager.theme.Fonts;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

public class SalesHistoryPanel extends JPanel {

    private final SaleDAO saleDAO = new SaleDAO();
    private DefaultTableModel tableModel;
    private JTable table;
    private JTextField searchField;
    private List<Sale> salesList = new ArrayList<>();

    public SalesHistoryPanel() {
        setLayout(new BorderLayout());
        setBackground(Colors.BACKGROUND);
        setBorder(new EmptyBorder(24, 28, 24, 28));

        buildUI();
        refreshData();
    }

    private void buildUI() {
        JPanel content = new JPanel(new BorderLayout(0, 20));
        content.setOpaque(false);

        // Header
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);

        JPanel headerText = new JPanel();
        headerText.setLayout(new BoxLayout(headerText, BoxLayout.Y_AXIS));
        headerText.setOpaque(false);

        JLabel title = new JLabel("Sales & Transaction History");
        title.setFont(Fonts.DISPLAY_LG);
        title.setForeground(Colors.ON_SURFACE);

        JLabel sub = new JLabel("Complete log of completed POS invoices, customer sales, and payment audit trails");
        sub.setFont(Fonts.BODY);
        sub.setForeground(Colors.ON_SURFACE_VARIANT);

        headerText.add(title);
        headerText.add(Box.createVerticalStrut(4));
        headerText.add(sub);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        actions.setOpaque(false);

        searchField = new JTextField(15);
        searchField.setPreferredSize(new Dimension(220, 36));
        searchField.putClientProperty("JTextField.placeholderText", "Search Invoice # or Customer...");
        searchField.addActionListener(e -> filterSales());

        JButton refreshBtn = new JButton("🔄  Refresh");
        refreshBtn.setFont(Fonts.BODY_SMALL);
        refreshBtn.addActionListener(e -> refreshData());

        actions.add(searchField);
        actions.add(refreshBtn);

        topPanel.add(headerText, BorderLayout.WEST);
        topPanel.add(actions, BorderLayout.EAST);

        content.add(topPanel, BorderLayout.NORTH);

        // Table Card
        JPanel tableCard = new JPanel(new BorderLayout());
        tableCard.setBackground(Colors.SURFACE_CONTAINER);
        tableCard.setBorder(new EmptyBorder(16, 16, 16, 16));

        String[] cols = {"Invoice #", "Customer", "Branch / Location", "Subtotal", "Discount", "Tax", "Grand Total", "Payment Method", "Status", "Sale Date"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        table = new JTable(tableModel);
        table.setBackground(Colors.SURFACE_CONTAINER);
        table.setForeground(Colors.ON_SURFACE);
        table.setGridColor(Colors.OUTLINE_VARIANT);
        table.setRowHeight(40);
        table.getTableHeader().setBackground(Colors.SURFACE_CONTAINER_HIGH);
        table.getTableHeader().setForeground(Colors.ON_SURFACE_VARIANT);
        table.getTableHeader().setFont(Fonts.LABEL_CAPS);
        table.setFont(Fonts.BODY);

        tableCard.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel tableFooter = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 12));
        tableFooter.setOpaque(false);

        JButton viewDetailsBtn = new JButton("View Invoice & Items Detail");
        viewDetailsBtn.setFont(Fonts.BODY_SMALL);
        viewDetailsBtn.setForeground(Colors.ON_PRIMARY);
        viewDetailsBtn.setBackground(Colors.PRIMARY);
        viewDetailsBtn.addActionListener(e -> viewDetailsSelected());

        tableFooter.add(viewDetailsBtn);
        tableCard.add(tableFooter, BorderLayout.SOUTH);

        content.add(tableCard, BorderLayout.CENTER);
        add(content, BorderLayout.CENTER);
    }

    public void refreshData() {
        int bId = UserSession.getBusinessId();
        if (!UserSession.isLoggedIn()) return;

        try {
            salesList = saleDAO.findByBusiness(bId);
            filterSales();
        } catch (SQLException e) {
            System.err.println("Sales history refresh error: " + e.getMessage());
        }
    }

    private void filterSales() {
        String query = searchField.getText().trim().toLowerCase();
        NumberFormat fmt = NumberFormat.getCurrencyInstance(new java.util.Locale("en", "IN"));
        tableModel.setRowCount(0);

        for (Sale s : salesList) {
            boolean matches = query.isEmpty() ||
                    s.getInvoiceNumber().toLowerCase().contains(query) ||
                    (s.getCustomerName() != null && s.getCustomerName().toLowerCase().contains(query));

            if (matches) {
                tableModel.addRow(new Object[]{
                        s.getInvoiceNumber(),
                        s.getCustomerName() != null ? s.getCustomerName() : "Walk-in Customer",
                        s.getBranchName() != null ? s.getBranchName() : "Main HQ",
                        fmt.format(s.getSubtotal()),
                        fmt.format(s.getDiscount()),
                        fmt.format(s.getTax()),
                        fmt.format(s.getTotal()),
                        s.getPaymentMethod().getDisplayName(),
                        s.getPaymentStatus().getDisplayName(),
                        s.getSaleDate() != null ? s.getSaleDate().toString().substring(0, 16) : ""
                });
            }
        }
    }

    private void viewDetailsSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a sale transaction to view details.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String invNum = (String) tableModel.getValueAt(row, 0);
        Sale sale = salesList.stream().filter(x -> x.getInvoiceNumber().equals(invNum)).findFirst().orElse(null);

        if (sale != null) {
            try {
                Sale fullSale = saleDAO.findById(sale.getId());
                showInvoiceDialog(fullSale != null ? fullSale : sale);
            } catch (SQLException e) {
                showInvoiceDialog(sale);
            }
        }
    }

    private void showInvoiceDialog(Sale s) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Invoice Details - " + s.getInvoiceNumber(), true);
        dialog.setSize(550, 600);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Colors.SURFACE_CONTAINER);
        panel.setBorder(new EmptyBorder(20, 24, 20, 24));

        JLabel title = new JLabel("INVOICE #" + s.getInvoiceNumber());
        title.setFont(Fonts.HEADLINE_MD);
        title.setForeground(Colors.PRIMARY);
        panel.add(title);
        panel.add(Box.createVerticalStrut(12));

        NumberFormat fmt = NumberFormat.getCurrencyInstance(new java.util.Locale("en", "IN"));
        panel.add(createDetailLabel("Customer: " + (s.getCustomerName() != null ? s.getCustomerName() : "Walk-in Customer")));
        panel.add(createDetailLabel("Operator / Cashier: " + (s.getUserName() != null ? s.getUserName() : "Admin")));
        panel.add(createDetailLabel("Payment Method: " + s.getPaymentMethod().getDisplayName()));
        panel.add(createDetailLabel("Date & Time: " + (s.getSaleDate() != null ? s.getSaleDate().toString() : "")));
        panel.add(Box.createVerticalStrut(16));

        String[] itemCols = {"Product", "Unit Price", "Qty", "Line Total"};
        DefaultTableModel itemModel = new DefaultTableModel(itemCols, 0);

        if (s.getItems() != null) {
            for (SaleItem item : s.getItems()) {
                itemModel.addRow(new Object[]{
                        item.getProductName(),
                        fmt.format(item.getUnitPrice()),
                        item.getQuantity(),
                        fmt.format(item.getLineTotal())
                });
            }
        }

        JTable itemTable = new JTable(itemModel);
        itemTable.setBackground(Colors.SURFACE_CONTAINER_LOW);
        itemTable.setForeground(Colors.ON_SURFACE);
        itemTable.setRowHeight(32);
        panel.add(new JScrollPane(itemTable));

        panel.add(Box.createVerticalStrut(16));
        panel.add(createDetailLabel("Subtotal: " + fmt.format(s.getSubtotal())));
        panel.add(createDetailLabel("Discount: " + fmt.format(s.getDiscount())));
        panel.add(createDetailLabel("Tax: " + fmt.format(s.getTax())));
        panel.add(createDetailLabel("Grand Total: " + fmt.format(s.getTotal())));
        panel.add(createDetailLabel("Amount Paid: " + fmt.format(s.getPaidAmount())));

        JButton closeBtn = new JButton("Close Receipt");
        closeBtn.addActionListener(e -> dialog.dispose());

        dialog.add(panel, BorderLayout.CENTER);
        dialog.add(closeBtn, BorderLayout.SOUTH);
        com.businessmanager.theme.UiStyles.apply(dialog);
        dialog.setVisible(true);
    }

    private JLabel createDetailLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Fonts.BODY);
        l.setForeground(Colors.ON_SURFACE);
        return l;
    }
}
