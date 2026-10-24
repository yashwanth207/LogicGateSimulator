package logicsim;

import javax.swing.SwingUtilities;
import logicsim.ui.MainFrame;

/**
 * Entry point for LogicGrid, an interactive logic gate simulator.
 *
 * <p>The engine (package {@code logicsim.engine}) is independent of Swing; the user interface
 * (package {@code logicsim.ui}) renders and edits circuits built from engine components.
 */
public final class LogicGateSimulator {

    private LogicGateSimulator() { }

    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        System.setProperty("apple.awt.application.name", "LogicGrid");
        SwingUtilities.invokeLater(() -> {
            MainFrame.installLookAndFeel();
            new MainFrame().setVisible(true);
        });
    }
}
