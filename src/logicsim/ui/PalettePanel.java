package logicsim.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RadialGradientPaint;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/** Left-hand component library: draggable (or click-to-arm) parts plus circuit actions. */
@SuppressWarnings("serial")
final class PalettePanel extends JPanel implements Scrollable {

    private final CircuitCanvas canvas;

    PalettePanel(CircuitCanvas canvas) {
        this.canvas = canvas;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(true);
        setBackground(Theme.PANEL);
        setBorder(BorderFactory.createEmptyBorder(16, 14, 16, 14));

        add(new SectionLabel("Inputs and outputs"));
        add(Box.createVerticalStrut(8));
        add(new PaletteItem(NodeType.INPUT_SWITCH));
        add(Box.createVerticalStrut(8));
        add(new PaletteItem(NodeType.OUTPUT_LIGHT));

        add(Box.createVerticalStrut(18));
        add(new SectionLabel("Logic gates"));
        add(Box.createVerticalStrut(8));
        NodeType[] gates = { NodeType.AND, NodeType.OR, NodeType.NOT, NodeType.XOR };
        for (int i = 0; i < gates.length; i++) {
            add(new PaletteItem(gates[i]));
            add(Box.createVerticalStrut(8));
        }

        add(Box.createVerticalStrut(10));
        add(new SectionLabel("Circuit"));
        add(Box.createVerticalStrut(8));

        NeonButton demo = new NeonButton("Load demo circuit", Theme.CYAN);
        demo.addActionListener(e -> canvas.loadDemo());
        add(demo);
        add(Box.createVerticalStrut(8));

        NeonButton reset = new NeonButton("Reset all inputs", Theme.VIOLET);
        reset.addActionListener(e -> canvas.resetInputs());
        add(reset);
        add(Box.createVerticalStrut(8));

        NeonButton clear = new NeonButton("Clear canvas", Theme.MAGENTA);
        clear.addActionListener(e -> canvas.clearAll());
        add(clear);
        add(Box.createVerticalGlue());
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setPaint(new GradientPaint(0, 0, Theme.PANEL_HI.darker(), 0, getHeight(), Theme.BG_DEEP));
        g.fillRect(0, 0, getWidth(), getHeight());
        g.dispose();
    }

    // ----- Scrollable: track the viewport width so cards never overflow horizontally -----

