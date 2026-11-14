package com.businessmanager.ui.pos;

import com.businessmanager.components.StitchIcon;
import com.businessmanager.dao.CustomerDAO;
import com.businessmanager.dao.SaleDAO;
import com.businessmanager.dao.BranchDAO;
import com.businessmanager.enums.PaymentStatus;
import com.businessmanager.model.Sale;
import com.businessmanager.model.SaleItem;
import com.businessmanager.enums.PaymentMethod;
import com.businessmanager.model.Customer;
import com.businessmanager.model.Product;
import com.businessmanager.model.PosCalculationResult;
import com.businessmanager.security.UserSession;
import com.businessmanager.service.ProductService;
import com.businessmanager.service.PosCalculationEngine;
import com.businessmanager.service.CouponService;
import com.businessmanager.theme.Colors;
import com.businessmanager.theme.Fonts;
import com.businessmanager.utils.InvoicePrinter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Modern, responsive Point of Sale (POS) Billing Panel matching Stitch design specs.
 * Includes central PosCalculationEngine, coupon/offer validation, GST billing, and live change calculation.
 */
public class POSBillingPanel extends JPanel {

    private final ProductService productService = new ProductService();
    private final BranchDAO branchDAO = new BranchDAO();
    private final CustomerDAO customerDAO = new CustomerDAO();
    private final SaleDAO saleDAO = new SaleDAO();
    private final PosCalculationEngine calculationEngine = new PosCalculationEngine();
    private final CouponService couponService = new CouponService();

    private List<Product> catalogProducts = new ArrayList<>();
    private final List<CartItem> cartList = new ArrayList<>();

    private JTextField searchProductField;
    private JPanel catalogGridPanel;
    private JPanel categoryPills;
    private Integer selectedCategoryId;
    private Integer activeBranchId;

    private DefaultTableModel cartTableModel;
    private JTable cartTable;

    // Billing Breakdown UI Controls
    private JLabel subtotalLbl;
    private JLabel discountLbl;
    private JLabel taxableLbl;
    private JLabel gstLbl;
    private JLabel totalLbl;
    private JLabel changeLbl;
    private JLabel warningLbl;

    private JComboBox<Customer> customerCombo;
    private JTextField couponField;
    private JLabel couponStatusLbl;
    private String appliedCouponCode = null;

    private JCheckBox gstCheckBox;
    private JComboBox<String> gstRateCombo;

    private JTextField paymentField;
    private JButton payExactBtn;

    private PosCalculationResult lastCalculation = new PosCalculationResult();

    public POSBillingPanel() {
        setLayout(new BorderLayout());
        setBackground(Colors.BACKGROUND);
        setBorder(new EmptyBorder(16, 20, 16, 20));

        buildUI();
        refreshData();
    }

    private static class CartItem {
        Product product;
        int quantity;
        BigDecimal unitPrice;

        CartItem(Product product, int quantity) {
            this.product = product;
            this.quantity = quantity;
            this.unitPrice = product.getSellingPrice();
        }

        BigDecimal getSubtotal() {
            return unitPrice.multiply(BigDecimal.valueOf(quantity));
        }

        SaleItem toSaleItem() {
            SaleItem item = new SaleItem();
            item.setProductId(product.getId());
            item.setProductName(product.getName());
            item.setProductSku(product.getSku());
            item.setUnitPrice(unitPrice);
            item.setQuantity(quantity);
            item.setDiscount(BigDecimal.ZERO);
            item.setTax(BigDecimal.ZERO);
            item.setLineTotal(getSubtotal());
            return item;
        }
    }

    private void buildUI() {
        JPanel main = new JPanel(new BorderLayout(0, 14));
        main.setOpaque(false);

        // 1. Top Header Bar
        main.add(createHeader(), BorderLayout.NORTH);

        // 2. Split Panel (Left Catalog 58%, Right Cart 42%)
        JPanel split = new JPanel(new GridBagLayout());
        split.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.58; gbc.insets = new Insets(0, 0, 0, 14);
        split.add(createCatalogSide(), gbc);

        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 0.42; gbc.insets = new Insets(0, 0, 0, 0);
        split.add(createCheckoutSide(), gbc);

        main.add(split, BorderLayout.CENTER);
        add(main, BorderLayout.CENTER);
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);

        JLabel title = new JLabel("Sales Terminal");
        title.setFont(Fonts.DISPLAY_LG);
        title.setForeground(Colors.ON_SURFACE);

