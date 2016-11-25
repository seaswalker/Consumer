package queue;

import java.util.concurrent.atomic.AtomicLongFieldUpdater;

/**
 * {@link SQueue}实现，基于CAS无锁操作.
 *
 * @author skywalker
 */
public class CASQueue<T> implements SQueue<T> {

    private final T[] array;
    private volatile long readIndex = 0;
    private volatile long writeIndex = 0;
    private final int capacity;
    private final int indexMask;
    private final AtomicLongFieldUpdater<CASQueue> readIndexUpdater = AtomicLongFieldUpdater.newUpdater(CASQueue.class, "readIndex");
    private final AtomicLongFieldUpdater<CASQueue> writeIndexUpdater = AtomicLongFieldUpdater.newUpdater(CASQueue.class, "writeIndex");

    public CASQueue(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("The param capacity must be bigger than 0.");
        }
        if (capacity % 2 > 0) {
            throw new IllegalArgumentException("The param capacity must be power of 2.");
        }
        this.array = (T[]) new Object[capacity];
        this.capacity = capacity;
        this.indexMask = capacity - 1;
    }

    @Override
    public boolean offer(T element) {
        if ((writeIndex - readIndex + 1) == capacity) {
            return false;
        }
        long index = 0;
        do {
            index = writeIndex;
        } while (!writeIndexUpdater.compareAndSet(this, index, index + 1));
        array[(int) (index & indexMask)] = element;
        return false;
    }

    @Override
    public T poll() {
        if (readIndex >= writeIndex) {
            return null;
        }
        long index = 0;
        do {
            index = readIndex;
        } while (!readIndexUpdater.compareAndSet(this, index, index + 1));
        int i = (int) (index & indexMask);
        T result = array[i];
        array[i] = null;
        return result;
    }

    @Override
    public int size() {
        return (int) (writeIndex - readIndex);
    }

    @Override
    public boolean isEmpty() {
        return (readIndex == writeIndex);
    }


}
