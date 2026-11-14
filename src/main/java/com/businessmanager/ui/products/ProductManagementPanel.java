package com.businessmanager.ui.products;

import com.businessmanager.dao.CategoryDAO;
import com.businessmanager.dao.ProductDAO;
import com.businessmanager.dao.SupplierDAO;
import com.businessmanager.model.Category;
import com.businessmanager.model.Product;
import com.businessmanager.model.Supplier;
import com.businessmanager.security.UserSession;
import com.businessmanager.theme.Colors;
import com.businessmanager.theme.Fonts;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
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

public class ProductManagementPanel extends JPanel {

    private final ProductDAO productDAO = new ProductDAO();
    private final CategoryDAO categoryDAO = new CategoryDAO();
    private final SupplierDAO supplierDAO = new SupplierDAO();

    private DefaultTableModel tableModel;
    private JTable table;
    private JTextField searchField;
    private JComboBox<String> categoryFilterCombo;
    private JComboBox<String> stockFilterCombo;

    private List<Product> masterProductList = new ArrayList<>();
    private List<Product> displayedProductList = new ArrayList<>();

    public ProductManagementPanel() {
        setLayout(new BorderLayout());
        setBackground(Colors.BACKGROUND);
        setBorder(new EmptyBorder(20, 24, 20, 24));

        buildUI();
        refreshData();
    }

    private void buildUI() {
        JPanel content = new JPanel(new BorderLayout(0, 16));
        content.setOpaque(false);

        // 1. Header
        content.add(createHeader(), BorderLayout.NORTH);

        // 2. Main Content Card with Search Bar & Table
        JPanel mainCard = new JPanel(new BorderLayout(0, 16));
        mainCard.setBackground(Colors.SURFACE_CONTAINER);
        mainCard.setBorder(new EmptyBorder(16, 20, 16, 20));

        mainCard.add(createFilterBar(), BorderLayout.NORTH);
        mainCard.add(createTableSection(), BorderLayout.CENTER);

        content.add(mainCard, BorderLayout.CENTER);
        add(content, BorderLayout.CENTER);
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);

        JLabel title = new JLabel("Product Management");
        title.setFont(Fonts.DISPLAY_LG);
        title.setForeground(Colors.ON_SURFACE);

        JLabel sub = new JLabel("Track stock levels, pricing, categories, and supplier sources.");
        sub.setFont(Fonts.BODY);
        sub.setForeground(Colors.ON_SURFACE_VARIANT);

