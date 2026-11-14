package com.businessmanager.ui.customers;

import com.businessmanager.components.StitchIcon;
import com.businessmanager.dao.CustomerDAO;
import com.businessmanager.model.Customer;
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

public class CustomerManagementPanel extends JPanel {

    private final CustomerDAO customerDAO = new CustomerDAO();
    private DefaultTableModel tableModel;
    private JTable table;
    private JTextField searchField;
    private JComboBox<String> statusFilterCombo;

    private JLabel totalCustomersLbl;
    private JLabel activeCreditLbl;
    private JLabel repeatRateLbl;

    private List<Customer> masterCustomerList = new ArrayList<>();
    private List<Customer> displayedCustomerList = new ArrayList<>();

    public CustomerManagementPanel() {
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

        JLabel title = new JLabel("Customer Directory");
        title.setFont(Fonts.DISPLAY_LG);
        title.setForeground(Colors.ON_SURFACE);

        JLabel sub = new JLabel("Manage customer profiles, purchase history, and credit accounts.");
        sub.setFont(Fonts.BODY);
        sub.setForeground(Colors.ON_SURFACE_VARIANT);

        left.add(title);
        left.add(Box.createVerticalStrut(4));
        left.add(sub);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);

        JButton exportBtn = new JButton("Export List");
        exportBtn.setFont(Fonts.BODY_SMALL);
        exportBtn.setForeground(Colors.ON_SURFACE);
        exportBtn.setBackground(Colors.SURFACE_CONTAINER_HIGH);
        exportBtn.setFocusPainted(false);
        exportBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        exportBtn.putClientProperty("JButton.buttonType", "roundRect");
        exportBtn.addActionListener(e -> exportCsv());

        JButton addBtn = new JButton("+  Add Customer");
        addBtn.setFont(Fonts.TITLE_SM);
        addBtn.setForeground(Colors.ON_PRIMARY);
        addBtn.setBackground(Colors.PRIMARY);
        addBtn.setFocusPainted(false);
        addBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addBtn.putClientProperty("JButton.buttonType", "roundRect");
        addBtn.addActionListener(e -> showCustomerDialog(null));

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

        totalCustomersLbl = new JLabel("0");
        activeCreditLbl = new JLabel("₹0.00");
        repeatRateLbl = new JLabel("0%");

