package com.businessmanager.ui.auth;

import com.businessmanager.enums.BusinessType;
import com.businessmanager.model.Business;
import com.businessmanager.model.User;
import com.businessmanager.service.AuthService;
import com.businessmanager.theme.Colors;
import com.businessmanager.theme.Fonts;
import com.businessmanager.ui.MainFrame;
import com.formdev.flatlaf.extras.FlatSVGIcon;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class BusinessTypePanel extends JPanel {

    private final MainFrame mainFrame;
    private final AuthService authService;

    private User draftUser;
    private Business draftBusiness;

    private BusinessType selectedType = BusinessType.SMALL;

    private JPanel smallBusinessCard;
    private JPanel largeBusinessCard;

    public BusinessTypePanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.authService = new AuthService();

        setLayout(new GridBagLayout());
        setBackground(Colors.BACKGROUND);
        buildUI();
    }

    public void setRegistrationData(User user, Business business) {
        this.draftUser = user;
        this.draftBusiness = business;
        removeAll();
        buildUI();
        revalidate();
        repaint();
    }

    private void buildUI() {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.setPreferredSize(new Dimension(850, 750));
        content.setMaximumSize(new Dimension(900, 800));

        // Header
        content.add(createHeader());
        content.add(Box.createVerticalStrut(32));

        // Cards row
        JPanel cardsRow = new JPanel(new GridLayout(1, 2, 24, 0));
        cardsRow.setOpaque(false);

        smallBusinessCard = createCard(
                BusinessType.SMALL,
                "Small Business",
                "Simple Mode",
                "Ideal for single-location shops, retail stores, local boutiques, and small business operators needing essential daily management.",
                new String[]{
                        "Quick POS & Billing Interface",
                        "Product & Inventory Stock Control",
                        "Customer & Supplier Database",
                        "Expenses & Profit/Loss Summaries",
                        "Focused & Easy-to-use Dashboard"
                }
        );

        largeBusinessCard = createCard(
                BusinessType.LARGE,
                "Large / Enterprise",
                "Advanced Mode",
                "Designed for growing enterprises, multi-branch chains, wholesalers, and businesses requiring staff controls and multi-store insights.",
                new String[]{
                        "Multi-Branch & HQ Management",
                        "Employee Roles & Access Control",
                        "Branch-Specific Inventory & Sales",
                        "Advanced Analytics & JFreeCharts",
                        "Comprehensive Enterprise Reports"
                }
        );

        cardsRow.add(smallBusinessCard);
        cardsRow.add(largeBusinessCard);

        content.add(cardsRow);
        content.add(Box.createVerticalStrut(40));

        // Bottom Actions Row
        JPanel actionRow = new JPanel(new BorderLayout());
        actionRow.setOpaque(false);

        JButton backBtn = new JButton("← Back");
        backBtn.setFont(Fonts.BODY);
        backBtn.setForeground(Colors.ON_SURFACE_VARIANT);
        backBtn.setOpaque(false);
        backBtn.setContentAreaFilled(false);
        backBtn.setBorderPainted(false);
        backBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backBtn.addActionListener(e -> mainFrame.showScreen(MainFrame.BUSINESS_INFO));

        JButton completeBtn = new JButton("Complete Setup & Open Workspace   →");
        completeBtn.setFont(Fonts.TITLE_SM);
        completeBtn.setForeground(Colors.ON_PRIMARY);
        completeBtn.setBackground(Colors.PRIMARY);
        completeBtn.setPreferredSize(new Dimension(340, 50));
        completeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        completeBtn.putClientProperty("JButton.buttonType", "roundRect");
        completeBtn.putClientProperty("Component.arc", 8);

        completeBtn.addActionListener(e -> handleCompleteRegistration());

        actionRow.add(backBtn, BorderLayout.WEST);
        actionRow.add(completeBtn, BorderLayout.EAST);

        content.add(actionRow);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.weightx = 1; gbc.weighty = 1;
        gbc.anchor = GridBagConstraints.CENTER;

        add(content, gbc);
        updateCardSelectionUI();
    }

    private JPanel createHeader() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);

        JPanel logoRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        logoRow.setOpaque(false);

        JLabel logoIconLabel = new JLabel("AM", SwingConstants.CENTER);
        logoIconLabel.setFont(Fonts.LABEL_CAPS);
        logoIconLabel.setForeground(Colors.ON_PRIMARY);
        logoIconLabel.setOpaque(true);
        logoIconLabel.setBackground(Colors.PRIMARY);
        logoIconLabel.setPreferredSize(new Dimension(32, 32));

        JLabel logo = new JLabel("ACCOUNT MANAGEMENT");
        logo.setFont(Fonts.HEADLINE_MD);
        logo.setForeground(Colors.ON_SURFACE);

        logoRow.add(logoIconLabel);
        logoRow.add(logo);
        logoRow.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(logoRow);

        panel.add(Box.createVerticalStrut(16));

        JLabel title = new JLabel("Choose your business type");
        title.setFont(Fonts.DISPLAY_LG);
        title.setForeground(Colors.ON_SURFACE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("Select the setup that best fits the way you operate.");
        subtitle.setFont(Fonts.BODY);
        subtitle.setForeground(Colors.ON_SURFACE_VARIANT);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(Box.createVerticalStrut(8));
        panel.add(title);
        panel.add(Box.createVerticalStrut(6));
        panel.add(subtitle);

        return panel;
    }

    private JPanel createCard(BusinessType type, String titleText, String badgeText, String descText, String[] features) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Colors.SURFACE_CONTAINER);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);

                if (selectedType == type) {
                    g2.setColor(Colors.PRIMARY);
                    g2.setStroke(new BasicStroke(2));
                    g2.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 16, 16);
                    g2.fillRect(0, 0, getWidth(), 4);
                } else {
                    g2.setColor(Colors.OUTLINE_VARIANT);
                    g2.setStroke(new BasicStroke(1));
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                }
                g2.dispose();
            }
        };

        card.setLayout(new BorderLayout());
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(24, 24, 24, 24));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setOpaque(false);

        // Header inside card
        JPanel cardHeader = new JPanel(new BorderLayout());
        cardHeader.setOpaque(false);

        JLabel titleLbl = new JLabel(titleText);
        titleLbl.setFont(Fonts.TITLE_SM);
        titleLbl.setForeground(type == selectedType ? Colors.PRIMARY : Colors.ON_SURFACE);

        JLabel badgeLbl = new JLabel(badgeText.toUpperCase());
        badgeLbl.setFont(Fonts.LABEL_CAPS);
        badgeLbl.setForeground(Colors.ON_SURFACE_VARIANT);

        JPanel titleGrp = new JPanel();
        titleGrp.setLayout(new BoxLayout(titleGrp, BoxLayout.Y_AXIS));
        titleGrp.setOpaque(false);
        titleGrp.add(titleLbl);
        titleGrp.add(Box.createVerticalStrut(2));
        titleGrp.add(badgeLbl);

        JLabel checkIcon = new JLabel();
        checkIcon.setPreferredSize(new Dimension(24, 24));

        cardHeader.add(titleGrp, BorderLayout.WEST);
        cardHeader.add(checkIcon, BorderLayout.EAST);

        inner.add(cardHeader);
        inner.add(Box.createVerticalStrut(16));

        JLabel descLbl = new JLabel("<html><body style='width: 300px;'>" + descText + "</body></html>");
        descLbl.setFont(Fonts.BODY_SMALL);
        descLbl.setForeground(Colors.ON_SURFACE_VARIANT);
        inner.add(descLbl);

        inner.add(Box.createVerticalStrut(20));

        JSeparator sep = new JSeparator();
        sep.setForeground(Colors.OUTLINE_VARIANT);
        inner.add(sep);

        inner.add(Box.createVerticalStrut(16));

        for (String feat : features) {
            JLabel featLbl = new JLabel("✓  " + feat);
            featLbl.setFont(Fonts.BODY_SMALL);
            featLbl.setForeground(Colors.ON_SURFACE);
            featLbl.setBorder(new EmptyBorder(4, 0, 4, 0));
            inner.add(featLbl);
        }

        card.add(inner, BorderLayout.CENTER);

        card.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                selectedType = type;
                updateCardSelectionUI();
            }
        });

        return card;
    }

    private void updateCardSelectionUI() {
        if (smallBusinessCard != null) smallBusinessCard.repaint();
        if (largeBusinessCard != null) largeBusinessCard.repaint();
    }

    private void handleCompleteRegistration() {
        if (draftUser == null || draftBusiness == null) {
            JOptionPane.showMessageDialog(this, "Registration state is missing. Please restart setup.", "Error", JOptionPane.ERROR_MESSAGE);
            mainFrame.showScreen(MainFrame.CREATE_ACCOUNT);
            return;
        }

        if (!mainFrame.ensureDatabaseReadyForRegisteredAccess()) {
            return;
        }

        draftBusiness.setType(selectedType);

        char[] password = draftUser.getRegistrationPassword();
        AuthService.AuthResult result = authService.registerBusinessAndUser(draftUser, draftBusiness, password);
        if (password != null) {
            java.util.Arrays.fill(password, '\0');
        }
        draftUser.clearRegistrationPassword();

        if (result.isSuccess()) {
            JOptionPane.showMessageDialog(
                    mainFrame,
                    "Congratulations! Your " + selectedType.getDisplayName() + " workspace is ready.",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE
            );
            mainFrame.onLoginSuccess();
        } else {
            JOptionPane.showMessageDialog(
                    mainFrame,
                    result.getMessage(),
                    "Registration Failed",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }
}
