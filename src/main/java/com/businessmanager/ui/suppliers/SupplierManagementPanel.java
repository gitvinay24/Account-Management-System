package com.businessmanager.ui.suppliers;

import com.businessmanager.components.StitchIcon;
import com.businessmanager.dao.SupplierDAO;
import com.businessmanager.model.Supplier;
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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SupplierManagementPanel extends JPanel {

    private final SupplierDAO supplierDAO = new SupplierDAO();
    private DefaultTableModel tableModel;
    private JTable table;
    private JTextField searchField;
    private JComboBox<String> statusFilterCombo;

    private JLabel activeSuppliersLbl;
    private JLabel totalPurchasesLbl;
    private JLabel payablesLbl;

    private List<Supplier> masterSupplierList = new ArrayList<>();
    private List<Supplier> displayedSupplierList = new ArrayList<>();

    public SupplierManagementPanel() {
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

        JLabel title = new JLabel("Supplier Directory");
        title.setFont(Fonts.DISPLAY_LG);
        title.setForeground(Colors.ON_SURFACE);

        JLabel sub = new JLabel("Manage vendor accounts, purchase orders, and payment terms.");
        sub.setFont(Fonts.BODY);
        sub.setForeground(Colors.ON_SURFACE_VARIANT);

        left.add(title);
        left.add(Box.createVerticalStrut(4));
        left.add(sub);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);

        JButton exportBtn = new JButton("Export Directory");
        exportBtn.setFont(Fonts.BODY_SMALL);
        exportBtn.setForeground(Colors.ON_SURFACE);
        exportBtn.setBackground(Colors.SURFACE_CONTAINER_HIGH);
        exportBtn.setFocusPainted(false);
        exportBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        exportBtn.putClientProperty("JButton.buttonType", "roundRect");
        exportBtn.addActionListener(e -> exportCsv());

        JButton addBtn = new JButton("+  Add Supplier");
        addBtn.setFont(Fonts.TITLE_SM);
        addBtn.setForeground(Colors.ON_PRIMARY);
        addBtn.setBackground(Colors.PRIMARY);
        addBtn.setFocusPainted(false);
        addBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addBtn.putClientProperty("JButton.buttonType", "roundRect");
        addBtn.addActionListener(e -> showSupplierDialog(null));

        actions.add(exportBtn);
        actions.add(addBtn);

        header.add(left, BorderLayout.WEST);
        header.add(actions, BorderLayout.EAST);
        return header;
    }

    private JPanel createSummaryCards() {
        JPanel grid = new JPanel(new GridLayout(1, 3, 16, 0));
        grid.setOpaque(false);
        grid.setMaximumSize(new Dimension(2000, 110));

        activeSuppliersLbl = new JLabel("0");
        totalPurchasesLbl = new JLabel("₹0.00");
        payablesLbl = new JLabel("₹0.00");

        grid.add(createCard("ACTIVE SUPPLIERS", activeSuppliersLbl, "Verified vendors", StitchIcon.Type.DOMAIN, Colors.PRIMARY));
        grid.add(createCard("TOTAL PURCHASES", totalPurchasesLbl, "Purchases total", StitchIcon.Type.PAYMENTS, Colors.SECONDARY));
        grid.add(createCard("ACCOUNTS PAYABLE", payablesLbl, "Outstanding vendor balances", StitchIcon.Type.DESCRIPTION, Colors.TERTIARY));

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
        searchField.putClientProperty("JTextField.placeholderText", "Search suppliers by company name, contact, or GSTIN...");
        searchField.setFont(Fonts.BODY);
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { applyFilters(); }
            public void removeUpdate(DocumentEvent e) { applyFilters(); }
            public void changedUpdate(DocumentEvent e) { applyFilters(); }
        });

        statusFilterCombo = new JComboBox<>(new String[]{"All Vendors", "Active", "Payable Due"});
        statusFilterCombo.setPreferredSize(new Dimension(160, 38));
        statusFilterCombo.addActionListener(e -> applyFilters());

        bar.add(searchField, BorderLayout.CENTER);
        bar.add(statusFilterCombo, BorderLayout.EAST);
        return bar;
    }

    private JComponent createTableSection() {
        String[] cols = {"SUPPLIER / COMPANY", "CONTACT PERSON", "PHONE & EMAIL", "GSTIN", "PAYABLE BAL.", "STATUS", "ACTIONS"};
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
                if (row >= 0 && row < displayedSupplierList.size()) {
                    Supplier s = displayedSupplierList.get(row);
                    if (e.getClickCount() == 2) {
                        showSupplierDialog(s);
                    } else if (table.getSelectedColumn() == 6) {
                        showActionMenu(e.getComponent(), e.getX(), e.getY(), s);
                    }
                }
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);

        return scroll;
    }

    private void showActionMenu(Component comp, int x, int y, Supplier s) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem editItem = new JMenuItem("Edit Supplier");
        editItem.addActionListener(e -> showSupplierDialog(s));

        JMenuItem deleteItem = new JMenuItem("Delete Supplier");
        deleteItem.addActionListener(e -> deleteSupplier(s));

        menu.add(editItem);
        menu.add(deleteItem);
        menu.show(comp, x, y);
    }

    private void deleteSupplier(Supplier s) {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete supplier '" + s.getName() + "'?",
                "Confirm Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                if (supplierDAO.delete(s.getId())) {
                    JOptionPane.showMessageDialog(this, "Supplier deleted successfully.", "Supplier Deleted", JOptionPane.INFORMATION_MESSAGE);
                    refreshData();
                } else {
                    JOptionPane.showMessageDialog(this, "Could not delete supplier.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showSupplierDialog(Supplier existingSupplier) {
        int bId = UserSession.getBusinessId();
        boolean isEdit = existingSupplier != null;

        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this) instanceof Frame f ? f : null,
                isEdit ? "Edit Supplier" : "Add Supplier", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(440, 540);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(20, 24, 20, 24));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 6, 6, 6);

        JTextField nameField = new JTextField(isEdit ? existingSupplier.getName() : "");
        JTextField contactField = new JTextField(isEdit && existingSupplier.getContactPerson() != null ? existingSupplier.getContactPerson() : "");
        JTextField emailField = new JTextField(isEdit && existingSupplier.getEmail() != null ? existingSupplier.getEmail() : "");
        JTextField phoneField = new JTextField(isEdit && existingSupplier.getPhone() != null ? existingSupplier.getPhone() : "");
        JTextField gstinField = new JTextField(isEdit && existingSupplier.getTaxId() != null ? existingSupplier.getTaxId() : "");
        JTextField addressField = new JTextField(isEdit && existingSupplier.getAddress() != null ? existingSupplier.getAddress() : "");
        JTextField cityField = new JTextField(isEdit && existingSupplier.getCity() != null ? existingSupplier.getCity() : "");
        JCheckBox activeBox = new JCheckBox("Active Supplier", !isEdit || existingSupplier.isActive());

        int row = 0;
        addFormRow(form, gbc, row++, "Company Name *:", nameField);
        addFormRow(form, gbc, row++, "Contact Person:", contactField);
        addFormRow(form, gbc, row++, "Phone *:", phoneField);
        addFormRow(form, gbc, row++, "Email:", emailField);
        addFormRow(form, gbc, row++, "GSTIN / Tax ID:", gstinField);
        addFormRow(form, gbc, row++, "Address:", addressField);
        addFormRow(form, gbc, row++, "City:", cityField);
        addFormRow(form, gbc, row++, "Status:", activeBox);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dialog.dispose());

        JButton saveBtn = new JButton(isEdit ? "Update Supplier" : "Save Supplier");
        saveBtn.setFont(Fonts.TITLE_SM);
        saveBtn.setForeground(Colors.ON_PRIMARY);
        saveBtn.setBackground(Colors.PRIMARY);
        saveBtn.addActionListener(e -> {
            String name = nameField.getText().trim();
            String phone = phoneField.getText().trim();
            if (name.isEmpty() || phone.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Company Name and Phone are required.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            Supplier s = isEdit ? existingSupplier : new Supplier();
            s.setBusinessId(bId);
            s.setName(name);
            s.setContactPerson(contactField.getText().trim());
            s.setEmail(emailField.getText().trim());
            s.setPhone(phone);
            s.setTaxId(gstinField.getText().trim());
            s.setAddress(addressField.getText().trim());
            s.setCity(cityField.getText().trim());
            s.setActive(activeBox.isSelected());

            try {
                if (isEdit) {
                    if (supplierDAO.update(s)) {
                        JOptionPane.showMessageDialog(dialog, "Supplier updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                        dialog.dispose();
                        refreshData();
                    } else {
                        JOptionPane.showMessageDialog(dialog, "Failed to update supplier.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    int id = supplierDAO.create(s);
                    if (id > 0) {
                        JOptionPane.showMessageDialog(dialog, "Supplier created successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                        dialog.dispose();
                        refreshData();
                    } else {
                        JOptionPane.showMessageDialog(dialog, "Failed to create supplier.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(dialog, "Database error: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnPanel.add(cancelBtn);
        btnPanel.add(saveBtn);

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
        String selectedStatus = (String) statusFilterCombo.getSelectedItem();

        displayedSupplierList = masterSupplierList.stream().filter(s -> {
            boolean matchesSearch = query.isEmpty()
                    || s.getName().toLowerCase().contains(query)
                    || (s.getContactPerson() != null && s.getContactPerson().toLowerCase().contains(query))
                    || (s.getPhone() != null && s.getPhone().toLowerCase().contains(query))
                    || (s.getTaxId() != null && s.getTaxId().toLowerCase().contains(query));

            boolean matchesStatus = true;
            if ("Active".equalsIgnoreCase(selectedStatus)) {
                matchesStatus = s.isActive();
            } else if ("Payable Due".equalsIgnoreCase(selectedStatus)) {
                matchesStatus = s.getOutstandingBalance() != null && s.getOutstandingBalance().signum() > 0;
            }

            return matchesSearch && matchesStatus;
        }).toList();

        tableModel.setRowCount(0);
        for (Supplier s : displayedSupplierList) {
            boolean hasPayable = s.getOutstandingBalance() != null && s.getOutstandingBalance().signum() > 0;
            tableModel.addRow(new Object[]{
                    s.getName(),
                    s.getContactPerson() != null && !s.getContactPerson().isEmpty() ? s.getContactPerson() : "N/A",
                    (s.getPhone() != null ? s.getPhone() : "") + (s.getEmail() != null && !s.getEmail().isEmpty() ? " | " + s.getEmail() : ""),
                    s.getTaxId() != null && !s.getTaxId().isEmpty() ? s.getTaxId() : "N/A",
                    "₹" + (s.getOutstandingBalance() != null ? s.getOutstandingBalance().setScale(2) : "0.00"),
                    hasPayable ? "PAYABLE DUE" : (s.isActive() ? "ACTIVE" : "INACTIVE"),
                    "Actions ⚙"
            });
        }
    }

    private void exportCsv() {
        if (displayedSupplierList.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No suppliers to export.", "Export CSV", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("suppliers_export.csv"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try (FileWriter writer = new FileWriter(file)) {
                writer.write("ID,Company Name,Contact Person,Phone,Email,GSTIN,City,Outstanding Payable,Status\n");
                for (Supplier s : displayedSupplierList) {
                    writer.write(String.format("%d,\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",%.2f,%s\n",
                            s.getId(),
                            s.getName().replace("\"", "\"\""),
                            s.getContactPerson() != null ? s.getContactPerson() : "",
                            s.getPhone() != null ? s.getPhone() : "",
                            s.getEmail() != null ? s.getEmail() : "",
                            s.getTaxId() != null ? s.getTaxId() : "",
                            s.getCity() != null ? s.getCity() : "",
                            s.getOutstandingBalance() != null ? s.getOutstandingBalance() : BigDecimal.ZERO,
                            s.isActive() ? "ACTIVE" : "INACTIVE"
                    ));
                }
                JOptionPane.showMessageDialog(this, "Exported supplier list to " + file.getAbsolutePath(), "CSV Exported", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to export CSV: " + ex.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void refreshData() {
        int bId = UserSession.getBusinessId();
        try {
            masterSupplierList = supplierDAO.findByBusiness(bId);
            BigDecimal totalPayable = BigDecimal.ZERO;
            int activeCount = 0;

            for (Supplier s : masterSupplierList) {
                if (s.getOutstandingBalance() != null) {
                    totalPayable = totalPayable.add(s.getOutstandingBalance());
                }
                if (s.isActive()) activeCount++;
            }

            activeSuppliersLbl.setText(String.valueOf(masterSupplierList.size()));
            payablesLbl.setText("₹" + totalPayable.setScale(2));
            totalPurchasesLbl.setText(activeCount + " Active");

            applyFilters();
        } catch (Exception e) {
            System.err.println("Supplier refresh error: " + e.getMessage());
        }
    }
}
