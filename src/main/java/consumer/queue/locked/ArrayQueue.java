package consumer.queue.locked;

import consumer.queue.SQueue;

/**
 * 用数组实现的队列，非线程安全.
 *
 * @author skywalker
 */
public final class ArrayQueue<T> implements SQueue<T> {

    private final T[] array;
    private int readIndex;
    private int writeIndex;
    private final int capacity;

    public ArrayQueue(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("The param capacity must be positive.");
        }
        this.readIndex = this.writeIndex = 0;
        this.capacity = capacity;
        this.array = (T[]) new Object[capacity];
    }

    @Override
    public boolean offer(T element) {
        if (writeIndex == readIndex && array[readIndex] != null) {
            return false;
        }
        array[writeIndex++] = element;
        writeIndex = (writeIndex == capacity ? 0 : writeIndex);
        return true;
    }

    @Override
    public T poll() {
        if (isEmpty()) {
            return null;
        }
        T result = array[readIndex];
        array[readIndex] = null;
        ++readIndex;
        readIndex = (readIndex == capacity ? 0 : readIndex);
        return result;
    }

    @Override
    public int size() {
        return capacity;
    }

    @Override
    public boolean isEmpty() {
        return readIndex == writeIndex && array[readIndex] == null;
    }

}