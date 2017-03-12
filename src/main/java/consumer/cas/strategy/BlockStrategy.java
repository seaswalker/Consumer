package consumer.cas.strategy;

import queue.SQueue;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * {@link RetryStrategy}实现，如果获取任务失败，那么将会进行阻塞直到有新的任务可以获取.
 *
 * @author skywalker
 */
public class BlockStrategy<T> implements RetryStrategy<T> {

    private final Lock lock = new ReentrantLock();
    private final Condition empty = lock.newCondition();
    /**
     * 正在进行等待的线程数.
     */
    private final AtomicInteger waiters = new AtomicInteger(0);

    @Override
    public T retry(SQueue<T> queue) throws InterruptedException {
        T task;
        if ((task = queue.poll()) == null) {
            waiters.incrementAndGet();
            lock.lock();
            try {
                empty.await();
            } finally {
                lock.unlock();
            }
        }
        return task;
    }

    @Override
    public boolean submit(SQueue<T> queue, T task) {
        boolean result = queue.offer(task);
        if (result) {
            signalIfNecessary(false);
        }
        return result;
    }

    @Override
    public void release() {
        signalIfNecessary(true);
    }

    /**
     * 如果有正在阻塞的消费线程，唤醒全部或其中一个.
     *
     * @param all 如果为true，那么唤醒所有正在阻塞的消费线程
     */
    private void signalIfNecessary(boolean all) {
        if (waiters.get() > 0) {
            lock.lock();
            try {
                if (all) {
                    empty.signalAll();
                } else {
                    empty.signal();
                }
            } finally {
                lock.unlock();
            }
        }
    }

    @Override
    public RetryStrategy<T> copy() {
        return new BlockStrategy<T>();
    }

}
