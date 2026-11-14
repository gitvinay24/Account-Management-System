package com.businessmanager.ui.employees;

import com.businessmanager.dao.BranchDAO;
import com.businessmanager.dao.EmployeeDAO;
import com.businessmanager.model.Branch;
import com.businessmanager.model.Employee;
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

public class EmployeeManagementPanel extends JPanel {

    private final EmployeeDAO employeeDAO = new EmployeeDAO();
    private final BranchDAO branchDAO = new BranchDAO();

    private DefaultTableModel tableModel;
    private JTable table;
    private JTextField searchField;
    private List<Employee> employeeList = new ArrayList<>();
    private List<Branch> branchList = new ArrayList<>();

    public EmployeeManagementPanel() {
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

        JLabel title = new JLabel("Employee & Staff Roster");
        title.setFont(Fonts.DISPLAY_LG);
        title.setForeground(Colors.ON_SURFACE);

        JLabel sub = new JLabel("Enterprise employee directory, roles, branch assignments, and payroll compensation");
        sub.setFont(Fonts.BODY);
        sub.setForeground(Colors.ON_SURFACE_VARIANT);

        headerText.add(title);
        headerText.add(Box.createVerticalStrut(4));
        headerText.add(sub);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        actions.setOpaque(false);

        searchField = new JTextField(15);
        searchField.setPreferredSize(new Dimension(200, 36));
        searchField.putClientProperty("JTextField.placeholderText", "Search Name or Code...");
        searchField.addActionListener(e -> filterEmployees());
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filterEmployees(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filterEmployees(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filterEmployees(); }
        });

        JButton addBtn = new JButton("+  Add Employee");
        addBtn.setFont(Fonts.TITLE_SM);
        addBtn.setForeground(Colors.ON_PRIMARY);
        addBtn.setBackground(Colors.PRIMARY);
        addBtn.setPreferredSize(new Dimension(160, 36));
        addBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addBtn.putClientProperty("JButton.buttonType", "roundRect");
        addBtn.putClientProperty("Component.arc", 8);
        addBtn.addActionListener(e -> showEmployeeDialog(null));

        actions.add(searchField);
        actions.add(addBtn);

        topPanel.add(headerText, BorderLayout.WEST);
        topPanel.add(actions, BorderLayout.EAST);

        content.add(topPanel, BorderLayout.NORTH);

        // Table Card
        JPanel tableCard = new JPanel(new BorderLayout());
        tableCard.setBackground(Colors.SURFACE_CONTAINER);
        tableCard.setBorder(new EmptyBorder(16, 16, 16, 16));

        String[] cols = {"ID", "Code", "Full Name", "Position", "Department", "Branch Location", "Salary", "Status"};
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

        JButton editBtn = new JButton("Edit Employee");
        editBtn.setFont(Fonts.BODY_SMALL);
        editBtn.addActionListener(e -> editSelected());

        JButton deleteBtn = new JButton("Delete Employee");
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
        if (bId <= 0) return;

