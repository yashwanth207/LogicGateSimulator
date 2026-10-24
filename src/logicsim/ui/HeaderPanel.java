package logicsim.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import javax.swing.JComponent;

/** Top bar: wordmark on the left, live circuit statistics on the right. */
@SuppressWarnings("serial")
final class HeaderPanel extends JComponent {

    private final CircuitCanvas canvas;

    HeaderPanel(CircuitCanvas canvas) {
        this.canvas = canvas;
        setPreferredSize(new Dimension(100, 64));
        setOpaque(true);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Theme.quality(g);
        int w = getWidth();
        int h = getHeight();

        g.setPaint(new GradientPaint(0, 0, Theme.PANEL_HI.darker(), w, 0, Theme.BG_DEEP));
        g.fillRect(0, 0, w, h);

        // logo glyph: a small AND gate in a chamfered frame
        Path2D frame = Theme.chamfer(18, 14, 46, 36, 9);
        Theme.glow(g, frame, Theme.CYAN, 1.5f);
        g.setColor(Theme.alpha(Theme.CYAN, 22));
        g.fill(frame);
        g.setColor(Theme.CYAN);
        g.setStroke(new java.awt.BasicStroke(1.5f));
        g.draw(frame);
        GateRenderer.draw(g, NodeType.AND, 28, 23, 24, 18, Theme.CYAN, Theme.alpha(Theme.CYAN, 40), 1.8f, false);
        g.setColor(Theme.SIGNAL_ON);
        g.fill(new Ellipse2D.Double(54, 29, 5, 5));

        // wordmark
        g.setFont(Theme.ui(java.awt.Font.BOLD, 23f));
        g.setPaint(new GradientPaint(80, 0, Theme.CYAN, 240, 0, Theme.MAGENTA));
        Theme.drawTracked(g, "LOGICGRID", 80, 31, 3.5f);

        g.setFont(Theme.ui(java.awt.Font.PLAIN, 12f));
        g.setColor(Theme.TEXT_DIM);
        g.drawString("Digital circuit simulator", 82, 49);

        // statistic chips, right-aligned
        String[] labels = { "Components", "Wires", "High signals" };
        int[] values = { canvas.nodeCount(), canvas.wireCount(), canvas.highWireCount() };
        Color[] colors = { Theme.CYAN, Theme.VIOLET, values[2] > 0 ? Theme.SIGNAL_ON : Theme.TEXT_DIM };
        double right = w - 20;
        for (int i = labels.length - 1; i >= 0; i--) {
            g.setFont(Theme.ui(java.awt.Font.PLAIN, 11.5f));
            FontMetrics lf = g.getFontMetrics();
            g.setFont(Theme.mono(java.awt.Font.BOLD, 17f));
            FontMetrics vf = g.getFontMetrics();
            String v = String.valueOf(values[i]);
            double cw = 18 + lf.stringWidth(labels[i]) + 12 + vf.stringWidth(v) + 16;
            double x = right - cw;
            Path2D chip = Theme.chamfer(x, 13, cw, 38, 9);
            g.setColor(Theme.alpha(colors[i], 18));
            g.fill(chip);
            g.setColor(Theme.alpha(colors[i], 130));
            g.setStroke(new java.awt.BasicStroke(1.2f));
            g.draw(chip);

            g.setFont(Theme.ui(java.awt.Font.PLAIN, 11.5f));
            g.setColor(Theme.TEXT_DIM);
            g.drawString(labels[i], (float) (x + 14), 36);
            g.setFont(Theme.mono(java.awt.Font.BOLD, 17f));
            g.setColor(colors[i]);
            g.drawString(v, (float) (x + 14 + lf.stringWidth(labels[i]) + 12), 38);
            right = x - 10;
        }

        // bottom neon rule
        g.setPaint(new GradientPaint(0, 0, Theme.alpha(Theme.CYAN, 255), w * 0.65f, 0, Theme.alpha(Theme.MAGENTA, 200)));
        g.fillRect(0, h - 2, (int) (w * 0.65), 2);
        g.setPaint(new GradientPaint(w * 0.65f, 0, Theme.alpha(Theme.MAGENTA, 200), w, 0, Theme.alpha(Theme.MAGENTA, 0)));
        g.fillRect((int) (w * 0.65), h - 2, w - (int) (w * 0.65), 2);
        g.setPaint(new GradientPaint(0, h - 8, Theme.alpha(Theme.CYAN, 0), 0, h - 2, Theme.alpha(Theme.CYAN, 38)));
        g.fillRect(0, h - 8, w, 6);
        g.dispose();
    }
}
