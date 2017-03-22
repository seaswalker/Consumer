package consumer.pool;

import consumer.Consumer;
import consumer.MultiThreadsConsumer;
import consumer.lifecycle.LifeCycle;

/**
 * 消费者池，一个池中包含下列元素:
 * <ul>
 * <li>一定数量的单线程消费者(SPSC).</li>
 * <li>一个多线程消费者(MPMC).</li>
 * </ul>
 *
 * @author skywalker
 */
public interface ConsumerPool<T> extends LifeCycle {

    /**
     * 申请一个消费者{@link Consumer}，遵循如下的策略:
     * <ul>
     * <li>如果当前尚有单线程消费者存在，返回之.</li>
     * <li>返回多线程消费者.</li>
     * </ul>
     *
     * @return {@linkplain Consumer} 可用的消费者
     */
    Consumer<T> acquire();

    /**
     * 申请单线程消费者(SPSC).
     *
     * @return {@link Consumer} 如果当前没有可用的消费者，返回null
     * @see #acquire()
     */
    Consumer<T> acquireSPSC();

    /**
     * 申请多线程消费者(MPSC).
     *
     * @return {@link Consumer} 如果当前没有可用的消费者，返回null
     * @see #acquire()
     */
    MultiThreadsConsumer<T> acquireMPMC();

    /**
     * 释放{@link Consumer}.当一个生产者不再使用一个消费者时，务必确保此方法被调用.
     *
     * @param consumer {@linkplain Consumer}
     */
    void release(Consumer<T> consumer);

}
