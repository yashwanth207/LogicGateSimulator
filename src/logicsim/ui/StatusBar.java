package logicsim.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Path2D;
import javax.swing.JComponent;

/** Bottom bar: current action, control hints, and cursor coordinates. */
@SuppressWarnings("serial")
final class StatusBar extends JComponent {

    private static final String HINTS =
            "Drag pin to pin to wire   |   Click a switch to toggle   |   Right-click to delete   |   Del removes selection";

    private final CircuitCanvas canvas;

    StatusBar(CircuitCanvas canvas) {
        this.canvas = canvas;
        setPreferredSize(new Dimension(100, 30));
        setOpaque(true);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Theme.quality(g);
        int w = getWidth();
        int h = getHeight();
        g.setColor(Theme.BG_DEEP);
        g.fillRect(0, 0, w, h);
        g.setColor(Theme.alpha(Theme.CYAN, 70));
        g.fillRect(0, 0, w, 1);

        String status = canvas.statusText();
        boolean idle = "Ready".equals(status);
        Color c = idle ? Theme.TEXT_DIM : Theme.CYAN;

        Path2D chevron = new Path2D.Double();
        chevron.moveTo(14, h / 2.0 - 5);
        chevron.lineTo(20, h / 2.0);
        chevron.lineTo(14, h / 2.0 + 5);
        g.setColor(c);
        g.setStroke(new java.awt.BasicStroke(1.8f, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
        g.draw(chevron);

        g.setFont(Theme.mono(java.awt.Font.PLAIN, 12f));
        FontMetrics fm = g.getFontMetrics();
        int base = (h + fm.getAscent() - fm.getDescent()) / 2;
        g.setColor(c);
        g.drawString(status, 30, base);
        int statusEnd = 30 + fm.stringWidth(status);

        Point p = canvas.cursorPoint();
        String pos = p.x < 0 ? "x ---  y ---" : String.format("x %03d  y %03d", p.x, p.y);
        int posW = fm.stringWidth(pos);
        g.setColor(Theme.TEXT_FAINT);
        g.drawString(pos, w - 16 - posW, base);

        g.setFont(Theme.ui(java.awt.Font.PLAIN, 11.5f));
        FontMetrics hf = g.getFontMetrics();
        int hw = hf.stringWidth(HINTS);
        int hx = w - 16 - posW - 30 - hw;
        if (hx > statusEnd + 40) {
            g.setColor(Theme.TEXT_FAINT);
            g.drawString(HINTS, hx, (h + hf.getAscent() - hf.getDescent()) / 2);
        }
        g.dispose();
    }
}
