package consumer;

import java.lang.Thread.UncaughtExceptionHandler;

import lifecycle.LifeCycle;

/**
 * 消费者.
 */
public interface Consumer<T> extends LifeCycle {

    /**
     * 任务消费.
     */
    void consume(T task);

    /**
     * 设置{@link RuntimeException}处理器.
     *
     * @param handler {@link UncaughtExceptionHandler}
     */
    void setUncaughtExceptionHandler(UncaughtExceptionHandler handler);

    /**
     * 统计成功消费的任务数.
     */
    long getConsumedCount();

}
