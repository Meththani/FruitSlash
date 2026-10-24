/**
 * Central place for every tunable gameplay constant.
 * Keeping them here (instead of scattering "magic numbers") makes the game easy to balance
 * and lets the unit tests refer to the same values as the engine.
 */
public final class GameConfig {

    private GameConfig() {
        // constants only
    }

    // ---- World / window (logical coordinate system: 1280 x 720, scaled to the window) ----
    public static final int WORLD_WIDTH = 1280;
    public static final int WORLD_HEIGHT = 720;
    public static final int MIN_WINDOW_WIDTH = 800;
    public static final int MIN_WINDOW_HEIGHT = 500;
    public static final int START_WINDOW_WIDTH = 1152;
    public static final int START_WINDOW_HEIGHT = 648;

    // ---- Lives / levels ----
    /** Lives at the start of a run. Change this one constant to make the game easier or harder. */
    public static final int STARTING_LIVES = 3;
    public static final int MAX_LEVEL = 8;
    public static final int LEVEL_BONUS_PER_LEVEL = 50;
    public static final int VICTORY_LIFE_BONUS = 100;
    public static final double LEVEL_TRANSITION_TIME = 2.6;
    public static final double INITIAL_SPAWN_DELAY = 0.8;

    // ---- Capacities of the bounded data structures ----
    public static final int MAX_ACTIVE_OBJECTS = 32;
    public static final int SPAWN_QUEUE_CAPACITY = 32;
    public static final int SPAWN_LOOKAHEAD = 6;
    public static final int TRAIL_MAX_POINTS = 20;
    public static final double TRAIL_MAX_AGE = 0.28;
    public static final int UNDO_HISTORY_CAPACITY = 32;
    public static final int MAX_PARTICLES = 450;
    public static final int MAX_FLOATING_TEXTS = 24;
    public static final int MAX_HALVES = 24;

    // ---- Physics / spawning ----
    public static final double BASE_GRAVITY = 900.0;
    public static final double SPAWN_Y = WORLD_HEIGHT + 80.0;
    public static final double OFFSCREEN_MARGIN = 150.0;

    // ---- Slicing / scoring ----
    public static final double BLADE_TOLERANCE = 6.0;
    public static final double COMBO_WINDOW = 1.1;
    public static final int COMBO_BONUS_PER_STEP = 2;
    public static final int COMBO_MAX_STEPS = 10;
    public static final int BOMB_SCORE_PENALTY = 25;

    // ---- Undo Slice power-up ----
    public static final int INITIAL_UNDO_CHARGES = 3;
    public static final int MAX_UNDO_CHARGES = 5;
    /** A slice can only be undone for this many seconds after it happened. */
    public static final double UNDO_WINDOW = 3.5;
    /** A restored object cannot be sliced again for this long (prevents instant re-slicing). */
    public static final double RESTORE_IMMUNITY = 0.45;

    // ---- UI ----
    /** Seconds after a screen change during which buttons ignore clicks (avoids accidental clicks). */
    public static final double UI_LOCK_TIME = 0.4;
}
