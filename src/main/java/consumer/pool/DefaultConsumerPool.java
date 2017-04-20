package consumer.pool;

import consumer.Consumer;
import consumer.MultiThreadsConsumer;
import consumer.cas.AbstractSPCASConsumer;
import consumer.cas.AbstractMPCASConsumer;
import consumer.cas.strategy.RetryStrategy;
import consumer.lifecycle.LifeCycle;
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
     * 每个单生产者队列消费线程数量.
     */
    private final int spThreads;
    /**
     * 单生产者队列大小.
     */
    private final int spQueueSize;
    /**
     * 多生产者消费线程数.
     */
    private final int mpThreads;
    /**
     * 多生产者队列大小.
     */
    private final int mpQueueSize;
    private final ConsumeActionFactory<T> factory;
    /**
     * 消费者列表，采取如下的存储策略:
     * <p>最后一个为多生产者模型，前面的为但生产者模型.</p>
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

    public DefaultConsumerPool(int sp, int spThreads, int mpThreads, int spQueueSize, int mpQueueSize, ConsumeActionFactory<T> factory) {
        Objects.requireNonNull(factory);
        this.mpThreads = mpThreads;
        this.spThreads = spThreads;
        this.factory = factory;
        this.spQueueSize = spQueueSize;
        this.mpQueueSize = mpQueueSize;
        this.consumers = sp + 1;
        this.list = new ArrayList<>(this.consumers);
    }

    @Override
    public boolean start() {
        delegate.checkStart(this);
        for (int i = 0; i < consumers - 1; i++) {
            InternalSPConsumer spConsumer = new InternalSPConsumer(spQueueSize, spThreads);
            prepareConsumer(spConsumer);
            if (!spConsumer.start()) {
                return false;
            }
            ConsumerWrapper wrapper = new ConsumerWrapper(spConsumer);
            spConsumer.wrapper = wrapper;
            list.add(wrapper);
        }
        AbstractMPCASConsumer<T> mpConsumer = new InternalMPConsumer(mpQueueSize, mpThreads);
        prepareConsumer(mpConsumer);
        if (!mpConsumer.start()) {
            return false;
        }
        list.add(new ConsumerWrapper(mpConsumer));
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
        Consumer<T> result = acquireSPSC();
        if (result == null) {
            if (log != null && log.isDebugEnabled()) {
                log.debug("Currently there are no sp consumer available, so use mp instead.");
            }
            result = acquireMPMC();
        }
        return result;
    }

    @Override
    public Consumer<T> acquireSPSC() {
        delegate.checkRunning(this);
        Consumer<T> result = null;
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
        if (!(consumer instanceof MultiThreadsConsumer)) {
            //无需释放
            synchronized (monitor) {
                InternalSPConsumer internal = (InternalSPConsumer) consumer;
                internal.wrapper.available = true;
            }
        }
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

    /**
     * {@link AbstractSPCASConsumer}实现，将其consume方法委托给{@link ConsumeAction#consume(Object)}.
     */
    private class InternalSPConsumer extends AbstractSPCASConsumer<T> {

        private ConsumerWrapper wrapper;

        private final ConsumeAction<T> action;

        InternalSPConsumer(int queueSize, int threads) {
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
     * {@link AbstractMPCASConsumer}实现，将其consume方法委托给{@link ConsumeAction#consume(Object)}.
     */
    private class InternalMPConsumer extends AbstractMPCASConsumer<T> {

        private final ConsumeAction<T> action;

        InternalMPConsumer(int queueSize, int threads) {
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
    private class ConsumerWrapper {

        private boolean available = true;
        private final Consumer<T> consumer;

        ConsumerWrapper(Consumer<T> consumer) {
            this.consumer = consumer;
        }

    }

}