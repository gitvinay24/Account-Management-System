package com.businessmanager.theme;

import java.awt.Color;

/**
 * Central color palette for the Account Management application.
 * Based on the official Google Stitch design system.
 */
public final class Colors {

    private Colors() {
        // Prevent object creation
    }

    // =========================================================
    // STITCH OFFICIAL DESIGN TOKENS
    // =========================================================

    /** Main background / canvas: deep evergreen */
    public static final Color INK = Color.decode("#0B1F17");
    
    /** Secondary surface / sidebar: forest */
    public static final Color INK_2 = Color.decode("#123126");
    
    /** Panel surface / container card: evergreen */
    public static final Color INK_3 = Color.decode("#183E30");
    
    /** Border lines / dividers */
    public static final Color LINE = Color.decode("#285542");
    
    /** Primary text */
    public static final Color TEXT = Color.decode("#EFF8F2");
    
    /** Secondary text */
    public static final Color SLATE = Color.decode("#A7C5B2");
    
    /** Muted text */
    public static final Color SLATE_DIM = Color.decode("#6E927E");
    
    /** Primary accent: emerald */
    public static final Color MARIGOLD = Color.decode("#30A46C");
    
    /** Success indicator */
    public static final Color GOOD = Color.decode("#59C58C");
    
    /** Warning indicator: --warn #E0A458 */
    public static final Color WARN = Color.decode("#E0A458");
    
    /** Danger / error indicator: --danger #D8735C */
    public static final Color DANGER = Color.decode("#D8735C");
    
    /** Light green-neutral accent */
    public static final Color PARCHMENT = Color.decode("#F4FAF6");


    // =========================================================
    // COMPATIBILITY ALIASES & SURFACES
    // =========================================================

    public static final Color BACKGROUND = INK;
    public static final Color SURFACE = INK;
    public static final Color SURFACE_DIM = INK;
    public static final Color SURFACE_BRIGHT = INK_3;
    public static final Color SURFACE_CONTAINER_LOWEST = INK;
    public static final Color SURFACE_CONTAINER_LOW = INK_2;
    public static final Color SURFACE_CONTAINER = INK_3;
    public static final Color SURFACE_CONTAINER_HIGH = Color.decode("#20513E");
    public static final Color SURFACE_CONTAINER_HIGHEST = Color.decode("#285542");
    public static final Color SURFACE_LIGHT = SURFACE_CONTAINER_HIGH;
    public static final Color SIDEBAR = INK_2;

    public static final Color PRIMARY = MARIGOLD;
    public static final Color PRIMARY_HOVER = Color.decode("#45BA7B");
    public static final Color PRIMARY_CONTAINER = Color.decode("#143B2B");
    public static final Color ON_PRIMARY = Color.WHITE;
    public static final Color ON_PRIMARY_CONTAINER = MARIGOLD;
    public static final Color PRIMARY_FIXED = Color.decode("#D8F3E3");
    public static final Color PRIMARY_FIXED_DIM = MARIGOLD;
    public static final Color PRIMARY_DARK = Color.decode("#1D774C");

    public static final Color SECONDARY = GOOD;
    public static final Color SECONDARY_CONTAINER = Color.decode("#16432D");
    public static final Color ON_SECONDARY = Color.WHITE;

    public static final Color TERTIARY = WARN;
    public static final Color TERTIARY_CONTAINER = Color.decode("#352A18");
    public static final Color ON_TERTIARY = Color.WHITE;

    public static final Color ERROR = DANGER;
    public static final Color ERROR_CONTAINER = Color.decode("#351C18");
    public static final Color ON_ERROR = Color.WHITE;

    public static final Color ON_SURFACE = TEXT;
    public static final Color ON_SURFACE_VARIANT = SLATE;
    public static final Color TEXT_PRIMARY = TEXT;
    public static final Color TEXT_SECONDARY = SLATE;
    public static final Color TEXT_MUTED = SLATE_DIM;
    public static final Color ON_BACKGROUND = TEXT;

    public static final Color OUTLINE = LINE;
    public static final Color OUTLINE_VARIANT = LINE;
    public static final Color BORDER = LINE;
    public static final Color BORDER_LIGHT = LINE;

    public static final Color SELECTION = new Color(48, 164, 108, 50);

    public static final Color WHITE = Color.WHITE;
    public static final Color BLACK = Color.BLACK;
}
