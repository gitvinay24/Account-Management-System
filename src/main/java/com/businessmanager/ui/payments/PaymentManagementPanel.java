package com.businessmanager.ui.payments;

import com.businessmanager.dao.PaymentDAO;
import com.businessmanager.config.DemoModeConfig;
import com.businessmanager.demo.DemoDataStore;
import com.businessmanager.enums.PaymentMethod;
import com.businessmanager.model.Purchase;
import com.businessmanager.model.Sale;
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

public class PaymentManagementPanel extends JPanel {

    private DefaultTableModel tableModel;
    private JTable table;
    private final PaymentDAO paymentDAO = new PaymentDAO();

    public PaymentManagementPanel() {
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

        JLabel title = new JLabel("Payments & Financial Ledger");
        title.setFont(Fonts.DISPLAY_LG);
        title.setForeground(Colors.ON_SURFACE);

        JLabel sub = new JLabel("Track customer receivables, supplier payables, and payment receipts");
        sub.setFont(Fonts.BODY);
        sub.setForeground(Colors.ON_SURFACE_VARIANT);

        headerText.add(title);
        headerText.add(Box.createVerticalStrut(4));
        headerText.add(sub);

        JButton receiveBtn = new JButton("+ Record Payment");
        receiveBtn.setFont(Fonts.BODY_SMALL);
        receiveBtn.setForeground(Colors.ON_PRIMARY);
        receiveBtn.setBackground(Colors.PRIMARY);
        receiveBtn.addActionListener(e -> showPaymentDialog());

        JButton refreshBtn = new JButton("🔄  Refresh Ledger");
        refreshBtn.setFont(Fonts.BODY_SMALL);
        refreshBtn.addActionListener(e -> refreshData());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(receiveBtn);
        actions.add(refreshBtn);
        topPanel.add(headerText, BorderLayout.WEST);
        topPanel.add(actions, BorderLayout.EAST);

        content.add(topPanel, BorderLayout.NORTH);

        // Table Card
        JPanel tableCard = new JPanel(new BorderLayout());
        tableCard.setBackground(Colors.SURFACE_CONTAINER);
        tableCard.setBorder(new EmptyBorder(16, 16, 16, 16));

        String[] cols = {"Payment ID", "Party Type", "Amount", "Payment Method", "Reference", "Date/Time", "Notes"};
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

        NumberFormat fmt = NumberFormat.getCurrencyInstance(new java.util.Locale("en", "IN"));
        tableModel.setRowCount(0);

        if (DemoModeConfig.isEnabled()) {
            for (DemoDataStore.PaymentEntry payment : DemoDataStore.payments()) {
                tableModel.addRow(new Object[]{"PAY-" + payment.id(), payment.partyType(), fmt.format(payment.amount()),
                        payment.method().getDisplayName(), payment.referenceType(), payment.date().toString().substring(0, 16), payment.notes()});
            }
            return;
        }

        String sql = "SELECT * FROM payments WHERE business_id = ? ORDER BY payment_date DESC";
        try (var conn = com.businessmanager.database.DBConnection.getConnection();
             var pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, bId);
            try (var rs = pstmt.executeQuery()) {
                while (rs.next()) tableModel.addRow(new Object[]{"PAY-" + rs.getInt("id"), rs.getString("party_type"), fmt.format(rs.getBigDecimal("amount")), PaymentMethod.valueOf(rs.getString("payment_method")).getDisplayName(), rs.getString("reference_type"), rs.getTimestamp("payment_date").toString().substring(0, 16), rs.getString("notes") != null ? rs.getString("notes") : ""});
            }
        } catch (SQLException e) {
            System.err.println("Error loading payments ledger: " + e.getMessage());
        }
    }

    private void showPaymentDialog() {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Record Payment", Dialog.ModalityType.APPLICATION_MODAL);
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Colors.SURFACE_CONTAINER);
        form.setBorder(new EmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6); gbc.fill = GridBagConstraints.HORIZONTAL;
        JComboBox<String> type = new JComboBox<>(new String[]{"Customer sale", "Supplier purchase"});
        JComboBox<Object> reference = new JComboBox<>();
        JTextField amount = new JTextField(14);
        JComboBox<PaymentMethod> method = new JComboBox<>(PaymentMethod.values());
        JTextField notes = new JTextField(14);
        addRow(form, gbc, 0, "Payment for", type); addRow(form, gbc, 1, "Open document", reference); addRow(form, gbc, 2, "Amount", amount); addRow(form, gbc, 3, "Method", method); addRow(form, gbc, 4, "Notes", notes);
        Runnable reload = () -> loadOpenDocuments(reference, type.getSelectedIndex() == 0);
        type.addActionListener(e -> reload.run());
        reference.addActionListener(e -> setOutstandingAmount(amount, reference.getSelectedItem()));
        reload.run();
        JButton save = new JButton("Record Payment");
        save.setBackground(Colors.PRIMARY); save.setForeground(Colors.ON_PRIMARY);
        save.addActionListener(e -> {
            try {
                BigDecimal value = new BigDecimal(amount.getText().trim());
                Object selected = reference.getSelectedItem();
                if (selected instanceof Sale sale) paymentDAO.settleSale(UserSession.getBusinessId(), sale.getId(), value, (PaymentMethod) method.getSelectedItem(), notes.getText());
                else if (selected instanceof Purchase purchase) paymentDAO.settlePurchase(UserSession.getBusinessId(), purchase.getId(), value, (PaymentMethod) method.getSelectedItem(), notes.getText());
                else throw new SQLException("Select an outstanding document.");
                dialog.dispose(); refreshData();
            } catch (NumberFormatException ex) { JOptionPane.showMessageDialog(dialog, "Enter a valid payment amount.", "Payment", JOptionPane.WARNING_MESSAGE); }
            catch (SQLException ex) { JOptionPane.showMessageDialog(dialog, ex.getMessage(), "Payment", JOptionPane.ERROR_MESSAGE); }
        });
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2; form.add(save, gbc);
        dialog.setContentPane(form); dialog.pack(); dialog.setLocationRelativeTo(this); com.businessmanager.theme.UiStyles.apply(dialog); dialog.setVisible(true);
    }

    private void loadOpenDocuments(JComboBox<Object> reference, boolean sales) {
        reference.removeAllItems();
        try {
            if (sales) for (Sale sale : paymentDAO.findOutstandingSales(UserSession.getBusinessId())) reference.addItem(sale);
            else for (Purchase purchase : paymentDAO.findOutstandingPurchases(UserSession.getBusinessId())) reference.addItem(purchase);
        } catch (SQLException ex) { System.err.println("Could not load open documents: " + ex.getMessage()); }
    }

    private void setOutstandingAmount(JTextField amount, Object reference) {
        if (reference instanceof Sale sale) amount.setText(sale.getTotal().subtract(sale.getPaidAmount()).toPlainString());
        if (reference instanceof Purchase purchase) amount.setText(purchase.getTotal().subtract(purchase.getPaidAmount()).toPlainString());
    }

    private void addRow(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent component) {
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 1; panel.add(new JLabel(label), gbc);
        gbc.gridx = 1; panel.add(component, gbc);
    }
}
