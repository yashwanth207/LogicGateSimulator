package logicsim.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;

/** Draws IEEE-style gate symbols. Shared by the canvas, the palette icons and the drag ghost. */
final class GateRenderer {

    private static final double XOR_SHIFT = 9;

    private GateRenderer() { }

    private static Path2D andBody(double x, double y, double w, double h) {
        Path2D p = new Path2D.Double();
        p.moveTo(x, y);
        p.lineTo(x + w * 0.5, y);
        p.append(new Arc2D.Double(x, y, w, h, 90, -180, Arc2D.OPEN), true);
        p.lineTo(x, y + h);
        p.closePath();
        return p;
    }

    private static Path2D orBody(double x, double y, double w, double h) {
        Path2D p = new Path2D.Double();
        p.moveTo(x, y);
        p.curveTo(x + w * 0.50, y, x + w * 0.82, y + h * 0.22, x + w, y + h / 2);
        p.curveTo(x + w * 0.82, y + h * 0.78, x + w * 0.50, y + h, x, y + h);
        p.curveTo(x + w * 0.28, y + h * 0.72, x + w * 0.28, y + h * 0.28, x, y);
        p.closePath();
        return p;
    }

    private static Path2D notBody(double x, double y, double w, double h) {
        double tip = x + w - 10;
        Path2D p = new Path2D.Double();
        p.moveTo(x, y);
        p.lineTo(tip, y + h / 2);
        p.lineTo(x, y + h);
        p.closePath();
        return p;
    }

    static Shape body(NodeType t, double x, double y, double w, double h) {
        switch (t) {
            case AND: return andBody(x, y, w, h);
            case OR:  return orBody(x, y, w, h);
            case NOT: return notBody(x, y, w, h);
            default:  return orBody(x + XOR_SHIFT, y, w - XOR_SHIFT, h);
        }
    }

    /** XOR's extra back arc, or NOT's inversion bubble; null for AND / OR. */
    static Shape extra(NodeType t, double x, double y, double w, double h) {
        if (t == NodeType.XOR) {
            double bw = w - XOR_SHIFT;
            Path2D p = new Path2D.Double();
            p.moveTo(x, y + h);
            p.curveTo(x + bw * 0.28, y + h * 0.72, x + bw * 0.28, y + h * 0.28, x, y);
            return p;
        }
        if (t == NodeType.NOT) {
            return new Ellipse2D.Double(x + w - 10, y + h / 2 - 5, 10, 10);
        }
        return null;
    }

    /** X coordinate where the input leads meet the symbol (accounts for the concave OR / XOR back). */
    static double inputLeadEnd(NodeType t, double x, double w) {
        if (t == NodeType.OR) {
            return x + w * 0.19;
        }
        if (t == NodeType.XOR) {
            return x + (w - XOR_SHIFT) * 0.19;
        }
        return x;
    }

    /** Horizontal centre of the usable interior, for placing the text label. */
    static double labelCenter(NodeType t, double x, double w) {
        switch (t) {
            case AND: return x + w * 0.42;
            case NOT: return x + w * 0.30;
            case OR:  return x + w * 0.48;
            default:  return x + w * 0.52;
        }
    }

    static void draw(Graphics2D g, NodeType t, double x, double y, double w, double h,
                     Color line, Color fill, float strokeWidth, boolean glow) {
        Shape body = body(t, x, y, w, h);
        Shape extra = extra(t, x, y, w, h);
        if (glow) {
            Theme.glow(g, body, line, strokeWidth);
            if (extra != null) {
                Theme.glow(g, extra, line, strokeWidth);
            }
        }
        g.setColor(fill);
        g.fill(body);
        if (t == NodeType.NOT && extra != null) {
            g.fill(extra);
        }
        g.setColor(line);
        g.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(body);
        if (extra != null) {
            g.draw(extra);
        }
        g.setStroke(new BasicStroke(1f));
    }
}