        JLabel sub = new JLabel("Fast billing, offers, GST tax controls, and instant checkout.");
        sub.setFont(Fonts.BODY);
        sub.setForeground(Colors.ON_SURFACE_VARIANT);

        left.add(title);
        left.add(Box.createVerticalStrut(4));
        left.add(sub);

        JPanel terminalStatus = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        terminalStatus.setBackground(Colors.SURFACE_CONTAINER_HIGH);
        terminalStatus.setBorder(new EmptyBorder(4, 12, 4, 12));

        JLabel dot = new JLabel("● ");
        dot.setForeground(Colors.SECONDARY);
        JLabel statusText = new JLabel("Terminal #01 Active");
        statusText.setFont(Fonts.LABEL_CAPS);
        statusText.setForeground(Colors.ON_SURFACE);

        terminalStatus.add(dot);
        terminalStatus.add(statusText);

        header.add(left, BorderLayout.WEST);
        header.add(terminalStatus, BorderLayout.EAST);
        return header;
    }

    private JPanel createCatalogSide() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setOpaque(false);

        // Search Bar & Filter Pills
        JPanel top = new JPanel(new BorderLayout(10, 8));
        top.setOpaque(false);

        searchProductField = new JTextField();
        searchProductField.setPreferredSize(new Dimension(320, 38));
        searchProductField.putClientProperty("JTextField.placeholderText", "Search product name, SKU, or scan barcode...");
        searchProductField.setFont(Fonts.BODY);

        categoryPills = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        categoryPills.setOpaque(false);

        top.add(searchProductField, BorderLayout.NORTH);
        top.add(categoryPills, BorderLayout.SOUTH);

        panel.add(top, BorderLayout.NORTH);

        // Product Grid inside responsive wrapping container
        catalogGridPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 12));
        catalogGridPanel.setOpaque(false);

        populateCatalog();
        searchProductField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { populateCatalog(); }
            @Override public void removeUpdate(DocumentEvent e) { populateCatalog(); }
            @Override public void changedUpdate(DocumentEvent e) { populateCatalog(); }
        });

        JScrollPane scroll = new JScrollPane(catalogGridPanel);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private void rebuildCategoryPills() {
        if (categoryPills == null) return;
        categoryPills.removeAll();
        categoryPills.add(createCatPill("All", null));
        Map<Integer, String> categories = new LinkedHashMap<>();
        for (Product product : catalogProducts) {
            if (product.getCategoryId() != null && product.getCategoryName() != null && !product.getCategoryName().isBlank()) {
                categories.putIfAbsent(product.getCategoryId(), product.getCategoryName());
            }
        }
        categories.forEach((id, name) -> categoryPills.add(createCatPill(name, id)));
        categoryPills.revalidate();
        categoryPills.repaint();
    }

    private JButton createCatPill(String text, Integer categoryId) {
        boolean active = java.util.Objects.equals(selectedCategoryId, categoryId);
        JButton btn = new JButton(text);
        btn.setFont(Fonts.LABEL_CAPS);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(6, 12, 6, 12));
        btn.putClientProperty("JButton.buttonType", "roundRect");
        if (active) {
            btn.setBackground(Colors.MARIGOLD);
            btn.setForeground(Colors.WHITE);
        } else {
            btn.setBackground(Colors.INK_2);
            btn.setForeground(Colors.SLATE);
        }
        btn.addActionListener(e -> {
            selectedCategoryId = categoryId;
            rebuildCategoryPills();
            populateCatalog();
        });
        return btn;
    }

    private void populateCatalog() {
        catalogGridPanel.removeAll();
        String query = searchProductField == null ? "" : searchProductField.getText().trim().toLowerCase();
        int matches = 0;
        for (Product p : catalogProducts) {
            boolean categoryMatches = selectedCategoryId == null || java.util.Objects.equals(selectedCategoryId, p.getCategoryId());
            if (categoryMatches && (query.isEmpty() || p.getName().toLowerCase().contains(query) || (p.getSku() != null && p.getSku().toLowerCase().contains(query)))) {
                catalogGridPanel.add(createProductCard(p));
                matches++;
            }
        }
        if (matches == 0) catalogGridPanel.add(createCatalogEmptyState(query));
        catalogGridPanel.revalidate();
        catalogGridPanel.repaint();
    }

    private JPanel createCatalogEmptyState(String query) {
        JPanel empty = new JPanel();
        empty.setOpaque(false);
        empty.setLayout(new BoxLayout(empty, BoxLayout.Y_AXIS));
        empty.setBorder(new EmptyBorder(48, 24, 48, 24));
        JLabel icon = new JLabel(new StitchIcon(StitchIcon.Type.STORE));
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel title = new JLabel(query.isEmpty() ? "No products available for this branch." : "No matching products found");
        title.setFont(Fonts.TITLE_SM);
        title.setForeground(Colors.ON_SURFACE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel hint = new JLabel(query.isEmpty()
                ? "Add active products with branch inventory to start billing."
                : "Try searching by name, SKU, or category.");
        hint.setFont(Fonts.BODY);
        hint.setForeground(Colors.ON_SURFACE_VARIANT);
        hint.setAlignmentX(Component.CENTER_ALIGNMENT);
        JButton refresh = new JButton("Refresh catalog");
        refresh.setFont(Fonts.LABEL_CAPS);
        refresh.setAlignmentX(Component.CENTER_ALIGNMENT);
        refresh.addActionListener(e -> refreshData());
        empty.add(icon);
        empty.add(Box.createVerticalStrut(12));
        empty.add(title);
        empty.add(Box.createVerticalStrut(6));
        empty.add(hint);
        empty.add(Box.createVerticalStrut(16));
        empty.add(refresh);
        return empty;
    }

    private JPanel createProductCard(Product product) {
        JPanel card = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Colors.SURFACE_CONTAINER);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(10, 12, 10, 12));
        card.setPreferredSize(new Dimension(200, 135));

        // Top Row: Icon + SKU
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JLabel iconLbl = new JLabel(new StitchIcon(StitchIcon.Type.STORE));
        JLabel skuLbl = new JLabel(product.getSku() != null ? product.getSku() : "NO-SKU");
        skuLbl.setFont(Fonts.LABEL_CAPS);
        skuLbl.setForeground(Colors.ON_SURFACE_VARIANT);
        top.add(iconLbl, BorderLayout.WEST);
        top.add(skuLbl, BorderLayout.EAST);

        // Center: Product Name
        JLabel nameLbl = new JLabel(product.getName());
        nameLbl.setFont(Fonts.TITLE_SM);
        nameLbl.setForeground(Colors.ON_SURFACE);

        // Bottom Row: Price + Stock (Left) & Add Button (Right)
        JPanel bottomLeft = new JPanel();
        bottomLeft.setLayout(new BoxLayout(bottomLeft, BoxLayout.Y_AXIS));
        bottomLeft.setOpaque(false);

        JLabel priceLbl = new JLabel("₹" + product.getSellingPrice().setScale(2, RoundingMode.HALF_UP));
        priceLbl.setFont(Fonts.TITLE_SM);
        priceLbl.setForeground(Colors.PRIMARY);

        boolean inStock = product.getStockQuantity() > 0;
        JLabel stockLbl = new JLabel(inStock ? "Stock: " + product.getStockQuantity() : "Out of Stock");
        stockLbl.setFont(Fonts.BODY_SMALL);
        stockLbl.setForeground(inStock ? Colors.ON_SURFACE_VARIANT : Colors.ERROR);

        bottomLeft.add(priceLbl);
        bottomLeft.add(stockLbl);

        JButton addBtn = new JButton(inStock ? "+ Add" : "Out of Stock");
        addBtn.setFont(Fonts.LABEL_CAPS);
        addBtn.setFocusPainted(false);
        addBtn.setEnabled(inStock);
        if (inStock) {
            addBtn.setForeground(Colors.ON_PRIMARY);
            addBtn.setBackground(Colors.PRIMARY);
            addBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addBtn.addActionListener(e -> addToCart(product));
        } else {
            addBtn.setForeground(Colors.ON_SURFACE_VARIANT);
            addBtn.setBackground(Colors.SURFACE_CONTAINER_HIGH);
        }

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(bottomLeft, BorderLayout.WEST);
        bottom.add(addBtn, BorderLayout.EAST);

        card.add(top, BorderLayout.NORTH);
        card.add(nameLbl, BorderLayout.CENTER);
        card.add(bottom, BorderLayout.SOUTH);

        return card;
    }

    private JPanel createCheckoutSide() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(Colors.SURFACE_CONTAINER);
        panel.setBorder(new EmptyBorder(14, 16, 14, 16));

        // Top: Header & Customer Selector
        JPanel top = new JPanel(new BorderLayout(8, 0));
        top.setOpaque(false);

        JLabel title = new JLabel("Current Bill");
        title.setFont(Fonts.TITLE_SM);
        title.setForeground(Colors.ON_SURFACE);

        customerCombo = new JComboBox<>();
        customerCombo.setPreferredSize(new Dimension(210, 32));

        top.add(title, BorderLayout.WEST);
        top.add(customerCombo, BorderLayout.EAST);

        panel.add(top, BorderLayout.NORTH);

        // Center: Cart Table + Actions
        String[] cols = {"Item Name", "Price", "Qty", "Subtotal"};
        cartTableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        cartTable = new JTable(cartTableModel);
        cartTable.setBackground(Colors.SURFACE_CONTAINER);
        cartTable.setForeground(Colors.ON_SURFACE);
        cartTable.setRowHeight(34);
        cartTable.getTableHeader().setBackground(Colors.SURFACE_CONTAINER_HIGH);
        cartTable.getTableHeader().setForeground(Colors.ON_SURFACE_VARIANT);
        cartTable.getTableHeader().setFont(Fonts.LABEL_CAPS);
        cartTable.setShowGrid(false);

        JPanel cartArea = new JPanel(new BorderLayout(0, 6));
        cartArea.setOpaque(false);
        cartArea.add(new JScrollPane(cartTable), BorderLayout.CENTER);

        JPanel cartActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        cartActions.setOpaque(false);
        JButton decrease = new JButton("− Qty");
        JButton increase = new JButton("+ Qty");
        JButton remove = new JButton("Remove item");
        JButton clear = new JButton("Clear Cart");

        decrease.setFont(Fonts.LABEL_CAPS);
        increase.setFont(Fonts.LABEL_CAPS);
        remove.setFont(Fonts.LABEL_CAPS);
        clear.setFont(Fonts.LABEL_CAPS);

        decrease.addActionListener(e -> changeSelectedQuantity(-1));
        increase.addActionListener(e -> changeSelectedQuantity(1));
        remove.addActionListener(e -> removeSelectedItem());
        clear.addActionListener(e -> clearCart());

        cartActions.add(decrease);
        cartActions.add(increase);
        cartActions.add(remove);
        cartActions.add(clear);

        cartArea.add(cartActions, BorderLayout.SOUTH);
        panel.add(cartArea, BorderLayout.CENTER);

        // Bottom: Offer Coupon + GST Tax + Totals Breakdown + Payment Controls
        JPanel bottom = new JPanel();
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        bottom.setOpaque(false);

        // Coupon Row
        JPanel couponPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        couponPanel.setOpaque(false);

        JLabel couponLbl = new JLabel("Coupon:");
        couponLbl.setFont(Fonts.BODY_SMALL);
        couponField = new JTextField(10);
        couponField.setFont(Fonts.BODY_SMALL);
        couponField.putClientProperty("JTextField.placeholderText", "WELCOME10 / FLAT500");

        JButton applyCouponBtn = new JButton("Apply");
        applyCouponBtn.setFont(Fonts.LABEL_CAPS);
        applyCouponBtn.addActionListener(e -> applyCoupon());

        JButton removeCouponBtn = new JButton("Remove");
        removeCouponBtn.setFont(Fonts.LABEL_CAPS);
        removeCouponBtn.addActionListener(e -> removeCoupon());

        couponStatusLbl = new JLabel("Offer: None");
        couponStatusLbl.setFont(Fonts.BODY_SMALL);
        couponStatusLbl.setForeground(Colors.SECONDARY);

        couponPanel.add(couponLbl);
        couponPanel.add(couponField);
        couponPanel.add(applyCouponBtn);
        couponPanel.add(removeCouponBtn);
        couponPanel.add(couponStatusLbl);

        // GST Row
        JPanel gstPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        gstPanel.setOpaque(false);

        gstCheckBox = new JCheckBox("Apply GST");
        gstCheckBox.setFont(Fonts.BODY_SMALL);
        gstCheckBox.setOpaque(false);
        gstCheckBox.setSelected(true);

        JLabel gstRateLbl = new JLabel("Rate:");
        gstRateLbl.setFont(Fonts.BODY_SMALL);

        gstRateCombo = new JComboBox<>(new String[]{"0%", "5%", "12%", "18%", "28%"});
        gstRateCombo.setSelectedItem("18%");
        gstRateCombo.setFont(Fonts.BODY_SMALL);

        gstCheckBox.addActionListener(e -> refreshTotals());
        gstRateCombo.addActionListener(e -> refreshTotals());

        gstPanel.add(gstCheckBox);
        gstPanel.add(gstRateLbl);
        gstPanel.add(gstRateCombo);

        // Totals Breakdown Labels
        subtotalLbl = new JLabel("Subtotal: ₹0.00");
        subtotalLbl.setFont(Fonts.BODY);

        discountLbl = new JLabel("Discount: ₹0.00");
        discountLbl.setFont(Fonts.BODY);
        discountLbl.setForeground(Colors.SECONDARY);

        taxableLbl = new JLabel("Taxable Amount: ₹0.00");
        taxableLbl.setFont(Fonts.BODY);

        gstLbl = new JLabel("GST (18%): ₹0.00");
        gstLbl.setFont(Fonts.BODY);

        totalLbl = new JLabel("Grand Total: ₹0.00");
        totalLbl.setFont(Fonts.HEADLINE_MD);
        totalLbl.setForeground(Colors.PRIMARY);

        // Payment & Change Fields
        JPanel payRow = new JPanel(new BorderLayout(8, 0));
        payRow.setOpaque(false);

        JLabel payLabel = new JLabel("Payment Received (₹):");
        payLabel.setFont(Fonts.BODY);

        paymentField = new JTextField();
        paymentField.setFont(Fonts.BODY);
        paymentField.setPreferredSize(new Dimension(140, 32));

        payExactBtn = new JButton("Pay Exact Amount");
        payExactBtn.setFont(Fonts.LABEL_CAPS);
        payExactBtn.addActionListener(e -> {
            paymentField.setText(lastCalculation.getGrandTotal().setScale(2, RoundingMode.HALF_UP).toString());
            refreshTotals();
        });

        payRow.add(payLabel, BorderLayout.WEST);
        payRow.add(paymentField, BorderLayout.CENTER);
        payRow.add(payExactBtn, BorderLayout.EAST);

        changeLbl = new JLabel("Change: ₹0.00");
        changeLbl.setFont(Fonts.TITLE_SM);
        changeLbl.setForeground(Colors.SECONDARY);

        warningLbl = new JLabel(" ");
        warningLbl.setFont(Fonts.BODY_SMALL);
        warningLbl.setForeground(Colors.ERROR);

        paymentField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { refreshTotals(); }
            @Override public void removeUpdate(DocumentEvent e) { refreshTotals(); }
            @Override public void changedUpdate(DocumentEvent e) { refreshTotals(); }
        });

        // Checkout Action Row
        JPanel btnRow = new JPanel(new GridLayout(1, 1, 0, 0));
        btnRow.setOpaque(false);

        JButton payBtn = new JButton("Pay & Print Invoice →");
        payBtn.setFont(Fonts.TITLE_SM);
        payBtn.setForeground(Colors.ON_PRIMARY);
        payBtn.setBackground(Colors.PRIMARY);
        payBtn.setFocusPainted(false);
        payBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        payBtn.setPreferredSize(new Dimension(200, 42));
        payBtn.addActionListener(e -> completeSale());

        btnRow.add(payBtn);

        bottom.add(couponPanel);
        bottom.add(gstPanel);
        bottom.add(Box.createVerticalStrut(4));
        bottom.add(subtotalLbl);
        bottom.add(discountLbl);
        bottom.add(taxableLbl);
        bottom.add(gstLbl);
        bottom.add(Box.createVerticalStrut(4));
        bottom.add(totalLbl);
        bottom.add(Box.createVerticalStrut(6));
        bottom.add(payRow);
        bottom.add(Box.createVerticalStrut(4));
        bottom.add(changeLbl);
        bottom.add(warningLbl);
        bottom.add(Box.createVerticalStrut(8));
        bottom.add(btnRow);

        panel.add(bottom, BorderLayout.SOUTH);

        return panel;
    }

    public void refreshData() {
        int bId = UserSession.getBusinessId();
        Integer branchId = UserSession.getBranchId();
        try {
            if (branchId != null && branchId > 0) {
                activeBranchId = branchId;
            } else {
                com.businessmanager.model.Branch mainBranch = branchDAO.findMainBranch(bId);
                if (mainBranch != null && mainBranch.getId() > 0) {
                    activeBranchId = mainBranch.getId();
                } else {
                    activeBranchId = null;
                }
            }
            catalogProducts = activeBranchId == null ? new ArrayList<>() : productService.getProductsForBranch(bId, activeBranchId);
            if (customerCombo != null) {
                customerCombo.removeAllItems();
                customerCombo.addItem(null); // Walk-in Customer
                for (Customer customer : customerDAO.findByBusiness(bId)) {
                    customerCombo.addItem(customer);
                }
            }
            rebuildCategoryPills();
            populateCatalog();
        } catch (Exception e) {
            catalogProducts = new ArrayList<>();
            rebuildCategoryPills();
            populateCatalog();
            System.err.println("POS refresh error: " + e.getMessage());
        }
    }

    private void applyCoupon() {
        String code = couponField.getText().trim();
        if (code.isEmpty()) {
            showWarning("Enter a coupon code first.");
            return;
        }
        com.businessmanager.model.Coupon coupon = couponService.findCoupon(code);
        if (coupon == null || !coupon.isActive()) {
            showWarning("Invalid or expired coupon code.");
            return;
        }
        BigDecimal currentSubtotal = cartList.stream().map(CartItem::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (currentSubtotal.compareTo(coupon.getMinPurchase()) < 0) {
            showWarning("Minimum purchase of ₹" + coupon.getMinPurchase() + " required for offer " + coupon.getCode() + ".");
            return;
        }
        appliedCouponCode = coupon.getCode();
        couponStatusLbl.setText("Offer: " + coupon.getCode());
        refreshTotals();
    }

    private void removeCoupon() {
        appliedCouponCode = null;
        couponField.setText("");
        couponStatusLbl.setText("Offer: None");
        refreshTotals();
    }

    private void addToCart(Product product) {
        CartItem existing = cartList.stream().filter(item -> item.product.getId() == product.getId()).findFirst().orElse(null);
        if (existing == null) {
            if (product.getStockQuantity() <= 0) {
                showWarning("Insufficient stock for " + product.getName() + ". Available: 0");
                return;
            }
            cartList.add(new CartItem(product, 1));
        } else if (existing.quantity < product.getStockQuantity()) {
            existing.quantity++;
        } else {
            showWarning("Insufficient stock for " + product.getName() + ". Available: " + product.getStockQuantity());
            return;
        }
        refreshCart();
    }

    private void changeSelectedQuantity(int delta) {
        int row = cartTable.getSelectedRow();
        if (row < 0) { showWarning("Select a cart item first."); return; }
        CartItem item = cartList.get(row);
        int quantity = item.quantity + delta;
        if (quantity <= 0) {
            cartList.remove(row);
        } else if (quantity > item.product.getStockQuantity()) {
            showWarning("Insufficient stock for " + item.product.getName() + ". Available: " + item.product.getStockQuantity());
            return;
        } else {
            item.quantity = quantity;
        }
        refreshCart();
    }

    private void removeSelectedItem() {
        int row = cartTable.getSelectedRow();
        if (row < 0) { showWarning("Select a cart item first."); return; }
        cartList.remove(row);
        refreshCart();
    }

    private void clearCart() {
        cartList.clear();
        paymentField.setText("");
        appliedCouponCode = null;
        couponField.setText("");
        couponStatusLbl.setText("Offer: None");
        refreshCart();
    }

    private void refreshCart() {
        cartTableModel.setRowCount(0);
        for (CartItem item : cartList) {
            cartTableModel.addRow(new Object[]{
                    item.product.getName(),
                    "₹" + item.unitPrice.setScale(2, RoundingMode.HALF_UP),
                    item.quantity,
                    "₹" + item.getSubtotal().setScale(2, RoundingMode.HALF_UP)
            });
        }
        refreshTotals();
    }

    private BigDecimal getSelectedGstRate() {
        if (gstRateCombo == null || gstRateCombo.getSelectedItem() == null) return new BigDecimal("18.00");
        String sel = gstRateCombo.getSelectedItem().toString().replace("%", "").trim();
        try { return new BigDecimal(sel); } catch (Exception ex) { return new BigDecimal("18.00"); }
    }

    private BigDecimal enteredPayment() {
        String raw = paymentField == null ? "" : paymentField.getText().trim().replace(",", "").replace("₹", "");
        if (raw.isEmpty()) return BigDecimal.ZERO;
        try { return new BigDecimal(raw); } catch (NumberFormatException ignored) { return BigDecimal.ZERO; }
    }

    private void refreshTotals() {
        List<SaleItem> saleItems = cartList.stream().map(CartItem::toSaleItem).toList();
        boolean gstEnabled = gstCheckBox != null && gstCheckBox.isSelected();
        BigDecimal gstRate = getSelectedGstRate();
        BigDecimal payment = enteredPayment();

        lastCalculation = calculationEngine.calculate(saleItems, appliedCouponCode, gstEnabled, gstRate, payment);

        subtotalLbl.setText("Subtotal: ₹" + lastCalculation.getSubtotal().setScale(2, RoundingMode.HALF_UP));
        discountLbl.setText("Discount: -₹" + lastCalculation.getTotalDiscount().setScale(2, RoundingMode.HALF_UP));
        taxableLbl.setText("Taxable Amount: ₹" + lastCalculation.getTaxableAmount().setScale(2, RoundingMode.HALF_UP));

        String rateText = gstEnabled ? "GST (" + gstRate.setScale(0, RoundingMode.HALF_UP) + "%): ₹" : "GST (Disabled): ₹";
        gstLbl.setText(rateText + lastCalculation.getGstAmount().setScale(2, RoundingMode.HALF_UP));

        totalLbl.setText("Grand Total: ₹" + lastCalculation.getGrandTotal().setScale(2, RoundingMode.HALF_UP));
        changeLbl.setText("Change: ₹" + lastCalculation.getChangeAmount().setScale(2, RoundingMode.HALF_UP));

        if (!cartList.isEmpty() && payment.signum() > 0 && payment.compareTo(lastCalculation.getGrandTotal()) < 0) {
            warningLbl.setText("Payment amount is less than the total.");
        } else {
            warningLbl.setText(" ");
        }
    }

    private void completeSale() {
        if (cartList.isEmpty()) {
            showWarning("Add at least one product to the cart before checkout.");
            return;
        }
        BigDecimal paid = enteredPayment();
        if (paid.compareTo(lastCalculation.getGrandTotal()) < 0) {
            showWarning("Payment amount is less than the total. Paid: ₹" + paid + ", Required: ₹" + lastCalculation.getGrandTotal());
            return;
        }
        if (activeBranchId == null || activeBranchId <= 0) {
            JOptionPane.showMessageDialog(this, "No active branch is available for this sale.", "POS Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            Sale sale = new Sale();
            sale.setBusinessId(UserSession.getBusinessId());
            sale.setBranchId(activeBranchId);
            sale.setUserId(UserSession.getUserId());

            Customer customer = (Customer) customerCombo.getSelectedItem();
            if (customer != null) sale.setCustomerId(customer.getId());

            sale.setInvoiceNumber("INV-" + System.currentTimeMillis());
            sale.setSubtotal(lastCalculation.getSubtotal());
            sale.setDiscount(lastCalculation.getTotalDiscount());
            sale.setTax(lastCalculation.getGstAmount());
            sale.setTotal(lastCalculation.getGrandTotal());
            sale.setPaidAmount(lastCalculation.getGrandTotal()); // Sale paid amount equals grand total settled
            sale.setChangeAmount(lastCalculation.getChangeAmount());
            sale.setPaymentMethod(PaymentMethod.CASH);
            sale.setPaymentStatus(PaymentStatus.PAID);
            sale.setCouponCode(lastCalculation.getCouponCode());
            sale.setGstRate(lastCalculation.isGstEnabled() ? lastCalculation.getGstRate() : BigDecimal.ZERO);

            for (CartItem cart : cartList) {
                sale.getItems().add(cart.toSaleItem());
            }

            if (!saleDAO.createSale(sale)) {
                throw new IllegalArgumentException("Sale could not be completed in database.");
            }

            // Print / Preview Invoice
            InvoicePrinter.showPreview(this, sale, UserSession.getCurrentBusiness());

            clearCart();
            refreshData();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Checkout Failed: " + ex.getMessage(), "POS Billing Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showWarning(String message) {
        JOptionPane.showMessageDialog(this, message, "POS Billing", JOptionPane.WARNING_MESSAGE);
    }
}
