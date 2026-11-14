package com.businessmanager.ui.auth;

import com.businessmanager.enums.BusinessType;
import com.businessmanager.model.Business;
import com.businessmanager.model.User;
import com.businessmanager.service.AuthService;
import com.businessmanager.ui.MainFrame;
import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

/** Step two: business details and business-size choice, committed through AuthService. */
public class BusinessInfoPanel extends JPanel {
    private final JFrame frame;
    private User draftUser;
    private final JTextField name=AuthUi.input("Rao General Store"), tax=AuthUi.input("22AAAAA0000A1Z5"), address=AuthUi.input("Shop 4, MI Road, Jaipur");
    private final JComboBox<String> category=new JComboBox<>(new String[]{"Retail & general store","Food & beverage","Services","Wholesale"});
    private final JComboBox<String> currency=new JComboBox<>(new String[]{"₹ Indian Rupee","$ US Dollar","€ Euro"});
    private final JLabel error=AuthUi.inlineError();
    private ChoiceCard small, large; private BusinessType type=BusinessType.SMALL;

    public BusinessInfoPanel(JFrame frame){this.frame=frame;setLayout(new BorderLayout());setBackground(AuthUi.PARCHMENT);add(AuthUi.shell(form(), 600),BorderLayout.CENTER);}
    public void setDraftUser(User user){draftUser=user;}
    private JPanel form(){
        JPanel p=AuthUi.formColumn();p.add(AuthUi.brand());AuthUi.gap(p,32);p.add(AuthUi.tracker(2));AuthUi.gap(p,30);p.add(AuthUi.heading("Now, about the business"));AuthUi.gap(p,10);p.add(AuthUi.text("This shapes your dashboard, invoices, and reports."));AuthUi.gap(p,28);
        category.setFont(AuthUi.BODY);currency.setFont(AuthUi.BODY);styleCombo(category);styleCombo(currency);
        p.add(AuthUi.pair(AuthUi.field("Business name",name),AuthUi.field("Category",category)));AuthUi.gap(p,18);p.add(AuthUi.pair(AuthUi.field("GST / Tax ID (optional)",tax),AuthUi.field("Currency",currency)));AuthUi.gap(p,18);p.add(AuthUi.field("Business address",address));AuthUi.gap(p,20);
        JPanel types=new JPanel(new GridLayout(1,2,12,0));types.setOpaque(false);types.setMaximumSize(new Dimension(Integer.MAX_VALUE,100));small=new ChoiceCard("Just me, or a small team","One till, simple stock, a handful of staff logins.",BusinessType.SMALL);large=new ChoiceCard("Multiple locations","Several outlets, warehousing, and role-based access.",BusinessType.LARGE);types.add(small);types.add(large);p.add(types);AuthUi.gap(p,4);p.add(error);AuthUi.gap(p,8);
        JPanel buttons=new JPanel(new GridLayout(1,2,12,0));buttons.setOpaque(false);buttons.setMaximumSize(new Dimension(Integer.MAX_VALUE,46));JButton back=AuthUi.ghost("Back"),create=AuthUi.primary("Create business");back.addActionListener(e->((MainFrame)frame).showScreen(MainFrame.CREATE_ACCOUNT));create.addActionListener(e->create());buttons.add(back);buttons.add(create);p.add(buttons);AuthUi.gap(p,20);p.add(AuthUi.text("By creating a business, you agree to the Terms of Service and Privacy Policy."));return p;
    }
    private void styleCombo(JComboBox<String> box){box.setBackground(Color.WHITE);box.setForeground(AuthUi.CHARCOAL);box.setBorder(BorderFactory.createLineBorder(AuthUi.LINE));box.setMaximumSize(new Dimension(Integer.MAX_VALUE,44));}
    private void pick(BusinessType selection){type=selection;small.repaint();large.repaint();}
    private void create(){
        if(draftUser==null){AuthUi.error(name,error,"Registration details are missing. Please start again.");return;} if(name.getText().trim().isEmpty()){AuthUi.error(name,error,"Business name is required.");name.requestFocusInWindow();return;} if(address.getText().trim().isEmpty()){AuthUi.error(address,error,"Business address is required.");address.requestFocusInWindow();return;}
        Business business=new Business();business.setName(name.getText().trim());business.setTaxId(tax.getText().trim());business.setAddress(address.getText().trim());business.setEmail(draftUser.getEmail());business.setPhone(draftUser.getPhone());business.setType(type);
        if (!((MainFrame) frame).ensureDatabaseReadyForRegisteredAccess()) return;
        char[] secret=draftUser.getRegistrationPassword();AuthService.AuthResult result=new AuthService().registerBusinessAndUser(draftUser,business,secret);if(secret!=null)Arrays.fill(secret,'\0');
        if(result.isSuccess()){draftUser.clearRegistrationPassword();((MainFrame)frame).onLoginSuccess();}else AuthUi.error(name,error,result.getMessage());
    }
    private class ChoiceCard extends JPanel { final BusinessType value; final JLabel title,desc; ChoiceCard(String t,String d,BusinessType value){this.value=value;setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));title=new JLabel(t);title.setFont(new Font("Segoe UI",Font.BOLD,14));desc=new JLabel("<html>"+d+"</html>");desc.setFont(new Font("Segoe UI",Font.PLAIN,12));add(title);add(Box.createVerticalStrut(5));add(desc);addMouseListener(new java.awt.event.MouseAdapter(){public void mouseClicked(java.awt.event.MouseEvent e){pick(value);}});}protected void paintComponent(Graphics g){setOpaque(true);boolean selected=type==value;setBackground(selected?Color.decode("#E7F4EC"):AuthUi.PARCHMENT);setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(selected?AuthUi.MARIGOLD_DEEP:AuthUi.LINE,1,true),BorderFactory.createEmptyBorder(14,14,12,14)));title.setForeground(AuthUi.INK);desc.setForeground(AuthUi.SLATE);super.paintComponent(g);}}
}
