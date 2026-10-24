package logicsim.ui;

import java.awt.Point;
import logicsim.engine.InputSwitch;
import logicsim.engine.LogicComponent;
import logicsim.engine.LogicGate;
import logicsim.engine.OutputLight;

/** A component placed on the canvas: engine object + position + wiring bookkeeping for drawing. */
final class CircuitNode {

    static final int W = 120;
    /** Length of the lead between a pin and the symbol body. */
    static final int STUB = 14;

    final NodeType type;
    final LogicComponent logic;
    final CircuitNode[] sources;
    String name = "";
    int x;
    int y;

    CircuitNode(NodeType type, int x, int y) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.logic = type.createLogic();
        this.sources = new CircuitNode[type.inputCount];
    }

    int height() {
        return type.height;
    }

    boolean output() {
        return logic.getOutput();
    }

    boolean contains(Point p) {
        return p.x >= x && p.x <= x + W && p.y >= y && p.y <= y + height();
    }

    Point inputPin(int i) {
        return new Point(x, y + height() * (i + 1) / (type.inputCount + 1));
    }

    Point outputPin() {
        return new Point(x + W, y + height() / 2);
    }

    /** Wires {@code src} into input {@code idx}; pass null to disconnect. Keeps engine and view in sync. */
    void connect(int idx, CircuitNode src) {
        sources[idx] = src;
        LogicComponent c = (src == null) ? null : src.logic;
        if (logic instanceof LogicGate) {
            ((LogicGate) logic).setInput(idx, c);
        } else if (logic instanceof OutputLight) {
            ((OutputLight) logic).setSource(c);
        }
    }

    boolean isSwitchOn() {
        return logic instanceof InputSwitch && ((InputSwitch) logic).isOn();
    }
}
