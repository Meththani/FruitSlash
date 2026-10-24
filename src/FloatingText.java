import java.awt.Color;

/** A text label (score gain, combo, warning) that drifts upwards and fades out. */
public class FloatingText {

    private final String text;
    private double x;
    private double y;
    private final double vy;
    private final int size;
    private final Color color;
    private final double maxLife;
    private double life;

    public FloatingText(String text, double x, double y, int size, Color color, double life) {
        this.text = text;
        this.x = x;
        this.y = y;
        this.vy = -60;
        this.size = size;
        this.color = color;
        this.maxLife = Math.max(0.01, life);
        this.life = this.maxLife;
    }

    /** @return false when expired. */
    public boolean update(double dt) {
        life -= dt;
        y += vy * dt;
        return life > 0;
    }

    public double getAlpha() { return Math.max(0.0, Math.min(1.0, life / (maxLife * 0.5))); }
    /** Pops in: starts bigger and settles to the normal size. */
    public double getScale() {
        double age = 1.0 - life / maxLife;
        return 1.0 + Math.max(0.0, 0.35 - age * 2.0);
    }
    public String getText() { return text; }
    public double getX() { return x; }
    public double getY() { return y; }
    public int getSize() { return size; }
    public Color getColor() { return color; }
}
