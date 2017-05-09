package consumer.pool.dispatch;

import consumer.Consumer;

import java.util.List;

/**
 * 简单的轮询以决定使用的{@link consumer.Consumer}.
 *
 * @author skywalker
 */
public class RoundRobinStrategy<T> extends AbstractDispatchStrategy<T> {

    private final Object monitor = new Object();
    private final int count;

    private int index = 0;

    public RoundRobinStrategy(List<Consumer<T>> consumers) {
        super(consumers);
        this.count = consumers.size();
    }

    @Override
    public Consumer<T> acquire() {
        Consumer<T> result;
        synchronized (monitor) {
            result = consumers.get(index);
            ++index;
            if (index >= count) {
                index = 0;
            }
        }
        return result;
    }

    @Override
    public void release(Consumer consumer) {
        //nothing here
    }

}
