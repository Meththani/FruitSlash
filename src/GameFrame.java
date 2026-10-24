import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * The application window. It creates the model objects, hands them to the {@link GamePanel}
 * (which manages every screen: menu, game, leaderboard, ...) and shuts everything down cleanly.
 */
public final class GameFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private final transient GamePanel panel;
    private final transient SoundManager sound;

    public GameFrame() {
        super("Fruit Slash: Arcade Challenge");
        LeaderboardManager leaderboard = new LeaderboardManager();
        GameEngine engine = new GameEngine();
        sound = new SoundManager(true);
        panel = new GamePanel(engine, leaderboard, sound, this::exitApplication);

        setContentPane(panel);
        setMinimumSize(new Dimension(GameConfig.MIN_WINDOW_WIDTH, GameConfig.MIN_WINDOW_HEIGHT));
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                exitApplication();
            }
        });
        pack();
        setLocationRelativeTo(null);
    }

    /** Shows the window and starts the animation loop. Must be called on the Event Dispatch Thread. */
    public void launch() {
        setVisible(true);
        panel.start();
        SwingUtilities.invokeLater(panel::requestFocusInWindow);
    }

    private void exitApplication() {
        panel.stop();
        sound.shutdown();
        dispose();
        System.exit(0);
    }
}
