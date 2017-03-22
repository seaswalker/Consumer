package consumer.lock;

import consumer.AbstractQueuedConsumer;
import consumer.Consumer;
import consumer.queue.locked.ArrayQueue;
import consumer.queue.SQueue;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * {@link AbstractQueuedConsumer}骨架实现，提供加锁(线程安全)的实现.子类只需实现{@link Consumer}.consume()方法即可.
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
    public final void submitSync(T task) throws InterruptedException {
        lock.lock();
        try {
            while (!this.jobQueue.offer(task))
                full.await();
            empty.signal();
        } finally {
            lock.unlock();
        }
    }

    @Override
    protected final T getTask() throws InterruptedException {
        T task;
        lock.lock();
        try {
            if ((task = jobQueue.poll()) == null) {
                empty.await();
            }
            if (task != null) {
                full.signalAll();
            }
        } finally {
            lock.unlock();
        }
        return task;
    }

    @Override
    protected final void doTerminate() {
        lock.lock();
        try {
            empty.signal();
        } finally {
            lock.unlock();
        }
    }

    @Override
    protected final void doTerminateNow() {
        doTerminate();
    }

}
