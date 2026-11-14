package com.businessmanager.theme;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import javax.imageio.ImageIO;

public final class StitchAssets {

    private static BufferedImage logoImage;
    private static BufferedImage authBgImage;

    static {
        try (InputStream logoStream = StitchAssets.class.getResourceAsStream("/assets/logo.png")) {
            if (logoStream != null) {
                logoImage = ImageIO.read(logoStream);
            }
        } catch (Exception e) {
            System.err.println("Could not load logo.png: " + e.getMessage());
        }

        try (InputStream bgStream = StitchAssets.class.getResourceAsStream("/assets/auth_bg.png")) {
            if (bgStream != null) {
                authBgImage = ImageIO.read(bgStream);
            }
        } catch (Exception e) {
            System.err.println("Could not load auth_bg.png: " + e.getMessage());
        }
    }

    private StitchAssets() {}

    public static ImageIcon getLogoIcon(int width, int height) {
        if (logoImage == null) {
            return null;
        }
        Image scaled = logoImage.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }

    public static Image getAuthBgImage() {
        return authBgImage;
    }
}
