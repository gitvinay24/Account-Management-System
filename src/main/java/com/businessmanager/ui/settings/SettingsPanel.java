package com.businessmanager.ui.settings;

import com.businessmanager.database.DBConnection;
import com.businessmanager.model.Business;
import com.businessmanager.model.User;
import com.businessmanager.service.AuthService;
import com.businessmanager.security.UserSession;
import com.businessmanager.theme.Colors;
import com.businessmanager.theme.Fonts;
import com.businessmanager.ui.MainFrame;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.Connection;

public class SettingsPanel extends JPanel {

    private final AuthService authService = new AuthService();

    public SettingsPanel() {
        setLayout(new BorderLayout());
        setBackground(Colors.BACKGROUND);
        setBorder(new EmptyBorder(24, 28, 24, 28));

        buildUI();
    }

    private void buildUI() {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);

        // Header
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);

        JPanel headerText = new JPanel();
        headerText.setLayout(new BoxLayout(headerText, BoxLayout.Y_AXIS));
        headerText.setOpaque(false);

        JLabel title = new JLabel("System Settings & User Account");
        title.setFont(Fonts.DISPLAY_LG);
        title.setForeground(Colors.ON_SURFACE);

        JLabel sub = new JLabel("Manage business profile settings, user security credentials, and system options");
        sub.setFont(Fonts.BODY);
        sub.setForeground(Colors.ON_SURFACE_VARIANT);

        headerText.add(title);
        headerText.add(Box.createVerticalStrut(4));
        headerText.add(sub);

        topPanel.add(headerText, BorderLayout.WEST);
        content.add(topPanel);
        content.add(Box.createVerticalStrut(24));

        // Settings Cards Row
        JPanel grid = new JPanel(new GridLayout(1, 2, 20, 0));
        grid.setOpaque(false);
        grid.setMaximumSize(new Dimension(1000, 420));

        // Card 1: Change Password
        grid.add(createPasswordCard());

        // Card 2: Business Profile Information
        grid.add(createBusinessProfileCard());

        content.add(grid);
        content.add(Box.createVerticalStrut(20));

        // Card 3: Database & System Connectivity
        JPanel databaseCard = createDatabaseCard();
        databaseCard.setMaximumSize(new Dimension(1000, 200));
        content.add(databaseCard);
        content.add(Box.createVerticalStrut(20));

        // Card 4: Session Logout Card
        JPanel logoutCard = createLogoutCard();
        logoutCard.setMaximumSize(new Dimension(1000, 160));
        content.add(logoutCard);

        JScrollPane scroll = new JScrollPane(content);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);

        add(scroll, BorderLayout.CENTER);
    }

    private JPanel createPasswordCard() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Colors.SURFACE_CONTAINER);
        card.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Security & Change Password");
        title.setFont(Fonts.TITLE_SM);
        title.setForeground(Colors.ON_SURFACE);
        card.add(title, BorderLayout.NORTH);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JPasswordField currentPassField = new JPasswordField(15);
        JPasswordField newPassField = new JPasswordField(15);
        JPasswordField confirmPassField = new JPasswordField(15);

        int r = 0;
        addFormRow(panel, gbc, r++, "Current Password *:", currentPassField);
        addFormRow(panel, gbc, r++, "New Password *:", newPassField);
        addFormRow(panel, gbc, r++, "Confirm New Password *:", confirmPassField);

        JButton changeBtn = new JButton("Update Password");
        changeBtn.setFont(Fonts.BODY_SMALL);
        changeBtn.setForeground(Colors.ON_PRIMARY);
        changeBtn.setBackground(Colors.PRIMARY);
        changeBtn.addActionListener(e -> {
            String oldPass = new String(currentPassField.getPassword());
            String newPass = new String(newPassField.getPassword());
            String confirmPass = new String(confirmPassField.getPassword());

            if (oldPass.isEmpty() || newPass.isEmpty()) {
                JOptionPane.showMessageDialog(this, "All password fields are required.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (!newPass.equals(confirmPass)) {
                JOptionPane.showMessageDialog(this, "New passwords do not match.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            AuthService.AuthResult res = authService.changePassword(UserSession.getUserId(), oldPass, newPass);
            if (res.isSuccess()) {
                JOptionPane.showMessageDialog(this, "Password updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                currentPassField.setText("");
                newPassField.setText("");
                confirmPassField.setText("");
            } else {
                JOptionPane.showMessageDialog(this, res.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnRow.setOpaque(false);
        btnRow.add(changeBtn);

        card.add(panel, BorderLayout.CENTER);
        card.add(btnRow, BorderLayout.SOUTH);
        return card;
    }

    private JPanel createBusinessProfileCard() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Colors.SURFACE_CONTAINER);
        card.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Business Profile Information");
        title.setFont(Fonts.TITLE_SM);
        title.setForeground(Colors.ON_SURFACE);
        card.add(title, BorderLayout.NORTH);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        Business b = UserSession.getCurrentBusiness();
        User u = UserSession.getCurrentUser();

        int r = 0;
        addFormRow(panel, gbc, r++, "Business Name:", new JLabel(b != null ? b.getName() : "N/A"));
        addFormRow(panel, gbc, r++, "Business Mode:", new JLabel(b != null ? b.getType().getDisplayName() : "N/A"));
        addFormRow(panel, gbc, r++, "Reg Number:", new JLabel(b != null && b.getRegistrationNumber() != null ? b.getRegistrationNumber() : "N/A"));
        addFormRow(panel, gbc, r++, "GSTIN / Tax ID:", new JLabel(b != null && b.getTaxId() != null ? b.getTaxId() : "N/A"));
        addFormRow(panel, gbc, r++, "Business Email:", new JLabel(b != null ? b.getEmail() : "N/A"));
        addFormRow(panel, gbc, r++, "Account Owner:", new JLabel(u != null ? u.getFullName() + " (" + u.getEmail() + ")" : "N/A"));

        card.add(panel, BorderLayout.CENTER);
        return card;
    }

    private void addFormRow(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent comp) {
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.4;
        JLabel lbl = new JLabel(label);
        lbl.setFont(Fonts.BODY);
        lbl.setForeground(Colors.ON_SURFACE);
        panel.add(lbl, gbc);

        gbc.gridx = 1; gbc.weightx = 0.6;
        if (comp instanceof JLabel l) {
            l.setFont(Fonts.BODY);
            l.setForeground(Colors.PRIMARY);
        }
        panel.add(comp, gbc);
    }

    /**
     * Pure-Swing database status card: shows the configured connection target and
     * lets the user verify connectivity at runtime without leaving the app.
     * The constructor never touches the network; the check happens on demand.
     */
    private JPanel createDatabaseCard() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Colors.SURFACE_CONTAINER);
        card.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);

        JLabel title = new JLabel("Database & System");
        title.setFont(Fonts.TITLE_SM);
        title.setForeground(Colors.ON_SURFACE);
        header.add(title);
        header.add(Box.createVerticalStrut(4));

        JLabel target = new JLabel("MySQL: " + DBConnection.getDbUrl());
        target.setFont(Fonts.BODY_SMALL);
        target.setForeground(Colors.TEXT_MUTED);
        header.add(target);
        card.add(header, BorderLayout.NORTH);

        JPanel body = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 12));
        body.setOpaque(false);

        JLabel status = new JLabel("●  Not checked yet");
        status.setFont(Fonts.BODY);
        status.setForeground(Colors.TEXT_SECONDARY);

        JButton testButton = new JButton("Test Connection");
        testButton.setFont(Fonts.BODY_SMALL);
        testButton.setForeground(Colors.ON_PRIMARY);
        testButton.setBackground(Colors.PRIMARY);
        testButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        testButton.addActionListener(e -> {
            Window window = SwingUtilities.getWindowAncestor(this);
            if (window != null) {
                window.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            }
            boolean reachable;
            String detail;
            try (Connection ignored = DBConnection.getConnection()) {
                reachable = true;
                detail = "Connected to database '" + DBConnection.getDatabaseName() + "'.";
            } catch (Exception ex) {
                reachable = false;
                detail = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
            }
            if (window != null) {
                window.setCursor(Cursor.getDefaultCursor());
            }
            if (reachable) {
                status.setText("●  Connected");
                status.setForeground(Colors.GOOD);
            } else {
                status.setText("●  Not connected");
                status.setForeground(Colors.ERROR);
                JOptionPane.showMessageDialog(this,
                        "Could not reach the database:\n\n" + detail,
                        "Database connection failed",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        body.add(testButton);
        body.add(Box.createHorizontalStrut(16));
        body.add(status);
        card.add(body, BorderLayout.CENTER);
        return card;
    }

    private JPanel createLogoutCard() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Colors.SURFACE_CONTAINER);
        card.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Account Session & System Security");
        title.setFont(Fonts.TITLE_SM);
        title.setForeground(Colors.ON_SURFACE);
        card.add(title, BorderLayout.NORTH);

        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);
        infoPanel.setBorder(new EmptyBorder(12, 0, 12, 0));

        JLabel desc = new JLabel("Log out of your active session. You will be redirected to the sign-in screen and will need your credentials to log back in.");
        desc.setFont(Fonts.BODY_SMALL);
        desc.setForeground(Colors.ON_SURFACE_VARIANT);
        infoPanel.add(desc);

        JButton logoutBtn = new JButton("Log Out of Session");
        logoutBtn.setFont(Fonts.BODY_SMALL);
        logoutBtn.setForeground(Colors.ON_ERROR);
        logoutBtn.setBackground(Colors.ERROR);
        logoutBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        logoutBtn.addActionListener(e -> {
            Window window = SwingUtilities.getWindowAncestor(this);
            if (window instanceof MainFrame mainFrame) {
                mainFrame.handleLogout();
            }
        });

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        btnRow.setOpaque(false);
        btnRow.add(logoutBtn);

        card.add(infoPanel, BorderLayout.CENTER);
        card.add(btnRow, BorderLayout.SOUTH);
        return card;
    }
}
