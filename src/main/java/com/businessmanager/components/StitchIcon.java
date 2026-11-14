package com.businessmanager.components;

import com.businessmanager.theme.Colors;

import javax.swing.*;
import java.awt.*;

/**
 * Material-style icons for the Account Management UI.
 *
 * This class is both:
 * 1. A Swing component (JComponent)
 * 2. A Swing Icon
 *
 * Therefore it can be:
 * - added directly to a JPanel
 * - passed to JButton.setIcon(...)
 */
public class StitchIcon extends JComponent implements Icon {

    public enum Type {
        PERSON,
        BADGE,
        MAIL,
        PHONE,
        LOCK,
        LOCK_RESET,
        VISIBILITY,
        VISIBILITY_OFF,
        ARROW_RIGHT,

        // Business Information icons
        STORE,
        CATEGORY,
        LOCATION,
        DESCRIPTION,
        PAYMENTS,
        EXPAND_MORE,
        STOREFRONT,
        DOMAIN,
        CHECK,
        LOGOUT
    }

    private static final int ICON_SIZE = 18;

    private final Type type;

    public StitchIcon(Type type) {

        this.type = type;

        setPreferredSize(
                new Dimension(
                        ICON_SIZE,
                        ICON_SIZE
                )
        );

        setMinimumSize(
                new Dimension(
                        ICON_SIZE,
                        ICON_SIZE
                )
        );

        setMaximumSize(
                new Dimension(
                        ICON_SIZE,
                        ICON_SIZE
                )
        );

        setOpaque(false);
    }

    // =========================================================
    // ICON INTERFACE
    // =========================================================

    @Override
    public int getIconWidth() {
        return ICON_SIZE;
    }

    @Override
    public int getIconHeight() {
        return ICON_SIZE;
    }

    @Override
    public void paintIcon(
            Component component,
            Graphics graphics,
            int x,
            int y
    ) {

        Graphics2D g2 =
                (Graphics2D) graphics.create();

        g2.translate(x, y);

        drawIcon(g2);

        g2.dispose();
    }

    // =========================================================
    // SWING COMPONENT
    // =========================================================

    @Override
    protected void paintComponent(
            Graphics g
    ) {

        super.paintComponent(g);

        Graphics2D g2 =
                (Graphics2D) g.create();

        drawIcon(g2);

        g2.dispose();
    }

    // =========================================================
    // DRAW ICON
    // =========================================================

    private void drawIcon(
            Graphics2D g2
    ) {

        g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
        );

        g2.setRenderingHint(
                RenderingHints.KEY_STROKE_CONTROL,
                RenderingHints.VALUE_STROKE_PURE
        );

        g2.setColor(
                Colors.OUTLINE
        );

        g2.setStroke(
                new BasicStroke(
                        1.5f,
                        BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND
                )
        );

