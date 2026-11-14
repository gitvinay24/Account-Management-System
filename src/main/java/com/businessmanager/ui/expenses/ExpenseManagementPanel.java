package com.businessmanager.ui.expenses;

import com.businessmanager.dao.ExpenseDAO;
import com.businessmanager.enums.ExpenseCategory;
import com.businessmanager.model.Expense;
import com.businessmanager.security.UserSession;
import com.businessmanager.theme.Colors;
import com.businessmanager.theme.Fonts;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

public class ExpenseManagementPanel extends JPanel {

    private final ExpenseDAO expenseDAO = new ExpenseDAO();
    private DefaultTableModel tableModel;
    private JTable table;
    private JTextField searchField;
    private JComboBox<String> categoryFilterCombo;
    private List<Expense> expenseList = new ArrayList<>();

    public ExpenseManagementPanel() {
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

        JLabel title = new JLabel("Expense Tracking & Audit");
        title.setFont(Fonts.DISPLAY_LG);
        title.setForeground(Colors.ON_SURFACE);

        JLabel sub = new JLabel("Log operating expenses, rent, utilities, payroll, and maintenance costs");
        sub.setFont(Fonts.BODY);
        sub.setForeground(Colors.ON_SURFACE_VARIANT);

        headerText.add(title);
        headerText.add(Box.createVerticalStrut(4));
        headerText.add(sub);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        actions.setOpaque(false);

        searchField = new JTextField(12);
        searchField.setPreferredSize(new Dimension(180, 36));
        searchField.putClientProperty("JTextField.placeholderText", "Search Expense...");
        searchField.addActionListener(e -> filterExpenses());
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filterExpenses(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filterExpenses(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filterExpenses(); }
        });

        categoryFilterCombo = new JComboBox<>();
        categoryFilterCombo.setPreferredSize(new Dimension(160, 36));
        categoryFilterCombo.addItem("All Categories");
        for (ExpenseCategory cat : ExpenseCategory.values()) {
            categoryFilterCombo.addItem(cat.getDisplayName());
        }
        categoryFilterCombo.addActionListener(e -> filterExpenses());

        JButton addBtn = new JButton("+  Log Expense");
        addBtn.setFont(Fonts.TITLE_SM);
        addBtn.setForeground(Colors.ON_PRIMARY);
        addBtn.setBackground(Colors.PRIMARY);
        addBtn.setPreferredSize(new Dimension(150, 36));
        addBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addBtn.putClientProperty("JButton.buttonType", "roundRect");
        addBtn.putClientProperty("Component.arc", 8);
        addBtn.addActionListener(e -> showExpenseDialog(null));

        actions.add(searchField);
        actions.add(categoryFilterCombo);
        actions.add(addBtn);

        topPanel.add(headerText, BorderLayout.WEST);
        topPanel.add(actions, BorderLayout.EAST);

        content.add(topPanel, BorderLayout.NORTH);

        // Table Card
        JPanel tableCard = new JPanel(new BorderLayout());
        tableCard.setBackground(Colors.SURFACE_CONTAINER);
        tableCard.setBorder(new EmptyBorder(16, 16, 16, 16));

        String[] cols = {"ID", "Expense Title", "Category", "Amount", "Expense Date", "Logged By", "Branch"};
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

        JButton editBtn = new JButton("Edit Expense");
        editBtn.setFont(Fonts.BODY_SMALL);
        editBtn.addActionListener(e -> editSelected());

        JButton deleteBtn = new JButton("Delete Expense");
        deleteBtn.setFont(Fonts.BODY_SMALL);
        deleteBtn.setForeground(Colors.ERROR);
        deleteBtn.addActionListener(e -> deleteSelected());

        tableFooter.add(editBtn);
        tableFooter.add(deleteBtn);

        tableCard.add(tableFooter, BorderLayout.SOUTH);