    @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }

    @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 24; }

    @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return 120; }

    @Override public boolean getScrollableTracksViewportWidth() { return true; }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return getParent() != null && getParent().getHeight() > getPreferredSize().height;
    }

    // =====================================================================

    /** Small heading with a trailing rule. */
    @SuppressWarnings("serial")
    private static final class SectionLabel extends JComponent {
        private final String text;

        SectionLabel(String text) {
            this.text = text;
            setAlignmentX(LEFT_ALIGNMENT);
            setPreferredSize(new Dimension(100, 20));
            setMinimumSize(new Dimension(40, 20));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Theme.quality(g);
            g.setColor(Theme.CYAN);
            g.fillRect(0, 6, 3, 9);
            g.setFont(Theme.ui(java.awt.Font.BOLD, 11.5f));
            FontMetrics fm = g.getFontMetrics();
            g.setColor(Theme.TEXT_DIM);
            g.drawString(text, 10, 15);
            int lx = 10 + fm.stringWidth(text) + 10;
            g.setColor(Theme.alpha(Theme.CYAN, 55));
            g.draw(new Line2D.Double(lx, 10.5, getWidth(), 10.5));
            g.dispose();
        }
    }

    /** One draggable card. A drag drops on release; a plain click arms click-to-place. */
    @SuppressWarnings("serial")
    private final class PaletteItem extends JComponent {
        private final NodeType type;
        private Point pressPoint;
        private boolean dragged;
        private boolean hover;

        PaletteItem(NodeType type) {
            this.type = type;
            setAlignmentX(LEFT_ALIGNMENT);
            setPreferredSize(new Dimension(150, 54));
            setMinimumSize(new Dimension(100, 54));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 54));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText("Drag onto the canvas, or click here and then click the canvas");

            MouseAdapter ma = new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }

                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }

                @Override public void mousePressed(MouseEvent e) {
                    pressPoint = e.getPoint();
                    dragged = false;
                }

                @Override public void mouseDragged(MouseEvent e) {
                    if (pressPoint != null && e.getPoint().distance(pressPoint) > 4) {
                        dragged = true;
                    }
                    if (dragged) {
                        Point p = SwingUtilities.convertPoint(PaletteItem.this, e.getPoint(), canvas);
                        if (canvas.contains(p)) {
                            canvas.setGhost(PaletteItem.this.type, p);
                        } else {
                            canvas.setGhost(null, null);
                        }
                    }
                }

                @Override public void mouseReleased(MouseEvent e) {
                    Point p = SwingUtilities.convertPoint(PaletteItem.this, e.getPoint(), canvas);
                    canvas.setGhost(null, null);
                    if (dragged) {
                        if (canvas.contains(p)) {
                            canvas.placeNode(PaletteItem.this.type, p);
                        }
                        canvas.setArmed(null);
                    } else if (contains(e.getPoint())) {
                        NodeType cur = canvas.getArmed();
                        canvas.setArmed(PaletteItem.this.type == cur ? null : PaletteItem.this.type);
                    }
                    dragged = false;
                    repaint();
                }
            };
            addMouseListener(ma);
            addMouseMotionListener(ma);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Theme.quality(g);
            int w = getWidth();
            int h = getHeight();
            boolean armed = type == canvas.getArmed();
            Color edge = armed ? Theme.MAGENTA : type.accent;

            Path2D card = Theme.chamfer(1.5, 1.5, w - 3, h - 3, 10);
            if (armed || hover) {
                Theme.glow(g, card, edge, 1.4f);
            }
            g.setColor(Theme.alpha(Theme.mix(Theme.PANEL_HI, edge, armed ? 0.16f : hover ? 0.10f : 0.03f), 255));
            g.fill(card);
            g.setColor(Theme.alpha(edge, armed ? 255 : hover ? 230 : 110));
            g.setStroke(new BasicStroke(armed ? 1.8f : 1.3f));
            g.draw(card);

            paintIcon(g, 10, h / 2.0);

            g.setFont(Theme.ui(java.awt.Font.BOLD, 13.5f));
            g.setColor(Theme.TEXT);
            g.drawString(type.displayName, 78, h / 2 - 2);
            g.setFont(Theme.ui(java.awt.Font.PLAIN, 11f));
            g.setColor(Theme.TEXT_DIM);
            g.drawString(type.caption, 78, h / 2 + 13);
            g.dispose();
        }

        private void paintIcon(Graphics2D g, double x, double cy) {
            Color c = type.accent;
            if (type.isGate()) {
                g.setColor(Theme.alpha(c, 170));
                g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(new Line2D.Double(x, cy, x + 6, cy));
                g.draw(new Line2D.Double(x + 54, cy, x + 60, cy));
                GateRenderer.draw(g, type, x + 6, cy - 14, 48, 28, c,
                        Theme.alpha(Theme.mix(Theme.PANEL, c, 0.18f), 255), 1.8f, false);
            } else if (type == NodeType.INPUT_SWITCH) {
                RoundRectangle2D track = new RoundRectangle2D.Double(x + 8, cy - 9, 44, 18, 18, 18);
                g.setColor(Theme.alpha(Theme.SIGNAL_ON, 60));
                g.fill(track);
                g.setColor(Theme.SIGNAL_ON);
                g.setStroke(new BasicStroke(1.5f));
                g.draw(track);
                g.fill(new Ellipse2D.Double(x + 8 + 44 - 18 + 3, cy - 6, 12, 12));
            } else {
                g.setPaint(new RadialGradientPaint(new Point2D.Double(x + 30, cy), 24f, new float[] { 0f, 1f },
                        new Color[] { Theme.alpha(Theme.YELLOW, 120), Theme.alpha(Theme.YELLOW, 0) }));
                g.fill(new Ellipse2D.Double(x + 6, cy - 24, 48, 48));
                g.setColor(Theme.YELLOW);
                g.fill(new Ellipse2D.Double(x + 21, cy - 9, 18, 18));
                g.setColor(Theme.alpha(Color.WHITE, 200));
                g.fill(new Ellipse2D.Double(x + 25, cy - 6, 5, 4));
            }
            g.setStroke(new BasicStroke(1f));
        }
    }
}
