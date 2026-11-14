package com.businessmanager.ui.branches;

import com.businessmanager.dao.BranchDAO;
import com.businessmanager.model.Branch;
import com.businessmanager.security.UserSession;
import com.businessmanager.theme.Colors;
import com.businessmanager.theme.Fonts;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BranchManagementPanel extends JPanel {

    private final BranchDAO branchDAO = new BranchDAO();
    private DefaultTableModel tableModel;
    private JTable table;
    private JTextField searchField;
    private List<Branch> branchList = new ArrayList<>();

    public BranchManagementPanel() {
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

        JLabel title = new JLabel("Enterprise Multi-Branch Management");
        title.setFont(Fonts.DISPLAY_LG);
        title.setForeground(Colors.ON_SURFACE);

        JLabel sub = new JLabel("Configure corporate branches, main HQ location, branch codes, and regional contacts");
        sub.setFont(Fonts.BODY);
        sub.setForeground(Colors.ON_SURFACE_VARIANT);

        headerText.add(title);
        headerText.add(Box.createVerticalStrut(4));
        headerText.add(sub);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        actions.setOpaque(false);

        searchField = new JTextField(15);
        searchField.setPreferredSize(new Dimension(200, 36));
        searchField.putClientProperty("JTextField.placeholderText", "Search Branch Name/Code...");
        searchField.addActionListener(e -> filterBranches());
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filterBranches(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filterBranches(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filterBranches(); }
        });

        JButton addBtn = new JButton("+  Add Branch");
        addBtn.setFont(Fonts.TITLE_SM);
        addBtn.setForeground(Colors.ON_PRIMARY);
        addBtn.setBackground(Colors.PRIMARY);
        addBtn.setPreferredSize(new Dimension(150, 36));
        addBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addBtn.putClientProperty("JButton.buttonType", "roundRect");
        addBtn.putClientProperty("Component.arc", 8);
        addBtn.addActionListener(e -> showBranchDialog(null));

        actions.add(searchField);
        actions.add(addBtn);

        topPanel.add(headerText, BorderLayout.WEST);
        topPanel.add(actions, BorderLayout.EAST);

        content.add(topPanel, BorderLayout.NORTH);

        // Table Card
        JPanel tableCard = new JPanel(new BorderLayout());
        tableCard.setBackground(Colors.SURFACE_CONTAINER);
        tableCard.setBorder(new EmptyBorder(16, 16, 16, 16));

        String[] cols = {"ID", "Branch Name", "Branch Code", "City", "State", "Phone", "Email", "Main HQ", "Status"};
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

        JButton editBtn = new JButton("Edit Branch");
        editBtn.setFont(Fonts.BODY_SMALL);
        editBtn.addActionListener(e -> editSelected());

        JButton deleteBtn = new JButton("Delete Branch");
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
            branchList = branchDAO.findByBusiness(bId);
            filterBranches();
        } catch (SQLException e) {
            System.err.println("Branches refresh error: " + e.getMessage());
        }
    }

    private void filterBranches() {
        String query = searchField.getText().trim().toLowerCase();
        tableModel.setRowCount(0);

        for (Branch b : branchList) {
            boolean matches = query.isEmpty() ||
                    b.getName().toLowerCase().contains(query) ||
                    b.getCode().toLowerCase().contains(query) ||
                    (b.getCity() != null && b.getCity().toLowerCase().contains(query));

            if (matches) {
                tableModel.addRow(new Object[]{
                        b.getId(),
                        b.getName(),
                        b.getCode(),
                        b.getCity() != null ? b.getCity() : "",
                        b.getState() != null ? b.getState() : "",
                        b.getPhone() != null ? b.getPhone() : "",
                        b.getEmail() != null ? b.getEmail() : "",
                        b.isMain() ? "YES (HQ)" : "NO",
                        b.isActive() ? "Active" : "Inactive"
                });
            }
        }
    }

    private void editSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a branch to edit.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int id = (int) tableModel.getValueAt(row, 0);
        Branch b = branchList.stream().filter(x -> x.getId() == id).findFirst().orElse(null);
        if (b != null) showBranchDialog(b);
    }

    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a branch to delete.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int id = (int) tableModel.getValueAt(row, 0);
        String name = (String) tableModel.getValueAt(row, 1);

        int confirm = JOptionPane.showConfirmDialog(this, "Delete branch '" + name + "'?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                branchDAO.delete(id);
                refreshData();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error deleting branch: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showBranchDialog(Branch toEdit) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), toEdit == null ? "Add Branch" : "Edit Branch", true);
        dialog.setSize(480, 480);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Colors.SURFACE_CONTAINER);
        panel.setBorder(new EmptyBorder(20, 24, 20, 24));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField nameField = new JTextField(20);
        JTextField codeField = new JTextField("BR-" + (System.currentTimeMillis() % 1000), 15);
        JTextField phoneField = new JTextField(20);
        JTextField emailField = new JTextField(20);
        JTextField addressField = new JTextField(20);
        JTextField cityField = new JTextField(20);
        JTextField stateField = new JTextField(20);
        JCheckBox mainCheckBox = new JCheckBox("Is Main Corporate HQ?");
        mainCheckBox.setOpaque(false);

        if (toEdit != null) {
            nameField.setText(toEdit.getName());
            codeField.setText(toEdit.getCode());
            phoneField.setText(toEdit.getPhone());
            emailField.setText(toEdit.getEmail());
            addressField.setText(toEdit.getAddress());
            cityField.setText(toEdit.getCity());
            stateField.setText(toEdit.getState());
            mainCheckBox.setSelected(toEdit.isMain());
        }

        int r = 0;
        addFormRow(panel, gbc, r++, "Branch Name *:", nameField);
        addFormRow(panel, gbc, r++, "Branch Code *:", codeField);
        addFormRow(panel, gbc, r++, "Phone:", phoneField);
        addFormRow(panel, gbc, r++, "Email:", emailField);
        addFormRow(panel, gbc, r++, "Address:", addressField);
        addFormRow(panel, gbc, r++, "City:", cityField);
        addFormRow(panel, gbc, r++, "State:", stateField);
        addFormRow(panel, gbc, r++, "Headquarters:", mainCheckBox);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        btnPanel.setBackground(Colors.SURFACE_CONTAINER);

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dialog.dispose());

        JButton saveBtn = new JButton("Save Branch");
        saveBtn.setFont(Fonts.BODY_SMALL);
        saveBtn.setForeground(Colors.ON_PRIMARY);
        saveBtn.setBackground(Colors.PRIMARY);
        saveBtn.addActionListener(e -> {
            String name = nameField.getText().trim();
            String code = codeField.getText().trim();

            if (name.isEmpty() || code.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Name and Code are required.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                Branch b = toEdit != null ? toEdit : new Branch();
                b.setBusinessId(UserSession.getBusinessId());
                b.setName(name);
                b.setCode(code);
                b.setPhone(phoneField.getText().trim());
                b.setEmail(emailField.getText().trim());
                b.setAddress(addressField.getText().trim());
                b.setCity(cityField.getText().trim());
                b.setState(stateField.getText().trim());
                b.setMain(mainCheckBox.isSelected());
                b.setActive(true);

                if (toEdit == null) {
                    branchDAO.create(b);
                } else {
                    branchDAO.update(b);
                }
                dialog.dispose();
                refreshData();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(dialog, "Error saving branch: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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
