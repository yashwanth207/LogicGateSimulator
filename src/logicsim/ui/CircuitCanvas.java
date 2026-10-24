package logicsim.ui;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import logicsim.engine.InputSwitch;

/**
 * The circuit workspace: renders the network, handles dragging, wiring, switch toggling,
 * palette drops and deletion. The engine is evaluated lazily, so every repaint shows live signal state.
 */
@SuppressWarnings({ "serial", "this-escape" })
public class CircuitCanvas extends JPanel {

    private static final int PIN_HIT = 11;
    private static final int GRID = 10;

    private static final class PinRef {
        final CircuitNode node;
        final int index;

        PinRef(CircuitNode node, int index) {
            this.node = node;
            this.index = index;
        }

        boolean is(CircuitNode n, int i) {
            return node == n && index == i;
        }
    }

    private final List<CircuitNode> nodes = new ArrayList<>();
    private CircuitNode selected;
    private CircuitNode dragNode;
    private CircuitNode wireSource;
    private CircuitNode hoverOutput;
    private PinRef hoverInput;
    private Point wireMouse;
    private Point pressPoint;
    private final Point mousePoint = new Point(-1, -1);
    private int grabDx;
    private int grabDy;
    private boolean moved;

    private NodeType armed;
    private NodeType ghostType;
    private Point ghostPoint;
    private Runnable armedListener = () -> { };

    private int switchCount;
    private int lightCount;
    private String caption;
    private String flashText;
    private long flashUntil;
    private float phase;
    private BufferedImage backdrop;

