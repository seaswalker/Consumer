package consumer.queue.cas;

import consumer.queue.SQueue;
import org.jctools.queues.MpscArrayQueue;

/**
 * {@link SQueue}无锁实现，将逻辑委托给{@link org.jctools.queues.MpscArrayQueue}.
 *
 * @author skywalker
 */
public class MpscBasedQueue<T> implements SQueue<T> {

    private final MpscArrayQueue<T> queue;

    public MpscBasedQueue(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("The param capacity must be positive.");
        }
        this.queue = new MpscArrayQueue<T>(capacity);
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
