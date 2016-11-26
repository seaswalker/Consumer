package consumer;

import queue.CASQueue;
import queue.SQueue;

/**
 * {@link SubmitableConsumer}骨架实现，基于CAS操作的消费者实现.
 *
 * @author skywalker
 */
public abstract class AbstractCASConsumer<T> extends AbstractQueuedConsumer<T> {

    public AbstractCASConsumer(int queueSize) {
        super(queueSize);
    }

    @Override
    protected SQueue<T> newQueue() {
        return new CASQueue<>(queueSize);
    }

    @Override
    public final boolean submit(T task) {
        return jobQueue.offer(task);
    }

    @Override
    public final void submitSync(T task) {
        boolean result = false;
        do {
            result = jobQueue.offer(task);
        } while (!result);
    }

    @Override
    public final void run() {
        T task = null;
        while (true) {
            task = jobQueue.poll();
            if (task != null) {
                try {
                    consume(task);
                } catch (RuntimeException e) {
                    handleUncheckedException(e);
                }
            }
        }
    }

}
