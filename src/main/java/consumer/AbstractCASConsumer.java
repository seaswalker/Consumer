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
        checkSubmit();
        return jobQueue.offer(task);
    }

    @Override
    public final void submitSync(T task) {
        checkSubmit();
        boolean result = false;
        do {
            result = jobQueue.offer(task);
        } while (!result);
    }

    @Override
    protected final T getTask() {
        T task = jobQueue.poll();
        return task;
    }

}
