package consumer.cas;

import consumer.AbstractQueuedConsumer;
import consumer.cas.strategy.BlockStrategy;
import consumer.cas.strategy.RetryStrategy;
import consumer.queue.SQueue;
import consumer.queue.cas.SpscBasedQueue;

/**
 * {@link AbstractQueuedConsumer}骨架实现，基于CAS操作的消费者实现.
 * <br>
 * 默认采用阻塞的等待策略.
 *
 * @author skywalker
 */
public abstract class AbstractCASConsumer<T> extends AbstractQueuedConsumer<T> {

    private RetryStrategy<T> retryStrategy = new BlockStrategy<T>();

    public AbstractCASConsumer(int queueSize) {
        super(queueSize);
    }

    @Override
    protected SQueue<T> newQueue() {
        return new SpscBasedQueue<T>(queueSize);
    }

    @Override
    public final boolean submit(T task) {
        return retryStrategy.submit(jobQueue, task);
    }

    @Override
    public final void submitSync(T task) {
        while (!submit(task)) ;
    }

    @Override
    protected final T getTask() throws InterruptedException {
        return retryStrategy.retry(jobQueue);
    }

    @Override
    protected void doTerminate() {
        retryStrategy.release();
    }

    @Override
    protected void doTerminateNow() {
        retryStrategy.release();
    }

    public void setRetryStrategy(RetryStrategy<T> retryStrategy) {
        this.retryStrategy = retryStrategy;
    }

}
