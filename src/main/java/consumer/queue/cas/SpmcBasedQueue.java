package consumer.queue.cas;

import consumer.queue.SQueue;
import org.jctools.queues.SpmcArrayQueue;

/**
 * {@link SQueue}无锁实现，将逻辑委托给{@link SpmcBasedQueue}.
 *
 * @author skywalker
 */
public class SpmcBasedQueue<T> implements SQueue<T> {

    private final SpmcArrayQueue<T> queue;

    public SpmcBasedQueue(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("The param capacity must be positive.");
        }
        this.queue = new SpmcArrayQueue<T>(capacity);
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
