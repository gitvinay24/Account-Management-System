package com.businessmanager.ui.purchases;

import com.businessmanager.dao.ProductDAO;
import com.businessmanager.dao.PurchaseDAO;
import com.businessmanager.dao.SupplierDAO;
import com.businessmanager.model.Product;
import com.businessmanager.model.Purchase;
import com.businessmanager.model.PurchaseItem;
import com.businessmanager.model.Supplier;
import com.businessmanager.security.UserSession;
import com.businessmanager.theme.Colors;
import com.businessmanager.theme.Fonts;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

public class PurchaseManagementPanel extends JPanel {

    private final PurchaseDAO purchaseDAO = new PurchaseDAO();
    private final SupplierDAO supplierDAO = new SupplierDAO();
    private final ProductDAO productDAO = new ProductDAO();

    private DefaultTableModel tableModel;
    private JTable table;
    private JTextField searchField;
    private List<Purchase> purchaseList = new ArrayList<>();

    public PurchaseManagementPanel() {
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

        JLabel title = new JLabel("Purchases & Inventory Replenishment");
        title.setFont(Fonts.DISPLAY_LG);
        title.setForeground(Colors.ON_SURFACE);

        JLabel sub = new JLabel("Log wholesale supplier purchase orders to automatically increase product stock inventory");
        sub.setFont(Fonts.BODY);
        sub.setForeground(Colors.ON_SURFACE_VARIANT);

        headerText.add(title);
        headerText.add(Box.createVerticalStrut(4));
        headerText.add(sub);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        actions.setOpaque(false);

        searchField = new JTextField(15);
        searchField.setPreferredSize(new Dimension(200, 36));
        searchField.putClientProperty("JTextField.placeholderText", "Search PO # or Supplier...");
        searchField.addActionListener(e -> filterPurchases());

        JButton addBtn = new JButton("+  Create Purchase Order");
        addBtn.setFont(Fonts.TITLE_SM);
        addBtn.setForeground(Colors.ON_PRIMARY);
        addBtn.setBackground(Colors.PRIMARY);
        addBtn.setPreferredSize(new Dimension(200, 36));
        addBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addBtn.putClientProperty("JButton.buttonType", "roundRect");
        addBtn.putClientProperty("Component.arc", 8);
        addBtn.addActionListener(e -> showCreatePurchaseDialog());

        actions.add(searchField);
        actions.add(addBtn);

        topPanel.add(headerText, BorderLayout.WEST);
        topPanel.add(actions, BorderLayout.EAST);

        content.add(topPanel, BorderLayout.NORTH);

        // Table Card
        JPanel tableCard = new JPanel(new BorderLayout());
        tableCard.setBackground(Colors.SURFACE_CONTAINER);
        tableCard.setBorder(new EmptyBorder(16, 16, 16, 16));

        String[] cols = {"PO Number", "Supplier", "Branch", "Subtotal", "Total Cost", "Paid Amount", "Payment Method", "Status", "Purchase Date"};
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
        content.add(tableCard, BorderLayout.CENTER);
        add(content, BorderLayout.CENTER);
    }

    public void refreshData() {
        int bId = UserSession.getBusinessId();
        if (!UserSession.isLoggedIn()) return;

        try {
            purchaseList = purchaseDAO.findByBusiness(bId);
            filterPurchases();
        } catch (SQLException e) {
            System.err.println("Purchase orders refresh error: " + e.getMessage());
        }
    }

    private void filterPurchases() {
        String query = searchField.getText().trim().toLowerCase();
        NumberFormat fmt = NumberFormat.getCurrencyInstance(new java.util.Locale("en", "IN"));
        tableModel.setRowCount(0);

        for (Purchase p : purchaseList) {
            boolean matches = query.isEmpty() ||
                    p.getPurchaseNumber().toLowerCase().contains(query) ||
                    (p.getSupplierName() != null && p.getSupplierName().toLowerCase().contains(query));

            if (matches) {
                tableModel.addRow(new Object[]{
                        p.getPurchaseNumber(),
                        p.getSupplierName() != null ? p.getSupplierName() : "Supplier",
                        p.getBranchName() != null ? p.getBranchName() : "Main HQ",
                        fmt.format(p.getSubtotal()),
                        fmt.format(p.getTotal()),
                        fmt.format(p.getPaidAmount()),
                        p.getPaymentMethod().getDisplayName(),
                        p.getPaymentStatus().getDisplayName(),
                        p.getPurchaseDate() != null ? p.getPurchaseDate().toString().substring(0, 16) : ""
                });
            }
        }
    }

