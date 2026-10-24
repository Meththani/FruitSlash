/**
 * Fixed-capacity array that stores the active fruits and bombs.
 *
 * <p>Indexing: valid indices are {@code 0 .. size()-1}; the slots from {@code size()} up to
 * {@code capacity()-1} are always {@code null}. Limitation: the capacity is fixed, so
 * {@link #add(GameObject)} returns {@code false} when the array is full (the engine then simply skips
 * that spawn). Objects are never removed in the middle of an update loop; instead they are
 * deactivated and {@link #compactInactive()} removes them afterwards in one stable pass.
 */
public class ObjectArray {

    private final GameObject[] items;
    private int count;

    public ObjectArray(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.items = new GameObject[capacity];
    }

    /** @return true if stored, false when the array is full or the object is null. */
    public boolean add(GameObject object) {
        if (object == null || count >= items.length) {
            return false;
        }
        items[count++] = object;
        return true;
    }

    /** @throws IndexOutOfBoundsException if the index is outside {@code 0 .. size()-1}. */
    public GameObject get(int index) {
        if (index < 0 || index >= count) {
            throw new IndexOutOfBoundsException("index " + index + ", size " + count);
        }
        return items[index];
    }

    /**
     * Removes the element at {@code index} by shifting later elements left (keeps drawing order).
     *
     * @return true if an element was removed, false for an invalid index
     */
    public boolean removeAt(int index) {
        if (index < 0 || index >= count) {
            return false;
        }
        for (int i = index; i < count - 1; i++) {
            items[i] = items[i + 1];
        }
        items[--count] = null;
        return true;
    }

    /**
     * Stable compaction: keeps active objects in their original order and frees the rest.
     * Uses a read index and a write index, so nothing is skipped and nothing is visited twice.
     *
     * @return how many objects were removed
     */
    public int compactInactive() {
        int write = 0;
        for (int read = 0; read < count; read++) {
            GameObject object = items[read];
            if (object != null && object.isActive()) {
                items[write++] = object;
            }
        }
        for (int i = write; i < count; i++) {
            items[i] = null;
        }
        int removed = count - write;
        count = write;
        return removed;
    }

    public int size() {
        return count;
    }

    public int capacity() {
        return items.length;
    }

    public boolean isFull() {
        return count >= items.length;
    }

    public void clear() {
        for (int i = 0; i < count; i++) {
            items[i] = null;
        }
        count = 0;
    }
}
