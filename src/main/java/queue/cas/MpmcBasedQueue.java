package queue.cas;

import org.jctools.queues.MpmcArrayQueue;
import queue.SQueue;

/**
 * {@link SQueue}无锁实现，将逻辑委托给{@link org.jctools.queues.MpmcArrayQueue}.
 *
 * @author skywalker
 */
public class MpmcBasedQueue<T> implements SQueue<T> {

    private final MpmcArrayQueue<T> queue;

    public MpmcBasedQueue(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("The param capacity must be positive.");
        }
        this.queue = new MpmcArrayQueue<T>(capacity);
    }

    @Override
    public boolean offer(T element) {
        return queue.offer(element);
    }

    @Override
    public T poll() {
        return queue.poll();
    }

    @Override
    public int size() {
        return queue.size();
    }

    @Override
    public boolean isEmpty() {
        return queue.isEmpty();
    }

}
