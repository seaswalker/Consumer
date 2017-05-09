package consumer.pool.dispatch;

import consumer.pool.ConsumerPool;

/**
 * 消费者的分配策略.
 *
 * @author skywalker
 */
public interface DispatchStrategy<T> extends ConsumerPool<T> {
}
