package consumer.cas.strategy;

import queue.SQueue;

/**
 * {@link RetryStrategy}实现，如果获取新的任务失败，那么将会不断的重试.
 *
 * @author skywalker
 */
public class LoopStrategy<T> implements RetryStrategy<T> {

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

    @Override
    public RetryStrategy<T> copy() {
        return new LoopStrategy<T>();
    }

}
