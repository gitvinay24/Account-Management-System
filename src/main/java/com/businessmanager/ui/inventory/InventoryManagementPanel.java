package com.businessmanager.ui.inventory;

import com.businessmanager.components.StitchIcon;
import com.businessmanager.dao.ProductDAO;
import com.businessmanager.database.DBConnection;
import com.businessmanager.enums.TransactionType;
import com.businessmanager.model.Product;
import com.businessmanager.security.UserSession;
import com.businessmanager.theme.Colors;
import com.businessmanager.theme.Fonts;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileWriter;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class InventoryManagementPanel extends JPanel {

    private final ProductDAO productDAO = new ProductDAO();
    private DefaultTableModel tableModel;
    private JTable table;
    private JTextField searchField;
    private JComboBox<String> locationFilterCombo;

    private JLabel totalSkusLbl;
    private JLabel totalValLbl;
    private JLabel lowStockLbl;
    private JLabel outOfStockLbl;

    private List<Product> masterProductList = new ArrayList<>();
    private List<Product> displayedProductList = new ArrayList<>();

    public InventoryManagementPanel() {
        setLayout(new BorderLayout());
        setBackground(Colors.BACKGROUND);
        setBorder(new EmptyBorder(20, 24, 20, 24));

        buildUI();
        refreshData();
    }

    private void buildUI() {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);

        // 1. Header
        content.add(createHeader());
        content.add(Box.createVerticalStrut(20));

        // 2. Summary Metric Cards
        content.add(createSummaryCards());
        content.add(Box.createVerticalStrut(20));

        // 3. Main Card with Search Bar & Table
        JPanel mainCard = new JPanel(new BorderLayout(0, 16));
        mainCard.setBackground(Colors.SURFACE_CONTAINER);
        mainCard.setBorder(new EmptyBorder(16, 20, 16, 20));

        mainCard.add(createFilterBar(), BorderLayout.NORTH);
        mainCard.add(createTableSection(), BorderLayout.CENTER);

        content.add(mainCard);

        JScrollPane scroll = new JScrollPane(content);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);

        add(scroll, BorderLayout.CENTER);
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);

        JLabel title = new JLabel("Inventory Management");
        title.setFont(Fonts.DISPLAY_LG);
        title.setForeground(Colors.ON_SURFACE);

        JLabel sub = new JLabel("Monitor stock levels, warehouse transfers, and inventory adjustments across branches.");
        sub.setFont(Fonts.BODY);
        sub.setForeground(Colors.ON_SURFACE_VARIANT);

        left.add(title);
        left.add(Box.createVerticalStrut(4));
        left.add(sub);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);

        JButton exportBtn = new JButton("Export Audit Log");
        exportBtn.setFont(Fonts.BODY_SMALL);
        exportBtn.setForeground(Colors.ON_SURFACE);
        exportBtn.setBackground(Colors.SURFACE_CONTAINER_HIGH);
        exportBtn.setFocusPainted(false);
        exportBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        exportBtn.putClientProperty("JButton.buttonType", "roundRect");
        exportBtn.addActionListener(e -> exportAuditLog());

        JButton adjustBtn = new JButton("+ Stock Adjustment");
        adjustBtn.setFont(Fonts.TITLE_SM);
        adjustBtn.setForeground(Colors.ON_PRIMARY);
        adjustBtn.setBackground(Colors.PRIMARY);
        adjustBtn.setFocusPainted(false);
        adjustBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        adjustBtn.putClientProperty("JButton.buttonType", "roundRect");
        adjustBtn.addActionListener(e -> showStockAdjustmentDialog(null));

        actions.add(exportBtn);
        actions.add(adjustBtn);

        header.add(left, BorderLayout.WEST);
        header.add(actions, BorderLayout.EAST);
        return header;
    }

    private JPanel createSummaryCards() {
        JPanel grid = new JPanel(new GridLayout(1, 4, 14, 0));
        grid.setOpaque(false);
        grid.setMaximumSize(new Dimension(2000, 110));

        totalSkusLbl = new JLabel("0");
        totalValLbl = new JLabel("₹0.00");
        lowStockLbl = new JLabel("0");
        outOfStockLbl = new JLabel("0");

        grid.add(createCard("TOTAL SKUS", totalSkusLbl, "Active items", StitchIcon.Type.CATEGORY, Colors.PRIMARY));
        grid.add(createCard("INVENTORY VALUE", totalValLbl, "Total cost value", StitchIcon.Type.PAYMENTS, Colors.SECONDARY));
        grid.add(createCard("LOW STOCK ALERT", lowStockLbl, "Items below reorder level", StitchIcon.Type.STORE, Colors.TERTIARY));
        grid.add(createCard("OUT OF STOCK", outOfStockLbl, "Items zero quantity", StitchIcon.Type.DESCRIPTION, Colors.ERROR));

        return grid;
    }

    private JPanel createCard(String title, JLabel valLbl, String badgeText, StitchIcon.Type iconType, Color badgeColor) {
        JPanel card = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Colors.SURFACE_CONTAINER);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(14, 16, 14, 16));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(Fonts.LABEL_CAPS);
        titleLbl.setForeground(Colors.ON_SURFACE_VARIANT);

        JLabel iconLbl = new JLabel(new StitchIcon(iconType));

        top.add(titleLbl, BorderLayout.WEST);
        top.add(iconLbl, BorderLayout.EAST);

        valLbl.setFont(Fonts.HEADLINE_MD);
        valLbl.setForeground(Colors.ON_SURFACE);

        JLabel badgeLbl = new JLabel(badgeText);
        badgeLbl.setFont(Fonts.BODY_SMALL);
        badgeLbl.setForeground(badgeColor);

        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.add(valLbl, BorderLayout.CENTER);
        center.add(badgeLbl, BorderLayout.SOUTH);

        card.add(top, BorderLayout.NORTH);
        card.add(center, BorderLayout.CENTER);

        return card;
    }

    private JPanel createFilterBar() {
        JPanel bar = new JPanel(new BorderLayout(12, 0));
        bar.setOpaque(false);

        searchField = new JTextField();
        searchField.setPreferredSize(new Dimension(380, 38));
        searchField.putClientProperty("JTextField.placeholderText", "Search stock by name, SKU, or warehouse location...");
        searchField.setFont(Fonts.BODY);
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { applyFilters(); }
            public void removeUpdate(DocumentEvent e) { applyFilters(); }
            public void changedUpdate(DocumentEvent e) { applyFilters(); }
        });

        locationFilterCombo = new JComboBox<>(new String[]{"All Warehouses / Branches", "Main Store"});
        locationFilterCombo.setPreferredSize(new Dimension(200, 38));
        locationFilterCombo.addActionListener(e -> applyFilters());

        bar.add(searchField, BorderLayout.CENTER);
        bar.add(locationFilterCombo, BorderLayout.EAST);
        return bar;
    }

    private JComponent createTableSection() {
        String[] cols = {"ITEM NAME", "SKU", "LOCATION / BRANCH", "AVAILABLE STOCK", "REORDER LEVEL", "UNIT COST", "STATUS", "ACTION"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        table = new JTable(tableModel);
        table.setBackground(Colors.SURFACE_CONTAINER);
        table.setForeground(Colors.ON_SURFACE);
        table.setRowHeight(44);
        table.getTableHeader().setBackground(Colors.SURFACE_CONTAINER_HIGH);
        table.getTableHeader().setForeground(Colors.ON_SURFACE_VARIANT);
        table.getTableHeader().setFont(Fonts.LABEL_CAPS);
        table.setShowGrid(false);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = table.getSelectedRow();
                if (row >= 0 && row < displayedProductList.size() && table.getSelectedColumn() == 7) {
                    Product p = displayedProductList.get(row);
                    showStockAdjustmentDialog(p);
                }
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);

        return scroll;
    }

    private void showStockAdjustmentDialog(Product preselectedProduct) {
        int bId = UserSession.getBusinessId();

        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this) instanceof Frame f ? f : null,
                "Stock Adjustment", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(440, 460);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(20, 24, 20, 24));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 6, 6, 6);

        JComboBox<Product> productCombo = new JComboBox<>();
        try {
            List<Product> products = productDAO.findByBusiness(bId);
            for (Product p : products) {
                productCombo.addItem(p);
                if (preselectedProduct != null && p.getId() == preselectedProduct.getId()) {
                    productCombo.setSelectedItem(p);
                }
            }
        } catch (SQLException ignored) {}

        JComboBox<String> actionCombo = new JComboBox<>(new String[]{"Add Stock (+)", "Remove Stock (-)", "Set Exact Stock (=)"});
        JSpinner qtySpinner = new JSpinner(new SpinnerNumberModel(5, 1, 1000000, 1));
        JTextField reasonField = new JTextField("Physical Stock Audit / Recount");

        int row = 0;
        addFormRow(form, gbc, row++, "Select Product *:", productCombo);
        addFormRow(form, gbc, row++, "Adjustment Mode *:", actionCombo);
        addFormRow(form, gbc, row++, "Quantity Amount *:", qtySpinner);
        addFormRow(form, gbc, row++, "Reason / Notes:", reasonField);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dialog.dispose());

        JButton applyBtn = new JButton("Apply Adjustment");
        applyBtn.setFont(Fonts.TITLE_SM);
        applyBtn.setForeground(Colors.ON_PRIMARY);
        applyBtn.setBackground(Colors.PRIMARY);
        applyBtn.addActionListener(e -> {
            Product selected = (Product) productCombo.getSelectedItem();
            if (selected == null) {
                JOptionPane.showMessageDialog(dialog, "Please select a product.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int amount = (Integer) qtySpinner.getValue();
            int currentStock = selected.getStockQuantity();
            int newStock;
            String mode = (String) actionCombo.getSelectedItem();

            if ("Remove Stock (-)".equals(mode)) {
                newStock = currentStock - amount;
                if (newStock < 0) {
                    JOptionPane.showMessageDialog(dialog, "Stock cannot be negative.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            } else if ("Set Exact Stock (=)".equals(mode)) {
                newStock = amount;
            } else {
                newStock = currentStock + amount;
            }

            String reason = reasonField.getText().trim();

            try (Connection conn = DBConnection.getConnection()) {
                conn.setAutoCommit(false);
                try {
                    selected.setStockQuantity(newStock);
                    if (!productDAO.updateStock(conn, selected.getId(), newStock)) {
                        throw new SQLException("Failed to update product stock.");
                    }

                    String logSql = "INSERT INTO inventory_transactions (business_id, branch_id, product_id, user_id, transaction_type, quantity, reference_type, reference_id, notes) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement pstmt = conn.prepareStatement(logSql)) {
                        pstmt.setInt(1, bId);
                        if (UserSession.getBranchId() != null && UserSession.getBranchId() > 0) {
                            pstmt.setInt(2, UserSession.getBranchId());
                        } else {
                            pstmt.setNull(2, java.sql.Types.INTEGER);
                        }
                        pstmt.setInt(3, selected.getId());
                        pstmt.setInt(4, UserSession.getUserId());
                        pstmt.setString(5, amount >= 0 ? TransactionType.ADJUSTMENT_ADD.name() : TransactionType.ADJUSTMENT_REMOVE.name());
                        pstmt.setInt(6, amount);
                        pstmt.setString(7, "ADJUSTMENT");
                        pstmt.setInt(8, selected.getId());
                        pstmt.setString(9, reason);
                        pstmt.executeUpdate();
                    }

                    conn.commit();
                    JOptionPane.showMessageDialog(dialog, "Stock adjusted successfully! New stock: " + newStock, "Adjustment Applied", JOptionPane.INFORMATION_MESSAGE);
                    dialog.dispose();
                    refreshData();
                } catch (Exception ex) {
                    conn.rollback();
                    throw ex;
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(dialog, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnPanel.add(cancelBtn);
        btnPanel.add(applyBtn);

        dialog.add(new JScrollPane(form), BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        com.businessmanager.theme.UiStyles.apply(dialog);
        dialog.setVisible(true);
    }

    private void addFormRow(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent comp) {
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.3;
        JLabel l = new JLabel(label);
        l.setFont(Fonts.BODY_BOLD);
        panel.add(l, gbc);

        gbc.gridx = 1; gbc.weightx = 0.7;
        panel.add(comp, gbc);
    }

    private void applyFilters() {
        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();

        displayedProductList = masterProductList.stream().filter(p -> query.isEmpty()
                || p.getName().toLowerCase().contains(query)
                || (p.getSku() != null && p.getSku().toLowerCase().contains(query))
                || (p.getCategoryName() != null && p.getCategoryName().toLowerCase().contains(query))
        ).toList();

        tableModel.setRowCount(0);
        for (Product p : displayedProductList) {
            String status = p.getStockQuantity() <= 0 ? "OUT OF STOCK" : (p.getStockQuantity() <= p.getMinStock() ? "LOW STOCK" : "IN STOCK");
            tableModel.addRow(new Object[]{
                    p.getName(),
                    p.getSku() != null ? p.getSku() : "SKU-" + p.getId(),
                    "Main Store",
                    p.getStockQuantity() + " Units",
                    p.getMinStock() + " Units",
                    "₹" + (p.getPurchasePrice() != null ? p.getPurchasePrice().setScale(2) : "0.00"),
                    status,
                    "Adjust ⚙"
            });
        }
    }

    private void exportAuditLog() {
        if (displayedProductList.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No inventory items to export.", "Export Audit Log", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("inventory_audit_log.csv"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try (FileWriter writer = new FileWriter(file)) {
                writer.write("ID,Product Name,SKU,Available Stock,Min Stock Level,Unit Cost,Total Value,Status\n");
                for (Product p : displayedProductList) {
                    BigDecimal cost = p.getPurchasePrice() != null ? p.getPurchasePrice() : BigDecimal.ZERO;
                    BigDecimal totalVal = cost.multiply(BigDecimal.valueOf(p.getStockQuantity()));
                    String status = p.getStockQuantity() <= 0 ? "OUT OF STOCK" : (p.getStockQuantity() <= p.getMinStock() ? "LOW STOCK" : "IN STOCK");
                    writer.write(String.format("%d,\"%s\",\"%s\",%d,%d,%.2f,%.2f,%s\n",
                            p.getId(),
                            p.getName().replace("\"", "\"\""),
                            p.getSku() != null ? p.getSku() : "",
                            p.getStockQuantity(),
                            p.getMinStock(),
                            cost,
                            totalVal,
                            status
                    ));
                }
                JOptionPane.showMessageDialog(this, "Exported inventory audit log to " + file.getAbsolutePath(), "Audit Log Exported", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to export audit log: " + ex.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void refreshData() {
        int bId = UserSession.getBusinessId();
        try {
            masterProductList = productDAO.findByBusiness(bId);
            int lowCount = 0;
            int outCount = 0;
            BigDecimal totalVal = BigDecimal.ZERO;

            for (Product p : masterProductList) {
                if (p.getStockQuantity() <= 0) outCount++;
                else if (p.getStockQuantity() <= p.getMinStock()) lowCount++;

                if (p.getPurchasePrice() != null) {
                    totalVal = totalVal.add(p.getPurchasePrice().multiply(BigDecimal.valueOf(p.getStockQuantity())));
                }
            }

            totalSkusLbl.setText(String.valueOf(masterProductList.size()));
            totalValLbl.setText("₹" + totalVal.setScale(2));
            lowStockLbl.setText(String.valueOf(lowCount));
            outOfStockLbl.setText(String.valueOf(outCount));

            applyFilters();
        } catch (Exception e) {
            System.err.println("Inventory refresh error: " + e.getMessage());
        }
    }
}
