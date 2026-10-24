package logicsim.engine;

/**
 * Base class for all gates. Manages an array of input {@link LogicComponent} nodes.
 * Inputs are evaluated lazily every time {@link #getOutput()} is called, so a change at
 * any source propagates through the whole network instantly. Unconnected inputs read as false.
 */
public abstract class LogicGate implements LogicComponent {

    private final LogicComponent[] inputs;

    protected LogicGate(int inputCount) {
        if (inputCount < 1) {
            throw new IllegalArgumentException("A gate needs at least one input");
        }
        this.inputs = new LogicComponent[inputCount];
    }

    public void setInput(int index, LogicComponent component) {
        inputs[index] = component;
    }

    public LogicComponent getInput(int index) {
        return inputs[index];
    }

    public int getInputCount() {
        return inputs.length;
    }

    /** Reads input {@code index}; an unconnected input is false. */
    protected boolean in(int index) {
        LogicComponent c = inputs[index];
        return c != null && c.getOutput();
    }
}
