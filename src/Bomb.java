/** A bomb: slicing it costs a life and some points. Ignoring it is always safe. */
public class Bomb extends GameObject {

    public static final double RADIUS = 34.0;

    public Bomb(double x, double y, double vx, double vy, double gravity, double spin) {
        super(x, y, vx, vy, gravity, spin, RADIUS);
    }

    @Override
    public boolean isBomb() {
        return true;
    }

    @Override
    public GameObject copy() {
        Bomb copy = new Bomb(x, y, vx, vy, gravity, spin);
        copy.rotation = rotation;
        return copy;
    }
}
