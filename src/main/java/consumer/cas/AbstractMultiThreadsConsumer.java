package consumer.cas;

import consumer.MultiThreadsConsumer;
import queue.SQueue;
import queue.cas.MpmcBasedQueue;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * {@link MultiThreadsConsumer}实现，暂且只提供给予CAS的多线程消费者实现.
 *
 * @author skywalker
 */
public abstract class AbstractMultiThreadsConsumer<T> extends AbstractCASConsumer<T> implements MultiThreadsConsumer<T> {

    /**
     * 消费线程的数量.
     */
    private final int threads;

    public AbstractMultiThreadsConsumer(int queueSize, int threads) {
        super(queueSize);
        this.threads = threads;
    }

    @Override
    protected final SQueue<T> newQueue() {
        return new MpmcBasedQueue<T>(queueSize);
    }

    @Override
    protected ExecutorService startExecutor(ThreadFactory threadFactory) {
        ExecutorService service = Executors.newFixedThreadPool(threads, threadFactory);
        int index = 0;
        while (index++ < threads) {
            service.execute(this);
        }
        return service;
    }



}
