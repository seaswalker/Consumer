package consumer.cas;

import consumer.AbstractQueuedConsumer;
import consumer.cas.strategy.BlockStrategy;
import consumer.cas.strategy.RetryStrategy;
import consumer.queue.SQueue;
import consumer.queue.cas.SpmcBasedQueue;
import consumer.queue.cas.SpscBasedQueue;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * {@link AbstractQueuedConsumer}骨架实现，基于无锁队列实现，子类可指定消费者线程数，
 * 如果有一个消费线程，那么使用{@link org.jctools.queues.SpscArrayQueue}，否则使用
 * {@link org.jctools.queues.SpmcArrayQueue}.
 * <br>
 * 默认采用阻塞的等待策略.
 *
 * @author skywalker
 */
public abstract class AbstractSPCASConsumer<T> extends AbstractQueuedConsumer<T> {

    private RetryStrategy<T> retryStrategy = new BlockStrategy<T>();

    /**
     * 消费线程的数量.
     */
    protected final int threads;

    public AbstractSPCASConsumer(int queueSize, int threads) {
        super(queueSize);
        this.threads = threads;
    }

    @Override
    protected SQueue<T> newQueue() {
        return (threads > 1 ? new SpmcBasedQueue<T>(queueSize) : new SpscBasedQueue<T>(queueSize));
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
        Objects.requireNonNull(retryStrategy);
        delegate.checkStart(this);
        this.retryStrategy = retryStrategy;
    }

    @Override
    protected final ExecutorService startExecutor(ThreadFactory threadFactory) {
        ExecutorService service = Executors.newFixedThreadPool(threads, threadFactory);
        int index = 0;
        while (index++ < threads) {
            service.execute(this);
        }
        return service;
    }

}