        content.add(tableCard, BorderLayout.CENTER);
        add(content, BorderLayout.CENTER);
    }

    public void refreshData() {
        int bId = UserSession.getBusinessId();
        if (!UserSession.isLoggedIn()) return;

        try {
            expenseList = expenseDAO.findByBusiness(bId);
            filterExpenses();
        } catch (SQLException e) {
            System.err.println("Expenses refresh error: " + e.getMessage());
        }
    }

    private void filterExpenses() {
        String query = searchField.getText().trim().toLowerCase();
        String selectedCat = (String) categoryFilterCombo.getSelectedItem();
        NumberFormat fmt = NumberFormat.getCurrencyInstance(new java.util.Locale("en", "IN"));
        tableModel.setRowCount(0);

        for (Expense e : expenseList) {
            boolean matchesSearch = query.isEmpty() ||
                    e.getTitle().toLowerCase().contains(query) ||
                    (e.getDescription() != null && e.getDescription().toLowerCase().contains(query));

            boolean matchesCategory = selectedCat == null || selectedCat.equals("All Categories") ||
                    e.getCategory().getDisplayName().equals(selectedCat);

            if (matchesSearch && matchesCategory) {
                tableModel.addRow(new Object[]{
                        e.getId(),
                        e.getTitle(),
                        e.getCategory().getDisplayName(),
                        fmt.format(e.getAmount()),
                        e.getExpenseDate() != null ? e.getExpenseDate().toString() : "",
                        e.getUserName() != null ? e.getUserName() : "Admin",
                        e.getBranchName() != null ? e.getBranchName() : "Main HQ"
                });
            }
        }
    }

    private void editSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select an expense to edit.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int id = (int) tableModel.getValueAt(row, 0);
        Expense e = expenseList.stream().filter(x -> x.getId() == id).findFirst().orElse(null);
        if (e != null) showExpenseDialog(e);
    }

    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select an expense to delete.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int id = (int) tableModel.getValueAt(row, 0);
        String titleText = (String) tableModel.getValueAt(row, 1);

        int confirm = JOptionPane.showConfirmDialog(this, "Delete expense '" + titleText + "'?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                expenseDAO.delete(id);
                refreshData();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error deleting expense: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showExpenseDialog(Expense toEdit) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), toEdit == null ? "Log New Expense" : "Edit Expense", true);
        dialog.setSize(480, 420);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Colors.SURFACE_CONTAINER);
        panel.setBorder(new EmptyBorder(20, 24, 20, 24));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField titleField = new JTextField(20);
        JComboBox<ExpenseCategory> catCombo = new JComboBox<>(ExpenseCategory.values());
        JTextField amountField = new JTextField("0.00", 10);
        JTextField dateField = new JTextField(new Date(System.currentTimeMillis()).toString(), 10);
        JTextArea descArea = new JTextArea(3, 20);

        if (toEdit != null) {
            titleField.setText(toEdit.getTitle());
            catCombo.setSelectedItem(toEdit.getCategory());
            amountField.setText(toEdit.getAmount().toString());
            dateField.setText(toEdit.getExpenseDate().toString());
            descArea.setText(toEdit.getDescription());
        }

        int r = 0;
        addFormRow(panel, gbc, r++, "Expense Title *:", titleField);
        addFormRow(panel, gbc, r++, "Category *:", catCombo);
        addFormRow(panel, gbc, r++, "Amount (₹) *:", amountField);
        addFormRow(panel, gbc, r++, "Date (YYYY-MM-DD) *:", dateField);
        addFormRow(panel, gbc, r++, "Description:", new JScrollPane(descArea));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        btnPanel.setBackground(Colors.SURFACE_CONTAINER);

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dialog.dispose());

        JButton saveBtn = new JButton("Save Expense");
        saveBtn.setFont(Fonts.BODY_SMALL);
        saveBtn.setForeground(Colors.ON_PRIMARY);
        saveBtn.setBackground(Colors.PRIMARY);
        saveBtn.addActionListener(e -> {
            String titleStr = titleField.getText().trim();
            String amtStr = amountField.getText().trim();

            if (titleStr.isEmpty() || amtStr.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Title and Amount are required.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                BigDecimal amt = new BigDecimal(amtStr);
                Date expDate = Date.valueOf(dateField.getText().trim());

                Expense exp = toEdit != null ? toEdit : new Expense();
                exp.setBusinessId(UserSession.getBusinessId());
                exp.setBranchId(UserSession.getBranchId());
                exp.setUserId(UserSession.getUserId());
                exp.setTitle(titleStr);
                exp.setCategory((ExpenseCategory) catCombo.getSelectedItem());
                exp.setAmount(amt);
                exp.setExpenseDate(expDate);
                exp.setDescription(descArea.getText().trim());

                if (toEdit == null) {
                    expenseDAO.create(exp);
                } else {
                    expenseDAO.update(exp);
                }
                dialog.dispose();
                refreshData();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Error: " + ex.getMessage(), "Input Error", JOptionPane.ERROR_MESSAGE);
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
