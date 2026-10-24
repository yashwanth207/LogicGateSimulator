package logicsim.engine;

/** A sink that reads one input component and displays its signal state. */
public class OutputLight implements LogicComponent {

    private LogicComponent source;

    public void setSource(LogicComponent source) {
        this.source = source;
    }

    public LogicComponent getSource() {
        return source;
    }

    public boolean isLit() {
        return getOutput();
    }

    @Override
    public boolean getOutput() {
        return source != null && source.getOutput();
    }
}
