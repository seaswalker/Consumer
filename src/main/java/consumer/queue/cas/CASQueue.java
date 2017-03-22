package consumer.queue.cas;

import consumer.queue.SQueue;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

import static consumer.util.UnsafeAccess.UNSAFE;

/**
 * {@link SQueue}实现，基于CAS无锁操作.用于测试目的.
 *
 * @author skywalker
 */
class CASQueue<T> implements SQueue<T> {

    private final T[] array;
    private final long[] pad1 = new long[8];
    private volatile long readIndex = 0;
    private final long[] pad2 = new long[8];
    private volatile long writeIndex = 0;
    private final long[] pad3 = new long[8];
    private final int capacity;
    private final long mask;
    private final int arrayBaseOffset;
    private final int arrayPointerOffset;
    private final AtomicLongFieldUpdater<CASQueue> readIndexUpdater =
            AtomicLongFieldUpdater.newUpdater(CASQueue.class, "readIndex");
    private final AtomicLongFieldUpdater<CASQueue> writeIndexUpdater =
            AtomicLongFieldUpdater.newUpdater(CASQueue.class, "writeIndex");

    public CASQueue(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("The param capacity must be bigger than 0.");
        }
        if (!isPowerOfTwo(capacity)) {
            throw new IllegalArgumentException("The param capacity must be power of 2.");
        }
        this.array = (T[]) new Object[capacity];
        this.capacity = capacity;
        this.mask = capacity - 1;
        this.arrayBaseOffset = UNSAFE.arrayBaseOffset(Object[].class);
        int scale = UNSAFE.arrayIndexScale(Object[].class);
        if (scale == 4) {
            this.arrayPointerOffset = 2;
        } else if (scale == 8) {
            this.arrayPointerOffset = 3;
        } else {
            throw new IllegalStateException("Unkown pointer size: " + scale);
        }
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
        long offset = calOffset(index);
        UNSAFE.putOrderedObject(array, offset, element);
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
        long offset = calOffset(index);
        T result = (T) UNSAFE.getObjectVolatile(array, offset);
        if (result == null) {
            //生产者尚未完成element保存
            do {
                result = (T) UNSAFE.getObjectVolatile(array, offset);
            } while (result == null);
        }
        return result;
    }

    /**
     * 计算给定的索引在数组中的偏移.
     */
    private long calOffset(long index) {
        return (arrayBaseOffset + ((index & mask) << arrayPointerOffset));
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
