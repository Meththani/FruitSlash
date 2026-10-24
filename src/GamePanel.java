import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * The single custom drawing surface of the game (a JPanel with an overridden paintComponent).
 *
 * <p>Responsibilities: animation (a Swing Timer, so everything runs on the Event Dispatch Thread),
 * mouse and keyboard input, converting window pixels to the logical 1280 x 720 world, and wiring the
 * buttons of every screen to engine actions. Game rules live in {@link GameEngine}; drawing lives in
 * {@link GameRenderer}.
 */
public final class GamePanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final int W = GameConfig.WORLD_WIDTH;
    private static final int H = GameConfig.WORLD_HEIGHT;
    private static final int FRAME_DELAY_MS = 16;
    private static final Color WARNING = new Color(255, 130, 130);

    private final transient GameEngine engine;
    private final transient LeaderboardManager board;
    private final transient SoundManager sound;
    private final transient Runnable exitAction;
    private final transient UiState ui = new UiState();
    private final transient GameRenderer renderer;
    private final transient Timer timer;

    private long lastNanos;
    private GameState shownState; // the state the buttons were built for (null before the first sync)
    private double uiLockUntil;
    private String lastPlayerName = "Player";

    private double scale = 1.0;
    private double offsetX;
    private double offsetY;

    private transient UiButton soundButton;
    private transient UiButton undoButton;
    private transient UiButton techButton;
    private transient UiButton saveButton;

    public GamePanel(GameEngine engine, LeaderboardManager board, SoundManager sound, Runnable exitAction) {
        this.engine = engine;
        this.board = board;
        this.sound = sound;
        this.exitAction = exitAction == null ? () -> { } : exitAction;
        this.renderer = new GameRenderer(engine, board, ui);

        setPreferredSize(new Dimension(GameConfig.START_WINDOW_WIDTH, GameConfig.START_WINDOW_HEIGHT));
        setMinimumSize(new Dimension(GameConfig.MIN_WINDOW_WIDTH, GameConfig.MIN_WINDOW_HEIGHT));
        setBackground(new Color(10, 8, 30));
        setFocusable(true);
        setFocusTraversalKeysEnabled(false); // Tab must not move focus away from the game
        engine.setSoundListener(sound::play);

        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                onMousePressed(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                onMouseReleased(e);
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                onMouseMoved(e);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                onMouseDragged(e);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                clearHover();
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                onKeyPressed(e);
            }

            @Override
            public void keyTyped(KeyEvent e) {
                onKeyTyped(e);
            }
        });
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                if (engine.getState() == GameState.PLAYING) {
                    engine.pause(); // never lose a run because the window went to the background
                    syncState();
                }
            }
        });

        timer = new Timer(FRAME_DELAY_MS, e -> tick());
        timer.setCoalesce(true);
        syncState();
    }

    // ===================================================================================== loop

    /** Starts the animation timer (call on the EDT). */
    public void start() {
        lastNanos = System.nanoTime();
        timer.start();
    }

    public void stop() {
        timer.stop();
    }

    /** One animation frame: update the model, keep the UI in sync, request a repaint. */
    private void tick() {
        long now = System.nanoTime();
        double dt = Math.min(0.1, (now - lastNanos) / 1_000_000_000.0);
        lastNanos = now;
        ui.animTime += dt;
        syncState();
        engine.update(dt);
        syncState();
        updateDynamicButtons();
        repaint();
        Toolkit.getDefaultToolkit().sync(); // smoother animation on some platforms
    }

    // ===================================================================================== painting

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            syncState();
            updateDynamicButtons();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            computeTransform();
            g.setColor(new Color(8, 6, 24));
            g.fillRect(0, 0, getWidth(), getHeight());
            g.translate(offsetX, offsetY);
            g.scale(scale, scale);
            g.setClip(0, 0, W, H);
            renderer.render(g);
        } finally {
            g.dispose();
        }
    }

    /** Uniform scaling with letter-boxing keeps the world's aspect ratio at any window size. */
    private void computeTransform() {
        double w = Math.max(1, getWidth());
        double h = Math.max(1, getHeight());
        scale = Math.min(w / W, h / H);
        offsetX = (w - W * scale) / 2.0;
        offsetY = (h - H * scale) / 2.0;
    }

    private Point2D.Double toWorld(MouseEvent e) {
        computeTransform();
        return new Point2D.Double((e.getX() - offsetX) / scale, (e.getY() - offsetY) / scale);
    }

    // ===================================================================================== state sync

    /** Rebuilds the screen's buttons whenever the engine's state changed. */
    private void syncState() {
        GameState current = engine.getState();
        if (current == shownState) {
            return;
        }
        GameState old = shownState;
        shownState = current;
        onStateChanged(old, current);
    }

    private void onStateChanged(GameState old, GameState now) {
        ui.pressed = null;
        ui.hovered = null;
        uiLockUntil = ui.animTime + GameConfig.UI_LOCK_TIME;
        ui.nameInput.setFocused(false);
        ui.searchInput.setFocused(false);

        if ((now == GameState.GAME_OVER || now == GameState.VICTORY) && old == GameState.PLAYING) {
            ui.scoreSaved = false;
            ui.savedRank = -1;
            ui.statusMessage = "";
            ui.nameInput.setText(lastPlayerName);
            ui.nameInput.setFocused(engine.getScore() > 0);
        }
        if (now == GameState.LEADERBOARD) {
            ui.leaderboardReturn = old == null ? GameState.MENU : old;
            ui.leaderboardPage = 0;
            ui.highlightRank = -1;
            ui.searchMessage = "";
            ui.searchInput.setText("");
            ui.searchInput.setFocused(true);
        }
        if (now == GameState.HOW_TO_PLAY) {
            ui.showTechInfo = false;
        }
        rebuildButtons();
        updateCursor();
    }

    private UiButton add(String label, double x, double y, double w, double h,
                         UiButton.Style style, Runnable action) {
        UiButton button = new UiButton(label, x, y, w, h, style, action);
        ui.buttons.add(button);
        return button;
    }

    private void rebuildButtons() {
        ui.buttons.clear();
        soundButton = null;
        undoButton = null;
        techButton = null;
        saveButton = null;
        double bw = 380;
        double bx = (W - bw) / 2.0;
        switch (shownState) {
            case MENU:
                add("PLAY", bx, 262, bw, 58, UiButton.Style.PRIMARY, this::startGame);
                add("LEADERBOARD", bx, 336, bw, 58, UiButton.Style.SECONDARY,
                        () -> engine.requestState(GameState.LEADERBOARD));
                add("HOW TO PLAY", bx, 410, bw, 58, UiButton.Style.SECONDARY,
                        () -> engine.requestState(GameState.HOW_TO_PLAY));
                soundButton = add("SOUND: ON", bx, 484, bw, 58, UiButton.Style.SECONDARY, this::toggleSound);
                add("EXIT", bx, 558, bw, 58, UiButton.Style.DANGER, exitAction);
                break;
            case HOW_TO_PLAY:
                add("BACK", 330, 640, 240, 54, UiButton.Style.SECONDARY, () -> engine.requestState(GameState.MENU));
                techButton = add("TECHNICAL INFO", 710, 640, 300, 54, UiButton.Style.PRIMARY,
                        () -> ui.showTechInfo = !ui.showTechInfo);
                break;
            case LEADERBOARD:
                add("SEARCH", 730, 92, 130, 44, UiButton.Style.PRIMARY, this::doSearch);
                add("CLEAR", 870, 92, 110, 44, UiButton.Style.SECONDARY, this::clearSearch);
                add("< PREV", 230, 584, 130, 40, UiButton.Style.SECONDARY, () -> changePage(-1));
                add("NEXT >", 920, 584, 130, 40, UiButton.Style.SECONDARY, () -> changePage(1));
                add("BACK", 540, 642, 200, 52, UiButton.Style.SECONDARY, this::backFromLeaderboard);
                break;
            case PLAYING:
                undoButton = add("UNDO x3", 1056, 12, 118, 42, UiButton.Style.SECONDARY, this::performUndo);
                add("PAUSE", 1182, 12, 90, 42, UiButton.Style.SECONDARY, engine::pause);
                break;
            case PAUSED:
                add("RESUME", bx, 290, bw, 58, UiButton.Style.PRIMARY, engine::resume);
                add("RESTART", bx, 360, bw, 58, UiButton.Style.SECONDARY, this::startGame);
                add("MAIN MENU", bx, 430, bw, 58, UiButton.Style.DANGER, () -> engine.requestState(GameState.MENU));
                break;
            case GAME_OVER:
            case VICTORY:
                if (engine.getScore() > 0) {
                    saveButton = add("SAVE", 556, 492, 130, 46, UiButton.Style.PRIMARY, this::saveScoreIfNeeded);
                }
                add("PLAY AGAIN", 240, 590, 240, 54, UiButton.Style.PRIMARY, () -> {
                    saveScoreIfNeeded();
                    startGame();
                });
                add("LEADERBOARD", 520, 590, 240, 54, UiButton.Style.SECONDARY, () -> {
                    saveScoreIfNeeded();
                    engine.requestState(GameState.LEADERBOARD);
                });
                add("MAIN MENU", 800, 590, 240, 54, UiButton.Style.DANGER, () -> {
                    saveScoreIfNeeded();
                    engine.requestState(GameState.MENU);
                });
                break;
            default:
                break; // LEVEL_TRANSITION has no buttons (avoids accidental clicks)
        }
        updateDynamicButtons();
    }

    /** Keeps labels that depend on live data (sound state, undo charges) up to date. */
    private void updateDynamicButtons() {
        if (soundButton != null) {
            soundButton.setLabel(sound.isEnabled() ? "SOUND: ON" : "SOUND: OFF");
        }
        if (undoButton != null) {
            undoButton.setLabel("UNDO x" + engine.getUndoCharges());
            undoButton.setDimmed(engine.getUndoCharges() == 0 || engine.getHistorySize() == 0);
        }
        if (techButton != null) {
            techButton.setLabel(ui.showTechInfo ? "HOW TO PLAY" : "TECHNICAL INFO");
        }
        if (saveButton != null) {
            saveButton.setEnabled(!ui.scoreSaved); // nothing left to save once the score is stored
        }
    }

    // ===================================================================================== actions

    private void startGame() {
        engine.startNewGame(board.getBestScore());
    }

    private void toggleSound() {
        sound.setEnabled(!sound.isEnabled());
        updateDynamicButtons();
    }

    private void performUndo() {
        GameEngine.UndoResult result = engine.tryUndo();
        if (result != GameEngine.UndoResult.SUCCESS) {
            engine.showMessage(result.getMessage(), WARNING);
        }
    }

    private void backFromLeaderboard() {
        if (!engine.requestState(ui.leaderboardReturn)) {
            engine.requestState(GameState.MENU);
        }
    }

    private void changePage(int delta) {
        int pages = Math.max(1, (board.size() + GameRenderer.LEADERBOARD_ROWS - 1) / GameRenderer.LEADERBOARD_ROWS);
        ui.leaderboardPage = Math.max(0, Math.min(pages - 1, ui.leaderboardPage + delta));
    }

    /** Looks the typed name up with the Linear Search and jumps to the page that shows the player. */
    private void doSearch() {
        String query = ScoreEntry.sanitizeName(ui.searchInput.getText());
        if (query.isEmpty()) {
            ui.highlightRank = -1;
            ui.searchMessage = "Type a player name to search.";
            return;
        }
        int rank = board.searchPlayerRank(query);
        if (rank < 0) {
            ui.highlightRank = -1;
            ui.searchMessage = "No player named \"" + query + "\" found.";
            return;
        }
        ScoreEntry found = board.getRankedEntries()[rank - 1];
        ui.highlightRank = rank;
        ui.leaderboardPage = (rank - 1) / GameRenderer.LEADERBOARD_ROWS;
        ui.searchMessage = "Found " + found.getName() + ": rank #" + rank + " with " + found.getScore() + " points";
    }

    private void clearSearch() {
        ui.searchInput.setText("");
        ui.searchInput.setFocused(true);
        ui.searchMessage = "";
        ui.highlightRank = -1;
    }

    /**
     * Saves the finished run's score once, using the (validated) name from the name box.
     *
     * @return true if the score is saved (now or earlier)
     */
    private boolean saveScoreIfNeeded() {
        GameState state = engine.getState();
        if (state != GameState.GAME_OVER && state != GameState.VICTORY) {
            return false;
        }
        if (ui.scoreSaved) {
            return true;
        }
        if (engine.getScore() <= 0) {
            return false;
        }
        String name = ScoreEntry.sanitizeName(ui.nameInput.getText());
        if (name.isEmpty()) {
            ui.statusMessage = "Please type a name (letters, numbers, space, _ or -).";
            return false;
        }
        ScoreEntry entry = board.addScore(name, engine.getScore());
        if (entry == null) {
            ui.statusMessage = "This score could not be saved.";
            return false;
        }
        ui.scoreSaved = true;
        ui.savedRank = board.getRankOf(entry);
        lastPlayerName = name;
        ui.nameInput.setText(name);
        ui.nameInput.setFocused(false);
        ui.statusMessage = board.getLastError().isEmpty()
                ? "Saved! You are ranked #" + ui.savedRank + "."
                : "Kept in memory, but the file could not be written.";
        return true;
    }

    // ===================================================================================== mouse

    private UiButton buttonAt(double x, double y) {
        for (int i = ui.buttons.size() - 1; i >= 0; i--) {
            UiButton button = ui.buttons.get(i);
            if (button.contains(x, y)) {
                return button;
            }
        }
        return null;
    }

    private void onMousePressed(MouseEvent e) {
        requestFocusInWindow();
        if (!SwingUtilities.isLeftMouseButton(e)) {
            return;
        }
        Point2D.Double p = toWorld(e);
        updateTextFocus(p);
        UiButton button = buttonAt(p.x, p.y);
        if (button != null) {
            if (button.isEnabled()) {
                ui.pressed = button;
                button.setPressed(true);
            }
            return; // a press on a button never slices
        }
        if (shownState == GameState.PLAYING) {
            engine.beginStroke(p.x, p.y);
        }
    }

    private void onMouseDragged(MouseEvent e) {
        Point2D.Double p = toWorld(e);
        updateHover(p);
        if (shownState == GameState.PLAYING && (e.getModifiersEx() & InputEvent.BUTTON1_DOWN_MASK) != 0) {
            engine.extendStroke(p.x, p.y);
        }
    }

    private void onMouseMoved(MouseEvent e) {
        updateHover(toWorld(e));
    }

    /** A button fires only when the mouse is released on the same button it was pressed on. */
    private void onMouseReleased(MouseEvent e) {
        Point2D.Double p = toWorld(e);
        UiButton button = ui.pressed;
        ui.pressed = null;
        if (button != null) {
            button.setPressed(false);
            if (button.contains(p.x, p.y) && ui.animTime >= uiLockUntil) {
                sound.play(SoundEffect.CLICK);
                button.click();
                syncState();
            }
        }
        engine.endStroke();
    }

    private void updateHover(Point2D.Double p) {
        UiButton hit = buttonAt(p.x, p.y);
        if (hit != ui.hovered) {
            if (ui.hovered != null) {
                ui.hovered.setHovered(false);
            }
            ui.hovered = hit;
            if (hit != null) {
                hit.setHovered(true);
            }
            updateCursor();
        }
    }

    private void clearHover() {
        if (ui.hovered != null) {
            ui.hovered.setHovered(false);
            ui.hovered = null;
        }
        updateCursor();
    }

    private void updateCursor() {
        int type = Cursor.DEFAULT_CURSOR;
        if (ui.hovered != null && ui.hovered.isEnabled()) {
            type = Cursor.HAND_CURSOR;
        } else if (shownState == GameState.PLAYING) {
            type = Cursor.CROSSHAIR_CURSOR;
        }
        setCursor(Cursor.getPredefinedCursor(type));
    }

    private void updateTextFocus(Point2D.Double p) {
        if (shownState == GameState.GAME_OVER || shownState == GameState.VICTORY) {
            ui.nameInput.setFocused(!ui.scoreSaved && engine.getScore() > 0 && ui.nameInput.contains(p.x, p.y));
        } else if (shownState == GameState.LEADERBOARD) {
            ui.searchInput.setFocused(ui.searchInput.contains(p.x, p.y));
        }
    }

    // ===================================================================================== keyboard

    private TextInput focusedInput() {
        if ((shownState == GameState.GAME_OVER || shownState == GameState.VICTORY) && ui.nameInput.isFocused()) {
            return ui.nameInput;
        }
        if (shownState == GameState.LEADERBOARD && ui.searchInput.isFocused()) {
            return ui.searchInput;
        }
        return null;
    }

    private void onKeyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        if (code == KeyEvent.VK_ESCAPE) {
            handleEscape();
            return;
        }
        if (shownState == GameState.LEADERBOARD) {
            if (code == KeyEvent.VK_LEFT || code == KeyEvent.VK_PAGE_UP) {
                changePage(-1);
                return;
            }
            if (code == KeyEvent.VK_RIGHT || code == KeyEvent.VK_PAGE_DOWN) {
                changePage(1);
                return;
            }
        }
        TextInput typing = focusedInput();
        if (typing != null) {
            if (code == KeyEvent.VK_BACK_SPACE) {
                typing.backspace();
            } else if (code == KeyEvent.VK_ENTER) {
                if (typing == ui.searchInput) {
                    doSearch();
                } else {
                    saveScoreIfNeeded();
                }
            }
            return; // while typing, letters are text and never shortcuts
        }
        switch (code) {
            case KeyEvent.VK_P:
                engine.togglePause();
                break;
            case KeyEvent.VK_U:
                if (shownState == GameState.PLAYING) {
                    performUndo();
                }
                break;
            case KeyEvent.VK_Z:
                if (e.isControlDown() && shownState == GameState.PLAYING) {
                    performUndo();
                }
                break;
            case KeyEvent.VK_R:
                if (shownState == GameState.PAUSED || shownState == GameState.GAME_OVER
                        || shownState == GameState.VICTORY) {
                    saveScoreIfNeeded();
                    startGame();
                }
                break;
            case KeyEvent.VK_M:
                toggleSound();
                break;
            case KeyEvent.VK_ENTER:
                if (shownState == GameState.GAME_OVER || shownState == GameState.VICTORY) {
                    saveScoreIfNeeded();
                }
                break;
            default:
                break;
        }
        syncState();
    }

    private void onKeyTyped(KeyEvent e) {
        TextInput typing = focusedInput();
        if (typing == null || e.isControlDown() || e.isAltDown() || e.isMetaDown()) {
            return;
        }
        char c = e.getKeyChar();
        if (c >= 32 && c != 127) {
            typing.typeChar(c);
        }
    }

    private void handleEscape() {
        switch (shownState) {
            case PLAYING:
                engine.pause();
                break;
            case PAUSED:
                engine.resume();
                break;
            case HOW_TO_PLAY:
                engine.requestState(GameState.MENU);
                break;
            case LEADERBOARD:
                backFromLeaderboard();
                break;
            case GAME_OVER:
            case VICTORY:
                saveScoreIfNeeded();
                engine.requestState(GameState.MENU);
                break;
            default:
                break; // MENU and LEVEL_TRANSITION ignore Escape
        }
        syncState();
    }
}
