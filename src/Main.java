import javax.swing.SwingUtilities;

/** Application entry point: builds the window on the Swing Event Dispatch Thread. */
public final class Main {

    private Main() {
        // not instantiable
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GameFrame().launch());
    }
}
