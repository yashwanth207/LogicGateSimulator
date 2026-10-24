package logicsim.engine;

/** Output is true when exactly one input is true. */
public class XorGate extends LogicGate {

    public XorGate() {
        super(2);
    }

    @Override
    public boolean getOutput() {
        return in(0) ^ in(1);
    }
}
