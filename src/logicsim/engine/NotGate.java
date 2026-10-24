package logicsim.engine;

/** Inverts its single input. */
public class NotGate extends LogicGate {

    public NotGate() {
        super(1);
    }

    @Override
    public boolean getOutput() {
        return !in(0);
    }
}
