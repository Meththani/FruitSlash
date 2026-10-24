import java.awt.Color;

/** The kinds of fruit that can be thrown, with their hitbox size, score value and colours. */
public enum FruitType {
    APPLE("Apple", 34, 10, new Color(0xE53935), new Color(0xFFF1B8), new Color(0xFF5252)),
    ORANGE("Orange", 36, 10, new Color(0xFB8C00), new Color(0xFFC46B), new Color(0xFFA726)),
    WATERMELON("Watermelon", 46, 20, new Color(0x2E9B3A), new Color(0xFF4D6D), new Color(0xFF4D6D)),
    BANANA("Banana", 38, 15, new Color(0xFBC02D), new Color(0xFFF2A8), new Color(0xFFEE58)),
    STRAWBERRY("Strawberry", 30, 15, new Color(0xD50032), new Color(0xFF8FA3), new Color(0xFF3D5A)),
    LEMON("Lemon", 33, 10, new Color(0xFFEB3B), new Color(0xFFF9C4), new Color(0xFFF176)),
    GRAPES("Grapes", 36, 15, new Color(0x7B1FA2), new Color(0xD7A6E6), new Color(0xBA68C8));

    private final String displayName;
    private final int radius;
    private final int points;
    private final Color rind;
    private final Color flesh;
    private final Color juice;

    FruitType(String displayName, int radius, int points, Color rind, Color flesh, Color juice) {
        this.displayName = displayName;
        this.radius = radius;
        this.points = points;
        this.rind = rind;
        this.flesh = flesh;
        this.juice = juice;
    }

    public String getDisplayName() { return displayName; }
    public int getRadius() { return radius; }
    public int getPoints() { return points; }
    public Color getRind() { return rind; }
    public Color getFlesh() { return flesh; }
    public Color getJuice() { return juice; }
}
