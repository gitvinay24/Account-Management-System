package com.businessmanager.ui.auth;

import com.businessmanager.theme.Colors;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

/** Shared visual language for the single-column authentication screens. */
final class AuthUi {
    static final Color INK=Colors.BACKGROUND, INK_2=Colors.SURFACE_CONTAINER_LOW, PARCHMENT=Colors.BACKGROUND, CHARCOAL=Colors.TEXT_PRIMARY, SLATE=Colors.TEXT_SECONDARY, MARIGOLD_DEEP=Colors.PRIMARY, LINE=Colors.OUTLINE, GOOD=Colors.GOOD, ERROR=Colors.ERROR;
    private static final String SANS=installed("Inter")?"Inter":"Segoe UI", SERIF=installed("Fraunces")?"Fraunces":"Georgia";
    static final Font BODY=new Font(SANS,Font.PLAIN,14), LABEL=new Font(SANS,Font.BOLD,12), HEADING=new Font(SERIF,Font.PLAIN,34);
    private AuthUi(){}
    private static boolean installed(String n){for(String f:GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames())if(n.equalsIgnoreCase(f))return true;return false;}

    /** Full-window parchment canvas with a naturally centered, scroll-safe form. */
    static JPanel shell(JPanel form,int width){
        form.setPreferredSize(new Dimension(width,form.getPreferredSize().height));form.setMaximumSize(new Dimension(width,Integer.MAX_VALUE));
        CenteringCanvas canvas=new CenteringCanvas();GridBagConstraints c=new GridBagConstraints();c.anchor=GridBagConstraints.CENTER;c.weightx=1;c.weighty=1;canvas.add(form,c);
        JScrollPane scroll=new JScrollPane(canvas);scroll.setBorder(null);scroll.setOpaque(false);scroll.getViewport().setOpaque(false);scroll.getVerticalScrollBar().setUnitIncrement(16);scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        JPanel root=new JPanel(new BorderLayout());root.setBackground(PARCHMENT);root.add(scroll,BorderLayout.CENTER);return root;
    }
    static JPanel formColumn(){JPanel p=new JPanel();p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));p.setBackground(PARCHMENT);p.setBorder(new EmptyBorder(28,28,28,28));return p;}
    static void left(JComponent c){c.setAlignmentX(Component.LEFT_ALIGNMENT);} static void gap(JPanel p,int h){p.add(Box.createVerticalStrut(h));}
    static JLabel brand(){JLabel l=new JLabel("◢  Account Management");l.setFont(new Font(SANS,Font.BOLD,15));l.setForeground(INK);left(l);return l;}
    static JLabel heading(String value){JLabel l=new JLabel(value);l.setFont(HEADING);l.setForeground(INK);left(l);return l;} static JLabel text(String value){JLabel l=new JLabel(value);l.setFont(BODY);l.setForeground(SLATE);left(l);return l;} static JLabel label(String value){JLabel l=new JLabel(value.toUpperCase());l.setFont(LABEL);l.setForeground(SLATE);left(l);return l;}
    static JPanel field(String caption,JComponent input){JPanel p=new JPanel();p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));p.setOpaque(false);left(p);p.add(label(caption));gap(p,7);input.setMaximumSize(new Dimension(Integer.MAX_VALUE,44));input.setPreferredSize(new Dimension(250,44));left(input);p.add(input);return p;}
    static JPanel pair(JComponent a,JComponent b){JPanel p=new JPanel(new GridLayout(1,2,16,0));p.setOpaque(false);p.setMaximumSize(new Dimension(Integer.MAX_VALUE,70));left(p);p.add(a);p.add(b);return p;}
    static JTextField input(String placeholder){return new AuthField(placeholder);}static JPasswordField password(String placeholder){AuthPassword p=new AuthPassword();p.putClientProperty("placeholder",placeholder);return p;}
    static JButton primary(String value){JButton b=button(value);b.setBackground(MARIGOLD_DEEP);b.setForeground(Color.WHITE);b.setBorder(new EmptyBorder(13,16,13,16));return b;}static JButton ghost(String value){JButton b=button(value);b.setBackground(INK_2);b.setForeground(Colors.TEXT_PRIMARY);b.setBorder(new CompoundBorder(new LineBorder(LINE,1,true),new EmptyBorder(12,16,12,16)));return b;}private static JButton button(String value){JButton b=new JButton(value);b.setFont(new Font(SANS,Font.BOLD,14));b.setFocusPainted(false);b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));left(b);b.setMaximumSize(new Dimension(Integer.MAX_VALUE,46));return b;}
    static JLabel inlineError(){JLabel l=new JLabel(" ");l.setFont(new Font(SANS,Font.PLAIN,12));l.setForeground(ERROR);left(l);return l;}static void error(JTextField f,JLabel m,String v){f.putClientProperty("error",v!=null);f.repaint();m.setText(v==null?" ":v);}static void link(JLabel l,Runnable action){l.setForeground(MARIGOLD_DEEP);l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));l.addMouseListener(new java.awt.event.MouseAdapter(){public void mouseClicked(java.awt.event.MouseEvent e){action.run();}});}
    static JPanel tracker(int current){JPanel p=new JPanel(new FlowLayout(FlowLayout.LEFT,26,0));p.setOpaque(false);p.setBorder(new MatteBorder(0,0,1,0,LINE));left(p);p.add(step("1  Personal details",current>1?GOOD:current==1?INK:Color.decode("#92A89B"),current==1));p.add(step("2  Business details",current==2?INK:Color.decode("#92A89B"),current==2));return p;}private static JLabel step(String t,Color c,boolean active){JLabel l=new JLabel(t);l.setFont(new Font(SANS,active?Font.BOLD:Font.PLAIN,13));l.setForeground(c);l.setBorder(new EmptyBorder(0,0,12,0));return l;}
    private static class AuthField extends JTextField{AuthField(String p){putClientProperty("placeholder",p);setup(this);}protected void paintComponent(Graphics g){super.paintComponent(g);decorate(g,this);}}
    private static class AuthPassword extends JPasswordField{AuthPassword(){setup(this);}protected void paintComponent(Graphics g){super.paintComponent(g);decorate(g,this);}}
    private static void setup(JTextField f){f.putClientProperty("ui.authInput",true);f.setFont(BODY);f.setForeground(CHARCOAL);f.setBackground(INK_2);f.setCaretColor(MARIGOLD_DEEP);f.setBorder(new EmptyBorder(0,13,0,13));f.addFocusListener(new FocusAdapter(){public void focusGained(FocusEvent e){f.repaint();}public void focusLost(FocusEvent e){f.repaint();}});}
    private static void decorate(Graphics g,JTextField f){if(f.getText().isEmpty()&&!f.hasFocus()){Object p=f.getClientProperty("placeholder");if(p!=null){g.setColor(Colors.TEXT_MUTED);g.setFont(BODY);g.drawString(p.toString(),14,f.getHeight()/2+5);}}Graphics2D g2=(Graphics2D)g.create();g2.setColor(Boolean.TRUE.equals(f.getClientProperty("error"))?ERROR:f.hasFocus()?MARIGOLD_DEEP:LINE);g2.setStroke(new BasicStroke(f.hasFocus()?2:1));g2.drawRoundRect(1,1,f.getWidth()-3,f.getHeight()-3,10,10);g2.dispose();}
    private static class CenteringCanvas extends JPanel implements Scrollable{CenteringCanvas(){super(new GridBagLayout());setOpaque(true);setBackground(PARCHMENT);}protected void paintComponent(Graphics g){super.paintComponent(g);Graphics2D g2=(Graphics2D)g.create();g2.setPaint(new GradientPaint(0,0,Colors.INK_2,0,getHeight(),Colors.BACKGROUND));g2.fillRect(0,0,getWidth(),getHeight());g2.dispose();}public Dimension getPreferredScrollableViewportSize(){return getPreferredSize();}public int getScrollableUnitIncrement(Rectangle r,int o,int d){return 16;}public int getScrollableBlockIncrement(Rectangle r,int o,int d){return Math.max(16,o==SwingConstants.VERTICAL?r.height-32:r.width-32);}public boolean getScrollableTracksViewportWidth(){return true;}public boolean getScrollableTracksViewportHeight(){return getParent() instanceof JViewport v&&v.getHeight()>=getPreferredSize().height;}}
}
