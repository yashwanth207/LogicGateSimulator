package logicsim.ui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.plaf.basic.BasicScrollBarUI;

/** Slim neon scrollbar so the palette stays on-theme when the window is short. */
@SuppressWarnings("serial")
final class ThinScrollBarUI extends BasicScrollBarUI {

    private static JButton zero() {
        JButton b = new JButton();
        b.setPreferredSize(new Dimension(0, 0));
        b.setMinimumSize(new Dimension(0, 0));
        b.setMaximumSize(new Dimension(0, 0));
        return b;
    }

    @Override protected JButton createDecreaseButton(int orientation) { return zero(); }

    @Override protected JButton createIncreaseButton(int orientation) { return zero(); }

    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
        g.setColor(Theme.PANEL);
        g.fillRect(r.x, r.y, r.width, r.height);
    }

    @Override
    protected void paintThumb(Graphics g0, JComponent c, Rectangle r) {
        if (r.isEmpty()) {
            return;
        }
        Graphics2D g = (Graphics2D) g0.create();
        Theme.quality(g);
        g.setColor(Theme.alpha(Theme.CYAN, isThumbRollover() ? 190 : 110));
        g.fillRoundRect(r.x + 2, r.y + 2, r.width - 4, r.height - 4, 4, 4);
        g.dispose();
    }
}
