package consumer;

import queue.ArrayQueue;
import queue.SQueue;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * {@link SubmitableConsumer}骨架实现，提供加锁(线程安全)的实现.
 * 子类只需实现{@link Consumer}.consume()方法即可.
 *
 * @author skywalker
 */
public abstract class AbstractLockedConsumer<T> extends AbstractQueuedConsumer<T> {

    private final Lock lock = new ReentrantLock();
    private final Condition empty = lock.newCondition();
    private final Condition full = lock.newCondition();

    public AbstractLockedConsumer(int queueSize) {
        super(queueSize);
    }

    @Override
    protected SQueue<T> newQueue() {
        return new ArrayQueue<>(queueSize);
    }

    @Override
    public final boolean submit(T task) {
        boolean result = false;
        lock.lock();
        try {
            result = jobQueue.offer(task);
            if (result) {
                empty.signal();
            }
        } finally {
            lock.unlock();
        }
        return result;
    }

    @Override
    public final void submitSync(T task) {
        lock.lock();
        try {
            while (!this.jobQueue.offer(task))
                full.await();
            empty.signal();
        } catch (InterruptedException e) {
            logger.error("InterruptedException occurred when submitSync() was invoked.", e);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public final void run() {
        while (true) {
            T task = null;
            lock.lock();
            try {
                while ((task = jobQueue.poll()) == null)
                    empty.await();
                full.signalAll();
            } catch (InterruptedException e) {
                logger.error("InterruptedException occurred when waiting on Condition 'empty'.", e);
            } finally {
                lock.unlock();
            }
            try {
                consume(task);
            } catch (RuntimeException e) {
                handleUncheckedException(e);
            }
        }
    }

}
