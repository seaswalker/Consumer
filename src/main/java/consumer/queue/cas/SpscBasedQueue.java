package consumer.queue.cas;

import org.jctools.queues.SpscArrayQueue;
import consumer.queue.SQueue;

/**
 * {@link consumer.queue.SQueue}无锁实现，将逻辑委托给{@link org.jctools.queues.SpscArrayQueue}.
 *
 * @author skywalker
 */
public class SpscBasedQueue<T> implements SQueue<T> {

    private final SpscArrayQueue<T> queue;

    public SpscBasedQueue(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("The param capacity must be positive.");
        }
        this.queue = new SpscArrayQueue<T>(capacity);
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
