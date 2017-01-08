package consumer.cas;

import consumer.AbstractQueuedConsumer;
import consumer.SubmitableConsumer;
import consumer.cas.strategy.RetryStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import queue.SQueue;
import queue.cas.SpscBasedQueue;

import java.util.Objects;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * {@link SubmitableConsumer}骨架实现，基于CAS操作的消费者实现.
 * <br>
 * 默认采用阻塞的等待策略.
 *
 * @author skywalker
 */
public abstract class AbstractCASConsumer<T> extends AbstractQueuedConsumer<T> {

    private final RetryStrategy<T> retryStrategy;

    public AbstractCASConsumer(int queueSize, int id) {
        this(queueSize, id, null);
    }

    public AbstractCASConsumer(int queueSize, int id, RetryStrategy retryStrategy) {
        super(queueSize, id);
        if (retryStrategy == null) {
            retryStrategy = newBlockStrategy();
        }
        this.retryStrategy = retryStrategy;
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
    protected final T getTask() {
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

    /**
     * 创建一个阻塞的{@link RetryStrategy}.
     */
    public BlockStrategy newBlockStrategy() {
        return new BlockStrategy(this);
    }

    /**
     * 创建一个不断重试的{@link RetryStrategy}.
     */
    public LoopStrategy newLoopStrategy() {
        return new LoopStrategy();
    }

    /**
     * 创建一个自旋实现的{@link RetryStrategy}.
     */
    public SpinStrategy newSpinStrategy(int spin) {
        return new SpinStrategy(this, spin);
    }

    /**
     * {@link RetryStrategy}实现，如果获取任务失败，那么将会进行阻塞直到有新的任务可以获取.
     */
    private static class BlockStrategy<T> implements RetryStrategy<T> {

        private final Lock lock = new ReentrantLock();
        private final Condition empty = lock.newCondition();
        private volatile boolean waitting = false;
        private final Logger logger = LoggerFactory.getLogger(this.getClass());

        protected final AbstractCASConsumer consumer;

        private BlockStrategy(AbstractCASConsumer consumer) {
            this.consumer = consumer;
        }

        @Override
        public T retry(SQueue<T> queue) {
            T task = null;
            try {
                lock.lock();
                try {
                    if ((task = queue.poll()) == null) {
                        waitting = true;
                        empty.await();
                    }
                } finally {
                    lock.unlock();
                }
            } catch (InterruptedException e) {
                logger.error("InterruptedException occurred when wait on Condition BlockStrategy.empty.", e);
            }
            return task;
        }

        @Override
        public boolean submit(SQueue<T> queue, T task) {
            boolean result = queue.offer(task);
            if (result && waitting) {
                lock.lock();
                try {
                    empty.signal();
                } finally {
                    lock.unlock();
                }
            }
            return result;
        }

        @Override
        public void release() {
            if (waitting) {
                lock.lock();
                try {
                    empty.signal();
                } finally {
                    lock.unlock();
                }
            }
        }

    }

    /**
     * {@link RetryStrategy}实现，如果获取新的任务失败，那么将会不断的重试.
     */
    private static class LoopStrategy<T> implements RetryStrategy<T> {

        @Override
        public T retry(SQueue<T> queue) {
            return queue.poll();
        }

        @Override
        public boolean submit(SQueue<T> queue, T task) {
            return queue.offer(task);
        }

        @Override
        public void release() {
            //do nothing
        }

    }

    /**
     * {@link RetryStrategy}实现，可指定一个自旋次数，如果自旋后仍不能成功获取任务，那么将会阻塞.
     *
     * @param <T>
     */
    private static class SpinStrategy<T> extends BlockStrategy<T> {

        private final int spin;

        private SpinStrategy(AbstractCASConsumer consumer, int spin) {
            super(consumer);
            this.spin = spin;
        }

        @Override
        public T retry(SQueue<T> queue) {
            int times = 0;
            T task = null;
            while (times < spin) {
                if (consumer.getState() != State.RUNNING) {
                    return task;
                }
                task = queue.poll();
                if (task != null) {
                    break;
                }
                ++times;
            }
            if (task == null) {
                task = super.retry(queue);
            }
            return task;
        }
    }

}