    private void showCreatePurchaseDialog() {
        int bId = UserSession.getBusinessId();
        List<Supplier> suppliers;
        List<Product> products;

        try {
            suppliers = supplierDAO.findByBusiness(bId);
            products = productDAO.findByBusiness(bId);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading suppliers/products: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (suppliers.isEmpty() || products.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please ensure at least one supplier and one product exist before creating a purchase order.", "Missing Dependency", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Create Purchase Order", true);
        dialog.setSize(550, 520);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Colors.SURFACE_CONTAINER);
        panel.setBorder(new EmptyBorder(20, 24, 20, 24));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JComboBox<Supplier> supCombo = new JComboBox<>(suppliers.toArray(new Supplier[0]));
        JComboBox<Product> prodCombo = new JComboBox<>(products.toArray(new Product[0]));
        JTextField unitPriceField = new JTextField("0.00", 10);
        JTextField qtyField = new JTextField("1", 10);

        prodCombo.addActionListener(e -> {
            Product sel = (Product) prodCombo.getSelectedItem();
            if (sel != null) {
                unitPriceField.setText(sel.getPurchasePrice().toString());
            }
        });
        if (!products.isEmpty()) unitPriceField.setText(products.get(0).getPurchasePrice().toString());

        int r = 0;
        addFormRow(panel, gbc, r++, "Select Supplier *:", supCombo);
        addFormRow(panel, gbc, r++, "Select Product to Restock *:", prodCombo);
        addFormRow(panel, gbc, r++, "Unit Purchase Price (₹) *:", unitPriceField);
        addFormRow(panel, gbc, r++, "Quantity to Add *:", qtyField);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        btnPanel.setBackground(Colors.SURFACE_CONTAINER);

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dialog.dispose());

        JButton saveBtn = new JButton("Complete Purchase & Add Stock");
        saveBtn.setFont(Fonts.BODY_SMALL);
        saveBtn.setForeground(Colors.ON_PRIMARY);
        saveBtn.setBackground(Colors.PRIMARY);
        saveBtn.addActionListener(e -> {
            try {
                Supplier selSup = (Supplier) supCombo.getSelectedItem();
                Product selProd = (Product) prodCombo.getSelectedItem();
                BigDecimal price = new BigDecimal(unitPriceField.getText().trim());
                int qty = Integer.parseInt(qtyField.getText().trim());

                if (selSup == null || selProd == null || qty <= 0) {
                    JOptionPane.showMessageDialog(dialog, "Please enter valid quantity and select supplier/product.", "Input Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                BigDecimal lineTotal = price.multiply(BigDecimal.valueOf(qty));

                Purchase p = new Purchase();
                p.setBusinessId(bId);
                p.setBranchId(UserSession.getBranchId());
                p.setSupplierId(selSup.getId());
                p.setUserId(UserSession.getUserId());
                p.setPurchaseNumber("PO-" + System.currentTimeMillis() % 1000000);
                p.setSubtotal(lineTotal);
                p.setTotal(lineTotal);
                p.setPaidAmount(lineTotal);
                p.setNotes("Inventory Stock Restock PO");

                PurchaseItem pi = new PurchaseItem();
                pi.setProductId(selProd.getId());
                pi.setUnitPrice(price);
                pi.setQuantity(qty);
                pi.setLineTotal(lineTotal);
                p.getItems().add(pi);

                boolean success = purchaseDAO.createPurchase(p);
                if (success) {
                    JOptionPane.showMessageDialog(dialog, "Purchase order recorded successfully! Product stock increased by " + qty, "Purchase Completed", JOptionPane.INFORMATION_MESSAGE);
                    dialog.dispose();
                    refreshData();
                }

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Purchase Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnPanel.add(cancelBtn);
        btnPanel.add(saveBtn);

        dialog.add(panel, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        com.businessmanager.theme.UiStyles.apply(dialog);
        dialog.setVisible(true);
    }

    private void addFormRow(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent comp) {
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.3;
        JLabel lbl = new JLabel(label);
        lbl.setFont(Fonts.BODY);
        lbl.setForeground(Colors.ON_SURFACE);
        panel.add(lbl, gbc);

        gbc.gridx = 1; gbc.weightx = 0.7;
        panel.add(comp, gbc);
    }
}
