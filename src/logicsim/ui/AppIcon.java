package logicsim.ui;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/** Draws the window / taskbar icon at runtime, so no image resources need to ship with the jar. */
final class AppIcon {

    private AppIcon() { }

    static List<Image> images() {
        List<Image> list = new ArrayList<>();
        for (int s : new int[] { 16, 32, 48, 64, 128 }) {
            list.add(render(s));
        }
        return list;
    }

    private static BufferedImage render(int s) {
        BufferedImage img = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        Theme.quality(g);
        double m = s * 0.06;
        Path2D frame = Theme.chamfer(m, m, s - 2 * m, s - 2 * m, s * 0.22);
        g.setColor(Theme.BG_DEEP);
        g.fill(frame);
        g.setColor(Theme.CYAN);
        g.setStroke(new java.awt.BasicStroke(Math.max(1f, s * 0.05f)));
        g.draw(frame);
        double gx = s * 0.22;
        double gy = s * 0.30;
        GateRenderer.draw(g, NodeType.AND, gx, gy, s * 0.46, s * 0.40, Theme.CYAN,
                Theme.alpha(Theme.CYAN, 50), Math.max(1.2f, s * 0.06f), false);
        g.setColor(Theme.SIGNAL_ON);
        double d = Math.max(2, s * 0.11);
        g.fill(new Ellipse2D.Double(s * 0.74, s * 0.50 - d / 2, d, d));
        g.dispose();
        return img;
    }
}
