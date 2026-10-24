/**
 * A custom generic LIFO stack backed by a fixed-capacity array.
 *
 * <p>The game stores one {@link SliceEvent} per slice on this stack so the "Undo Slice"
 * power-up can reverse the most recent slice first. Invalid operations never throw:
 * pop/peek on an empty stack return {@code null} and push on a full stack returns {@code false}.
 *
 * @param <T> element type (null elements are rejected)
 */
public class ArrayStack<T> {

    private final Object[] data;
    private int top; // number of stored elements == index of the next free slot

    public ArrayStack(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.data = new Object[capacity];
    }

    /** @return true if the element was stored, false if the stack is full or the item is null. */
    public boolean push(T item) {
        if (item == null || top == data.length) {
            return false;
        }
        data[top++] = item;
        return true;
    }

    /** Removes and returns the newest element, or null when empty. */
    @SuppressWarnings("unchecked")
    public T pop() {
        if (top == 0) {
            return null;
        }
        T item = (T) data[--top];
        data[top] = null; // allow garbage collection
        return item;
    }

    /** Returns the newest element without removing it, or null when empty. */
    @SuppressWarnings("unchecked")
    public T peek() {
        return top == 0 ? null : (T) data[top - 1];
    }

    public boolean isEmpty() {
        return top == 0;
    }

    public boolean isFull() {
        return top == data.length;
    }

    public int size() {
        return top;
    }

    public int capacity() {
        return data.length;
    }

    public void clear() {
        for (int i = 0; i < top; i++) {
            data[i] = null;
        }
        top = 0;
    }
}
