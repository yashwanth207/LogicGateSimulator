package logicsim.ui;

import java.awt.Color;
import logicsim.engine.AndGate;
import logicsim.engine.InputSwitch;
import logicsim.engine.LogicComponent;
import logicsim.engine.NotGate;
import logicsim.engine.OrGate;
import logicsim.engine.OutputLight;
import logicsim.engine.XorGate;

/** Every component kind that can be placed on the canvas, with its presentation data. */
enum NodeType {

    INPUT_SWITCH("Input Switch", "SW", 0, true, 60, Theme.CYAN, "Toggle source"),
    OUTPUT_LIGHT("Output Light", "LED", 1, false, 60, Theme.YELLOW, "Signal indicator"),
    AND("AND", "AND", 2, true, 72, Theme.CYAN, "Both inputs high"),
    OR("OR", "OR", 2, true, 72, Theme.VIOLET, "Either input high"),
    NOT("NOT", "NOT", 1, true, 54, Theme.MAGENTA, "Inverts the signal"),
    XOR("XOR", "XOR", 2, true, 72, Theme.ORANGE, "Inputs differ");

    final String displayName;
    final String symbol;
    final int inputCount;
    final boolean hasOutput;
    final int height;
    final Color accent;
    final String caption;

    NodeType(String displayName, String symbol, int inputCount, boolean hasOutput, int height,
             Color accent, String caption) {
        this.displayName = displayName;
        this.symbol = symbol;
        this.inputCount = inputCount;
        this.hasOutput = hasOutput;
        this.height = height;
        this.accent = accent;
        this.caption = caption;
    }

    boolean isGate() {
        return this == AND || this == OR || this == NOT || this == XOR;
    }

    /** Creates the engine object behind this node type. */
    LogicComponent createLogic() {
        switch (this) {
            case INPUT_SWITCH: return new InputSwitch();
            case OUTPUT_LIGHT: return new OutputLight();
            case AND:          return new AndGate();
            case OR:           return new OrGate();
            case NOT:          return new NotGate();
            default:           return new XorGate();
        }
    }
}
