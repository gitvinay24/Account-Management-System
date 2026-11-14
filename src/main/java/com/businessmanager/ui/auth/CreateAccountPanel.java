package com.businessmanager.ui.auth;

import com.businessmanager.model.User;
import com.businessmanager.ui.MainFrame;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.Arrays;

/** Step one of the real account-registration workflow. */
public class CreateAccountPanel extends JPanel {
    private final JFrame frame;
    private final JTextField fullName=AuthUi.input("Anjali Rao"), username=AuthUi.input("anjali.rao"), email=AuthUi.input("anjali@yourshop.com"), phone=AuthUi.input("+91 98765 43210");
    private final JPasswordField password=AuthUi.password("At least 8 characters"), confirm=AuthUi.password("Re-enter your password");
    private final JLabel error=AuthUi.inlineError(), strength=new JLabel(" ");
    private final JProgressBar meter=new JProgressBar(0,4);

    public CreateAccountPanel(JFrame frame) { this.frame=frame; setLayout(new BorderLayout()); setBackground(AuthUi.PARCHMENT); add(AuthUi.shell(form(), 600),BorderLayout.CENTER); password.getDocument().addDocumentListener(new DocumentListener(){public void insertUpdate(DocumentEvent e){score();} public void removeUpdate(DocumentEvent e){score();} public void changedUpdate(DocumentEvent e){score();}}); }
    private JPanel form(){
        JPanel p=AuthUi.formColumn(); p.add(AuthUi.brand()); AuthUi.gap(p,32); p.add(AuthUi.tracker(1)); AuthUi.gap(p,30); p.add(AuthUi.heading("Tell us who's running the shop")); AuthUi.gap(p,10); p.add(AuthUi.text("This becomes your owner login — you can add staff accounts later.")); AuthUi.gap(p,28);
        p.add(AuthUi.pair(AuthUi.field("Full name",fullName),AuthUi.field("Username",username))); AuthUi.gap(p,18); p.add(AuthUi.pair(AuthUi.field("Email address",email),AuthUi.field("Phone number",phone))); AuthUi.gap(p,18); p.add(AuthUi.field("Password",password));
        JPanel s=new JPanel(new BorderLayout(8,0)); s.setOpaque(false); meter.setValue(0); meter.setForeground(AuthUi.GOOD); meter.setBackground(AuthUi.LINE); meter.setBorderPainted(false); strength.setFont(new Font("Segoe UI",Font.BOLD,11)); strength.setForeground(AuthUi.GOOD); s.add(meter,BorderLayout.CENTER);s.add(strength,BorderLayout.EAST);AuthUi.left(s);p.add(s);AuthUi.gap(p,18);p.add(AuthUi.field("Confirm password",confirm));AuthUi.gap(p,4);p.add(error);AuthUi.gap(p,10);
        JButton next=AuthUi.primary("Continue to business details");next.addActionListener(e->next());p.add(next);AuthUi.gap(p,20); JPanel sw=new JPanel(new FlowLayout(FlowLayout.CENTER,4,0));sw.setOpaque(false);JLabel a=AuthUi.text("Already have an account?"),b=AuthUi.text("Sign in");AuthUi.link(b,()->((MainFrame)frame).showScreen(MainFrame.LOGIN));sw.add(a);sw.add(b);p.add(sw);return p;
    }
    private void score(){String v=new String(password.getPassword());int n=0;if(v.length()>=6)n++;if(v.length()>=10)n++;if(v.matches(".*\\d.*"))n++;if(v.matches(".*[^A-Za-z0-9].*"))n++;meter.setValue(n);strength.setText(v.isEmpty()?" ":new String[]{"","Weak","Fair","Good","Strong"}[n]);}
    private void next(){
        String msg=null; JTextField focus=null; char[] a=password.getPassword(),b=confirm.getPassword();
        if(fullName.getText().trim().isEmpty()){msg="Full name is required.";focus=fullName;}else if(!username.getText().trim().matches("[A-Za-z0-9_.-]{3,100}")){msg="Username must be at least 3 characters.";focus=username;}else if(!email.getText().trim().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")){msg="Enter a valid email address.";focus=email;}else if(phone.getText().trim().isEmpty()){msg="Phone number is required.";focus=phone;}else if(a.length<8){msg="Password must be at least 8 characters.";focus=password;}else if(!strong(new String(a))){msg="Use upper-case, lower-case, a number, and a symbol in your password.";focus=password;}else if(!Arrays.equals(a,b)){msg="Passwords do not match.";focus=confirm;}
        if(msg!=null){AuthUi.error(focus,error,msg);focus.requestFocusInWindow();Arrays.fill(a,'\0');Arrays.fill(b,'\0');return;}
        User user=new User();user.setFullName(fullName.getText().trim());user.setUsername(username.getText().trim());user.setEmail(email.getText().trim());user.setPhone(phone.getText().trim());user.setRegistrationPassword(a);Arrays.fill(a,'\0');Arrays.fill(b,'\0');((MainFrame)frame).passUserToBusinessInfo(user);
    }
    private boolean strong(String value){return value.chars().anyMatch(Character::isUpperCase)&&value.chars().anyMatch(Character::isLowerCase)&&value.chars().anyMatch(Character::isDigit)&&value.chars().anyMatch(c->!Character.isLetterOrDigit(c));}
}
