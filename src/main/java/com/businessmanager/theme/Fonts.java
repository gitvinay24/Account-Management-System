package com.businessmanager.theme;

import java.awt.Font;

public final class Fonts {

    private Fonts() {
    }

    /* Inter is the Stitch font. Use a metrically close installed fallback when the project has
       no embedded Inter resource, rather than letting AWT silently select Dialog. */
    private static final String FONT_NAME = isInstalled("Inter") ? "Inter" : "Segoe UI";
    private static final String MONO_FONT_NAME = isInstalled("JetBrains Mono") ? "JetBrains Mono" : "Consolas";

    private static boolean isInstalled(String family) {
        for (String installed : java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
            if (family.equalsIgnoreCase(installed)) return true;
        }
        return false;
    }

    // 32px / 40px / 700
    public static final Font DISPLAY_LG =
            new Font(
                    FONT_NAME,
                    Font.BOLD,
                    32
            );

    // 24px / 32px / 600
    public static final Font HEADLINE_MD =
            new Font(
                    FONT_NAME,
                    Font.BOLD,
                    24
            );

    // 18px / 24px / 600
    public static final Font TITLE_SM =
            new Font(
                    FONT_NAME,
                    Font.BOLD,
                    18
            );

    // 14px / 20px / 400
    public static final Font BODY =
            new Font(
                    FONT_NAME,
                    Font.PLAIN,
                    14
            );

    // 14px / 20px / 700
    public static final Font BODY_BOLD =
            new Font(
                    FONT_NAME,
                    Font.BOLD,
                    14
            );

    // 13px / 18px / 400
    public static final Font BODY_SMALL =
            new Font(
                    FONT_NAME,
                    Font.PLAIN,
                    13
            );

    // 11px / 16px / 600
    public static final Font LABEL_CAPS =
            new Font(
                    FONT_NAME,
                    Font.BOLD,
                    11
            );

    public static final Font DATA_MONO =
            new Font(
                    MONO_FONT_NAME,
                    Font.PLAIN,
                    13
            );
}
