/**
 * A custom generic FIFO queue implemented as a circular (ring) buffer.
 *
 * <p>The game keeps upcoming {@link SpawnRequest}s here: new waves are enqueued at the rear and
 * the engine dequeues from the front when a launch is due. Because the indices wrap around, no
 * elements are ever shifted, so enqueue and dequeue are both O(1).
 *
 * @param <T> element type (null elements are rejected)
 */
public class CircularQueue<T> {

    private final Object[] data;
    private int head; // index of the front element
    private int size;

    public CircularQueue(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.data = new Object[capacity];
    }

    /** @return true if stored, false if the queue is full or the item is null. */
    public boolean enqueue(T item) {
        if (item == null || size == data.length) {
            return false;
        }
        int rear = (head + size) % data.length;
        data[rear] = item;
        size++;
        return true;
    }

    /** Removes and returns the front element, or null when empty. */
    @SuppressWarnings("unchecked")
    public T dequeue() {
        if (size == 0) {
            return null;
        }
        T item = (T) data[head];
        data[head] = null;
        head = (head + 1) % data.length;
        size--;
        return item;
    }

    /** Returns the front element without removing it, or null when empty. */
    @SuppressWarnings("unchecked")
    public T peek() {
        return size == 0 ? null : (T) data[head];
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public boolean isFull() {
        return size == data.length;
    }

    public int size() {
        return size;
    }

    public int capacity() {
        return data.length;
    }

    public void clear() {
        for (int i = 0; i < data.length; i++) {
            data[i] = null;
        }
        head = 0;
        size = 0;
    }
}
