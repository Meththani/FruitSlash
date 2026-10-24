/**
 * An immutable description of one object that will be launched in the future. Requests wait in a
 * {@link CircularQueue}; the engine dequeues the front request when its {@code delay} (seconds
 * since the previous launch) has elapsed.
 */
public final class SpawnRequest {

    /** What kind of object to launch. */
    public enum Kind { FRUIT, BOMB }

    private final Kind kind;
    private final FruitType fruitType; // null for bombs
    private final double x;
    private final double vx;
    private final double vy;
    private final double gravity;
    private final double spin;
    private final double delay;

    public SpawnRequest(Kind kind, FruitType fruitType, double x, double vx, double vy,
                        double gravity, double spin, double delay) {
        if (kind == Kind.FRUIT && fruitType == null) {
            throw new IllegalArgumentException("a fruit request needs a fruit type");
        }
        this.kind = kind;
        this.fruitType = fruitType;
        this.x = x;
        this.vx = vx;
        this.vy = vy;
        this.gravity = gravity;
        this.spin = spin;
        this.delay = delay;
    }

    public Kind getKind() { return kind; }
    public FruitType getFruitType() { return fruitType; }
    public double getX() { return x; }
    public double getVx() { return vx; }
    public double getVy() { return vy; }
    public double getGravity() { return gravity; }
    public double getSpin() { return spin; }
    public double getDelay() { return delay; }
}
