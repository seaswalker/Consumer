package consumer.cas;

import consumer.MultiThreadsConsumer;
import consumer.queue.SQueue;
import consumer.queue.cas.MpmcBasedQueue;
import consumer.queue.cas.MpscBasedQueue;

/**
 * {@link MultiThreadsConsumer}实现，暂且只提供给予CAS的多线程消费者实现.
 *
 * @author skywalker
 */
public abstract class AbstractMPCASConsumer<T> extends AbstractSPCASConsumer<T> implements MultiThreadsConsumer<T> {

    public AbstractMPCASConsumer(int queueSize, int threads) {
        super(queueSize, threads);
    }

    @Override
    protected final SQueue<T> newQueue() {
        return (threads > 1 ? new MpmcBasedQueue<T>(queueSize) : new MpscBasedQueue<T>(queueSize));
    }

}
