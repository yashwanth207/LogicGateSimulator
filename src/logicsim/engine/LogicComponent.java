package logicsim.engine;

/** Anything that produces a boolean signal: switches, gates, or any future component. */
public interface LogicComponent {

    /** Returns the current signal level, evaluated live from the component's inputs. */
    boolean getOutput();
}
