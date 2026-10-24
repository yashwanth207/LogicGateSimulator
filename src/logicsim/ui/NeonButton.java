package logicsim.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import javax.swing.JButton;

/** Flat, chamfered action button with an accent tick and a hover glow. */
@SuppressWarnings("serial")
final class NeonButton extends JButton {

    private final Color accent;

    NeonButton(String text, Color accent) {
        super(text);
        this.accent = accent;
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFont(Theme.ui(java.awt.Font.BOLD, 12.5f));
        setAlignmentX(LEFT_ALIGNMENT);
        setPreferredSize(new Dimension(100, 38));
        setMinimumSize(new Dimension(60, 38));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Theme.quality(g);
        int w = getWidth();
        int h = getHeight();
        boolean hover = getModel().isRollover();
        boolean press = getModel().isArmed() && getModel().isPressed();

        Path2D shape = Theme.chamfer(1.5, 1.5, w - 3, h - 3, 9);
        if (hover) {
            Theme.glow(g, shape, accent, 1.4f);
        }
        g.setColor(Theme.alpha(accent, press ? 80 : hover ? 46 : 20));
        g.fill(shape);
        g.setColor(Theme.alpha(accent, hover ? 255 : 160));
        g.setStroke(new java.awt.BasicStroke(1.4f));
        g.draw(shape);

        g.setColor(accent);
        g.fillRect(12, h / 2 - 6, 3, 12);

        g.setFont(getFont());
        FontMetrics fm = g.getFontMetrics();
        g.setColor(hover ? Color.WHITE : Theme.TEXT);
        g.drawString(getText(), 26, (h + fm.getAscent() - fm.getDescent()) / 2);
        g.dispose();
    }
}
