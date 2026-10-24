package logicsim.engine;

/** Output is true only when both inputs are true. */
public class AndGate extends LogicGate {

    public AndGate() {
        super(2);
    }

    @Override
    public boolean getOutput() {
        return in(0) && in(1);
    }
}