        left.add(title);
        left.add(Box.createVerticalStrut(4));
        left.add(sub);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);

        JButton exportBtn = new JButton("Export CSV");
        exportBtn.setFont(Fonts.BODY_SMALL);
        exportBtn.setForeground(Colors.ON_SURFACE);
        exportBtn.setBackground(Colors.SURFACE_CONTAINER_HIGH);
        exportBtn.setFocusPainted(false);
        exportBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        exportBtn.putClientProperty("JButton.buttonType", "roundRect");
        exportBtn.addActionListener(e -> exportCsv());

        JButton addCategoryBtn = new JButton("+  Add Category");
        addCategoryBtn.setFont(Fonts.BODY_SMALL);
        addCategoryBtn.setForeground(Colors.ON_SURFACE);
        addCategoryBtn.setBackground(Colors.SURFACE_CONTAINER_HIGH);
        addCategoryBtn.setFocusPainted(false);
        addCategoryBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addCategoryBtn.putClientProperty("JButton.buttonType", "roundRect");
        addCategoryBtn.addActionListener(e -> showQuickCategoryDialog());

        JButton addBtn = new JButton("+  Add Product");
        addBtn.setFont(Fonts.TITLE_SM);
        addBtn.setForeground(Colors.ON_PRIMARY);
        addBtn.setBackground(Colors.PRIMARY);
        addBtn.setFocusPainted(false);
        addBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addBtn.putClientProperty("JButton.buttonType", "roundRect");
        addBtn.addActionListener(e -> showProductDialog(null));

        actions.add(exportBtn);
        actions.add(addCategoryBtn);
        actions.add(addBtn);

        header.add(left, BorderLayout.WEST);
        header.add(actions, BorderLayout.EAST);
        return header;
    }

    private JPanel createFilterBar() {
        JPanel bar = new JPanel(new BorderLayout(12, 0));
        bar.setOpaque(false);

        searchField = new JTextField();
        searchField.setPreferredSize(new Dimension(380, 38));
        searchField.putClientProperty("JTextField.placeholderText", "Search products by name, SKU, or category...");
        searchField.setFont(Fonts.BODY);
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { applyFilters(); }
            public void removeUpdate(DocumentEvent e) { applyFilters(); }
            public void changedUpdate(DocumentEvent e) { applyFilters(); }
        });

        JPanel dropdowns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        dropdowns.setOpaque(false);

        categoryFilterCombo = new JComboBox<>(new String[]{"All Categories"});
        categoryFilterCombo.setPreferredSize(new Dimension(160, 38));
        categoryFilterCombo.addActionListener(e -> applyFilters());

        stockFilterCombo = new JComboBox<>(new String[]{"All Stock", "In Stock", "Low Stock", "Out of Stock"});
        stockFilterCombo.setPreferredSize(new Dimension(140, 38));
        stockFilterCombo.addActionListener(e -> applyFilters());

        dropdowns.add(categoryFilterCombo);
        dropdowns.add(stockFilterCombo);

        bar.add(searchField, BorderLayout.CENTER);
        bar.add(dropdowns, BorderLayout.EAST);
        return bar;
    }

    private JComponent createTableSection() {
        String[] cols = {"PRODUCT NAME", "SKU", "CATEGORY", "STOCK LEVEL", "SELLING PRICE", "STATUS", "ACTIONS"};
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
                if (row >= 0 && row < displayedProductList.size()) {
                    Product p = displayedProductList.get(row);
                    if (e.getClickCount() == 2) {
                        showProductDialog(p);
                    } else if (table.getSelectedColumn() == 6) {
                        showActionMenu(e.getComponent(), e.getX(), e.getY(), p);
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

    private void showActionMenu(Component comp, int x, int y, Product p) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem editItem = new JMenuItem("Edit Product");
        editItem.addActionListener(e -> showProductDialog(p));

        JMenuItem deleteItem = new JMenuItem("Delete Product");
        deleteItem.addActionListener(e -> deleteProduct(p));

        menu.add(editItem);
        menu.add(deleteItem);
        menu.show(comp, x, y);
    }

    private void deleteProduct(Product p) {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete product '" + p.getName() + "'?",
                "Confirm Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                if (productDAO.delete(p.getId())) {
                    JOptionPane.showMessageDialog(this, "Product deleted successfully.", "Product Deleted", JOptionPane.INFORMATION_MESSAGE);
                    refreshData();
                } else {
                    JOptionPane.showMessageDialog(this, "Could not delete product.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showProductDialog(Product existingProduct) {
        int bId = UserSession.getBusinessId();
        boolean isEdit = existingProduct != null;

        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this) instanceof Frame f ? f : null,
                isEdit ? "Edit Product" : "Add Product", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(480, 620);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(20, 24, 20, 24));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 6, 6, 6);

        JTextField nameField = new JTextField(isEdit ? existingProduct.getName() : "");
        JTextField skuField = new JTextField(isEdit && existingProduct.getSku() != null ? existingProduct.getSku() : "");
        JTextField barcodeField = new JTextField(isEdit && existingProduct.getBarcode() != null ? existingProduct.getBarcode() : "");
        JTextField purchasePriceField = new JTextField(isEdit && existingProduct.getPurchasePrice() != null ? existingProduct.getPurchasePrice().toString() : "0.00");
        JTextField sellingPriceField = new JTextField(isEdit && existingProduct.getSellingPrice() != null ? existingProduct.getSellingPrice().toString() : "0.00");
        JSpinner stockSpinner = new JSpinner(new SpinnerNumberModel(isEdit ? existingProduct.getStockQuantity() : 10, 0, 1000000, 1));
        JSpinner minStockSpinner = new JSpinner(new SpinnerNumberModel(isEdit ? existingProduct.getMinStock() : 5, 0, 10000, 1));
        JComboBox<String> unitCombo = new JComboBox<>(new String[]{"pcs", "kg", "g", "liter", "meter", "box", "pack"});
        if (isEdit && existingProduct.getUnit() != null) unitCombo.setSelectedItem(existingProduct.getUnit());

        JComboBox<Category> catCombo = new JComboBox<>();
        catCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value == null) {
                    setText("-- Select Category (Optional) --");
                } else if (value instanceof Category c) {
                    setText(c.getName());
                }
                return this;
            }
        });
        catCombo.addItem(null);
        try {
            List<Category> categories = categoryDAO.findByBusiness(bId);
            for (Category c : categories) {
                catCombo.addItem(c);
                if (isEdit && existingProduct.getCategoryId() != null && existingProduct.getCategoryId() == c.getId()) {
                    catCombo.setSelectedItem(c);
                }
            }
        } catch (SQLException ignored) {}

        JPanel catPanel = new JPanel(new BorderLayout(6, 0));
        catPanel.setOpaque(false);
        catPanel.add(catCombo, BorderLayout.CENTER);

        JButton addCatBtn = new JButton("+ New");
        addCatBtn.setFont(Fonts.BODY_SMALL);
        addCatBtn.setToolTipText("Add a new category");
        addCatBtn.addActionListener(e -> {
            String newCatName = JOptionPane.showInputDialog(dialog, "Enter new Category Name:", "Add New Category", JOptionPane.QUESTION_MESSAGE);
            if (newCatName != null && !newCatName.trim().isEmpty()) {
                Category newCat = new Category();
                newCat.setBusinessId(bId);
                newCat.setName(newCatName.trim());
                try {
                    int newCatId = categoryDAO.create(newCat);
                    if (newCatId > 0) {
                        newCat.setId(newCatId);
                        catCombo.addItem(newCat);
                        catCombo.setSelectedItem(newCat);
                        refreshData();
                        JOptionPane.showMessageDialog(dialog, "Category '" + newCatName.trim() + "' created and selected!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(dialog, "Failed to create category: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        catPanel.add(addCatBtn, BorderLayout.EAST);

        JComboBox<Supplier> supCombo = new JComboBox<>();
        supCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value == null) {
                    setText("-- Select Supplier (Optional) --");
                } else if (value instanceof Supplier s) {
                    setText(s.getName());
                }
                return this;
            }
        });
        supCombo.addItem(null);
        try {
            List<Supplier> suppliers = supplierDAO.findByBusiness(bId);
            for (Supplier s : suppliers) {
                supCombo.addItem(s);
                if (isEdit && existingProduct.getSupplierId() != null && existingProduct.getSupplierId() == s.getId()) {
                    supCombo.setSelectedItem(s);
                }
            }
        } catch (SQLException ignored) {}

        JPanel supPanel = new JPanel(new BorderLayout(6, 0));
        supPanel.setOpaque(false);
        supPanel.add(supCombo, BorderLayout.CENTER);

        JButton addSupBtn = new JButton("+ New");
        addSupBtn.setFont(Fonts.BODY_SMALL);
        addSupBtn.setToolTipText("Add a new supplier");
        addSupBtn.addActionListener(e -> {
            String newSupName = JOptionPane.showInputDialog(dialog, "Enter new Supplier Name:", "Add New Supplier", JOptionPane.QUESTION_MESSAGE);
            if (newSupName != null && !newSupName.trim().isEmpty()) {
                Supplier newSup = new Supplier();
                newSup.setBusinessId(bId);
                newSup.setName(newSupName.trim());
                try {
                    int newSupId = supplierDAO.create(newSup);
                    if (newSupId > 0) {
                        newSup.setId(newSupId);
                        supCombo.addItem(newSup);
                        supCombo.setSelectedItem(newSup);
                        refreshData();
                        JOptionPane.showMessageDialog(dialog, "Supplier '" + newSupName.trim() + "' created and selected!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(dialog, "Failed to create supplier: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        supPanel.add(addSupBtn, BorderLayout.EAST);

        JCheckBox activeBox = new JCheckBox("Active Product", !isEdit || existingProduct.isActive());

        int row = 0;
        addFormRow(form, gbc, row++, "Product Name *:", nameField);
        addFormRow(form, gbc, row++, "SKU Code *:", skuField);
        addFormRow(form, gbc, row++, "Barcode:", barcodeField);
        addFormRow(form, gbc, row++, "Category:", catPanel);
        addFormRow(form, gbc, row++, "Supplier:", supPanel);
        addFormRow(form, gbc, row++, "Purchase Price (₹) *:", purchasePriceField);
        addFormRow(form, gbc, row++, "Selling Price (₹) *:", sellingPriceField);
        addFormRow(form, gbc, row++, "Stock Quantity *:", stockSpinner);
        addFormRow(form, gbc, row++, "Min Stock Level:", minStockSpinner);
        addFormRow(form, gbc, row++, "Unit:", unitCombo);
        addFormRow(form, gbc, row++, "Status:", activeBox);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dialog.dispose());

        JButton saveBtn = new JButton(isEdit ? "Update Product" : "Save Product");
        saveBtn.setFont(Fonts.TITLE_SM);
        saveBtn.setForeground(Colors.ON_PRIMARY);
        saveBtn.setBackground(Colors.PRIMARY);
        saveBtn.addActionListener(e -> {
            String name = nameField.getText().trim();
            String sku = skuField.getText().trim();
            if (name.isEmpty() || sku.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Product Name and SKU are required.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            BigDecimal purchasePrice;
            BigDecimal sellingPrice;
            try {
                purchasePrice = new BigDecimal(purchasePriceField.getText().trim());
                sellingPrice = new BigDecimal(sellingPriceField.getText().trim());
                if (purchasePrice.signum() < 0 || sellingPrice.signum() < 0) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Prices must be valid non-negative numbers.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                if (productDAO.existsBySku(bId, sku, isEdit ? existingProduct.getId() : null)) {
                    JOptionPane.showMessageDialog(dialog, "A product with SKU '" + sku + "' already exists.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                Product p = isEdit ? existingProduct : new Product();
                p.setBusinessId(bId);
                p.setName(name);
                p.setSku(sku);
                p.setBarcode(barcodeField.getText().trim());
                Category selectedCat = (Category) catCombo.getSelectedItem();
                p.setCategoryId(selectedCat != null ? selectedCat.getId() : null);
                Supplier selectedSup = (Supplier) supCombo.getSelectedItem();
                p.setSupplierId(selectedSup != null ? selectedSup.getId() : null);
                p.setPurchasePrice(purchasePrice);
                p.setSellingPrice(sellingPrice);
                p.setStockQuantity((Integer) stockSpinner.getValue());
                p.setMinStock((Integer) minStockSpinner.getValue());
                p.setUnit((String) unitCombo.getSelectedItem());
                p.setActive(activeBox.isSelected());

                if (isEdit) {
                    if (productDAO.update(p)) {
                        JOptionPane.showMessageDialog(dialog, "Product updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                        dialog.dispose();
                        refreshData();
                    } else {
                        JOptionPane.showMessageDialog(dialog, "Failed to update product.", "Database Error", JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    int id = productDAO.create(p);
                    if (id > 0) {
                        JOptionPane.showMessageDialog(dialog, "Product created successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                        dialog.dispose();
                        refreshData();
                    } else {
                        JOptionPane.showMessageDialog(dialog, "Failed to create product.", "Database Error", JOptionPane.ERROR_MESSAGE);
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
        String selectedCategory = (String) categoryFilterCombo.getSelectedItem();
        String selectedStock = (String) stockFilterCombo.getSelectedItem();

        displayedProductList = masterProductList.stream().filter(p -> {
            boolean matchesSearch = query.isEmpty()
                    || p.getName().toLowerCase().contains(query)
                    || (p.getSku() != null && p.getSku().toLowerCase().contains(query))
                    || (p.getCategoryName() != null && p.getCategoryName().toLowerCase().contains(query));

            boolean matchesCategory = selectedCategory == null || selectedCategory.equals("All Categories")
                    || (p.getCategoryName() != null && p.getCategoryName().equalsIgnoreCase(selectedCategory));

            boolean matchesStock = true;
            if ("In Stock".equalsIgnoreCase(selectedStock)) {
                matchesStock = p.getStockQuantity() > p.getMinStock();
            } else if ("Low Stock".equalsIgnoreCase(selectedStock)) {
                matchesStock = p.getStockQuantity() > 0 && p.getStockQuantity() <= p.getMinStock();
            } else if ("Out of Stock".equalsIgnoreCase(selectedStock)) {
                matchesStock = p.getStockQuantity() <= 0;
            }

            return matchesSearch && matchesCategory && matchesStock;
        }).toList();

        tableModel.setRowCount(0);
        for (Product p : displayedProductList) {
            String stockStatus;
            if (p.getStockQuantity() <= 0) stockStatus = "Out of Stock";
            else if (p.getStockQuantity() <= p.getMinStock()) stockStatus = p.getStockQuantity() + " Low Stock";
            else stockStatus = p.getStockQuantity() + " in stock";

            tableModel.addRow(new Object[]{
                    p.getName(),
                    p.getSku() != null ? p.getSku() : "SKU-" + p.getId(),
                    p.getCategoryName() != null ? p.getCategoryName() : "General",
                    stockStatus,
                    "₹" + (p.getSellingPrice() != null ? p.getSellingPrice().setScale(2) : "0.00"),
                    p.isActive() ? "ACTIVE" : "INACTIVE",
                    "Actions ⚙"
            });
        }
    }

    private void exportCsv() {
        if (displayedProductList.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No products to export.", "Export CSV", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("products_export.csv"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try (FileWriter writer = new FileWriter(file)) {
                writer.write("ID,Product Name,SKU,Category,Supplier,Purchase Price,Selling Price,Stock Quantity,Min Stock,Status\n");
                for (Product p : displayedProductList) {
                    writer.write(String.format("%d,\"%s\",\"%s\",\"%s\",\"%s\",%.2f,%.2f,%d,%d,%s\n",
                            p.getId(),
                            p.getName().replace("\"", "\"\""),
                            p.getSku() != null ? p.getSku() : "",
                            p.getCategoryName() != null ? p.getCategoryName() : "",
                            p.getSupplierName() != null ? p.getSupplierName() : "",
                            p.getPurchasePrice() != null ? p.getPurchasePrice() : BigDecimal.ZERO,
                            p.getSellingPrice() != null ? p.getSellingPrice() : BigDecimal.ZERO,
                            p.getStockQuantity(),
                            p.getMinStock(),
                            p.isActive() ? "ACTIVE" : "INACTIVE"
                    ));
                }
                JOptionPane.showMessageDialog(this, "Exported products to " + file.getAbsolutePath(), "CSV Exported", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to export CSV: " + ex.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void refreshData() {
        int bId = UserSession.getBusinessId();
        try {
            masterProductList = productDAO.findByBusiness(bId);

            // Refresh category filter dropdown options
            categoryFilterCombo.removeAllItems();
            categoryFilterCombo.addItem("All Categories");
            List<Category> categories = categoryDAO.findByBusiness(bId);
            for (Category c : categories) {
                categoryFilterCombo.addItem(c.getName());
            }

            applyFilters();
        } catch (Exception e) {
            System.err.println("Products refresh error: " + e.getMessage());
        }
    }

    private void showQuickCategoryDialog() {
        int bId = UserSession.getBusinessId();
        String name = JOptionPane.showInputDialog(this, "Enter new Category Name:", "Add Category", JOptionPane.QUESTION_MESSAGE);
        if (name != null && !name.trim().isEmpty()) {
            Category cat = new Category();
            cat.setBusinessId(bId);
            cat.setName(name.trim());
            try {
                int id = categoryDAO.create(cat);
                if (id > 0) {
                    JOptionPane.showMessageDialog(this, "Category '" + name.trim() + "' created successfully!", "Category Created", JOptionPane.INFORMATION_MESSAGE);
                    refreshData();
                } else {
                    JOptionPane.showMessageDialog(this, "Could not create category.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error creating category: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
