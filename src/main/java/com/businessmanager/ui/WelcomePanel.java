package com.businessmanager.ui;

import com.businessmanager.theme.Colors;
import com.businessmanager.theme.Fonts;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.geom.RoundRectangle2D;
import java.awt.*;

public class WelcomePanel extends JPanel {

    private final MainFrame frame;

    public WelcomePanel(MainFrame frame) {
        this.frame = frame;
        setLayout(new GridBagLayout());
        setOpaque(false);

        createUI();
    }

    /** Deep, low-contrast canvas that keeps the welcome screen readable at any window size. */
    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setPaint(new GradientPaint(0, 0, Color.decode("#0C2219"), 0, getHeight(), Color.decode("#050F0B")));
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setColor(new Color(255, 255, 255, 10));
        for (int x = 20; x < getWidth(); x += 32)
            for (int y = 20; y < getHeight(); y += 32) g.fillOval(x, y, 2, 2);
        float radius = Math.max(getWidth(), getHeight()) * .45f;
        g.setPaint(new RadialGradientPaint(new Point(getWidth() / 2, (int) (getHeight() * .42)), radius,
                new float[]{0f, 1f}, new Color[]{new Color(47, 166, 110, 55), new Color(47, 166, 110, 0)}));
        g.fillRect(0, 0, getWidth(), getHeight());
        g.dispose();
    }

    private void createUI() {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(24, 24, 24, 24));
        content.setMaximumSize(new Dimension(680, 660));

        JLabel eyebrow = new JLabel("ACCOUNT MANAGEMENT  •  SECURE WORKSPACE");
        eyebrow.setFont(Fonts.LABEL_CAPS);
        eyebrow.setForeground(Colors.PRIMARY);
        eyebrow.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(eyebrow);

        content.add(Box.createVerticalStrut(18));

        JComponent logo = new LogoBadge(112);
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(logo);

        content.add(Box.createVerticalStrut(24));

        // Application Title
        JLabel title = new JLabel("Account Management");
        title.setFont(Fonts.DISPLAY_LG);
        title.setForeground(Colors.TEXT_PRIMARY);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(title);

        content.add(Box.createVerticalStrut(4));

        // Subtitle / Headline
        JLabel tagline = new JLabel("Clarity for every account.");
        tagline.setFont(Fonts.HEADLINE_MD);
        tagline.setForeground(Colors.PRIMARY);
        tagline.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(tagline);

        content.add(Box.createVerticalStrut(34));

        // Actions
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0));
        buttonPanel.setOpaque(false);

        JButton loginButton = createSecondaryButton("Sign in");
        loginButton.addActionListener(e -> {
            if (frame != null) frame.showScreen(MainFrame.LOGIN);
        });

        JButton createAccountButton = createPrimaryButton("Set up workspace");
        createAccountButton.addActionListener(e -> {
            if (frame != null) frame.showScreen(MainFrame.CREATE_ACCOUNT);
        });

        buttonPanel.add(loginButton);
        buttonPanel.add(createAccountButton);

        content.add(buttonPanel);

        content.add(Box.createVerticalStrut(30));
        JPanel chips = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        chips.setOpaque(false);
        for (String value : new String[]{"End-to-end secure", "Role-based access", "Audit trail"}) chips.add(new Chip(value));
        content.add(chips);

        add(content);
    }

    private JButton createSecondaryButton(String text) {
        JButton button = new JButton(text);
        button.setFont(Fonts.LABEL_CAPS);
        button.setForeground(Colors.TEXT_PRIMARY);
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Colors.PRIMARY_DARK, 1, true), new EmptyBorder(12, 32, 12, 32)));
        button.setPreferredSize(new Dimension(132, 44));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }

    private JButton createPrimaryButton(String text) {
        JButton button = new JButton(text);
        button.setFont(Fonts.LABEL_CAPS);
        button.setForeground(Colors.ON_PRIMARY);
        button.setBackground(Colors.PRIMARY);
        button.setBorder(new EmptyBorder(12, 32, 12, 32));
        button.setPreferredSize(new Dimension(202, 44));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }

    private static final class LogoBadge extends JComponent {
        private final int size;
        LogoBadge(int size) { this.size = size; setPreferredSize(new Dimension(size + 28, size + 28)); setMaximumSize(getPreferredSize()); }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int x = (getWidth() - size) / 2, y = (getHeight() - size) / 2, arc = size / 4;
            for (int i = 14; i > 0; i--) { g.setColor(new Color(47, 166, 110, 5)); g.fill(new RoundRectangle2D.Float(x-i, y-i, size+2*i, size+2*i, arc+i, arc+i)); }
            g.setPaint(new GradientPaint(x, y, Colors.PRIMARY_HOVER, x + size, y + size, Colors.PRIMARY_DARK));
            g.fill(new RoundRectangle2D.Float(x, y, size, size, arc, arc));
            g.setColor(new Color(255, 255, 255, 45)); g.draw(new RoundRectangle2D.Float(x + 1, y + 1, size - 2, size - 2, arc, arc));
            g.setFont(new Font("Segoe UI", Font.BOLD, Math.round(size * .38f))); g.setColor(Color.WHITE);
            FontMetrics fm = g.getFontMetrics(); g.drawString("AM", x + (size - fm.stringWidth("AM")) / 2, y + (size + fm.getAscent() - fm.getDescent()) / 2);
            g.dispose();
        }
    }

    private static final class Chip extends JLabel {
        Chip(String text) { super(text); setFont(Fonts.BODY_SMALL); setForeground(Colors.TEXT_SECONDARY); setBorder(new EmptyBorder(6, 14, 6, 14)); }
        @Override protected void paintComponent(Graphics graphics) { Graphics2D g=(Graphics2D)graphics.create(); g.setColor(new Color(255,255,255,14)); g.fillRoundRect(0,0,getWidth()-1,getHeight()-1,getHeight(),getHeight()); g.setColor(new Color(255,255,255,30)); g.drawRoundRect(0,0,getWidth()-1,getHeight()-1,getHeight(),getHeight()); g.dispose(); super.paintComponent(graphics); }
    }

}
