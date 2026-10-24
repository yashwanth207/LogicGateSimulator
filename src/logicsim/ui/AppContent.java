package logicsim.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/** Assembles header, palette, canvas and status bar, and owns the repaint loop. */
@SuppressWarnings("serial")
final class AppContent extends JPanel {

    final CircuitCanvas canvas = new CircuitCanvas();
    private final Timer timer;

    AppContent() {
        super(new BorderLayout());
        setBackground(Theme.BG_DEEP);

        HeaderPanel header = new HeaderPanel(canvas);
        StatusBar status = new StatusBar(canvas);
        PalettePanel palette = new PalettePanel(canvas);

        JScrollPane scroll = new JScrollPane(palette, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Theme.alpha(Theme.CYAN, 90)));
        scroll.getViewport().setBackground(Theme.PANEL);
        scroll.getVerticalScrollBar().setUI(new ThinScrollBarUI());
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        scroll.setPreferredSize(new Dimension(246, 100));

        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);
        body.add(scroll, BorderLayout.WEST);
        body.add(canvas, BorderLayout.CENTER);

        add(header, BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);

        canvas.setArmedListener(palette::repaint);

        // Event-driven repaints cover every interaction; this loop also drives the signal-flow animation
        // and keeps the header statistics and status text live.
        timer = new Timer(33, e -> {
            canvas.tick();
            header.repaint();
            status.repaint();
        });
    }

    /** Loads the demo circuit and starts the animation loop. */
    void start() {
        // Deferred so the canvas already has its real size and the demo can be centred.
        SwingUtilities.invokeLater(canvas::loadDemo);
        timer.start();
    }
}
