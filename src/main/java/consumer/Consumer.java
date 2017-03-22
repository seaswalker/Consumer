package consumer;

import java.lang.Thread.UncaughtExceptionHandler;

import consumer.lifecycle.LifeCycle;

/**
 * 消费者.只有一个线程负责从队列中取出任务并调用{@link #consume(Object)}方法.
 *
 * @author skywalker
 */
public interface Consumer<T> extends LifeCycle, Submitable<T> {

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

}
