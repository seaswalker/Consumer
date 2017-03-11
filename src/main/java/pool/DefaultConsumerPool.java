package pool;

import consumer.Consumer;
import consumer.MultiThreadsConsumer;
import lifecycle.LifeCycle;

import java.util.concurrent.Future;

/**
 * {@link ConsumerPool}的默认实现.
 *
 * @author skywalker
 */
public class DefaultConsumerPool<T> implements ConsumerPool<T> {

    /**
     * SPSC消费者数量.
     */
    private final int spsc;
    /**
     * MPMC消费者线程数.
     */
    private final int mpmcThreads;
    private final ConsumeAction action;

    public DefaultConsumerPool(int spsc, int mpmcThreads, ConsumeAction<T> action) {
        this.spsc = spsc;
        this.mpmcThreads = mpmcThreads;
        this.action = action;
    }

    @Override
    public boolean start() {
        return false;
    }

    @Override
    public <T> Future<T> terminate() {
        return null;
    }

    @Override
    public <T> Future<T> terminateNow() {
        return null;
    }

    @Override
    public State getState() {
        return null;
    }

    @Override
    public Consumer<T> acquire() {
        return null;
    }

    @Override
    public Consumer<T> acquireSPSC() {
        return null;
    }

    @Override
    public MultiThreadsConsumer<T> acquireMPMC() {
        return null;
    }

    @Override
    public void release(Consumer<T> consumer) {

    }

}
