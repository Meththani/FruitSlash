/**
 * The glowing blade trail behind the mouse. The recent mouse positions are kept in a custom
 * {@link SinglyLinkedList}: every mouse move appends a point at the tail, and the oldest points are
 * removed from the head when there are too many or they are too old. This keeps the trail bounded.
 */
public class SliceTrail {

    /** One recorded mouse position. */
    public static final class TrailPoint {
        private final double x;
        private final double y;
        private final double time;
        private final int strokeId;

        TrailPoint(double x, double y, double time, int strokeId) {
            this.x = x;
            this.y = y;
            this.time = time;
            this.strokeId = strokeId;
        }

        public double getX() { return x; }
        public double getY() { return y; }
        public double getTime() { return time; }
        /** Points of different strokes are never joined by a line. */
        public int getStrokeId() { return strokeId; }
    }

    private final SinglyLinkedList<TrailPoint> points = new SinglyLinkedList<>();
    private final int maxPoints;
    private final double maxAge;
    private int strokeId;

    public SliceTrail(int maxPoints, double maxAge) {
        if (maxPoints <= 0 || maxAge <= 0) {
            throw new IllegalArgumentException("maxPoints and maxAge must be positive");
        }
        this.maxPoints = maxPoints;
        this.maxAge = maxAge;
    }

    /** Starts a new stroke (called on mouse press) so it is not joined to the previous one. */
    public void beginStroke() {
        strokeId++;
    }

    /** Appends a point and trims the head so the list never exceeds {@code maxPoints}. */
    public void addPoint(double x, double y, double now) {
        points.addLast(new TrailPoint(x, y, now, strokeId));
        while (points.size() > maxPoints) {
            points.removeFirst();
        }
    }

    /** Drops points older than the maximum age. Oldest points are always at the head. */
    public void update(double now) {
        while (!points.isEmpty() && now - points.peekFirst().getTime() > maxAge) {
            points.removeFirst();
        }
    }

    public void clear() {
        points.clear();
    }

    public int size() {
        return points.size();
    }

    public double getMaxAge() {
        return maxAge;
    }

    /** Read-only walk for the renderer. */
    public Iterable<TrailPoint> getPoints() {
        return points;
    }
}
