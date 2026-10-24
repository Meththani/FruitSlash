import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * A custom generic singly linked list with a tail pointer.
 *
 * <p>Used by {@link SliceTrail} to hold the most recent mouse positions: new points are appended
 * with {@link #addLast(Object)} (O(1) thanks to the tail pointer) and the oldest points are
 * dropped with {@link #removeFirst()} (O(1)). Java's own LinkedList is deliberately not used.
 *
 * @param <T> element type (null elements are not allowed)
 */
public class SinglyLinkedList<T> implements Iterable<T> {

    private static final class Node<T> {
        final T value;
        Node<T> next;

        Node(T value) {
            this.value = value;
        }
    }

    private Node<T> head;
    private Node<T> tail;
    private int size;

    /** Appends an element at the end of the list in O(1). */
    public void addLast(T value) {
        Objects.requireNonNull(value, "null elements are not supported");
        Node<T> node = new Node<>(value);
        if (tail == null) {
            head = node;
            tail = node;
        } else {
            tail.next = node;
            tail = node;
        }
        size++;
    }

    /**
     * Removes and returns the first element.
     *
     * @return the removed element, or {@code null} when the list is empty (never throws)
     */
    public T removeFirst() {
        if (head == null) {
            return null;
        }
        T value = head.value;
        head = head.next;
        if (head == null) {
            tail = null; // list became empty: keep head and tail consistent
        }
        size--;
        return value;
    }

    /** @return the first element without removing it, or null if empty. */
    public T peekFirst() {
        return head == null ? null : head.value;
    }

    /** @return the last element without removing it, or null if empty. */
    public T peekLast() {
        return tail == null ? null : tail.value;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    /** Removes every element. */
    public void clear() {
        head = null;
        tail = null;
        size = 0;
    }

    /** Walks from head to tail; the list must not be modified while iterating. */
    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private Node<T> current = head;

            @Override
            public boolean hasNext() {
                return current != null;
            }

            @Override
            public T next() {
                if (current == null) {
                    throw new NoSuchElementException();
                }
                T value = current.value;
                current = current.next;
                return value;
            }
        };
    }
}
