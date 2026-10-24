/** A sliceable fruit. */
public class Fruit extends GameObject {

    private final FruitType type;

    public Fruit(FruitType type, double x, double y, double vx, double vy,
                 double gravity, double spin) {
        super(x, y, vx, vy, gravity, spin, type.getRadius());
        this.type = type;
    }

    public FruitType getType() {
        return type;
    }

    public int getBasePoints() {
        return type.getPoints();
    }

    @Override
    public boolean isBomb() {
        return false;
    }

    @Override
    public GameObject copy() {
        Fruit copy = new Fruit(type, x, y, vx, vy, gravity, spin);
        copy.rotation = rotation;
        return copy;
    }
}
