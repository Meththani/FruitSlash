import java.util.Random;

/**
 * Difficulty progression and the recursive level-initialisation function.
 */
public final class LevelManager {

    private LevelManager() {
        // static helpers only
    }

    /**
     * Recursively advances a level configuration until it reaches {@code targetLevel}.
     *
     * <ul>
     *   <li><b>Base case:</b> the current level is already at (or beyond) the clamped target, so the
     *       current configuration is returned unchanged.</li>
     *   <li><b>Recursive case:</b> call this method again with {@code current.nextHarder()}. Each call
     *       raises the level by exactly one, so the recursion always moves towards the base case.</li>
     *   <li><b>Safety:</b> the target is clamped to {@code 1 .. maximumLevel}, therefore the recursion depth
     *       can never exceed {@code maximumLevel - 1} and a stack overflow is impossible.</li>
     * </ul>
     *
     * @param current      configuration to start from (normally {@link LevelConfig#first()})
     * @param targetLevel  level that should be reached
     * @param maximumLevel highest level of the game; targets above it are clamped
     * @return the configuration of the (clamped) target level
     */
    public static LevelConfig advanceLevelRecursively(LevelConfig current, int targetLevel, int maximumLevel) {
        int maxLevel = Math.max(1, maximumLevel);
        int target = Math.max(1, Math.min(targetLevel, maxLevel));
        if (current.getLevel() >= target) {
            return current; // base case
        }
        return advanceLevelRecursively(current.nextHarder(), target, maxLevel); // progress towards base case
    }

    /** Builds the configuration of any level by recursively advancing from level 1. */
    public static LevelConfig configForLevel(int level) {
        return advanceLevelRecursively(LevelConfig.first(), level, GameConfig.MAX_LEVEL);
    }

    /**
     * Creates one spawn request. The launch speed is chosen so that the object's highest point lies
     * between 20% and 50% of the screen height (from the top): v = sqrt(2 * g * rise).
     */
    public static SpawnRequest createRequest(LevelConfig config, Random random, boolean bomb, double delay) {
        double speed = config.getSpeedScale();
        double gravity = GameConfig.BASE_GRAVITY * speed * speed;
        double x = GameConfig.WORLD_WIDTH * (0.10 + 0.80 * random.nextDouble());
        double apexY = GameConfig.WORLD_HEIGHT * (0.20 + 0.30 * random.nextDouble());
        double rise = GameConfig.SPAWN_Y - apexY;
        double vy = -Math.sqrt(2.0 * gravity * rise);
        double towardsCentre = GameConfig.WORLD_WIDTH / 2.0 - x;
        double vx = towardsCentre * (0.08 + 0.35 * random.nextDouble()) * speed
                + (random.nextDouble() * 2.0 - 1.0) * 40.0 * speed;
        double spin = (random.nextDouble() * 2.0 - 1.0) * 5.0;
        if (bomb) {
            return new SpawnRequest(SpawnRequest.Kind.BOMB, null, x, vx, vy, gravity, spin, delay);
        }
        FruitType[] types = FruitType.values();
        FruitType type = types[random.nextInt(types.length)];
        return new SpawnRequest(SpawnRequest.Kind.FRUIT, type, x, vx, vy, gravity, spin, delay);
    }
}