        try {
            branchList = branchDAO.findByBusiness(bId);
            employeeList = employeeDAO.findByBusiness(bId);
            filterEmployees();
        } catch (SQLException e) {
            System.err.println("Employees refresh error: " + e.getMessage());
        }
    }

    private void filterEmployees() {
        String query = searchField.getText().trim().toLowerCase();
        NumberFormat fmt = NumberFormat.getCurrencyInstance(new java.util.Locale("en", "IN"));
        tableModel.setRowCount(0);

        for (Employee emp : employeeList) {
            boolean matches = query.isEmpty() ||
                    emp.getFullName().toLowerCase().contains(query) ||
                    emp.getEmployeeCode().toLowerCase().contains(query) ||
                    (emp.getPosition() != null && emp.getPosition().toLowerCase().contains(query));

            if (matches) {
                tableModel.addRow(new Object[]{
                        emp.getId(),
                        emp.getEmployeeCode(),
                        emp.getFullName(),
                        emp.getPosition(),
                        emp.getDepartment() != null ? emp.getDepartment() : "General",
                        emp.getBranchName() != null ? emp.getBranchName() : "HQ",
                        fmt.format(emp.getSalary()),
                        emp.isActive() ? "Active" : "Inactive"
                });
            }
        }
    }

    private void editSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select an employee to edit.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int id = (int) tableModel.getValueAt(row, 0);
        Employee emp = employeeList.stream().filter(x -> x.getId() == id).findFirst().orElse(null);
        if (emp != null) showEmployeeDialog(emp);
    }

    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select an employee to delete.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int id = (int) tableModel.getValueAt(row, 0);
        String name = (String) tableModel.getValueAt(row, 2);

        int confirm = JOptionPane.showConfirmDialog(this, "Delete employee '" + name + "'?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                employeeDAO.delete(id);
                refreshData();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error deleting employee: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showEmployeeDialog(Employee toEdit) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), toEdit == null ? "Add Employee" : "Edit Employee", true);
        dialog.setSize(500, 520);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Colors.SURFACE_CONTAINER);
        panel.setBorder(new EmptyBorder(20, 24, 20, 24));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField codeField = new JTextField("EMP-" + (System.currentTimeMillis() % 1000), 15);
        JTextField nameField = new JTextField(20);
        JTextField emailField = new JTextField(20);
        JTextField phoneField = new JTextField(20);
        JTextField positionField = new JTextField("Sales Executive", 20);
        JTextField deptField = new JTextField("Sales", 20);
        JTextField salaryField = new JTextField("45000.00", 10);
        JComboBox<Branch> branchCombo = new JComboBox<>(branchList.toArray(new Branch[0]));

        if (toEdit != null) {
            codeField.setText(toEdit.getEmployeeCode());
            nameField.setText(toEdit.getFullName());
            emailField.setText(toEdit.getEmail());
            phoneField.setText(toEdit.getPhone());
            positionField.setText(toEdit.getPosition());
            deptField.setText(toEdit.getDepartment());
            salaryField.setText(toEdit.getSalary().toString());

            if (toEdit.getBranchId() != null) {
                for (int i = 0; i < branchCombo.getItemCount(); i++) {
                    if (branchCombo.getItemAt(i).getId() == toEdit.getBranchId()) {
                        branchCombo.setSelectedIndex(i);
                        break;
                    }
                }
            }
        }

        int r = 0;
        addFormRow(panel, gbc, r++, "Employee Code *:", codeField);
        addFormRow(panel, gbc, r++, "Full Name *:", nameField);
        addFormRow(panel, gbc, r++, "Email:", emailField);
        addFormRow(panel, gbc, r++, "Phone:", phoneField);
        addFormRow(panel, gbc, r++, "Position / Title *:", positionField);
        addFormRow(panel, gbc, r++, "Department:", deptField);
        addFormRow(panel, gbc, r++, "Monthly Salary (₹):", salaryField);
        addFormRow(panel, gbc, r++, "Assigned Branch:", branchCombo);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        btnPanel.setBackground(Colors.SURFACE_CONTAINER);

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dialog.dispose());

        JButton saveBtn = new JButton("Save Employee");
        saveBtn.setFont(Fonts.BODY_SMALL);
        saveBtn.setForeground(Colors.ON_PRIMARY);
        saveBtn.setBackground(Colors.PRIMARY);
        saveBtn.addActionListener(e -> {
            String code = codeField.getText().trim();
            String name = nameField.getText().trim();
            String pos = positionField.getText().trim();

            if (code.isEmpty() || name.isEmpty() || pos.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Code, Name, and Position are required.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                BigDecimal sal = new BigDecimal(salaryField.getText().trim());
                Branch selBr = (Branch) branchCombo.getSelectedItem();

                Employee emp = toEdit != null ? toEdit : new Employee();
                emp.setBusinessId(UserSession.getBusinessId());
                emp.setBranchId(selBr != null ? selBr.getId() : null);
                emp.setEmployeeCode(code);
                emp.setFullName(name);
                emp.setEmail(emailField.getText().trim());
                emp.setPhone(phoneField.getText().trim());
                emp.setPosition(pos);
                emp.setDepartment(deptField.getText().trim());
                emp.setSalary(sal);
                emp.setHireDate(new Date(System.currentTimeMillis()));

                if (toEdit == null) {
                    employeeDAO.create(emp);
                } else {
                    employeeDAO.update(emp);
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
