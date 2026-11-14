package com.businessmanager.theme;

import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.*;
import java.awt.*;

/**
 * Global theme configuration for Account Management.
 *
 * Uses FlatLaf Dark as the base Look & Feel and applies
 * the Account Management / Stitch design system.
 */
public final class AppTheme {

    private AppTheme() {
        // Prevent object creation
    }

    /**
     * Initializes the complete application theme.
     */
    public static void setup() {

        try {

            // =================================================
            // FLATLAF DARK THEME
            // =================================================

            UIManager.setLookAndFeel(
                    new FlatDarkLaf()
            );

        } catch (UnsupportedLookAndFeelException e) {

            System.err.println(
                    "Unable to initialize FlatLaf."
            );

            e.printStackTrace();
        }

        configureDefaults();
    }


    /**
     * Applies Account Management's global UI settings.
     */
    private static void configureDefaults() {

        // =====================================================
        // GENERAL
        // =====================================================

        UIManager.put(
                "Panel.background",
                Colors.BACKGROUND
        );

        UIManager.put(
                "OptionPane.background",
                Colors.SURFACE
        );

        UIManager.put(
                "Label.foreground",
                Colors.TEXT_PRIMARY
        );


        // =====================================================
        // TEXT FIELDS
        // =====================================================

        UIManager.put(
                "TextField.background",
                Colors.SURFACE_LIGHT
        );

        UIManager.put(
                "TextField.foreground",
                Colors.TEXT_PRIMARY
        );

        UIManager.put(
                "TextField.placeholderForeground",
                Colors.TEXT_MUTED
        );

        UIManager.put(
                "TextField.caretForeground",
                Colors.TEXT_PRIMARY
        );

        UIManager.put(
                "TextField.showClearButton",
                true
        );


        // =====================================================
        // BUTTONS
        // =====================================================

        UIManager.put(
                "Button.background",
                Colors.PRIMARY
        );

        UIManager.put(
                "Button.foreground",
                Colors.WHITE
        );

        UIManager.put(
                "Button.hoverBackground",
                Colors.PRIMARY_HOVER
        );

        UIManager.put(
                "Button.pressedBackground",
                Colors.PRIMARY_DARK
        );

        UIManager.put("Button.margin", new Insets(9, 16, 9, 16));
        UIManager.put("Button.borderWidth", 0);
        UIManager.put("Button.default.boldText", true);


        // =====================================================
        // TABLE
        // =====================================================

        UIManager.put(
                "Table.background",
                Colors.SURFACE
        );

        UIManager.put(
                "Table.foreground",
                Colors.TEXT_PRIMARY
        );

        UIManager.put(
                "Table.selectionBackground",
                Colors.PRIMARY_DARK
        );

        UIManager.put(
                "Table.selectionForeground",
                Colors.WHITE
        );

        UIManager.put(
                "Table.gridColor",
                new Color(0, 0, 0, 0)
        );

        UIManager.put(
                "Table.rowHeight",
                40
        );

        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("Table.showVerticalLines", false);
        UIManager.put("Table.alternateRowColor", Colors.SURFACE_CONTAINER_LOW);


        // =====================================================
        // TABLE HEADER
        // =====================================================

        UIManager.put(
                "TableHeader.background",
                Colors.SURFACE_LIGHT
        );

        UIManager.put(
                "TableHeader.foreground",
                Colors.TEXT_SECONDARY
        );
        UIManager.put("TableHeader.height", 42);
        UIManager.put("TableHeader.separatorColor", Colors.OUTLINE_VARIANT);


        // =====================================================
        // SCROLL BAR
        // =====================================================

        UIManager.put(
                "ScrollBar.track",
                Colors.BACKGROUND
        );

        UIManager.put(
                "ScrollBar.thumb",
                Colors.BORDER_LIGHT
        );

        UIManager.put(
                "ScrollBar.width",
                10
        );


        // =====================================================
        // FLATLAF ROUNDNESS
        // =====================================================

        /*
         * Global component roundness.
         */
        UIManager.put(
                "Component.arc",
                12
        );

        UIManager.put(
                "Button.arc",
                12
        );

        UIManager.put(
                "TextComponent.arc",
                12
        );


        // =====================================================
        // FOCUS
        // =====================================================

        UIManager.put(
                "Component.focusWidth",
                1
        );

        UIManager.put(
                "Component.innerFocusWidth",
                0
        );

        UIManager.put(
                "Button.focusWidth",
                0
        );

        UIManager.put("Component.focusColor", Colors.PRIMARY);
        UIManager.put("TabbedPane.selectedBackground", Colors.SURFACE_CONTAINER_HIGH);
        UIManager.put("TabbedPane.underlineColor", Colors.PRIMARY);
        UIManager.put("defaultFont", Fonts.BODY);
        UIManager.put("Label.font", Fonts.BODY);
        UIManager.put("TextField.font", Fonts.BODY);
        UIManager.put("PasswordField.font", Fonts.BODY);
        UIManager.put("PasswordField.background", Colors.SURFACE_LIGHT);
        UIManager.put("PasswordField.foreground", Colors.TEXT_PRIMARY);
        UIManager.put("PasswordField.caretForeground", Colors.PRIMARY);
        UIManager.put("ComboBox.font", Fonts.BODY);
        UIManager.put("ComboBox.background", Colors.SURFACE_LIGHT);
        UIManager.put("ComboBox.foreground", Colors.TEXT_PRIMARY);
        UIManager.put("ComboBox.selectionBackground", Colors.PRIMARY_DARK);
        UIManager.put("ComboBox.selectionForeground", Colors.WHITE);
        UIManager.put("Button.font", Fonts.BODY_BOLD);
        UIManager.put("Button.minimumWidth", 88);
        UIManager.put("Table.font", Fonts.BODY);
        UIManager.put("TableHeader.font", Fonts.BODY_BOLD);
        UIManager.put("TableHeader.hoverBackground", Colors.SURFACE_CONTAINER_HIGH);
        UIManager.put("ScrollPane.background", Colors.SURFACE);
        UIManager.put("ScrollPane.border", BorderFactory.createLineBorder(Colors.OUTLINE_VARIANT));
        UIManager.put("TabbedPane.background", Colors.SURFACE);
        UIManager.put("TabbedPane.foreground", Colors.TEXT_SECONDARY);
        UIManager.put("TabbedPane.selectedForeground", Colors.TEXT_PRIMARY);
        UIManager.put("TabbedPane.tabHeight", 42);
        UIManager.put("CheckBox.background", Colors.SURFACE);
        UIManager.put("CheckBox.foreground", Colors.TEXT_PRIMARY);
        UIManager.put("CheckBox.font", Fonts.BODY);
        UIManager.put("RadioButton.background", Colors.SURFACE);
        UIManager.put("RadioButton.foreground", Colors.TEXT_PRIMARY);
        UIManager.put("RadioButton.font", Fonts.BODY);
        UIManager.put("OptionPane.messageFont", Fonts.BODY);
        UIManager.put("OptionPane.buttonFont", Fonts.BODY_BOLD);
        UIManager.put("OptionPane.messageForeground", Colors.TEXT_PRIMARY);
        UIManager.put("ToolTip.background", Colors.SURFACE_CONTAINER_HIGH);
        UIManager.put("ToolTip.foreground", Colors.ON_SURFACE);
    }
}
