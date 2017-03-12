package consumer.cas.strategy;

import queue.SQueue;

/**
 * 当{@link consumer.cas.AbstractCASConsumer}获取任务失败时采取的重试策略.
 *
 * @author skywalker
 */
public interface RetryStrategy<T> {

    /**
     * 重试.
     *
     * @param queue {@link queue.cas.CASQueue}.
     * @throws InterruptedException 如果线程在睡眠等待时被中断
     */
    T retry(SQueue<T> queue) throws InterruptedException;

    /**
     * 任务提交.
     */
    boolean submit(SQueue<T> queue, T task);

    /**
     * 放弃重试，比如对于{@link BlockStrategy},那么就应该从阻塞中醒来.
     */
    void release();

    /**
     * 返回当前对象的一个拷贝.
     *
     * @return {@link RetryStrategy} 副本
     */
    RetryStrategy<T> copy();

}