        grid.add(createCard("TOTAL CUSTOMERS", totalCustomersLbl, "Registered accounts", StitchIcon.Type.PERSON, Colors.PRIMARY));
        grid.add(createCard("ACTIVE CREDIT", activeCreditLbl, "Outstanding balances", StitchIcon.Type.PAYMENTS, Colors.TERTIARY));
        grid.add(createCard("ACTIVE ACCOUNTS", repeatRateLbl, "Accounts with activity", StitchIcon.Type.STOREFRONT, Colors.SECONDARY));

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
        searchField.putClientProperty("JTextField.placeholderText", "Search customers by name, phone, or email...");
        searchField.setFont(Fonts.BODY);
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { applyFilters(); }
            public void removeUpdate(DocumentEvent e) { applyFilters(); }
            public void changedUpdate(DocumentEvent e) { applyFilters(); }
        });

        statusFilterCombo = new JComboBox<>(new String[]{"All Customers", "Active", "Credit Due"});
        statusFilterCombo.setPreferredSize(new Dimension(160, 38));
        statusFilterCombo.addActionListener(e -> applyFilters());

        bar.add(searchField, BorderLayout.CENTER);
        bar.add(statusFilterCombo, BorderLayout.EAST);
        return bar;
    }

    private JComponent createTableSection() {
        String[] cols = {"CUSTOMER NAME", "CONTACT INFO", "CITY / REGION", "TOTAL SPENT", "OUTSTANDING BAL.", "STATUS", "ACTIONS"};
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
                if (row >= 0 && row < displayedCustomerList.size()) {
                    Customer c = displayedCustomerList.get(row);
                    if (e.getClickCount() == 2) {
                        showCustomerDialog(c);
                    } else if (table.getSelectedColumn() == 6) {
                        showActionMenu(e.getComponent(), e.getX(), e.getY(), c);
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

    private void showActionMenu(Component comp, int x, int y, Customer c) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem editItem = new JMenuItem("Edit Customer");
        editItem.addActionListener(e -> showCustomerDialog(c));

        JMenuItem deleteItem = new JMenuItem("Delete Customer");
        deleteItem.addActionListener(e -> deleteCustomer(c));

        menu.add(editItem);
        menu.add(deleteItem);
        menu.show(comp, x, y);
    }

    private void deleteCustomer(Customer c) {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete customer '" + c.getName() + "'?",
                "Confirm Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                if (customerDAO.delete(c.getId())) {
                    JOptionPane.showMessageDialog(this, "Customer deleted successfully.", "Customer Deleted", JOptionPane.INFORMATION_MESSAGE);
                    refreshData();
                } else {
                    JOptionPane.showMessageDialog(this, "Could not delete customer.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showCustomerDialog(Customer existingCustomer) {
        int bId = UserSession.getBusinessId();
        boolean isEdit = existingCustomer != null;

        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this) instanceof Frame f ? f : null,
                isEdit ? "Edit Customer" : "Add Customer", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(440, 520);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(20, 24, 20, 24));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 6, 6, 6);

        JTextField nameField = new JTextField(isEdit ? existingCustomer.getName() : "");
        JTextField emailField = new JTextField(isEdit && existingCustomer.getEmail() != null ? existingCustomer.getEmail() : "");
        JTextField phoneField = new JTextField(isEdit && existingCustomer.getPhone() != null ? existingCustomer.getPhone() : "");
        JTextField addressField = new JTextField(isEdit && existingCustomer.getAddress() != null ? existingCustomer.getAddress() : "");
        JTextField cityField = new JTextField(isEdit && existingCustomer.getCity() != null ? existingCustomer.getCity() : "");
        JTextField taxIdField = new JTextField(isEdit && existingCustomer.getTaxId() != null ? existingCustomer.getTaxId() : "");
        JCheckBox activeBox = new JCheckBox("Active Account", !isEdit || existingCustomer.isActive());

        int row = 0;
        addFormRow(form, gbc, row++, "Full Name *:", nameField);
        addFormRow(form, gbc, row++, "Email:", emailField);
        addFormRow(form, gbc, row++, "Phone *:", phoneField);
        addFormRow(form, gbc, row++, "Address:", addressField);
        addFormRow(form, gbc, row++, "City:", cityField);
        addFormRow(form, gbc, row++, "GSTIN / Tax ID:", taxIdField);
        addFormRow(form, gbc, row++, "Status:", activeBox);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dialog.dispose());

        JButton saveBtn = new JButton(isEdit ? "Update Customer" : "Save Customer");
        saveBtn.setFont(Fonts.TITLE_SM);
        saveBtn.setForeground(Colors.ON_PRIMARY);
        saveBtn.setBackground(Colors.PRIMARY);
        saveBtn.addActionListener(e -> {
            String name = nameField.getText().trim();
            String phone = phoneField.getText().trim();
            if (name.isEmpty() || phone.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Customer Name and Phone are required.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            Customer c = isEdit ? existingCustomer : new Customer();
            c.setBusinessId(bId);
            c.setName(name);
            c.setEmail(emailField.getText().trim());
            c.setPhone(phone);
            c.setAddress(addressField.getText().trim());
            c.setCity(cityField.getText().trim());
            c.setTaxId(taxIdField.getText().trim());
            c.setActive(activeBox.isSelected());

            try {
                if (isEdit) {
                    if (customerDAO.update(c)) {
                        JOptionPane.showMessageDialog(dialog, "Customer updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                        dialog.dispose();
                        refreshData();
                    } else {
                        JOptionPane.showMessageDialog(dialog, "Failed to update customer.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    int id = customerDAO.create(c);
                    if (id > 0) {
                        JOptionPane.showMessageDialog(dialog, "Customer created successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                        dialog.dispose();
                        refreshData();
                    } else {
                        JOptionPane.showMessageDialog(dialog, "Failed to create customer.", "Error", JOptionPane.ERROR_MESSAGE);
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

        displayedCustomerList = masterCustomerList.stream().filter(c -> {
            boolean matchesSearch = query.isEmpty()
                    || c.getName().toLowerCase().contains(query)
                    || (c.getPhone() != null && c.getPhone().toLowerCase().contains(query))
                    || (c.getEmail() != null && c.getEmail().toLowerCase().contains(query));

            boolean matchesStatus = true;
            if ("Active".equalsIgnoreCase(selectedStatus)) {
                matchesStatus = c.isActive();
            } else if ("Credit Due".equalsIgnoreCase(selectedStatus)) {
                matchesStatus = c.getOutstandingBalance() != null && c.getOutstandingBalance().signum() > 0;
            }

            return matchesSearch && matchesStatus;
        }).toList();

        tableModel.setRowCount(0);
        for (Customer c : displayedCustomerList) {
            boolean hasCredit = c.getOutstandingBalance() != null && c.getOutstandingBalance().signum() > 0;
            tableModel.addRow(new Object[]{
                    c.getName(),
                    (c.getPhone() != null ? c.getPhone() : "") + (c.getEmail() != null && !c.getEmail().isEmpty() ? " | " + c.getEmail() : ""),
                    c.getCity() != null && !c.getCity().isEmpty() ? c.getCity() : "Local",
                    "₹" + (c.getTotalPurchased() != null ? c.getTotalPurchased().setScale(2) : "0.00"),
                    "₹" + (c.getOutstandingBalance() != null ? c.getOutstandingBalance().setScale(2) : "0.00"),
                    hasCredit ? "CREDIT DUE" : (c.isActive() ? "ACTIVE" : "INACTIVE"),
                    "Actions ⚙"
            });
        }
    }

    private void exportCsv() {
        if (displayedCustomerList.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No customers to export.", "Export CSV", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("customers_export.csv"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try (FileWriter writer = new FileWriter(file)) {
                writer.write("ID,Customer Name,Phone,Email,City,Total Spent,Outstanding Balance,Tax ID,Status\n");
                for (Customer c : displayedCustomerList) {
                    writer.write(String.format("%d,\"%s\",\"%s\",\"%s\",\"%s\",%.2f,%.2f,\"%s\",%s\n",
                            c.getId(),
                            c.getName().replace("\"", "\"\""),
                            c.getPhone() != null ? c.getPhone() : "",
                            c.getEmail() != null ? c.getEmail() : "",
                            c.getCity() != null ? c.getCity() : "",
                            c.getTotalPurchased() != null ? c.getTotalPurchased() : BigDecimal.ZERO,
                            c.getOutstandingBalance() != null ? c.getOutstandingBalance() : BigDecimal.ZERO,
                            c.getTaxId() != null ? c.getTaxId() : "",
                            c.isActive() ? "ACTIVE" : "INACTIVE"
                    ));
                }
                JOptionPane.showMessageDialog(this, "Exported customer list to " + file.getAbsolutePath(), "CSV Exported", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to export CSV: " + ex.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void refreshData() {
        int bId = UserSession.getBusinessId();
        try {
            masterCustomerList = customerDAO.findByBusiness(bId);
            BigDecimal totalCredit = BigDecimal.ZERO;
            int activeCount = 0;

            for (Customer c : masterCustomerList) {
                if (c.getOutstandingBalance() != null) {
                    totalCredit = totalCredit.add(c.getOutstandingBalance());
                }
                if (c.isActive()) activeCount++;
            }

            totalCustomersLbl.setText(String.valueOf(masterCustomerList.size()));
            activeCreditLbl.setText("₹" + totalCredit.setScale(2));
            repeatRateLbl.setText(activeCount + " Active");

            applyFilters();
        } catch (Exception e) {
            System.err.println("Customer refresh error: " + e.getMessage());
        }
    }
}
