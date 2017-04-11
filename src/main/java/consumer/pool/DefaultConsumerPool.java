package consumer.pool;

import consumer.Consumer;
import consumer.MultiThreadsConsumer;
import consumer.cas.AbstractCASConsumer;
import consumer.cas.AbstractMultiThreadsConsumer;
import consumer.cas.strategy.RetryStrategy;
import consumer.lifecycle.StateCheckDelegate;
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
     * SPSC消费者数量.
     */
    private final int spsc;
    /**
     * MPMC消费者线程数.
     */
    private final int mpmcThreads;
    private final int spscQueueSize;
    private final int mpmcQueueSize;
    private final ConsumeActionFactory<T> factory;
    /**
     * 消费者列表，采取如下的存储策略:
     * <p>最后一个消费者为MPMC，前面的全部都是SPSC.</p>
     */
    private final List<ConsumerWrapper> list;
    private final StateCheckDelegate delegate = StateCheckDelegate.getInstance();
    private final Object monitor = new Object();
    /**
     * 消费者总数.
     */
    private final int consumers;
    private final Logger log = Util.getLogger(this.getClass());

    private RetryStrategy<T> retryStrategy;
    private ThreadNameGenerator threadNameGenerator;
    private Thread.UncaughtExceptionHandler handler;

    private volatile State state = State.INIT;

    /**
     * 用于等待消费者终结的线程的名称.
     */
    private static final String defaultTerminateThreadName = "DefaultConsumerPool-terminate-thread";

    public DefaultConsumerPool(int spsc, int mpmcThreads, int spscQueueSize, int mpmcQueueSize, ConsumeActionFactory<T> factory) {
        this.spsc = spsc;
        this.mpmcThreads = mpmcThreads;
        this.factory = factory;
        this.spscQueueSize = spscQueueSize;
        this.mpmcQueueSize = mpmcQueueSize;
        this.consumers = spsc + 1;
        this.list = new ArrayList<>(this.consumers);
    }

    @Override
    public boolean start() {
        delegate.checkStart(this);
        for (int i = 0; i < consumers - 1; i++) {
            InternalSPSCConsumer spsc = new InternalSPSCConsumer(spscQueueSize);
            prepareConsumer(spsc);
            if (!spsc.start()) {
                return false;
            }
            ConsumerWrapper wrapper = new ConsumerWrapper(spsc);
            spsc.wrapper = wrapper;
            list.add(wrapper);
        }
        AbstractMultiThreadsConsumer<T> mpmc = new InternalMPMCConsumer(mpmcQueueSize, mpmcThreads);
        prepareConsumer(mpmc);
        if (!mpmc.start()) {
            return false;
        }
        list.add(new ConsumerWrapper(mpmc));
        this.state = State.RUNNING;
        return true;
    }

    /**
     * 准备{@link AbstractCASConsumer}，比如调用其{@linkplain AbstractCASConsumer
     * #setUncaughtExceptionHandler(Thread.UncaughtExceptionHandler)}方法.
     */
    private void prepareConsumer(AbstractCASConsumer<T> consumer) {
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
        return terminateHelper(t -> t.terminate());
    }

    @Override
    public Future<Void> terminateNow() {
        delegate.checkTerminated(this);
        return terminateHelper(t -> t.terminateNow());
    }

    /**
     * terminate()和terminateNow()辅助方法.
     *
     * @param function {@link Function}
     */
    private Future<Void> terminateHelper(Function<Consumer<T>, Future<Void>> function) {
        delegate.checkTerminated(this);
        final Future<Void>[] futures = new Future[this.consumers];
        for (int i = 0; i < this.consumers; i++) {
            futures[i] = function.apply(list.get(i).consumer);
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
        Consumer result = acquireSPSC();
        if (result == null) {
            if (log != null && log.isDebugEnabled()) {
                log.debug("Currently there are no spsc consumer available, so use mpmc instead.");
            }
            result = acquireMPMC();
        }
        return result;
    }

    @Override
    public Consumer<T> acquireSPSC() {
        delegate.checkRunning(this);
        Consumer result = null;
        synchronized (monitor) {
            for (int i = 0; i < consumers - 1; i++) {
                ConsumerWrapper wrapper = list.get(i);
                if (wrapper.available) {
                    result = wrapper.consumer;
                    wrapper.available = false;
                    break;
                }
            }
        }
        return result;
    }

    @Override
    public MultiThreadsConsumer<T> acquireMPMC() {
        delegate.checkRunning(this);
        return (MultiThreadsConsumer<T>) list.get(consumers - 1).consumer;
    }

    @Override
    public void release(Consumer<T> consumer) {
        if (consumer instanceof MultiThreadsConsumer) {
            //无需释放
        } else {
            synchronized (monitor) {
                InternalSPSCConsumer internal = (InternalSPSCConsumer) consumer;
                internal.wrapper.available = true;
            }
        }
    }

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

    public void setThreadNameGenerator(ThreadNameGenerator threadNameGenerator) {
        if (state != State.INIT) {
            throw new IllegalStateException("Can't set ThreadNameGenerator when the state is " + state + ".");
        }
        Objects.requireNonNull(threadNameGenerator);
        this.threadNameGenerator = threadNameGenerator;
    }

    public Thread.UncaughtExceptionHandler getHandler() {
        return handler;
    }

    public void setHandler(Thread.UncaughtExceptionHandler handler) {
        if (state != State.INIT) {
            throw new IllegalStateException("Can't set UncaughtExceptionHandler when the state is " + state + ".");
        }
        Objects.requireNonNull(handler);
        this.handler = handler;
    }

    public ThreadNameGenerator getThreadNameGenerator() {
        return threadNameGenerator;
    }

    /**
     * {@link AbstractCASConsumer}实现，将其consume方法委托给{@link ConsumeAction#consume(Object)}.
     */
    private class InternalSPSCConsumer extends AbstractCASConsumer<T> {

        private ConsumerWrapper wrapper;

        private final ConsumeAction<T> action;

        public InternalSPSCConsumer(int queueSize) {
            super(queueSize);
            this.action = factory.newAction();
        }

        @Override
        public void consume(T task) {
            action.consume(task);
        }

        @Override
        protected String getThreadName(Thread t) {
            return (threadNameGenerator == null ? super.getThreadName(t) : threadNameGenerator.generate(t));
        }

    }

    /**
     * {@link AbstractMultiThreadsConsumer}实现，将其consume方法委托给{@link ConsumeAction#consume(Object)}.
     */
    private class InternalMPMCConsumer extends AbstractMultiThreadsConsumer<T> {

        private final ConsumeAction<T> action;

        public InternalMPMCConsumer(int queueSize, int threads) {
            super(queueSize, threads);
            this.action = factory.newAction();
        }

        @Override
        public void consume(T task) {
            action.consume(task);
        }

        @Override
        protected String getThreadName(Thread t) {
            return (threadNameGenerator == null ? super.getThreadName(t) : threadNameGenerator.generate(t));
        }

    }

    /**
     * 对{@link Consumer}进行包装，增加是否可用的标志位.
     */
    private class ConsumerWrapper<T> {

        private boolean available = true;
        private final Consumer<T> consumer;

        public ConsumerWrapper(Consumer<T> consumer) {
            this.consumer = consumer;
        }

    }

}