    public CircuitCanvas() {
        setOpaque(true);
        setBackground(Theme.BG_DEEP);
        setFocusable(true);
        setPreferredSize(new Dimension(900, 640));

        MouseAdapter ma = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { onPress(e); }
            @Override public void mouseDragged(MouseEvent e) { onDrag(e); }
            @Override public void mouseReleased(MouseEvent e) { onRelease(e); }
            @Override public void mouseMoved(MouseEvent e) { onMove(e); }
            @Override public void mouseExited(MouseEvent e) {
                mousePoint.setLocation(-1, -1);
                if (armed != null) {
                    ghostPoint = null;
                }
                hoverOutput = null;
                hoverInput = null;
            }
        };
        addMouseListener(ma);
        addMouseMotionListener(ma);

        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "del");
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "del");
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel");
        getActionMap().put("del", new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override public void actionPerformed(ActionEvent e) {
                if (selected != null) {
                    removeNode(selected);
                    selected = null;
                    repaint();
                }
            }
        });
        getActionMap().put("cancel", new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override public void actionPerformed(ActionEvent e) {
                setArmed(null);
                wireSource = null;
                wireMouse = null;
                repaint();
            }
        });
    }

    // =====================================================================
    //  Public API used by the palette, header and status bar
    // =====================================================================

    void setArmedListener(Runnable r) {
        this.armedListener = r;
    }

    NodeType getArmed() {
        return armed;
    }

    void setArmed(NodeType type) {
        this.armed = type;
        if (type == null) {
            ghostType = null;
            ghostPoint = null;
        }
        armedListener.run();
        repaint();
    }

    void setGhost(NodeType type, Point p) {
        this.ghostType = type;
        this.ghostPoint = p;
        repaint();
    }

    /** Drops a new node centred on {@code center} (snapped to the grid and kept inside the canvas). */
    CircuitNode placeNode(NodeType type, Point center) {
        Point o = origin(type, center);
        CircuitNode n = addNode(type, o.x, o.y, null);
        selected = n;
        caption = null;
        repaint();
        return n;
    }

    void clearAll() {
        nodes.clear();
        selected = null;
        dragNode = null;
        wireSource = null;
        wireMouse = null;
        switchCount = 0;
        lightCount = 0;
        caption = null;
        flash("Canvas cleared");
        repaint();
    }

    void resetInputs() {
        for (CircuitNode n : nodes) {
            if (n.logic instanceof InputSwitch) {
                ((InputSwitch) n.logic).setOn(false);
            }
        }
        flash("All switches set to 0");
        repaint();
    }

    /** Builds a half adder: SUM = A xor B, CARRY = A and B. */
    void loadDemo() {
        clearAll();
        int cw = getWidth() > 0 ? getWidth() : 900;
        int ch = getHeight() > 0 ? getHeight() : 640;
        int ox = snap(Math.max(-60, (cw - 660) / 2 - 80));
        int oy = snap(Math.max(-70, (ch - 246) / 2 - 150));
        CircuitNode a = addNode(NodeType.INPUT_SWITCH, 80 + ox, 130 + oy, "A");
        CircuitNode b = addNode(NodeType.INPUT_SWITCH, 80 + ox, 300 + oy, "B");
        CircuitNode xor = addNode(NodeType.XOR, 340 + ox, 150 + oy, "");
        CircuitNode and = addNode(NodeType.AND, 340 + ox, 310 + oy, "");
        CircuitNode sum = addNode(NodeType.OUTPUT_LIGHT, 620 + ox, 160 + oy, "SUM");
        CircuitNode carry = addNode(NodeType.OUTPUT_LIGHT, 620 + ox, 316 + oy, "CARRY");
        xor.connect(0, a);
        xor.connect(1, b);
        and.connect(0, a);
        and.connect(1, b);
        sum.connect(0, xor);
        carry.connect(0, and);
        ((InputSwitch) a.logic).setOn(true);
        selected = null;
        caption = "Half adder  |  SUM = A XOR B   CARRY = A AND B";
        flash("Demo circuit loaded - click the switches");
        repaint();
    }

    /** Advances the signal-flow animation and repaints. Called by the frame's timer. */
    void tick() {
        phase += 0.9f;
        repaint();
    }

    int nodeCount() {
        return nodes.size();
    }

    int wireCount() {
        int c = 0;
        for (CircuitNode n : nodes) {
            for (CircuitNode s : n.sources) {
                if (s != null) {
                    c++;
                }
            }
        }
        return c;
    }

    int highWireCount() {
        int c = 0;
        for (CircuitNode n : nodes) {
            for (CircuitNode s : n.sources) {
                if (s != null && s.output()) {
                    c++;
                }
            }
        }
        return c;
    }

    Point cursorPoint() {
        return mousePoint;
    }

    String statusText() {
        if (System.currentTimeMillis() < flashUntil) {
            return flashText;
        }
        if (armed != null) {
            return "Placing " + armed.displayName + " - click the canvas to drop it, Esc to cancel";
        }
        if (wireSource != null) {
            return "Wiring - release on an input pin to connect";
        }
        if (dragNode != null && moved) {
            return "Moving component";
        }
        if (selected != null) {
            return "Selected " + selected.type.displayName + " - Delete removes it";
        }
        return "Ready";
    }

    private void flash(String text) {
        flashText = text;
        flashUntil = System.currentTimeMillis() + 2600;
    }

    // =====================================================================
    //  Graph helpers
    // =====================================================================

    private CircuitNode addNode(NodeType type, int x, int y, String name) {
        CircuitNode n = new CircuitNode(type, x, y);
        if (name != null) {
            n.name = name;
        } else if (type == NodeType.INPUT_SWITCH) {
            n.name = String.valueOf((char) ('A' + (switchCount % 26)));
        } else if (type == NodeType.OUTPUT_LIGHT) {
            n.name = "Y" + (lightCount + 1);
        }
        if (type == NodeType.INPUT_SWITCH) {
            switchCount++;
        } else if (type == NodeType.OUTPUT_LIGHT) {
            lightCount++;
        }
        nodes.add(n);
        return n;
    }

    private Point origin(NodeType type, Point center) {
        int x = snap(center.x - CircuitNode.W / 2);
        int y = snap(center.y - type.height / 2);
        if (getWidth() > 0) {
            x = Math.max(0, Math.min(x, getWidth() - CircuitNode.W));
            y = Math.max(0, Math.min(y, getHeight() - type.height));
        }
        return new Point(x, y);
    }

    private static int snap(int v) {
        return Math.round(v / (float) GRID) * GRID;
    }

    private void removeNode(CircuitNode n) {
        nodes.remove(n);
        for (CircuitNode o : nodes) {
            for (int i = 0; i < o.sources.length; i++) {
                if (o.sources[i] == n) {
                    o.connect(i, null);
                }
            }
        }
        caption = null;
    }

    /** True when {@code target} sits upstream of {@code from} (or is {@code from}); wiring that way would loop. */
    private boolean reaches(CircuitNode from, CircuitNode target) {
        if (from == target) {
            return true;
        }
        for (CircuitNode s : from.sources) {
            if (s != null && reaches(s, target)) {
                return true;
            }
        }
        return false;
    }

    private CircuitNode nodeAt(Point p) {
        for (int i = nodes.size() - 1; i >= 0; i--) {
            if (nodes.get(i).contains(p)) {
                return nodes.get(i);
            }
        }
        return null;
    }

    private CircuitNode outputPinAt(Point p) {
        for (int i = nodes.size() - 1; i >= 0; i--) {
            CircuitNode n = nodes.get(i);
            if (n.type.hasOutput && p.distance(n.outputPin()) <= PIN_HIT) {
                return n;
            }
        }
        return null;
    }

    private PinRef inputPinAt(Point p) {
        for (int i = nodes.size() - 1; i >= 0; i--) {
            CircuitNode n = nodes.get(i);
            for (int k = 0; k < n.type.inputCount; k++) {
                if (p.distance(n.inputPin(k)) <= PIN_HIT) {
                    return new PinRef(n, k);
                }
            }
        }
        return null;
    }

    private boolean canConnect(CircuitNode source, PinRef pin) {
        return pin != null && pin.node != source && !reaches(source, pin.node);
    }

    // =====================================================================
    //  Mouse handling
    // =====================================================================

    private void onPress(MouseEvent e) {
        requestFocusInWindow();
        Point p = e.getPoint();

        if (SwingUtilities.isRightMouseButton(e)) {
            if (armed != null) {
                setArmed(null);
                return;
            }
            PinRef pin = inputPinAt(p);
            if (pin != null && pin.node.sources[pin.index] != null) {
                pin.node.connect(pin.index, null);
                caption = null;
                flash("Wire removed");
            } else {
                CircuitNode n = nodeAt(p);
                if (n != null) {
                    removeNode(n);
                    if (selected == n) {
                        selected = null;
                    }
                    flash(n.type.displayName + " deleted");
                }
            }
            repaint();
            return;
        }
        if (!SwingUtilities.isLeftMouseButton(e)) {
            return;
        }

        if (armed != null) {
            placeNode(armed, p);
            setArmed(null);
            return;
        }

        CircuitNode out = outputPinAt(p);
        if (out != null) {
            wireSource = out;
            wireMouse = p;
            selected = out;
            repaint();
            return;
        }

        CircuitNode n = nodeAt(p);
        if (n != null) {
            dragNode = n;
            grabDx = p.x - n.x;
            grabDy = p.y - n.y;
            pressPoint = p;
            moved = false;
            selected = n;
            nodes.remove(n);
            nodes.add(n);
        } else {
            selected = null;
        }
        repaint();
    }

    private void onDrag(MouseEvent e) {
        Point p = e.getPoint();
        mousePoint.setLocation(p);
        if (wireSource != null) {
            wireMouse = p;
            PinRef pin = inputPinAt(p);
            hoverInput = canConnect(wireSource, pin) ? pin : null;
        } else if (dragNode != null) {
            if (!moved && pressPoint != null && p.distance(pressPoint) > 3) {
                moved = true;
            }
            if (moved) {
                int nx = snap(p.x - grabDx);
                int ny = snap(p.y - grabDy);
                dragNode.x = Math.max(0, Math.min(nx, Math.max(0, getWidth() - CircuitNode.W)));
                dragNode.y = Math.max(0, Math.min(ny, Math.max(0, getHeight() - dragNode.height())));
            }
        }
        repaint();
    }

    private void onRelease(MouseEvent e) {
        Point p = e.getPoint();
        if (wireSource != null) {
            PinRef target = inputPinAt(p);
            if (target != null) {
                if (canConnect(wireSource, target)) {
                    target.node.connect(target.index, wireSource);
                    caption = null;
                    flash("Connected");
                } else {
                    flash("Connection rejected - feedback loops are not supported");
                }
            }
            wireSource = null;
            wireMouse = null;
            hoverInput = null;
        } else if (dragNode != null) {
            if (!moved && dragNode.logic instanceof InputSwitch) {
                ((InputSwitch) dragNode.logic).toggle();
            }
            dragNode = null;
        }
        repaint();
    }

    private void onMove(MouseEvent e) {
        Point p = e.getPoint();
        mousePoint.setLocation(p);
        hoverOutput = outputPinAt(p);
        PinRef pin = inputPinAt(p);
        hoverInput = pin;
        if (armed != null) {
            ghostType = armed;
            ghostPoint = p;
            setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        } else if (hoverOutput != null || pin != null) {
            setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        } else if (nodeAt(p) != null) {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        } else {
            setCursor(Cursor.getDefaultCursor());
        }
        repaint();
    }

    // =====================================================================
    //  Rendering
    // =====================================================================

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Theme.quality(g);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            g.dispose();
            return;
        }
        if (backdrop == null || backdrop.getWidth() != w || backdrop.getHeight() != h) {
            backdrop = buildBackdrop(w, h);
        }
        g.drawImage(backdrop, 0, 0, null);

        for (CircuitNode n : nodes) {
            for (int i = 0; i < n.sources.length; i++) {
                CircuitNode s = n.sources[i];
                if (s != null) {
                    drawWire(g, s.outputPin(), n.inputPin(i), s.output(), false);
                }
            }
        }
        if (wireSource != null && wireMouse != null) {
            drawWire(g, wireSource.outputPin(), wireMouse, wireSource.output(), true);
        }

        for (CircuitNode n : nodes) {
            drawNode(g, n);
        }

        if (wireSource != null) {
            drawTargetRings(g);
        }
        drawGhost(g);
        drawOverlay(g, w, h);
        g.dispose();
    }

    private BufferedImage buildBackdrop(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        Theme.quality(g);
        g.setPaint(new java.awt.GradientPaint(0, 0, Theme.BG_BASE, 0, h, Theme.BG_DEEP));
        g.fillRect(0, 0, w, h);

        // major lines every 100 px
        g.setColor(Theme.alpha(Theme.CYAN, 16));
        for (int x = 0; x < w; x += 100) {
            g.drawLine(x, 0, x, h);
        }
        for (int y = 0; y < h; y += 100) {
            g.drawLine(0, y, w, y);
        }
        // dot lattice every 20 px
        g.setColor(Theme.alpha(Theme.CYAN, 46));
        for (int x = 0; x < w; x += 20) {
            for (int y = 0; y < h; y += 20) {
                g.fillRect(x, y, 1, 1);
            }
        }
        // faint scanlines
        g.setColor(new Color(0, 0, 0, 34));
        for (int y = 0; y < h; y += 3) {
            g.drawLine(0, y, w, y);
        }
        // magenta / cyan light leaks in opposite corners
        float r = Math.max(w, h) * 0.55f;
        g.setPaint(new RadialGradientPaint(new Point2D.Float(0, h), r, new float[] { 0f, 1f },
                new Color[] { Theme.alpha(Theme.MAGENTA, 30), Theme.alpha(Theme.MAGENTA, 0) }));
        g.fillRect(0, 0, w, h);
        g.setPaint(new RadialGradientPaint(new Point2D.Float(w, 0), r, new float[] { 0f, 1f },
                new Color[] { Theme.alpha(Theme.CYAN, 26), Theme.alpha(Theme.CYAN, 0) }));
        g.fillRect(0, 0, w, h);
        // vignette
        g.setPaint(new RadialGradientPaint(new Point2D.Float(w / 2f, h / 2f), Math.max(w, h) * 0.75f,
                new float[] { 0.55f, 1f }, new Color[] { new Color(0, 0, 0, 0), new Color(0, 0, 0, 170) }));
        g.fillRect(0, 0, w, h);
        // HUD corner brackets
        g.setStroke(new BasicStroke(1.5f));
        g.setColor(Theme.alpha(Theme.CYAN, 120));
        int m = 10;
        int l = 24;
        g.drawLine(m, m, m + l, m);
        g.drawLine(m, m, m, m + l);
        g.drawLine(w - m, m, w - m - l, m);
        g.drawLine(w - m, m, w - m, m + l);
        g.drawLine(m, h - m, m + l, h - m);
        g.drawLine(m, h - m, m, h - m - l);
        g.drawLine(w - m, h - m, w - m - l, h - m);
        g.drawLine(w - m, h - m, w - m, h - m - l);
        g.dispose();
        return img;
    }

    private void drawWire(Graphics2D g, Point a, Point b, boolean on, boolean pending) {
        double dx = Math.max(50, Math.abs(b.x - a.x) / 2.0);
        CubicCurve2D curve = new CubicCurve2D.Double(a.x, a.y, a.x + dx, a.y, b.x - dx, b.y, b.x, b.y);
        if (pending) {
            g.setColor(on ? Theme.SIGNAL_ON : Theme.SIGNAL_OFF);
            g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10f,
                    new float[] { 8f, 6f }, 0f));
            g.draw(curve);
            g.setStroke(new BasicStroke(1f));
            return;
        }
        if (on) {
            Theme.glow(g, curve, Theme.SIGNAL_ON, 2.6f);
            g.setColor(Theme.SIGNAL_ON);
            g.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(curve);
            float dash = 18f;
            float ph = dash - (phase % dash);
            g.setColor(new Color(235, 255, 242, 230));
            g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10f,
                    new float[] { 2f, dash - 2f }, ph));
            g.draw(curve);
        } else {
            g.setColor(Theme.SIGNAL_OFF);
            g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(curve);
        }
        g.setStroke(new BasicStroke(1f));
    }

    private void drawNode(Graphics2D g, CircuitNode n) {
        double x = n.x;
        double y = n.y;
        double h = n.height();
        double bx = x + CircuitNode.STUB;
        double bw = CircuitNode.W - 2 * CircuitNode.STUB;
        boolean out = n.output();
        NodeType t = n.type;

        // leads between pins and body
        g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < t.inputCount; i++) {
            Point p = n.inputPin(i);
            double end = t.isGate() ? GateRenderer.inputLeadEnd(t, bx, bw) : bx;
            CircuitNode src = n.sources[i];
            g.setColor(src == null ? Theme.TEXT_FAINT : (src.output() ? Theme.SIGNAL_ON : Theme.SIGNAL_OFF));
            g.draw(new Line2D.Double(p.x, p.y, end, p.y));
        }
        if (t.hasOutput) {
            Point p = n.outputPin();
            g.setColor(out ? Theme.SIGNAL_ON : Theme.SIGNAL_OFF);
            g.draw(new Line2D.Double(bx + bw, p.y, p.x, p.y));
        }
        g.setStroke(new BasicStroke(1f));

        // body
        if (t.isGate()) {
            drawGateBody(g, n, bx, y, bw, h, out);
        } else if (t == NodeType.INPUT_SWITCH) {
            drawSwitchBody(g, n, bx, y, bw, h, out);
        } else {
            drawLightBody(g, n, bx, y, bw, h, out);
        }

        // pins
        for (int i = 0; i < t.inputCount; i++) {
            CircuitNode src = n.sources[i];
            Color c = src == null ? Theme.TEXT_FAINT : (src.output() ? Theme.SIGNAL_ON : Theme.SIGNAL_OFF);
            drawPin(g, n.inputPin(i), c, src != null, hoverInput != null && hoverInput.is(n, i));
        }
        if (t.hasOutput) {
            drawPin(g, n.outputPin(), out ? Theme.SIGNAL_ON : Theme.SIGNAL_OFF, true,
                    hoverOutput == n || wireSource == n);
        }

        if (n == selected) {
            drawBrackets(g, bx - 6, y - 6, bw + 12, h + 12);
        }
    }

    private void drawGateBody(Graphics2D g, CircuitNode n, double bx, double y, double bw, double h, boolean out) {
        NodeType t = n.type;
        Color line = out ? Theme.SIGNAL_ON : t.accent;
        Color fill = Theme.alpha(Theme.mix(Theme.PANEL, out ? Theme.SIGNAL_ON : t.accent, 0.14f), 245);
        GateRenderer.draw(g, t, bx, y, bw, h, line, fill, 2.2f, out);

        g.setFont(Theme.mono(java.awt.Font.BOLD, 12f));
        FontMetrics fm = g.getFontMetrics();
        g.setColor(out ? Theme.TEXT : Theme.TEXT_DIM);
        double cx = GateRenderer.labelCenter(t, bx, bw);
        String s = t.symbol;
        g.drawString(s, (float) (cx - fm.stringWidth(s) / 2.0), (float) (y + h / 2 + fm.getAscent() / 2.0 - 2));
    }

    private void drawSwitchBody(Graphics2D g, CircuitNode n, double bx, double y, double bw, double h, boolean on) {
        Color line = on ? Theme.SIGNAL_ON : n.type.accent;
        Path2D body = Theme.chamfer(bx, y, bw, h, 9);
        if (on) {
            Theme.glow(g, body, line, 1.8f);
        }
        g.setColor(Theme.alpha(Theme.mix(Theme.PANEL, line, 0.10f), 245));
        g.fill(body);
        g.setColor(line);
        g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(body);
        g.setStroke(new BasicStroke(1f));

        g.setFont(Theme.mono(java.awt.Font.BOLD, 10f));
        g.setColor(Theme.TEXT_DIM);
        Theme.drawTracked(g, "IN " + n.name, (float) (bx + 16), (float) (y + 17), 1.2f);

        // toggle track + knob
        double pw = 40;
        double ph = 20;
        double px = bx + 16;
        double py = y + 28;
        RoundRectangle2D track = new RoundRectangle2D.Double(px, py, pw, ph, ph, ph);
        g.setColor(on ? Theme.alpha(Theme.SIGNAL_ON, 70) : new Color(30, 38, 62));
        g.fill(track);
        g.setColor(on ? Theme.SIGNAL_ON : Theme.TEXT_FAINT);
        g.draw(track);
        double kx = on ? px + pw - ph + 3 : px + 3;
        g.setColor(on ? Theme.SIGNAL_ON : Theme.TEXT_DIM);
        g.fill(new Ellipse2D.Double(kx, py + 3, ph - 6, ph - 6));

        g.setFont(Theme.mono(java.awt.Font.BOLD, 20f));
        FontMetrics fm = g.getFontMetrics();
        g.setColor(on ? Theme.SIGNAL_ON : Theme.TEXT_FAINT);
        String s = on ? "1" : "0";
        g.drawString(s, (float) (bx + bw - 12 - fm.stringWidth(s)), (float) (y + 46));
    }

    private void drawLightBody(Graphics2D g, CircuitNode n, double bx, double y, double bw, double h, boolean lit) {
        Color line = lit ? Theme.YELLOW : Theme.mix(n.type.accent, Theme.BG_DEEP, 0.45f);
        Path2D body = Theme.chamfer(bx, y, bw, h, 9);
        if (lit) {
            Theme.glow(g, body, line, 1.8f);
        }
        g.setColor(Theme.alpha(Theme.mix(Theme.PANEL, line, 0.10f), 245));
        g.fill(body);
        g.setColor(line);
        g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(body);
        g.setStroke(new BasicStroke(1f));

        g.setFont(Theme.mono(java.awt.Font.BOLD, 10f));
        g.setColor(Theme.TEXT_DIM);
        Theme.drawTracked(g, "OUT " + n.name, (float) (bx + 16), (float) (y + 17), 1.2f);

        double cx = bx + 30;
        double cy = y + 39;
        double r = 11;
        if (lit) {
            g.setPaint(new RadialGradientPaint(new Point2D.Double(cx, cy), 36f, new float[] { 0f, 1f },
                    new Color[] { Theme.alpha(Theme.YELLOW, 150), Theme.alpha(Theme.YELLOW, 0) }));
            g.fill(new Ellipse2D.Double(cx - 36, cy - 36, 72, 72));
        }
        g.setColor(lit ? Theme.YELLOW : new Color(34, 40, 60));
        g.fill(new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r));
        if (lit) {
            g.setColor(new Color(255, 250, 215, 200));
            g.fill(new Ellipse2D.Double(cx - 5, cy - 6, 6, 5));
        }
        g.setColor(lit ? Theme.alpha(Theme.YELLOW, 255) : Theme.TEXT_FAINT);
        g.setStroke(new BasicStroke(1.6f));
        g.draw(new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r));
        g.setStroke(new BasicStroke(1f));

        g.setFont(Theme.mono(java.awt.Font.BOLD, 20f));
        FontMetrics fm = g.getFontMetrics();
        g.setColor(lit ? Theme.YELLOW : Theme.TEXT_FAINT);
        String s = lit ? "1" : "0";
        g.drawString(s, (float) (bx + bw - 12 - fm.stringWidth(s)), (float) (y + 46));
    }

    private void drawPin(Graphics2D g, Point p, Color c, boolean connected, boolean highlight) {
        double r = 5.5;
        if (highlight) {
            g.setColor(Theme.alpha(Theme.CYAN, 70));
            g.fill(new Ellipse2D.Double(p.x - 11, p.y - 11, 22, 22));
            g.setColor(Theme.alpha(Theme.CYAN, 200));
            g.setStroke(new BasicStroke(1.2f));
            g.draw(new Ellipse2D.Double(p.x - 11, p.y - 11, 22, 22));
        }
        g.setColor(Theme.BG_DEEP);
        g.fill(new Ellipse2D.Double(p.x - r - 1, p.y - r - 1, 2 * r + 2, 2 * r + 2));
        g.setColor(c);
        g.setStroke(new BasicStroke(2f));
        g.draw(new Ellipse2D.Double(p.x - r, p.y - r, 2 * r, 2 * r));
        if (connected) {
            g.fill(new Ellipse2D.Double(p.x - 2.5, p.y - 2.5, 5, 5));
        }
        g.setStroke(new BasicStroke(1f));
    }

    private void drawBrackets(Graphics2D g, double x, double y, double w, double h) {
        double l = 10;
        g.setColor(Theme.MAGENTA);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
        g.draw(new Line2D.Double(x, y, x + l, y));
        g.draw(new Line2D.Double(x, y, x, y + l));
        g.draw(new Line2D.Double(x + w, y, x + w - l, y));
        g.draw(new Line2D.Double(x + w, y, x + w, y + l));
        g.draw(new Line2D.Double(x, y + h, x + l, y + h));
        g.draw(new Line2D.Double(x, y + h, x, y + h - l));
        g.draw(new Line2D.Double(x + w, y + h, x + w - l, y + h));
        g.draw(new Line2D.Double(x + w, y + h, x + w, y + h - l));
        g.setStroke(new BasicStroke(1f));
    }

    /** While wiring, ring every input pin that would accept the connection. */
    private void drawTargetRings(Graphics2D g) {
        double pulse = 0.5 + 0.5 * Math.sin(phase * 0.12);
        for (CircuitNode n : nodes) {
            for (int i = 0; i < n.type.inputCount; i++) {
                PinRef ref = new PinRef(n, i);
                if (!canConnect(wireSource, ref)) {
                    continue;
                }
                Point p = n.inputPin(i);
                double r = 9 + 3 * pulse;
                g.setColor(Theme.alpha(Theme.CYAN, (int) (80 + 120 * pulse)));
                g.setStroke(new BasicStroke(1.6f));
                g.draw(new Ellipse2D.Double(p.x - r, p.y - r, 2 * r, 2 * r));
            }
        }
        g.setStroke(new BasicStroke(1f));
    }

    private void drawGhost(Graphics2D g) {
        if (ghostType == null || ghostPoint == null) {
            return;
        }
        NodeType t = ghostType;
        Point o = origin(t, ghostPoint);
        double bx = o.x + CircuitNode.STUB;
        double bw = CircuitNode.W - 2 * CircuitNode.STUB;
        double y = o.y;
        double h = t.height;
        java.awt.Composite old = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f));
        if (t.isGate()) {
            GateRenderer.draw(g, t, bx, y, bw, h, t.accent,
                    Theme.alpha(Theme.mix(Theme.PANEL, t.accent, 0.2f), 255), 2.2f, true);
        } else {
            Path2D body = Theme.chamfer(bx, y, bw, h, 9);
            Theme.glow(g, body, t.accent, 1.8f);
            g.setColor(Theme.alpha(Theme.mix(Theme.PANEL, t.accent, 0.2f), 255));
            g.fill(body);
            g.setColor(t.accent);
            g.setStroke(new BasicStroke(1.8f));
            g.draw(body);
            g.setFont(Theme.mono(java.awt.Font.BOLD, 11f));
            FontMetrics fm = g.getFontMetrics();
            String s = t == NodeType.INPUT_SWITCH ? "INPUT" : "OUTPUT";
            g.setColor(Theme.TEXT);
            g.drawString(s, (float) (bx + (bw - fm.stringWidth(s)) / 2), (float) (y + h / 2 + 4));
        }
        g.setComposite(old);
        g.setStroke(new BasicStroke(1f));
    }

    private void drawOverlay(Graphics2D g, int w, int h) {
        if (caption != null && !nodes.isEmpty()) {
            g.setFont(Theme.mono(java.awt.Font.BOLD, 12f));
            g.setColor(Theme.alpha(Theme.CYAN, 190));
            g.drawString(caption, 30, 38);
        }

        if (nodes.isEmpty()) {
            double bw = 420;
            double bh = 96;
            double bx = (w - bw) / 2;
            double by = (h - bh) / 2 - 20;
            Path2D box = Theme.chamfer(bx, by, bw, bh, 14);
            g.setColor(Theme.alpha(Theme.PANEL, 190));
            g.fill(box);
            g.setColor(Theme.alpha(Theme.CYAN, 130));
            g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f,
                    new float[] { 6f, 5f }, 0f));
            g.draw(box);
            g.setStroke(new BasicStroke(1f));
            g.setFont(Theme.ui(java.awt.Font.BOLD, 15f));
            FontMetrics fm = g.getFontMetrics();
            String a = "Canvas is empty";
            g.setColor(Theme.TEXT);
            g.drawString(a, (float) (bx + (bw - fm.stringWidth(a)) / 2), (float) (by + 38));
            g.setFont(Theme.ui(java.awt.Font.PLAIN, 12.5f));
            fm = g.getFontMetrics();
            String b = "Drag a component from the library, or load the demo circuit.";
            g.setColor(Theme.TEXT_DIM);
            g.drawString(b, (float) (bx + (bw - fm.stringWidth(b)) / 2), (float) (by + 62));
        }

        // live indicator, bottom-right
        double pulse = 0.5 + 0.5 * Math.sin(phase * 0.1);
        g.setFont(Theme.mono(java.awt.Font.BOLD, 10f));
        FontMetrics fm = g.getFontMetrics();
        String s = "SIMULATION LIVE";
        float tw = Theme.trackedWidth(fm, s, 1.6f);
        float tx = w - 22 - tw;
        g.setColor(Theme.alpha(Theme.SIGNAL_ON, 150));
        Theme.drawTracked(g, s, tx, h - 20, 1.6f);
        g.setColor(Theme.alpha(Theme.SIGNAL_ON, (int) (110 + 145 * pulse)));
        g.fill(new Ellipse2D.Double(tx - 14, h - 28, 7, 7));
    }
}
