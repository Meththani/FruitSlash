/**
 * Base class for everything that flies through the play area (fruits and bombs).
 * Coordinates use the logical 1280 x 720 world (y grows downwards).
 */
public abstract class GameObject {

    protected double x;
    protected double y;
    protected double vx;
    protected double vy;
    protected final double gravity;
    protected double rotation;
    protected final double spin;
    protected final double radius;

    private boolean active = true;
    private boolean sliced = false;
    private double immuneUntil = Double.NEGATIVE_INFINITY;

    protected GameObject(double x, double y, double vx, double vy,
                         double gravity, double spin, double radius) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.gravity = gravity;
        this.spin = spin;
        this.radius = radius;
    }

    /** Advances the physics by {@code dt} seconds (semi-implicit Euler integration). */
    public void update(double dt) {
        vy += gravity * dt;
        x += vx * dt;
        y += vy * dt;
        rotation += spin * dt;
    }

    /** @return true when the object has fallen below the bottom of the screen without being sliced. */
    public boolean isMissed(double worldHeight) {
        return active && vy > 0 && y - radius > worldHeight;
    }

    /** @return true when the object drifted far outside the left/right edges (removed silently). */
    public boolean isFarOutside(double worldWidth) {
        return x < -radius - GameConfig.OFFSCREEN_MARGIN
                || x > worldWidth + radius + GameConfig.OFFSCREEN_MARGIN;
    }

    /** Tests whether the blade segment (x0,y0)-(x1,y1) touches this object's circular hitbox. */
    public boolean intersectsSegment(double x0, double y0, double x1, double y1, double tolerance) {
        return segmentHitsCircle(x0, y0, x1, y1, x, y, radius + tolerance);
    }

    /**
     * Geometry helper: shortest distance from the circle centre to the segment compared with the
     * radius. A zero-length segment degenerates into a simple point-in-circle test (a click).
     */
    public static boolean segmentHitsCircle(double x0, double y0, double x1, double y1,
                                            double cx, double cy, double r) {
        double dx = x1 - x0;
        double dy = y1 - y0;
        double lengthSquared = dx * dx + dy * dy;
        double t = 0.0;
        if (lengthSquared > 1e-9) {
            t = ((cx - x0) * dx + (cy - y0) * dy) / lengthSquared;
            t = Math.max(0.0, Math.min(1.0, t));
        }
        double px = x0 + t * dx;
        double py = y0 + t * dy;
        double ex = cx - px;
        double ey = cy - py;
        return ex * ex + ey * ey <= r * r;
    }

    /** Creates an independent, active copy with the same position/velocity (used for undo snapshots). */
    public abstract GameObject copy();

    public abstract boolean isBomb();

    // ---- state flags ----
    public boolean isActive() {
        return active;
    }

    public void deactivate() {
        active = false;
    }

    public boolean isSliced() {
        return sliced;
    }

    /** Marks the object as sliced exactly once; it is deactivated so it can never be sliced again. */
    public void markSliced() {
        sliced = true;
        active = false;
    }

    public boolean isImmune(double now) {
        return now < immuneUntil;
    }

    public void setImmuneUntil(double time) {
        this.immuneUntil = time;
    }

    // ---- getters ----
    public double getX() { return x; }
    public double getY() { return y; }
    public double getVx() { return vx; }
    public double getVy() { return vy; }
    public double getGravity() { return gravity; }
    public double getRotation() { return rotation; }
    public double getSpin() { return spin; }
    public double getRadius() { return radius; }
}
