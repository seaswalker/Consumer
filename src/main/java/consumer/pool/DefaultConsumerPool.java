package consumer.pool;

import consumer.Consumer;
import consumer.cas.AbstractSPCASConsumer;
import consumer.cas.AbstractMPCASConsumer;
import consumer.cas.strategy.RetryStrategy;
import consumer.lifecycle.LifeCycle;
import consumer.lifecycle.StateCheckDelegate;
import consumer.pool.dispatch.DispatchStrategy;
import consumer.pool.dispatch.RoundRobinStrategy;
import consumer.pool.dispatch.SingleProducerStrategy;
import consumer.pool.internal.InternalMPConsumer;
import consumer.pool.internal.InternalSPConsumer;
import org.slf4j.Logger;
import consumer.util.Util;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Function;

/**
 * {@link ConsumerPool}的默认实现.
 *
 * @author skywalker
 */
public class DefaultConsumerPool<T> implements ConsumerPool<T> {

    /**
     * 每个消费者的消费线程数.
     */
    private final int threads;
    private final int queueSize;
    private final ConsumeActionFactory<T> factory;
    private final boolean isSingleProducer;
    private final StateCheckDelegate delegate = StateCheckDelegate.getInstance();
    private final List<Consumer<T>> list;
    /**
     * 消费者总数.
     */
    private final int consumers;
    private final Logger log = Util.getLogger(this.getClass());

    private RetryStrategy<T> retryStrategy;
    private ThreadNameGenerator threadNameGenerator;
    private Thread.UncaughtExceptionHandler handler;
    private DispatchStrategy<T> dispatchStrategy;

    private volatile State state = State.INIT;

    /**
     * 用于等待消费者终结的线程的名称.
     */
    private static final String defaultTerminateThreadName = "DefaultConsumerPool-terminate-thread";

    public DefaultConsumerPool(boolean isSingleProducer, int number, int consumerThreads, int queueSize, ConsumeActionFactory<T> factory) {
        Objects.requireNonNull(factory);
        this.factory = factory;
        this.consumers = number;
        this.queueSize = queueSize;
        this.threads = consumerThreads;
        this.isSingleProducer = isSingleProducer;
        this.list = new ArrayList<>(consumers);
    }

    @Override
    public boolean start() {
        delegate.checkStart(this);
        if (isSingleProducer) {
            //单生产者模式，我们需要number - 1个InternalSPConsumer和1个InternalMPConsumer
            for (int i = 0; i < consumers - 1; i++) {
                InternalSPConsumer<T> spConsumer = new InternalSPConsumer<>(queueSize, threads, factory, threadNameGenerator);
                prepareConsumer(spConsumer);
                if (!spConsumer.start()) {
                    return false;
                }
                list.add(spConsumer);
            }
            InternalMPConsumer<T> mpConsumer = new InternalMPConsumer<>(queueSize, threads, factory, threadNameGenerator);
            prepareConsumer(mpConsumer);
            if (!mpConsumer.start()) {
                return false;
            }
            list.add(mpConsumer);
            dispatchStrategy = new SingleProducerStrategy<>(list);
        } else {
            for (int i = 0; i < consumers; i++) {
                AbstractMPCASConsumer<T> mpConsumer = new InternalMPConsumer<>(queueSize, threads, factory, threadNameGenerator);
                prepareConsumer(mpConsumer);
                if (!mpConsumer.start()) {
                    return false;
                }
                list.add(mpConsumer);
            }
            dispatchStrategy = new RoundRobinStrategy<>(list);
        }
        this.state = State.RUNNING;
        return true;
    }

    /**
     * 准备{@link AbstractSPCASConsumer}，比如调用其{@linkplain AbstractSPCASConsumer
     * #setUncaughtExceptionHandler(Thread.UncaughtExceptionHandler)}方法.
     */
    private void prepareConsumer(AbstractSPCASConsumer<T> consumer) {
        if (retryStrategy != null) {
            consumer.setRetryStrategy(retryStrategy.copy());
        }
        if (handler != null) {
            consumer.setUncaughtExceptionHandler(handler);
        }
    }

    @Override
    public Future<Void> terminate() {
        delegate.checkTerminated(this);
        return terminateHelper(LifeCycle::terminate);
    }

    @Override
    public Future<Void> terminateNow() {
        delegate.checkTerminated(this);
        return terminateHelper(LifeCycle::terminateNow);
    }

    /**
     * terminate()和terminateNow()辅助方法.
     *
     * @param function {@link Function}
     */
    private Future<Void> terminateHelper(Function<Consumer<T>, Future<Void>> function) {
        delegate.checkTerminated(this);
        final Future[] futures = new Future[this.consumers];
        for (int i = 0; i < this.consumers; i++) {
            futures[i] = function.apply(list.get(i));
        }
        final CompletableFuture<Void> future = new CompletableFuture<>();
        new Thread(() -> {
            for (int i = 0; i < this.consumers; i++) {
                try {
                    futures[i].get();
                } catch (InterruptedException e) {
                    if (log != null) {
                        log.error("{} was interrupted when waiting for consumer termination, and we will ignore it " +
                                "continues to wait for the next.", defaultTerminateThreadName, e);
                    }
                } catch (ExecutionException e) {
                    if (log != null) {
                        log.error("When the consumer termination error occurred, and we will ignore it " +
                                "continues to wait for the next.", defaultTerminateThreadName, e);
                    }
                }
            }
            future.complete(null);
        }, defaultTerminateThreadName).start();
        return future;
    }

    @Override
    public State getState() {
        return state;
    }

    @Override
    public Consumer<T> acquire() {
        delegate.checkRunning(this);
        return dispatchStrategy.acquire();
    }

    @Override
    public void release(Consumer<T> consumer) {
        dispatchStrategy.release(consumer);
    }

    @SuppressWarnings("unused")
    public RetryStrategy<T> getRetryStrategy() {
        return retryStrategy;
    }

    public void setRetryStrategy(RetryStrategy<T> retryStrategy) {
        if (state != State.INIT) {
            throw new IllegalStateException("Can't set RetryStrategy when the state is " + state + ".");
        }
        Objects.requireNonNull(retryStrategy);
        this.retryStrategy = retryStrategy;
    }

    @SuppressWarnings("unused")
    public void setThreadNameGenerator(ThreadNameGenerator threadNameGenerator) {
        if (state != State.INIT) {
            throw new IllegalStateException("Can't set ThreadNameGenerator when the state is " + state + ".");
        }
        Objects.requireNonNull(threadNameGenerator);
        this.threadNameGenerator = threadNameGenerator;
    }

    @SuppressWarnings("unused")
    public Thread.UncaughtExceptionHandler getHandler() {
        return handler;
    }

    @SuppressWarnings("unused")
    public void setHandler(Thread.UncaughtExceptionHandler handler) {
        if (state != State.INIT) {
            throw new IllegalStateException("Can't set UncaughtExceptionHandler when the state is " + state + ".");
        }
        Objects.requireNonNull(handler);
        this.handler = handler;
    }

    @SuppressWarnings("unused")
    public ThreadNameGenerator getThreadNameGenerator() {
        return threadNameGenerator;
    }

}