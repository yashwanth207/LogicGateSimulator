package logicsim.ui;

import java.awt.Dimension;
import java.awt.Toolkit;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;

/** The application window. */
@SuppressWarnings("serial")
public final class MainFrame extends JFrame {

    public MainFrame() {
        super("LogicGrid - Digital Circuit Simulator");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setIconImages(AppIcon.images());

        AppContent content = new AppContent();
        setContentPane(content);

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int w = Math.min(1280, screen.width - 80);
        int h = Math.min(780, screen.height - 80);
        setSize(w, h);
        setMinimumSize(new Dimension(Math.min(960, w), Math.min(600, h)));
        setLocationRelativeTo(null);

        content.start();
    }

    /** Applies the cross-platform look and a dark tooltip style. Call once before creating the frame. */
    public static void installLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {
            // keep the platform default
        }
        UIManager.put("ToolTip.background", new ColorUIResource(Theme.PANEL_HI));
        UIManager.put("ToolTip.foreground", new ColorUIResource(Theme.TEXT));
        UIManager.put("ToolTip.border", BorderFactory.createLineBorder(Theme.alpha(Theme.CYAN, 160)));
        UIManager.put("ToolTip.font", Theme.ui(java.awt.Font.PLAIN, 12f));
    }
}
