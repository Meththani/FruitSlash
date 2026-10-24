/**
 * Everything needed to reverse one slice. An instance is pushed on the undo
 * {@link ArrayStack} whenever a fruit or bomb is sliced.
 *
 * <p>The snapshot is a copy of the object at the moment it was sliced; undo creates a fresh object
 * from that copy, so the original (already finished) object is never "revived" and duplicates
 * cannot appear.
 */
public final class SliceEvent {

    private final GameObject snapshot;
    private final int scoreDelta;      // points added (+) or removed (-) by the slice
    private final int livesDelta;      // 0 for fruit, -1 for a bomb
    private final int fruitDelta;      // 1 for fruit, 0 for a bomb
    private final int comboBefore;
    private final int comboAfter;
    private final double lastSliceTimeBefore;
    private final int bestComboBefore;
    private final double time;
    private final int level;

    public SliceEvent(GameObject snapshot, int scoreDelta, int livesDelta, int fruitDelta,
                      int comboBefore, int comboAfter, double lastSliceTimeBefore,
                      int bestComboBefore, double time, int level) {
        this.snapshot = snapshot;
        this.scoreDelta = scoreDelta;
        this.livesDelta = livesDelta;
        this.fruitDelta = fruitDelta;
        this.comboBefore = comboBefore;
        this.comboAfter = comboAfter;
        this.lastSliceTimeBefore = lastSliceTimeBefore;
        this.bestComboBefore = bestComboBefore;
        this.time = time;
        this.level = level;
    }

    public GameObject getSnapshot() { return snapshot; }
    public int getScoreDelta() { return scoreDelta; }
    public int getLivesDelta() { return livesDelta; }
    public int getFruitDelta() { return fruitDelta; }
    public int getComboBefore() { return comboBefore; }
    public int getComboAfter() { return comboAfter; }
    public double getLastSliceTimeBefore() { return lastSliceTimeBefore; }
    public int getBestComboBefore() { return bestComboBefore; }
    public double getTime() { return time; }
    public int getLevel() { return level; }
}
