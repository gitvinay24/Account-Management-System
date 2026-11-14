package com.businessmanager.ui.categories;

import com.businessmanager.dao.CategoryDAO;
import com.businessmanager.model.Category;
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

public class CategoryManagementPanel extends JPanel {

    private final CategoryDAO categoryDAO = new CategoryDAO();
    private DefaultTableModel tableModel;
    private JTable table;
    private JTextField searchField;
    private List<Category> categoryList = new ArrayList<>();

    public CategoryManagementPanel() {
        setLayout(new BorderLayout());
        setBackground(Colors.BACKGROUND);
        setBorder(new EmptyBorder(24, 28, 24, 28));

        buildUI();
        refreshData();
    }

    private void buildUI() {
        JPanel content = new JPanel(new BorderLayout(0, 20));
        content.setOpaque(false);

        // Header + Toolbar
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);

        JPanel headerText = new JPanel();
        headerText.setLayout(new BoxLayout(headerText, BoxLayout.Y_AXIS));
        headerText.setOpaque(false);

        JLabel title = new JLabel("Category Management");
        title.setFont(Fonts.DISPLAY_LG);
        title.setForeground(Colors.ON_SURFACE);

        JLabel sub = new JLabel("Organize products into logical business categories for inventory and sales reporting");
        sub.setFont(Fonts.BODY);
        sub.setForeground(Colors.ON_SURFACE_VARIANT);

        headerText.add(title);
        headerText.add(Box.createVerticalStrut(4));
        headerText.add(sub);

        // Toolbar Action Buttons
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        actions.setOpaque(false);

        searchField = new JTextField(15);
        searchField.setPreferredSize(new Dimension(200, 36));
        searchField.putClientProperty("JTextField.placeholderText", "Search category...");
        searchField.addActionListener(e -> filterCategories());

        JButton addBtn = new JButton("+  Add Category");
        addBtn.setFont(Fonts.TITLE_SM);
        addBtn.setForeground(Colors.ON_PRIMARY);
        addBtn.setBackground(Colors.PRIMARY);
        addBtn.setPreferredSize(new Dimension(160, 36));
        addBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addBtn.putClientProperty("JButton.buttonType", "roundRect");
        addBtn.putClientProperty("Component.arc", 8);
        addBtn.addActionListener(e -> showCategoryDialog(null));

        actions.add(searchField);
        actions.add(addBtn);

        topPanel.add(headerText, BorderLayout.WEST);
        topPanel.add(actions, BorderLayout.EAST);

        content.add(topPanel, BorderLayout.NORTH);

        // Category Table Card
        JPanel tableCard = new JPanel(new BorderLayout());
        tableCard.setBackground(Colors.SURFACE_CONTAINER);
        tableCard.setBorder(new EmptyBorder(16, 16, 16, 16));

        String[] cols = {"ID", "Category Name", "Description", "Created Date", "Actions"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
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

        // Action Buttons below table
        JPanel tableFooter = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 12));
        tableFooter.setOpaque(false);

        JButton editBtn = new JButton("Edit Selected");
        editBtn.setFont(Fonts.BODY_SMALL);
        editBtn.addActionListener(e -> editSelected());

        JButton deleteBtn = new JButton("Delete Selected");
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
            categoryList = categoryDAO.findByBusiness(bId);
            filterCategories();
        } catch (SQLException e) {
            System.err.println("Categories refresh error: " + e.getMessage());
        }
    }

    private void filterCategories() {
        String query = searchField.getText().trim().toLowerCase();
        tableModel.setRowCount(0);
        for (Category c : categoryList) {
            if (query.isEmpty() || c.getName().toLowerCase().contains(query) || (c.getDescription() != null && c.getDescription().toLowerCase().contains(query))) {
                tableModel.addRow(new Object[]{
                        c.getId(),
                        c.getName(),
                        c.getDescription() != null ? c.getDescription() : "",
                        c.getCreatedAt() != null ? c.getCreatedAt().toString().substring(0, 10) : "",
                        "Edit / Delete"
                });
            }
        }
    }

    private void editSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a category to edit.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int catId = (int) tableModel.getValueAt(row, 0);
        Category c = categoryList.stream().filter(x -> x.getId() == catId).findFirst().orElse(null);
        if (c != null) {
            showCategoryDialog(c);
        }
    }

    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a category to delete.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int catId = (int) tableModel.getValueAt(row, 0);
        String name = (String) tableModel.getValueAt(row, 1);

        try {
            if (categoryDAO.hasAssociatedProducts(catId)) {
                JOptionPane.showMessageDialog(this, "Cannot delete category '" + name + "' because products are assigned to it.", "Deletion Blocked", JOptionPane.ERROR_MESSAGE);
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete category '" + name + "'?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                categoryDAO.delete(catId);
                refreshData();
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error deleting category: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showCategoryDialog(Category categoryToEdit) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), categoryToEdit == null ? "Add Category" : "Edit Category", true);
        dialog.setSize(450, 320);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Colors.SURFACE_CONTAINER);
        panel.setBorder(new EmptyBorder(20, 24, 20, 24));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel nameLbl = new JLabel("Category Name:");
        nameLbl.setFont(Fonts.BODY);
        nameLbl.setForeground(Colors.ON_SURFACE);

        JTextField nameField = new JTextField(20);
        nameField.setFont(Fonts.BODY);
        if (categoryToEdit != null) nameField.setText(categoryToEdit.getName());

        JLabel descLbl = new JLabel("Description:");
        descLbl.setFont(Fonts.BODY);
        descLbl.setForeground(Colors.ON_SURFACE);

        JTextArea descArea = new JTextArea(3, 20);
        descArea.setFont(Fonts.BODY);
        if (categoryToEdit != null) descArea.setText(categoryToEdit.getDescription());

        gbc.gridx = 0; gbc.gridy = 0; panel.add(nameLbl, gbc);
        gbc.gridx = 1; panel.add(nameField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; panel.add(descLbl, gbc);
        gbc.gridx = 1; panel.add(new JScrollPane(descArea), gbc);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        btnPanel.setBackground(Colors.SURFACE_CONTAINER);

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dialog.dispose());

        JButton saveBtn = new JButton("Save Category");
        saveBtn.setFont(Fonts.BODY_SMALL);
        saveBtn.setForeground(Colors.ON_PRIMARY);
        saveBtn.setBackground(Colors.PRIMARY);
        saveBtn.addActionListener(e -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Category name is required.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                if (categoryToEdit == null) {
                    Category c = new Category();
                    c.setBusinessId(UserSession.getBusinessId());
                    c.setName(name);
                    c.setDescription(descArea.getText().trim());
                    categoryDAO.create(c);
                } else {
                    categoryToEdit.setName(name);
                    categoryToEdit.setDescription(descArea.getText().trim());
                    categoryDAO.update(categoryToEdit);
                }
                dialog.dispose();
                refreshData();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(dialog, "Error saving category: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnPanel.add(cancelBtn);
        btnPanel.add(saveBtn);

        dialog.add(panel, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        com.businessmanager.theme.UiStyles.apply(dialog);
        dialog.setVisible(true);
    }
}
