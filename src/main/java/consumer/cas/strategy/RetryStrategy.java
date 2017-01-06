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
     */
    T retry(SQueue<T> queue);

    /**
     * 任务提交.
     */
    boolean submit(SQueue<T> queue, T task);

}
