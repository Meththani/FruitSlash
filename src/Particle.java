import java.awt.Color;

/** A short-lived visual effect: juice droplet, spark, smoke puff or splat. Purely cosmetic. */
public class Particle {

    private double x;
    private double y;
    private double vx;
    private double vy;
    private final double gravity;
    private final double drag;
    private final double startSize;
    private final double endSize;
    private final double maxLife;
    private double life;
    private final Color color;
    private final boolean glow;

    public Particle(double x, double y, double vx, double vy, double gravity, double drag,
                    double startSize, double endSize, double life, Color color, boolean glow) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.gravity = gravity;
        this.drag = drag;
        this.startSize = startSize;
        this.endSize = endSize;
        this.maxLife = Math.max(0.01, life);
        this.life = this.maxLife;
        this.color = color;
        this.glow = glow;
    }

    /** @return false when the particle has expired and must be removed. */
    public boolean update(double dt) {
        life -= dt;
        if (life <= 0) {
            return false;
        }
        double damping = Math.max(0.0, 1.0 - drag * dt);
        vx *= damping;
        vy = vy * damping + gravity * dt;
        x += vx * dt;
        y += vy * dt;
        return true;
    }

    /** 1.0 when new, 0.0 when expired. */
    public double getLifeFraction() {
        return Math.max(0.0, Math.min(1.0, life / maxLife));
    }

    public double getSize() {
        double t = 1.0 - getLifeFraction();
        return startSize + (endSize - startSize) * t;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public Color getColor() { return color; }
    public boolean isGlow() { return glow; }
}
