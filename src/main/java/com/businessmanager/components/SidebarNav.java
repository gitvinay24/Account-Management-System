package com.businessmanager.components;

import com.businessmanager.enums.BusinessType;
import com.businessmanager.enums.UserRole;
import com.businessmanager.model.Business;
import com.businessmanager.model.User;
import com.businessmanager.security.UserSession;
import com.businessmanager.security.AccessControl;
import com.businessmanager.theme.Colors;
import com.businessmanager.theme.Fonts;
import com.businessmanager.theme.StitchAssets;
import com.businessmanager.ui.MainFrame;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class SidebarNav extends JPanel {

    private final MainFrame mainFrame;
    private final NavSelectionListener selectionListener;

    private String activeItem = "DASHBOARD";
    private final List<NavItemButton> navButtons = new ArrayList<>();

    public interface NavSelectionListener {
        void onNavSelected(String itemKey);
    }

    public SidebarNav(MainFrame mainFrame, NavSelectionListener listener) {
        this.mainFrame = mainFrame;
        this.selectionListener = listener;

        setPreferredSize(new Dimension(236, 900));
        setMinimumSize(new Dimension(236, 600));
        setBackground(Colors.INK_2);
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Colors.LINE));
        setLayout(new BorderLayout());

        buildSidebar();
    }

    private void buildSidebar() {
        removeAll();
        navButtons.clear();

        JPanel topContainer = new JPanel();
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));
        topContainer.setOpaque(false);

        // Stitch Header
        topContainer.add(createHeader());
        topContainer.add(Box.createVerticalStrut(20));

        // Menu Items
        JPanel menuPanel = new JPanel();
        menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.Y_AXIS));
        menuPanel.setOpaque(false);
        menuPanel.setBorder(new EmptyBorder(0, 12, 0, 12));

        addNavItem(menuPanel, "DASHBOARD", "Dashboard", StitchIcon.Type.STOREFRONT);
        addNavItem(menuPanel, "POS", "Sales", StitchIcon.Type.PAYMENTS);
        addNavItem(menuPanel, "PRODUCTS", "Products", StitchIcon.Type.CATEGORY);
        addNavItem(menuPanel, "INVENTORY", "Inventory", StitchIcon.Type.STORE);
        addNavItem(menuPanel, "CUSTOMERS", "Customers", StitchIcon.Type.PERSON);
        addNavItem(menuPanel, "SUPPLIERS", "Suppliers", StitchIcon.Type.DOMAIN);

        if (UserSession.isLargeBusiness()) {
            addNavItem(menuPanel, "EMPLOYEES", "Employees", StitchIcon.Type.BADGE);
            addNavItem(menuPanel, "BRANCHES", "Branches", StitchIcon.Type.LOCATION);
        }

        addNavItem(menuPanel, "REPORTS", "Reports", StitchIcon.Type.DESCRIPTION);

        JScrollPane scroll = new JScrollPane(menuPanel);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        topContainer.add(scroll);
        add(topContainer, BorderLayout.CENTER);

        // Bottom Settings, Support, Collapse Menu
        add(createFooter(), BorderLayout.SOUTH);
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 16));
        header.setOpaque(false);

        JLabel logoIcon = new JLabel();
        ImageIcon icon = StitchAssets.getLogoIcon(28, 28);
        if (icon != null) {
            logoIcon.setIcon(icon);
        }

        JPanel textGroup = new JPanel();
        textGroup.setLayout(new BoxLayout(textGroup, BoxLayout.Y_AXIS));
        textGroup.setOpaque(false);

        Business b = UserSession.getCurrentBusiness();
        String edition = UserSession.isGuestSession() ? "Guest Sample Access"
                : b != null && b.getType() == BusinessType.LARGE ? "Enterprise Edition" : "Standard Edition";
        JLabel subtitle = new JLabel(edition);
        subtitle.setFont(Fonts.LABEL_CAPS);
        subtitle.setForeground(Colors.ON_SURFACE_VARIANT);

        textGroup.add(subtitle);

        header.add(logoIcon);
        header.add(textGroup);
        return header;
    }

    private void addNavItem(JPanel panel, String key, String labelText, StitchIcon.Type iconType) {
        if (!AccessControl.canAccessModule(key)) return;
        NavItemButton btn = new NavItemButton(key, labelText, iconType);
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.putClientProperty("ui.navigation", true);
        btn.setMaximumSize(new Dimension(216, 44));

        btn.addActionListener(e -> {
            setActiveItem(key);
            if (selectionListener != null) {
                selectionListener.onNavSelected(key);
            }
        });

        navButtons.add(btn);
        panel.add(btn);
        panel.add(Box.createVerticalStrut(4));
    }

    public void setActiveItem(String key) {
        this.activeItem = key;
        for (NavItemButton btn : navButtons) {
            btn.setSelected(btn.getKey().equals(key));
        }
    }

    private JPanel createFooter() {
        JPanel footer = new JPanel();
        footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(12, 16, 20, 16));

        JButton settingsBtn = createFooterButton("Settings", StitchIcon.Type.STORE);
        settingsBtn.addActionListener(e -> {
            setActiveItem("SETTINGS");
            if (selectionListener != null) selectionListener.onNavSelected("SETTINGS");
        });

        JButton supportBtn = createFooterButton("Support", StitchIcon.Type.DESCRIPTION);
        JButton logoutBtn = createFooterButton(UserSession.isGuestSession() ? "Exit guest" : "Log out", StitchIcon.Type.LOGOUT);
        logoutBtn.setForeground(Colors.ERROR);
        logoutBtn.addActionListener(e -> mainFrame.handleLogout());

        if (!UserSession.isGuestSession()) {
            footer.add(settingsBtn);
            footer.add(Box.createVerticalStrut(8));
        }
        footer.add(supportBtn);
        footer.add(Box.createVerticalStrut(8));
        footer.add(logoutBtn);

        return footer;
    }

    private JButton createFooterButton(String text, StitchIcon.Type iconType) {
        JButton btn = new JButton("  " + text);
        btn.setFont(Fonts.BODY_SMALL);
        btn.setForeground(Colors.ON_SURFACE_VARIANT);
        btn.setIcon(new StitchIcon(iconType));
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        return btn;
    }

    private class NavItemButton extends JButton {
        private final String key;

        NavItemButton(String key, String text, StitchIcon.Type iconType) {
            super("  " + text);
            this.key = key;

            setFont(Fonts.BODY);
            setForeground(Colors.ON_SURFACE_VARIANT);
            setBackground(Colors.SURFACE);
            setIcon(new StitchIcon(iconType));
            setHorizontalAlignment(SwingConstants.LEFT);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(10, 14, 10, 14));
            putClientProperty("JButton.buttonType", "roundRect");
            putClientProperty("Component.arc", 12);
            putClientProperty("ui.navigation", true);
        }

        public String getKey() {
            return key;
        }

        @Override
        public void setSelected(boolean selected) {
            super.setSelected(selected);
            if (selected) {
                setBackground(Color.decode("#24221C"));
                setForeground(Colors.MARIGOLD);
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 3, 0, 0, Colors.MARIGOLD),
                        new EmptyBorder(10, 11, 10, 14)
                ));
            } else {
                setBackground(Colors.INK_2);
                setForeground(Colors.SLATE);
                setBorder(new EmptyBorder(10, 14, 10, 14));
            }
            repaint();
        }
    }
}
