package logicsim.engine;

/** A user-controlled source that holds a toggleable true/false state. */
public class InputSwitch implements LogicComponent {

    private boolean on;

    public void toggle() {
        on = !on;
    }

    public void setOn(boolean on) {
        this.on = on;
    }

    public boolean isOn() {
        return on;
    }

    @Override
    public boolean getOutput() {
        return on;
    }
}
