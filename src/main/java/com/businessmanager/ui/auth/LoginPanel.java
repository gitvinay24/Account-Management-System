package com.businessmanager.ui.auth;

import com.businessmanager.service.AuthService;
import com.businessmanager.security.UserSession;
import com.businessmanager.theme.Colors;
import com.businessmanager.ui.MainFrame;
import javax.swing.*;
import java.awt.*;

/** Real authentication form using the ledger/parchment sign-in design. */
public class LoginPanel extends JPanel {
    private final JFrame frame;
    private final JTextField email = AuthUi.input("you@yourshop.com");
    private final JPasswordField password = AuthUi.password("Enter your password");
    private final JLabel error = AuthUi.inlineError();

    public LoginPanel(JFrame frame) {
        this.frame = frame;
        setLayout(new GridLayout(1, 2));
        setOpaque(false);
        add(brandPanel());
        add(formPanel());
    }

    private JPanel brandPanel() {
        JPanel left = new JPanel(new GridBagLayout());
        left.setOpaque(false);
        JPanel content = new JPanel(); content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS)); content.setOpaque(false);
        JLabel mark = new JLabel("AM", SwingConstants.CENTER); mark.setFont(new Font("Segoe UI", Font.BOLD, 28)); mark.setForeground(Color.WHITE); mark.setOpaque(true); mark.setBackground(Colors.PRIMARY); mark.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24)); mark.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel title = new JLabel("Account Management"); title.setFont(new Font("Segoe UI", Font.BOLD, 30)); title.setForeground(Colors.TEXT_PRIMARY); title.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel subtitle = new JLabel("Clarity for every account."); subtitle.setFont(AuthUi.BODY); subtitle.setForeground(Colors.PRIMARY_HOVER); subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(mark); content.add(Box.createVerticalStrut(20)); content.add(title); content.add(Box.createVerticalStrut(8)); content.add(subtitle); content.add(Box.createVerticalStrut(36));
        for (String value : new String[]{"✓  Every account, one clear dashboard", "✓  Role-based access you control", "✓  Complete audit trail, always on"}) { JLabel feature = new JLabel(value); feature.setFont(AuthUi.BODY); feature.setForeground(Colors.TEXT_SECONDARY); feature.setAlignmentX(Component.CENTER_ALIGNMENT); content.add(feature); content.add(Box.createVerticalStrut(14)); }
        left.add(content); return left;
    }

    private JPanel formPanel() {
        JPanel right = new JPanel(new GridBagLayout()); right.setBackground(Colors.SURFACE_CONTAINER_LOW); right.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, Colors.OUTLINE));
        right.add(form()); return right;
    }

    private JPanel form() {
        JPanel p = AuthUi.formColumn(); p.setBorder(BorderFactory.createEmptyBorder(32, 42, 32, 42)); p.setPreferredSize(new Dimension(430, 520));
        JLabel back = AuthUi.text("←  Back to welcome"); AuthUi.link(back, () -> ((MainFrame) frame).showScreen(MainFrame.WELCOME)); p.add(back); AuthUi.gap(p, 34);
        p.add(AuthUi.heading("Welcome back")); AuthUi.gap(p, 10);
        p.add(AuthUi.text("Sign in to pick up where the day left off.")); AuthUi.gap(p, 36);
        p.add(AuthUi.field("Email address", email)); AuthUi.gap(p, 20);
        p.add(AuthUi.field("Password", password)); AuthUi.gap(p, 8); p.add(error); AuthUi.gap(p, 8);
        JPanel aux = new JPanel(new BorderLayout()); aux.setOpaque(false); aux.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        JCheckBox keep = new JCheckBox("Keep me signed in"); keep.setFont(AuthUi.BODY); keep.setForeground(AuthUi.SLATE); keep.setOpaque(false); keep.setFocusPainted(false);
        JLabel forgot = new JLabel("Forgot password?"); forgot.setFont(AuthUi.BODY); AuthUi.link(forgot, () -> JOptionPane.showMessageDialog(frame, "Please contact your administrator to reset your password.", "Account Management", JOptionPane.INFORMATION_MESSAGE));
        aux.add(keep, BorderLayout.WEST); aux.add(forgot, BorderLayout.EAST); AuthUi.left(aux); p.add(aux); AuthUi.gap(p, 28);
        JButton signIn = AuthUi.primary("Sign in"); signIn.addActionListener(e -> login()); p.add(signIn); AuthUi.gap(p, 12);
        JButton guest = AuthUi.ghost("Continue as Guest");
        guest.setToolTipText("Explore read-only sample data without creating an account.");
        guest.addActionListener(e -> startGuestSession());
        p.add(guest); AuthUi.gap(p, 8);
        JLabel guestNote = AuthUi.text("Sample data · Read-only · No account required");
        guestNote.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        p.add(guestNote); AuthUi.gap(p, 14);
        JPanel switcher = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0)); switcher.setOpaque(false); JLabel a=AuthUi.text("New to Account Management?"), b=AuthUi.text("Set up your workspace"); AuthUi.link(b, () -> ((MainFrame) frame).showScreen(MainFrame.CREATE_ACCOUNT)); switcher.add(a); switcher.add(b); AuthUi.left(switcher); p.add(switcher); p.add(Box.createVerticalGlue()); return p;
    }

    private void login() {
        String identifier=email.getText().trim(), secret=new String(password.getPassword());
        AuthUi.error(email,error,null); AuthUi.error(password,error,null);
        if(identifier.isEmpty()){fail(email,"Enter your email address or username."); return;}
        if(secret.isEmpty()){fail(password,"Enter your password."); return;}
        if (!((MainFrame) frame).ensureDatabaseReadyForRegisteredAccess()) return;
        AuthService.AuthResult result=new AuthService().login(identifier,secret);
        if(result.isSuccess()) ((MainFrame)frame).onLoginSuccess(); else fail(password,result.getMessage());
    }
    private void startGuestSession() {
        AuthUi.error(email,error,null); AuthUi.error(password,error,null);
        password.setText("");
        UserSession.startGuestSession();
        ((MainFrame) frame).onLoginSuccess();
    }
    private void fail(JTextField target,String message){AuthUi.error(target,error,message);target.requestFocusInWindow();}

    public void resetForm() {
        password.setText("");
        error.setText(" ");
        error.setVisible(false);
    }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setPaint(new GradientPaint(0, 0, new Color(0x0C2219), 0, getHeight(), new Color(0x050F0B)));
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.setColor(new Color(255, 255, 255, 9));
        for (int x = 20; x < getWidth(); x += 32)
            for (int y = 20; y < getHeight(); y += 32) g2.fillOval(x, y, 2, 2);
        float radius = Math.max(getWidth(), getHeight()) * .48f;
        g2.setPaint(new RadialGradientPaint(new Point(getWidth() / 3, (int) (getHeight() * .42)), radius,
                new float[]{0f, 1f}, new Color[]{new Color(47, 166, 110, 48), new Color(47, 166, 110, 0)}));
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.dispose();
    }
}
