package pool;

/**
 * 消费{@link consumer.Consumer}获得的消息，此接口应和{@link DefaultConsumerPool}配合使用.
 *
 * @author skywalker
 */
public interface ConsumeAction<T> {

    /**
     * 消费.
     *
     * @param message 消息
     */
    void consume(T message);

}
