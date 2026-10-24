/**
 * Immutable difficulty settings of one level. Each level is defined from the previous one by
 * {@link #nextHarder()}, which is exactly the recurrence that the recursive level function in
 * {@link LevelManager} applies.
 */
public final class LevelConfig {

    private final int level;
    private final int fruitTarget;
    private final double waveInterval;
    private final double speedScale;
    private final double bombChance;
    private final int maxWaveSize;

    private LevelConfig(int level, int fruitTarget, double waveInterval,
                        double speedScale, double bombChance, int maxWaveSize) {
        this.level = level;
        this.fruitTarget = fruitTarget;
        this.waveInterval = waveInterval;
        this.speedScale = speedScale;
        this.bombChance = bombChance;
        this.maxWaveSize = maxWaveSize;
    }

    /** The settings of level 1 (the base case of the recurrence). */
    public static LevelConfig first() {
        return new LevelConfig(1, 10, 1.70, 1.00, 0.10, 3);
    }

    /** @return the configuration of the next level: more fruit to slice, faster and busier. */
    public LevelConfig nextHarder() {
        int nextLevel = level + 1;
        return new LevelConfig(
                nextLevel,
                fruitTarget + 3,
                Math.max(0.70, waveInterval * 0.92),
                Math.min(1.40, speedScale + 0.05),
                Math.min(0.35, bombChance + 0.03),
                Math.min(6, 3 + (nextLevel - 1) / 2));
    }

    public int getLevel() { return level; }
    /** Fruits that must be sliced to complete the level. */
    public int getFruitTarget() { return fruitTarget; }
    /** Average seconds between launch waves (smaller = busier). */
    public double getWaveInterval() { return waveInterval; }
    /** Multiplier applied to launch speed and gravity (bigger = faster). */
    public double getSpeedScale() { return speedScale; }
    /** Probability that an extra slot of a wave is a bomb. */
    public double getBombChance() { return bombChance; }
    public int getMaxWaveSize() { return maxWaveSize; }
}
