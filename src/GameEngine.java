import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

/**
 * The game model: owns the game state, every data structure and all gameplay rules.
 * It knows nothing about Swing, so it can be tested without opening a window.
 *
 * <p>Where each data structure is used:
 * <ul>
 *   <li>{@link ObjectArray} - active fruits and bombs (updated, hit-tested, compacted every frame)</li>
 *   <li>{@link SliceTrail} / {@link SinglyLinkedList} - recent mouse points of the blade</li>
 *   <li>{@link ArrayStack} - slice history for the Undo Slice power-up</li>
 *   <li>{@link CircularQueue} - upcoming {@link SpawnRequest}s</li>
 *   <li>{@link LevelManager#advanceLevelRecursively} - builds the next level's difficulty</li>
 * </ul>
 */
public class GameEngine {

    /** Outcome of an undo attempt; the message is shown to the player. */
    public enum UndoResult {
        SUCCESS("Undo!"),
        NOT_PLAYING("Undo is not available now"),
        NO_CHARGES("No undo charges left"),
        NOTHING_TO_UNDO("Nothing to undo"),
        TOO_OLD("Too late to undo"),
        NO_ROOM("No room to restore");

        private final String message;

        UndoResult(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    private static final Color GOLD = new Color(255, 214, 64);
    private static final Color WHITE = new Color(255, 255, 255);
    private static final Color RED = new Color(255, 82, 82);
    private static final Color CYAN = new Color(90, 220, 255);
    private static final Color GREEN = new Color(120, 255, 140);

    private final int startingLives;
    private final Random random;
    private Consumer<SoundEffect> soundListener = effect -> { };

    // ---- state machine ----
    private GameState state = GameState.MENU;
    private GameState resultState; // GAME_OVER or VICTORY after a finished run, else null

    // ---- data structures ----
    private final ObjectArray objects = new ObjectArray(GameConfig.MAX_ACTIVE_OBJECTS);
    private final CircularQueue<SpawnRequest> spawnQueue = new CircularQueue<>(GameConfig.SPAWN_QUEUE_CAPACITY);
    private final ArrayStack<SliceEvent> sliceHistory = new ArrayStack<>(GameConfig.UNDO_HISTORY_CAPACITY);
    private final SliceTrail trail = new SliceTrail(GameConfig.TRAIL_MAX_POINTS, GameConfig.TRAIL_MAX_AGE);

    // ---- visual effect lists (bounded) ----
    private final List<Particle> particles = new ArrayList<>();
    private final List<FruitHalf> halves = new ArrayList<>();
    private final List<FloatingText> floatingTexts = new ArrayList<>();

    // ---- run statistics ----
    private int score;
    private int lives;
    private int level = 1;
    private int combo;
    private int bestCombo;
    private int fruitsSliced;
    private int levelFruitsSliced;
    private int undoCharges;
    private int undosUsed;
    private int bestScoreAtStart;
    private boolean newBest;
    private int lastLevelBonus;

    // ---- timing ----
    private double gameTime;
    private double lastSliceTime = -100.0;
    private double spawnCooldown;
    private double transitionTimer;
    private double shakeTime;
    private double flashAlpha;
    private Color flashColor = WHITE;
    private double comboPulse;

    // ---- level data ----
    private LevelConfig levelConfig = LevelManager.configForLevel(1);
    private LevelConfig pendingConfig = levelConfig;

    // ---- blade input ----
    private boolean strokeActive;
    private double lastStrokeX;
    private double lastStrokeY;

    public GameEngine() {
        this(GameConfig.STARTING_LIVES, System.nanoTime());
    }

    /**
     * @param startingLives lives at the start of every run (at least 1)
     * @param seed          random seed (a fixed seed makes spawning deterministic for tests)
     */
    public GameEngine(int startingLives, long seed) {
        this.startingLives = Math.max(1, startingLives);
        this.random = new Random(seed);
        this.lives = this.startingLives;
    }

    public void setSoundListener(Consumer<SoundEffect> listener) {
        this.soundListener = listener == null ? effect -> { } : listener;
    }

    private void playSound(SoundEffect effect) {
        soundListener.accept(effect);
    }

    // =====================================================================================
    // State machine
    // =====================================================================================

    /** Performs a transition if (and only if) {@link GameState#canTransitionTo} allows it. */
    private boolean changeState(GameState next) {
        if (!state.canTransitionTo(next)) {
            return false;
        }
        state = next;
        if (next != GameState.PLAYING) {
            strokeActive = false; // a stale mouse drag must never continue into another state
            trail.clear();
        }
        return true;
    }

    /**
     * Starts a fresh run. Allowed from the menu, the pause screen and the result screens.
     *
     * @param bestScoreOnDisk the best score currently saved in the leaderboard
     * @return false if the current state does not allow starting a game
     */
    public boolean startNewGame(int bestScoreOnDisk) {
        boolean allowed = state == GameState.MENU || state == GameState.PAUSED
                || state == GameState.GAME_OVER || state == GameState.VICTORY;
        if (!allowed) {
            return false;
        }
        score = 0;
        lives = startingLives;
        level = 1;
        combo = 0;
        bestCombo = 0;
        fruitsSliced = 0;
        levelFruitsSliced = 0;
        undoCharges = GameConfig.INITIAL_UNDO_CHARGES;
        undosUsed = 0;
        newBest = false;
        lastLevelBonus = 0;
        bestScoreAtStart = Math.max(0, bestScoreOnDisk);
        gameTime = 0;
        lastSliceTime = -100.0;
        shakeTime = 0;
        flashAlpha = 0;
        comboPulse = 0;
        resultState = null;
        clearPlayfield();
        particles.clear();
        halves.clear();
        floatingTexts.clear();
        levelConfig = LevelManager.configForLevel(1);
        pendingConfig = levelConfig;
        if (!changeState(GameState.PLAYING)) {
            return false;
        }
        refillSpawnQueue();
        spawnCooldown = GameConfig.INITIAL_SPAWN_DELAY;
        return true;
    }

    /**
     * Requests a screen change that does not involve gameplay logic (menu, how-to-play, leaderboard,
     * pause, returning to a finished run's result screen).
     */
    public boolean requestState(GameState target) {
        if (target == GameState.PLAYING || target == GameState.LEVEL_TRANSITION) {
            return false; // these are only entered by startNewGame / resume / level logic
        }
        if ((target == GameState.GAME_OVER || target == GameState.VICTORY) && target != resultState) {
            return false; // can only go back to the result screen of a run that really ended that way
        }
        return changeState(target);
    }

    public boolean pause() {
        return state == GameState.PLAYING && changeState(GameState.PAUSED);
    }

    public boolean resume() {
        return state == GameState.PAUSED && changeState(GameState.PLAYING);
    }

    public void togglePause() {
        if (state == GameState.PLAYING) {
            pause();
        } else if (state == GameState.PAUSED) {
            resume();
        }
    }

    private void clearPlayfield() {
        objects.clear();
        spawnQueue.clear();
        sliceHistory.clear();
        trail.clear();
        strokeActive = false;
    }

    // =====================================================================================
    // Per-frame update
    // =====================================================================================

    /** Advances the game by {@code deltaSeconds}. Does nothing in states that must stay frozen. */
    public void update(double deltaSeconds) {
        final double dt = Math.max(0.0, Math.min(deltaSeconds, 0.05));
        switch (state) {
            case PLAYING:
                updatePlaying(dt);
                break;
            case LEVEL_TRANSITION:
                updateTransition(dt);
                break;
            case GAME_OVER:
            case VICTORY:
                updateEffects(dt); // let particles finish; gameplay is over
                break;
            default:
                break; // MENU, PAUSED, HOW_TO_PLAY, LEADERBOARD: nothing changes
        }
    }

    private void updatePlaying(double dt) {
        gameTime += dt;
        updateSpawning(dt);
        updateObjects(dt);
        if (state == GameState.PLAYING) {
            if (combo > 0 && gameTime - lastSliceTime > GameConfig.COMBO_WINDOW) {
                combo = 0;
            }
            pruneHistory();
            trail.update(gameTime);
        }
        updateEffects(dt);
    }

    private void updateTransition(double dt) {
        transitionTimer -= dt;
        updateEffects(dt);
        if (transitionTimer <= 0) {
            beginPendingLevel();
        }
    }

    /**
     * Consumes spawn requests from the FRONT of the queue when their delay has elapsed and keeps the
     * queue topped up at the REAR. A guard limits the number of launches per frame.
     */
    private void updateSpawning(double dt) {
        spawnCooldown -= dt;
        int guard = 0;
        while (spawnCooldown <= 0 && guard++ < 8) {
            if (spawnQueue.isEmpty()) {
                refillSpawnQueue();
            }
            SpawnRequest request = spawnQueue.dequeue();
            if (request == null) {
                spawnCooldown = levelConfig.getWaveInterval();
                break;
            }
            launch(request);
            if (spawnQueue.size() < GameConfig.SPAWN_LOOKAHEAD) {
                refillSpawnQueue();
            }
            SpawnRequest next = spawnQueue.peek();
            spawnCooldown += next == null ? levelConfig.getWaveInterval() : next.getDelay();
        }
    }

    /** Enqueues whole waves until the queue holds at least SPAWN_LOOKAHEAD requests. */
    private void refillSpawnQueue() {
        int guard = 0;
        while (spawnQueue.size() < GameConfig.SPAWN_LOOKAHEAD && guard++ < 10) {
            int waveSize = 1 + random.nextInt(levelConfig.getMaxWaveSize());
            boolean bombUsed = false;
            for (int i = 0; i < waveSize; i++) {
                double delay;
                if (i == 0) {
                    delay = levelConfig.getWaveInterval() * (0.85 + 0.30 * random.nextDouble());
                } else {
                    delay = 0.12 + 0.12 * random.nextDouble();
                }
                // first item of a wave is always a fruit, at most one bomb per wave
                boolean bomb = i > 0 && !bombUsed && random.nextDouble() < levelConfig.getBombChance();
                bombUsed |= bomb;
                if (!spawnQueue.enqueue(LevelManager.createRequest(levelConfig, random, bomb, delay))) {
                    return; // queue full: stop safely
                }
            }
        }
    }

    private void launch(SpawnRequest request) {
        GameObject object;
        if (request.getKind() == SpawnRequest.Kind.BOMB) {
            object = new Bomb(request.getX(), GameConfig.SPAWN_Y, request.getVx(), request.getVy(),
                    request.getGravity(), request.getSpin());
        } else {
            object = new Fruit(request.getFruitType(), request.getX(), GameConfig.SPAWN_Y,
                    request.getVx(), request.getVy(), request.getGravity(), request.getSpin());
        }
        objects.add(object); // returns false when the array is full: the launch is skipped
    }

    /**
     * Moves every object, then removes finished ones in one compaction pass after the loop
     * (never while iterating), so no element is skipped.
     */
    private void updateObjects(double dt) {
        for (int i = 0; i < objects.size(); i++) {
            GameObject object = objects.get(i);
            if (!object.isActive()) {
                continue;
            }
            object.update(dt);
            if (object.isMissed(GameConfig.WORLD_HEIGHT)) {
                object.deactivate();
                if (!object.isBomb()) {
                    loseLifeForMiss(object);
                }
            } else if (object.isFarOutside(GameConfig.WORLD_WIDTH)) {
                object.deactivate(); // drifted away sideways: removed without penalty
            }
            if (state != GameState.PLAYING) {
                break;
            }
        }
        objects.compactInactive();
    }

    private void loseLifeForMiss(GameObject fruit) {
        lives--;
        combo = 0;
        double textX = Math.max(80, Math.min(GameConfig.WORLD_WIDTH - 80, fruit.getX()));
        addText("MISS!", textX, GameConfig.WORLD_HEIGHT - 70, 34, RED, 1.0);
        playSound(SoundEffect.MISS);
        flash(RED, 0.25);
        if (lives <= 0) {
            finishRun(false);
        }
    }

    /** Because events are time-ordered, if the newest event is too old then all of them are. */
    private void pruneHistory() {
        SliceEvent newest = sliceHistory.peek();
        if (newest != null && gameTime - newest.getTime() > GameConfig.UNDO_WINDOW) {
            sliceHistory.clear();
        }
    }

    private void updateEffects(double dt) {
        particles.removeIf(p -> !p.update(dt));
        halves.removeIf(h -> !h.update(dt));
        floatingTexts.removeIf(t -> !t.update(dt));
        shakeTime = Math.max(0, shakeTime - dt);
        flashAlpha = Math.max(0, flashAlpha - dt * 1.8);
        comboPulse = Math.max(0, comboPulse - dt * 3.0);
    }

    // =====================================================================================
    // Blade input (mouse)
    // =====================================================================================

    /** Mouse pressed: starts a new stroke; a plain click also slices what is under the pointer. */
    public void beginStroke(double x, double y) {
        if (state != GameState.PLAYING) {
            return; // stale events after pause / game over are ignored
        }
        strokeActive = true;
        trail.beginStroke();
        trail.addPoint(x, y, gameTime);
        lastStrokeX = x;
        lastStrokeY = y;
        sliceSegment(x, y, x, y);
    }

    /** Mouse dragged: tests the segment from the previous point to the new point. */
    public void extendStroke(double x, double y) {
        if (state != GameState.PLAYING || !strokeActive) {
            return;
        }
        double dx = x - lastStrokeX;
        double dy = y - lastStrokeY;
        if (dx * dx + dy * dy < 1.0) {
            return; // ignore sub-pixel jitter
        }
        trail.addPoint(x, y, gameTime);
        sliceSegment(lastStrokeX, lastStrokeY, x, y);
        lastStrokeX = x;
        lastStrokeY = y;
    }

    public void endStroke() {
        strokeActive = false;
    }

    /**
     * Collision detection: each active, un-sliced, non-immune object is tested against the blade
     * segment. An object is marked sliced the moment it is hit, so repeated mouse events can never
     * slice it twice. Objects are removed only after the loop.
     */
    private void sliceSegment(double x0, double y0, double x1, double y1) {
        double angle = Math.atan2(y1 - y0, x1 - x0);
        if (x0 == x1 && y0 == y1) {
            angle = random.nextDouble() * Math.PI; // a click has no direction: pick a random cut
        }
        for (int i = 0; i < objects.size(); i++) {
            GameObject object = objects.get(i);
            if (!object.isActive() || object.isSliced() || object.isImmune(gameTime)) {
                continue;
            }
            if (!object.intersectsSegment(x0, y0, x1, y1, GameConfig.BLADE_TOLERANCE)) {
                continue;
            }
            if (object.isBomb()) {
                sliceBomb((Bomb) object);
            } else {
                sliceFruit((Fruit) object, angle);
            }
            if (state != GameState.PLAYING) {
                break; // the bomb ended the run
            }
        }
        objects.compactInactive();
        checkLevelComplete();
    }

    private void sliceFruit(Fruit fruit, double angle) {
        int comboBefore = combo;
        double lastTimeBefore = lastSliceTime;
        int bestComboBefore = bestCombo;
        GameObject snapshot = fruit.copy();

        fruit.markSliced();
        combo = (combo > 0 && gameTime - lastSliceTime <= GameConfig.COMBO_WINDOW) ? combo + 1 : 1;
        lastSliceTime = gameTime;
        bestCombo = Math.max(bestCombo, combo);
        int points = fruit.getBasePoints()
                + GameConfig.COMBO_BONUS_PER_STEP * Math.min(combo - 1, GameConfig.COMBO_MAX_STEPS);
        score = Math.min(ScoreEntry.MAX_SCORE, score + points);
        fruitsSliced++;
        levelFruitsSliced++;
        comboPulse = 1.0;

        pushHistory(new SliceEvent(snapshot, points, 0, 1, comboBefore, combo,
                lastTimeBefore, bestComboBefore, gameTime, level));
        spawnSliceEffects(fruit, angle, points);
        playSound(SoundEffect.SLICE);
    }

    private void sliceBomb(Bomb bomb) {
        int comboBefore = combo;
        double lastTimeBefore = lastSliceTime;
        int bestComboBefore = bestCombo;
        GameObject snapshot = bomb.copy();

        bomb.markSliced();
        int penalty = Math.min(score, GameConfig.BOMB_SCORE_PENALTY);
        score -= penalty;
        lives--;
        combo = 0;

        pushHistory(new SliceEvent(snapshot, -penalty, -1, 0, comboBefore, 0,
                lastTimeBefore, bestComboBefore, gameTime, level));
        spawnBombEffects(bomb, penalty);
        playSound(SoundEffect.BOMB);
        if (lives <= 0) {
            finishRun(false);
        }
    }

    private void pushHistory(SliceEvent event) {
        if (!sliceHistory.push(event)) {
            sliceHistory.clear(); // full (should be rare): drop the stale history and keep the newest event
            sliceHistory.push(event);
        }
    }

    // =====================================================================================
    // Undo Slice power-up (uses the stack)
    // =====================================================================================

    /**
     * Reverses the most recent eligible slice.
     *
     * <p>Order of operations: validate everything first using {@code peek}, and only then
     * {@code pop}. A failed attempt therefore never changes any counter and never loses the event.
     * The slice's score, life and fruit counters are reversed exactly once (the event is popped
     * exactly once), and the object is re-created from its snapshot as a NEW object that is briefly
     * immune, so the same mouse swipe cannot instantly slice it again.
     */
    public UndoResult tryUndo() {
        if (state != GameState.PLAYING) {
            return UndoResult.NOT_PLAYING;
        }
        if (undoCharges <= 0) {
            return UndoResult.NO_CHARGES;
        }
        SliceEvent event = sliceHistory.peek();
        if (event == null) {
            return UndoResult.NOTHING_TO_UNDO;
        }
        if (event.getLevel() != level || gameTime - event.getTime() > GameConfig.UNDO_WINDOW) {
            sliceHistory.clear();
            return UndoResult.TOO_OLD;
        }
        if (objects.isFull()) {
            return UndoResult.NO_ROOM;
        }

        sliceHistory.pop(); // validated: now commit

        GameObject restored = event.getSnapshot().copy();
        restored.setImmuneUntil(gameTime + GameConfig.RESTORE_IMMUNITY);
        objects.add(restored);

        score = Math.max(0, score - event.getScoreDelta());
        lives = Math.min(startingLives, lives - event.getLivesDelta());
        fruitsSliced = Math.max(0, fruitsSliced - event.getFruitDelta());
        levelFruitsSliced = Math.max(0, levelFruitsSliced - event.getFruitDelta());
        bestCombo = event.getBestComboBefore();
        if (combo == event.getComboAfter()) {
            // nothing else changed the combo since this slice: restore it if its time window is still open
            if (gameTime - event.getLastSliceTimeBefore() <= GameConfig.COMBO_WINDOW) {
                combo = event.getComboBefore();
                lastSliceTime = event.getLastSliceTimeBefore();
            } else {
                combo = 0;
            }
        }
        undoCharges--;
        undosUsed++;

        String text = event.getLivesDelta() < 0 ? "UNDO! +1 life" : "UNDO! -" + event.getScoreDelta();
        addText(text, restored.getX(), Math.min(restored.getY(), GameConfig.WORLD_HEIGHT - 120), 30, CYAN, 1.3);
        for (int i = 0; i < 18; i++) {
            double a = random.nextDouble() * Math.PI * 2;
            double speed = 80 + random.nextDouble() * 220;
            addParticle(new Particle(restored.getX(), restored.getY(), Math.cos(a) * speed, Math.sin(a) * speed,
                    0, 2.5, 6, 1, 0.55, CYAN, true));
        }
        playSound(SoundEffect.UNDO);
        return UndoResult.SUCCESS;
    }

    // =====================================================================================
    // Level progression / end of run
    // =====================================================================================

    private void checkLevelComplete() {
        if (state == GameState.PLAYING && levelFruitsSliced >= levelConfig.getFruitTarget()) {
            completeLevel();
        }
    }

    private void completeLevel() {
        lastLevelBonus = level * GameConfig.LEVEL_BONUS_PER_LEVEL;
        score = Math.min(ScoreEntry.MAX_SCORE, score + lastLevelBonus);
        if (level >= GameConfig.MAX_LEVEL) {
            finishRun(true);
            return;
        }
        // Recursive level initialisation: the next level's difficulty is derived step by step from level 1.
        pendingConfig = LevelManager.configForLevel(level + 1);
        undoCharges = Math.min(GameConfig.MAX_UNDO_CHARGES, undoCharges + 1);
        clearPlayfield();
        floatingTexts.clear(); // keep the level-complete banner readable
        combo = 0;
        transitionTimer = GameConfig.LEVEL_TRANSITION_TIME;
        changeState(GameState.LEVEL_TRANSITION);
        playSound(SoundEffect.LEVEL_UP);
    }

    private void beginPendingLevel() {
        levelConfig = pendingConfig;
        level = levelConfig.getLevel();
        levelFruitsSliced = 0;
        combo = 0;
        clearPlayfield();
        if (changeState(GameState.PLAYING)) {
            refillSpawnQueue();
            spawnCooldown = 0.7;
        }
    }

    private void finishRun(boolean victory) {
        if (state != GameState.PLAYING) {
            return;
        }
        if (victory) {
            score = Math.min(ScoreEntry.MAX_SCORE, score + lives * GameConfig.VICTORY_LIFE_BONUS);
        }
        newBest = score > bestScoreAtStart;
        clearPlayfield();
        resultState = victory ? GameState.VICTORY : GameState.GAME_OVER;
        changeState(resultState);
        playSound(victory ? SoundEffect.VICTORY : SoundEffect.GAME_OVER);
    }

    // =====================================================================================
    // Visual effects
    // =====================================================================================

    private void spawnSliceEffects(Fruit fruit, double angle, int points) {
        double x = fruit.getX();
        double y = fruit.getY();
        FruitType type = fruit.getType();
        double nx = -Math.sin(angle);
        double ny = Math.cos(angle);
        double cutRotation = angle - Math.PI / 2.0;

        if (halves.size() + 2 <= GameConfig.MAX_HALVES) {
            for (int side = -1; side <= 1; side += 2) {
                double dir = -side;
                double hvx = fruit.getVx() * 0.5 + nx * dir * 170;
                double hvy = fruit.getVy() * 0.4 + ny * dir * 170 - 60;
                halves.add(new FruitHalf(type, fruit.getRadius(), side, x, y, hvx, hvy,
                        cutRotation, dir * (1.5 + random.nextDouble() * 2.5)));
            }
        }
        for (int i = 0; i < 16; i++) {
            double spread = (random.nextDouble() - 0.5) * 2.0;
            double dir = random.nextBoolean() ? 1 : -1;
            double speed = 120 + random.nextDouble() * 330;
            double vx = nx * dir * speed + spread * 90;
            double vy = ny * dir * speed + spread * 90 - 120;
            addParticle(new Particle(x, y, vx, vy, 900, 0.4,
                    5 + random.nextDouble() * 5, 1.5, 0.6 + random.nextDouble() * 0.5, type.getJuice(), false));
        }
        addParticle(new Particle(x, y, 0, 0, 0, 0, fruit.getRadius() * 1.5, fruit.getRadius() * 1.9,
                1.6, withAlpha(type.getJuice(), 120), false)); // juice splat on the background
        // bright slash flash along the cut
        for (int i = -3; i <= 3; i++) {
            addParticle(new Particle(x + Math.cos(angle) * i * 9, y + Math.sin(angle) * i * 9,
                    Math.cos(angle) * i * 40, Math.sin(angle) * i * 40, 0, 4, 7, 0, 0.25, WHITE, true));
        }
        double textX = Math.max(60, Math.min(GameConfig.WORLD_WIDTH - 60, x));
        double textY = Math.max(110, y - 30);
        addText("+" + points, textX, textY, 30, GOLD, 0.9);
        if (combo >= 3) {
            addText("COMBO x" + combo + "!", textX, Math.max(100, textY - 38), 36 + Math.min(combo, 10),
                    comboColor(combo), 1.1);
        }
    }

    private void spawnBombEffects(Bomb bomb, int penalty) {
        double x = bomb.getX();
        double y = bomb.getY();
        for (int i = 0; i < 40; i++) {
            double a = random.nextDouble() * Math.PI * 2;
            double speed = 100 + random.nextDouble() * 520;
            Color c = random.nextBoolean() ? new Color(255, 170, 40) : new Color(255, 90, 40);
            addParticle(new Particle(x, y, Math.cos(a) * speed, Math.sin(a) * speed, 300, 1.8,
                    7 + random.nextDouble() * 7, 1, 0.5 + random.nextDouble() * 0.5, c, true));
        }
        for (int i = 0; i < 14; i++) {
            double a = random.nextDouble() * Math.PI * 2;
            double speed = 30 + random.nextDouble() * 140;
            addParticle(new Particle(x, y, Math.cos(a) * speed, Math.sin(a) * speed - 40, -60, 1.2,
                    16, 40, 0.9 + random.nextDouble() * 0.5, new Color(70, 70, 80, 150), false));
        }
        flash(WHITE, 0.85);
        shakeTime = 0.45;
        String message = penalty > 0 ? "BOMB! -1 life  -" + penalty : "BOMB! -1 life";
        addText(message, Math.max(180, Math.min(GameConfig.WORLD_WIDTH - 180, x)), Math.max(120, y - 40),
                38, RED, 1.4);
    }

    private static Color comboColor(int combo) {
        if (combo >= 10) {
            return new Color(255, 90, 220);
        }
        if (combo >= 6) {
            return new Color(255, 140, 50);
        }
        if (combo >= 4) {
            return GOLD;
        }
        return GREEN;
    }

    private static Color withAlpha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    }

