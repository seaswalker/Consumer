package consumer.pool;

import consumer.Consumer;
import consumer.MultiThreadsConsumer;
import consumer.lifecycle.LifeCycle;

/**
 * 消费者池.
 *
 * @author skywalker
 */
public interface ConsumerPool<T> extends LifeCycle {

    /**
     * 申请一个消费者{@link Consumer}.
     *
     * @return {@linkplain Consumer} 可用的消费者
     */
    Consumer<T> acquire();

    /**
     * 释放{@link Consumer}.当一个生产者不再使用一个消费者时，务必确保此方法被调用.
     */
    void release(Consumer<T> consumer);

}
