package logicsim.engine;

/** Output is true when at least one input is true. */
public class OrGate extends LogicGate {

    public OrGate() {
        super(2);
    }

    @Override
    public boolean getOutput() {
        return in(0) || in(1);
    }
}
