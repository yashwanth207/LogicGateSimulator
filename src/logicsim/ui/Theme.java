package logicsim.ui;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GraphicsEnvironment;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Path2D;
import java.util.HashSet;
import java.util.Set;

/** Central place for the neon / cyberpunk palette, fonts and small drawing helpers. */
final class Theme {

    private Theme() { }

    // ----- palette -----
    static final Color BG_DEEP    = new Color(4, 7, 14);
    static final Color BG_BASE    = new Color(8, 12, 23);
    static final Color PANEL      = new Color(10, 15, 28);
    static final Color PANEL_HI   = new Color(17, 25, 46);
    static final Color CYAN       = new Color(0, 229, 255);
    static final Color MAGENTA    = new Color(255, 46, 190);
    static final Color VIOLET     = new Color(166, 120, 255);
    static final Color ORANGE     = new Color(255, 150, 50);
    static final Color YELLOW     = new Color(255, 216, 64);
    static final Color SIGNAL_ON  = new Color(57, 255, 136);
    static final Color SIGNAL_OFF = new Color(150, 52, 88);
    static final Color TEXT       = new Color(222, 232, 250);
    static final Color TEXT_DIM   = new Color(122, 140, 176);
    static final Color TEXT_FAINT = new Color(72, 87, 120);

    // ----- fonts (first installed family wins; logical fonts are the final fallback) -----
    private static final Set<String> FAMILIES = new HashSet<>();

    static {
        try {
            for (String f : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
                FAMILIES.add(f);
            }
        } catch (RuntimeException ignored) {
            // fall through to logical fonts
        }
    }

    private static String pick(String... names) {
        for (String n : names) {
            if (FAMILIES.contains(n)) {
                return n;
            }
        }
        return names[names.length - 1];
    }

    private static final String MONO_FAMILY = pick("JetBrains Mono", "Cascadia Mono", "Consolas", "Menlo",
            "DejaVu Sans Mono", Font.MONOSPACED);
    private static final String UI_FAMILY = pick("Segoe UI", "SF Pro Text", "Helvetica Neue", "Inter", "Roboto",
            "DejaVu Sans", Font.SANS_SERIF);

    static Font mono(int style, float size) {
        return new Font(MONO_FAMILY, style, 12).deriveFont(size);
    }

    static Font ui(int style, float size) {
        return new Font(UI_FAMILY, style, 12).deriveFont(size);
    }

    // ----- colour helpers -----
    static Color alpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, a)));
    }

    static Color mix(Color a, Color b, float t) {
        float u = 1f - t;
        return new Color(
                Math.round(a.getRed() * u + b.getRed() * t),
                Math.round(a.getGreen() * u + b.getGreen() * t),
                Math.round(a.getBlue() * u + b.getBlue() * t));
    }

    // ----- drawing helpers -----
    static void quality(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
    }

    /** Rectangle with the top-left and bottom-right corners cut at 45 degrees. */
    static Path2D chamfer(double x, double y, double w, double h, double c) {
        Path2D p = new Path2D.Double();
        p.moveTo(x + c, y);
        p.lineTo(x + w, y);
        p.lineTo(x + w, y + h - c);
        p.lineTo(x + w - c, y + h);
        p.lineTo(x, y + h);
        p.lineTo(x, y + c);
        p.closePath();
        return p;
    }

    /** Soft neon bloom: a few wide, very transparent strokes under the real line. */
    static void glow(Graphics2D g, Shape s, Color c, float coreWidth) {
        java.awt.Stroke old = g.getStroke();
        int[] alphas = { 14, 26, 44 };
        float[] extra = { 12f, 7f, 3.5f };
        for (int i = 0; i < alphas.length; i++) {
            g.setColor(alpha(c, alphas[i]));
            g.setStroke(new BasicStroke(coreWidth + extra[i], BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(s);
        }
        g.setStroke(old);
    }

    static float trackedWidth(FontMetrics fm, String s, float tracking) {
        float w = 0;
        for (int i = 0; i < s.length(); i++) {
            w += fm.charWidth(s.charAt(i)) + tracking;
        }
        return w - (s.isEmpty() ? 0 : tracking);
    }

    /** Draws text with extra letter-spacing; returns the drawn width. */
    static float drawTracked(Graphics2D g, String s, float x, float y, float tracking) {
        FontMetrics fm = g.getFontMetrics();
        float cx = x;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            g.drawString(String.valueOf(ch), cx, y);
            cx += fm.charWidth(ch) + tracking;
        }
        return cx - x - tracking;
    }

    static void withAlpha(Graphics2D g, float a, Runnable r) {
        java.awt.Composite old = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a));
        r.run();
        g.setComposite(old);
    }
}
