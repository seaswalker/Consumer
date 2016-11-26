package queue;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

/**
 * {@link SQueue}实现，基于CAS无锁操作.
 *
 * @author skywalker
 */
public class CASQueue<T> implements SQueue<T> {

    private T[] array;
    private long[] pad1 = new long[8];
    private volatile long readIndex = 0;
    private long[] pad2 = new long[8];
    private volatile long writeIndex = 0;
    private long[] pad3 = new long[8];
    private final int capacity;
    private final int indexMask;
    private final AtomicLongFieldUpdater<CASQueue> readIndexUpdater = AtomicLongFieldUpdater.newUpdater(CASQueue.class, "readIndex");
    private final AtomicLongFieldUpdater<CASQueue> writeIndexUpdater = AtomicLongFieldUpdater.newUpdater(CASQueue.class, "writeIndex");

    public CASQueue(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("The param capacity must be bigger than 0.");
        }
        if (!isPowerOfTwo(capacity)) {
            throw new IllegalArgumentException("The param capacity must be power of 2.");
        }
        this.array = (T[]) new Object[capacity];
        this.capacity = capacity;
        this.indexMask = capacity - 1;
    }

    /**
     * 判断给定的数字是否是2的整次幂.
     */
    private boolean isPowerOfTwo(int n) {
        return ((n & (n - 1)) == 0);
    }

    @Override
    public boolean offer(T element) {
        long index = 0;
        do {
            index = writeIndex;
            if ((index - readIndex) == capacity) {
                return false;
            }
        } while (!writeIndexUpdater.compareAndSet(this, index, index + 1));
        array[(int) (index & indexMask)] = element;
        return true;
    }

    @Override
    public T poll() {
        long index = 0;
        do {
            index = readIndex;
            if (index >= writeIndex) {
                return null;
            }
        } while (!readIndexUpdater.compareAndSet(this, index, index + 1));
        int i = (int) (index & indexMask);
        T result = array[i];
        //不能置为null，否则会因为指令重排导致返回null
        //array[i] = null;
        return result;
    }

    @Override
    public int size() {
        return (int) (writeIndex - readIndex);
    }

    @Override
    public boolean isEmpty() {
        return (readIndex >= writeIndex);
    }

    @Override
    public String toString() {
        return "CASQueue{" +
                "array=" + Arrays.toString(array) +
                ", readIndex=" + readIndex +
                ", writeIndex=" + writeIndex +
                ", capacity=" + capacity +
                '}';
    }

}