        switch (type) {

            case PERSON ->
                    drawPerson(g2);

            case BADGE ->
                    drawBadge(g2);

            case MAIL ->
                    drawMail(g2);

            case PHONE ->
                    drawPhone(g2);

            case LOCK ->
                    drawLock(g2);

            case LOCK_RESET ->
                    drawLockReset(g2);

            case VISIBILITY ->
                    drawVisibility(g2);

            case VISIBILITY_OFF ->
                    drawVisibilityOff(g2);

            case ARROW_RIGHT ->
                    drawArrowRight(g2);

            case STORE ->
                    drawStore(g2);

            case CATEGORY ->
                    drawCategory(g2);

            case LOCATION ->
                    drawLocation(g2);

            case DESCRIPTION ->
                    drawDescription(g2);

            case PAYMENTS ->
                    drawPayments(g2);

            case EXPAND_MORE ->
                    drawExpandMore(g2);

            case STOREFRONT ->
                    drawStorefront(g2);

            case DOMAIN ->
                    drawDomain(g2);

            case CHECK ->
                    drawCheck(g2);

            case LOGOUT ->
                    drawLogout(g2);
        }
    }

    // =========================================================
    // PERSON
    // =========================================================

    private void drawPerson(
            Graphics2D g
    ) {

        g.fillOval(
                6,
                2,
                6,
                6
        );

        g.drawArc(
                3,
                9,
                12,
                8,
                0,
                180
        );
    }

    // =========================================================
    // BADGE
    // =========================================================

    private void drawBadge(
            Graphics2D g
    ) {

        g.drawRoundRect(
                3,
                2,
                12,
                14,
                2,
                2
        );

        g.drawOval(
                6,
                4,
                6,
                5
        );

        g.drawLine(
                5,
                12,
                13,
                12
        );
    }

    // =========================================================
    // MAIL
    // =========================================================

    private void drawMail(
            Graphics2D g
    ) {

        g.drawRoundRect(
                2,
                4,
                14,
                10,
                2,
                2
        );

        g.drawLine(
                3,
                5,
                9,
                10
        );

        g.drawLine(
                9,
                10,
                15,
                5
        );
    }

    // =========================================================
    // PHONE
    // =========================================================

    private void drawPhone(
            Graphics2D g
    ) {

        g.drawArc(
                4,
                2,
                10,
                14,
                80,
                200
        );

        g.drawLine(
                5,
                5,
                3,
                7
        );

        g.drawLine(
                13,
                13,
                15,
                11
        );
    }

    // =========================================================
    // LOCK
    // =========================================================

    private void drawLock(
            Graphics2D g
    ) {

        g.drawRoundRect(
                4,
                8,
                10,
                8,
                2,
                2
        );

        g.drawArc(
                6,
                3,
                6,
                8,
                0,
                180
        );

        g.fillOval(
                8,
                10,
                2,
                2
        );
    }

    // =========================================================
    // LOCK RESET
    // =========================================================

    private void drawLockReset(
            Graphics2D g
    ) {

        drawLock(g);

        g.drawArc(
                1,
                2,
                8,
                8,
                80,
                220
        );

        g.drawLine(
                2,
                3,
                2,
                6
        );

        g.drawLine(
                2,
                3,
                5,
                3
        );
    }

    // =========================================================
    // VISIBILITY
    // =========================================================

    private void drawVisibility(
            Graphics2D g
    ) {

        g.drawOval(
                2,
                5,
                14,
                8
        );

        g.fillOval(
                7,
                7,
                4,
                4
        );
    }

    // =========================================================
    // VISIBILITY OFF
    // =========================================================

    private void drawVisibilityOff(
            Graphics2D g
    ) {

        g.drawArc(
                2,
                5,
                14,
                8,
                200,
                140
        );

        g.drawLine(
                3,
                3,
                15,
                15
        );
    }

    // =========================================================
    // ARROW RIGHT
    // =========================================================

    private void drawArrowRight(
            Graphics2D g
    ) {

        g.drawLine(
                3,
                9,
                14,
                9
        );

        g.drawLine(
                10,
                5,
                14,
                9
        );

        g.drawLine(
                10,
                13,
                14,
                9
        );
    }

    // =========================================================
    // STORE
    // =========================================================

    private void drawStore(
            Graphics2D g
    ) {

        // Building
        g.drawRect(
                3,
                7,
                12,
                9
        );

        // Roof
        g.drawLine(
                3,
                7,
                4,
                3
        );

        g.drawLine(
                4,
                3,
                14,
                3
        );

        g.drawLine(
                14,
                3,
                15,
                7
        );

        // Door
        g.drawLine(
                6,
                7,
                6,
                16
        );

        g.drawLine(
                12,
                7,
                12,
                16
        );
    }

    // =========================================================
    // CATEGORY
    // =========================================================

    private void drawCategory(
            Graphics2D g
    ) {

        g.drawRoundRect(
                3,
                3,
                12,
                12,
                2,
                2
        );

        drawCircle(
                g,
                6,
                7,
                1
        );

        drawCircle(
                g,
                10,
                7,
                1
        );

        drawCircle(
                g,
                14,
                7,
                1
        );

        g.drawLine(
                5,
                11,
                13,
                11
        );
    }

    // =========================================================
    // LOCATION
    // =========================================================

    private void drawLocation(
            Graphics2D g
    ) {

        g.drawOval(
                4,
                2,
                10,
                12
        );

        g.fillOval(
                7,
                5,
                4,
                4
        );

        g.drawArc(
                6,
                10,
                6,
                6,
                0,
                180
        );
    }

    // =========================================================
    // DESCRIPTION
    // =========================================================

    private void drawDescription(
            Graphics2D g
    ) {

        g.drawRoundRect(
                3,
                2,
                12,
                14,
                2,
                2
        );

        g.drawLine(
                6,
                6,
                12,
                6
        );

        g.drawLine(
                6,
                9,
                12,
                9
        );

        g.drawLine(
                6,
                12,
                10,
                12
        );
    }

    // =========================================================
    // PAYMENTS
    // =========================================================

    private void drawPayments(
            Graphics2D g
    ) {

        g.drawRoundRect(
                2,
                4,
                14,
                10,
                2,
                2
        );

        g.drawLine(
                2,
                7,
                16,
                7
        );

        g.drawLine(
                5,
                11,
                8,
                11
        );
    }

    // =========================================================
    // EXPAND MORE
    // =========================================================

    private void drawExpandMore(
            Graphics2D g
    ) {

        g.drawLine(
                4,
                7,
                9,
                12
        );

        g.drawLine(
                9,
                12,
                14,
                7
        );
    }

    // =========================================================
    // STOREFRONT
    // =========================================================

    private void drawStorefront(
            Graphics2D g
    ) {

        g.drawRect(
                3,
                7,
                12,
                9
        );

        g.drawLine(
                2,
                7,
                16,
                7
        );

        g.drawLine(
                3,
                7,
                4,
                3
        );

        g.drawLine(
                4,
                3,
                14,
                3
        );

        g.drawLine(
                14,
                3,
                15,
                7
        );

        g.drawLine(
                6,
                10,
                12,
                10
        );
    }

    // =========================================================
    // DOMAIN
    // =========================================================

    private void drawDomain(
            Graphics2D g
    ) {

        g.drawRect(
                3,
                2,
                12,
                14
        );

        g.drawLine(
                6,
                6,
                12,
                6
        );

        g.drawLine(
                6,
                9,
                12,
                9
        );

        g.drawLine(
                6,
                12,
                10,
                12
        );
    }

    // =========================================================
    // CHECK
    // =========================================================

    private void drawCheck(
            Graphics2D g
    ) {

        g.drawLine(
                3,
                9,
                7,
                13
        );

        g.drawLine(
                7,
                13,
                15,
                5
        );
    }

    // =========================================================
    // LOGOUT
    // =========================================================

    private void drawLogout(
            Graphics2D g
    ) {
        g.drawArc(2, 3, 10, 12, 90, 180);
        g.drawLine(7, 3, 12, 3);
        g.drawLine(7, 15, 12, 15);
        g.drawLine(8, 9, 16, 9);
        g.drawLine(13, 6, 16, 9);
        g.drawLine(13, 12, 16, 9);
    }

    // =========================================================
    // CIRCLE HELPER
    // =========================================================

    private void drawCircle(
            Graphics2D g,
            int x,
            int y,
            int radius
    ) {

        g.fillOval(
                x - radius,
                y - radius,
                radius * 2,
                radius * 2
        );
    }
}
