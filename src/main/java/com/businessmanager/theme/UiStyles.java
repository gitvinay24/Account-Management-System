package com.businessmanager.theme;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.JTableHeader;
import java.awt.*;

/** Shared visual rules for semantic controls and dynamically assembled forms. */
public final class UiStyles {
    public static final int CONTROL_HEIGHT = 40;
    private UiStyles() { }

    public enum ButtonRole { PRIMARY, SECONDARY, SUCCESS, DANGER, NEUTRAL }

    public static void button(JButton button, ButtonRole role) {
        button.setFont(Fonts.BODY_BOLD);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setMargin(new Insets(0, 16, 0, 16));
        button.setPreferredSize(minimumHeight(button.getPreferredSize(), CONTROL_HEIGHT));
        button.putClientProperty("JButton.buttonType", "roundRect");
        button.putClientProperty("Component.arc", 10);
        Color bg = Colors.SURFACE_LIGHT, fg = Colors.TEXT_SECONDARY, hover = Colors.SURFACE_CONTAINER_HIGHEST, pressed = Colors.SURFACE_CONTAINER;
        if (role == ButtonRole.PRIMARY) { bg = Colors.PRIMARY; fg = Colors.ON_PRIMARY; hover = Colors.PRIMARY_HOVER; pressed = Colors.PRIMARY_DARK; }
        if (role == ButtonRole.SUCCESS) { bg = Colors.SECONDARY; fg = Colors.ON_SECONDARY; hover = Colors.GOOD; pressed = Colors.PRIMARY_DARK; }
        if (role == ButtonRole.DANGER) { bg = Colors.ERROR; fg = Colors.ON_ERROR; hover = Colors.DANGER; pressed = Color.decode("#A84F42"); }
        if (role == ButtonRole.SECONDARY) { bg = Colors.SURFACE_CONTAINER_HIGH; fg = Colors.TEXT_PRIMARY; hover = Colors.SURFACE_CONTAINER_HIGHEST; pressed = Colors.SURFACE_CONTAINER; }
        button.setBackground(bg);
        button.setForeground(fg);
        button.putClientProperty("Button.hoverBackground", hover);
        button.putClientProperty("Button.pressedBackground", pressed);
    }

    public static void input(JTextField field) {
        field.setFont(Fonts.BODY);
        field.setBackground(Colors.SURFACE_LIGHT);
        field.setForeground(Colors.TEXT_PRIMARY);
        field.setCaretColor(Colors.PRIMARY);
        field.setBorder(new CompoundBorder(new LineBorder(Colors.OUTLINE, 1, true), new EmptyBorder(0, 12, 0, 12)));
        field.setPreferredSize(minimumHeight(field.getPreferredSize(), CONTROL_HEIGHT));
    }

    public static void comboBox(JComboBox<?> comboBox) {
        comboBox.setFont(Fonts.BODY);
        comboBox.setBackground(Colors.SURFACE_LIGHT);
        comboBox.setForeground(Colors.TEXT_PRIMARY);
        comboBox.setPreferredSize(minimumHeight(comboBox.getPreferredSize(), CONTROL_HEIGHT));
        comboBox.putClientProperty("JComponent.roundRect", true);
    }

    public static void table(JTable table) {
        table.setFont(Fonts.BODY);
        table.setRowHeight(40);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(Colors.OUTLINE_VARIANT);
        table.setSelectionBackground(Colors.PRIMARY_DARK);
        table.setSelectionForeground(Colors.WHITE);
        JTableHeader header = table.getTableHeader();
        if (header != null) { header.setFont(Fonts.BODY_BOLD); header.setPreferredSize(new Dimension(header.getPreferredSize().width, 42)); }
    }

    /** Applies the system to a completed view, including dynamically constructed forms. */
    public static void apply(Component root) {
        if (root instanceof JButton button && !Boolean.TRUE.equals(button.getClientProperty("ui.navigation"))) button(button, roleFor(button));
        else if (root instanceof JTextField field && !Boolean.TRUE.equals(field.getClientProperty("ui.authInput"))) input(field);
        else if (root instanceof JComboBox<?> comboBox) comboBox(comboBox);
        else if (root instanceof JTable table) table(table);
        if (root instanceof Container container) for (Component child : container.getComponents()) apply(child);
    }

    private static ButtonRole roleFor(JButton button) {
        String text = button.getText() == null ? "" : button.getText().toLowerCase();
        if (text.contains("delete") || text.contains("remove") || text.contains("log out") || text.contains("clear")) return ButtonRole.DANGER;
        if (text.contains("save") || text.contains("add") || text.contains("create") || text.contains("update") || text.contains("apply") || text.contains("pay") || text.contains("record")) return ButtonRole.PRIMARY;
        if (text.contains("cancel") || text.contains("close") || text.contains("back") || text.contains("export")) return ButtonRole.SECONDARY;
        return ButtonRole.NEUTRAL;
    }

    private static Dimension minimumHeight(Dimension source, int height) { return new Dimension(Math.max(0, source.width), Math.max(height, source.height)); }
}
