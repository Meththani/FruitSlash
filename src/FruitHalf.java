/** One half of a sliced fruit flying apart and falling off screen. */
public class FruitHalf {

    private final FruitType type;
    private final double radius;
    private final int side; // -1 = left half, +1 = right half (in the half's own rotated frame)
    private double x;
    private double y;
    private double vx;
    private double vy;
    private double rotation;
    private final double spin;
    private double life = 1.8;
    private static final double GRAVITY = 1100.0;

    public FruitHalf(FruitType type, double radius, int side, double x, double y,
                     double vx, double vy, double rotation, double spin) {
        this.type = type;
        this.radius = radius;
        this.side = side;
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.rotation = rotation;
        this.spin = spin;
    }

    /** @return false when finished (timed out or fell below the screen). */
    public boolean update(double dt) {
        life -= dt;
        vy += GRAVITY * dt;
        x += vx * dt;
        y += vy * dt;
        rotation += spin * dt;
        return life > 0 && y < GameConfig.WORLD_HEIGHT + 150;
    }

    public FruitType getType() { return type; }
    public double getRadius() { return radius; }
    public int getSide() { return side; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getRotation() { return rotation; }
    public double getAlpha() { return Math.max(0.0, Math.min(1.0, life / 0.5)); }
}
