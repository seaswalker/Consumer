package consumer;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import queue.SQueue;

/**
 * {@link SubmitableConsumer}骨架实现，提供基本的生命周期以及{@link RuntimeException}处理.
 *
 * @author skywalker
 */
public abstract class AbstractQueuedConsumer<T> implements SubmitableConsumer<T>, Runnable {

    protected SQueue<T> jobQueue;
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    protected Thread thread;
    protected UncaughtExceptionHandler handler;
    protected final int queueSize;

    public AbstractQueuedConsumer(int queueSize) {
        this.queueSize = queueSize;
    }

    /**
     * 由子类确定队列{@link SQueue}类型.
     *
     * @return {@linkplain SQueue}
     */
    protected abstract SQueue<T> newQueue();

    @Override
    public boolean start() {
        this.jobQueue = newQueue();
        String name = getThreadName();
        Thread t = new Thread(this, name);
        t.start();
        this.thread = t;
        logger.info("{} start successfully.", name);
        return false;
    }

    /**
     * 由子类确定线程名称.
     */
    protected abstract String getThreadName();

    /**
     * 处理{@link RuntimeException}, 子类可以定义自己的处理策略。
     *
     * @param e {@linkplain RuntimeException}
     */
    protected void handleUncheckedException(RuntimeException e) {
        if (handler != null) {
            handler.uncaughtException(thread, e);
        } else {
            logger.error("RuntimeException occurred when consume() was invoked.", e);
        }
    }

    @Override
    public void setUncaughtExceptionHandler(UncaughtExceptionHandler handler) {
        Objects.requireNonNull(handler);
        this.handler = handler;
    }

}