    private void flash(Color color, double strength) {
        flashColor = color;
        flashAlpha = Math.max(flashAlpha, strength);
    }

    private void addParticle(Particle particle) {
        if (particles.size() < GameConfig.MAX_PARTICLES) {
            particles.add(particle);
        }
    }

    private void addText(String text, double x, double y, int size, Color color, double life) {
        if (floatingTexts.size() >= GameConfig.MAX_FLOATING_TEXTS) {
            floatingTexts.remove(0);
        }
        floatingTexts.add(new FloatingText(text, x, y, size, color, life));
    }

    /** Lets the UI show a short message such as "No undo charges left". */
    public void showMessage(String text, Color color) {
        addText(text, GameConfig.WORLD_WIDTH / 2.0, GameConfig.WORLD_HEIGHT / 2.0, 32, color, 1.4);
    }

    // =====================================================================================
    // Read-only accessors (used by the renderer, the UI and the tests)
    // =====================================================================================

    public GameState getState() { return state; }
    public int getScore() { return score; }
    public int getLives() { return lives; }
    public int getStartingLives() { return startingLives; }
    public int getLevel() { return level; }
    public int getCombo() { return combo; }
    public int getBestCombo() { return bestCombo; }
    public int getFruitsSliced() { return fruitsSliced; }
    public int getLevelFruitsSliced() { return levelFruitsSliced; }
    public int getLevelTarget() { return levelConfig.getFruitTarget(); }
    public int getUndoCharges() { return undoCharges; }
    public int getUndosUsed() { return undosUsed; }
    public int getBestScoreAtStart() { return bestScoreAtStart; }
    public int getBestScore() { return Math.max(bestScoreAtStart, score); }
    public boolean isNewBest() { return newBest; }
    public int getLastLevelBonus() { return lastLevelBonus; }
    public double getGameTime() { return gameTime; }
    public double getShakeTime() { return shakeTime; }
    public double getFlashAlpha() { return flashAlpha; }
    public Color getFlashColor() { return flashColor; }
    public double getComboPulse() { return comboPulse; }
    public LevelConfig getLevelConfig() { return levelConfig; }
    public LevelConfig getPendingConfig() { return pendingConfig; }
    public double getTransitionProgress() {
        return 1.0 - Math.max(0, transitionTimer) / GameConfig.LEVEL_TRANSITION_TIME;
    }
    public ObjectArray getObjects() { return objects; }
    public SliceTrail getTrail() { return trail; }
    public List<Particle> getParticles() { return particles; }
    public List<FruitHalf> getHalves() { return halves; }
    public List<FloatingText> getFloatingTexts() { return floatingTexts; }
    public int getHistorySize() { return sliceHistory.size(); }
    public int getSpawnQueueSize() { return spawnQueue.size(); }
}
