package consumer.cas.strategy;

import consumer.queue.SQueue;

/**
 * {@link RetryStrategy}实现，可指定一个自旋次数，如果自旋后仍不能成功获取任务，那么将会阻塞.
 *
 * @author skywalker
 */
public class SpinStrategy<T> extends BlockStrategy<T> {

    private final int spin;

    public SpinStrategy(int spin) {
        this.spin = spin;
    }

    @Override
    public T retry(SQueue<T> queue) throws InterruptedException {
        int times = 0;
        T task = null;
        while (times < spin) {
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

    @Override
    public RetryStrategy<T> copy() {
        return new SpinStrategy<T>(spin);
    }

}
