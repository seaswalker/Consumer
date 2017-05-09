package consumer.pool.dispatch;

import consumer.Consumer;
import consumer.MultiThreadsConsumer;
import consumer.pool.internal.InternalSPConsumer;
import consumer.util.Util;
import org.slf4j.Logger;

import java.util.List;

/**
 *  {@link DispatchStrategy}实现，用以处理单生产模型下的消费者分配.
 *
 *  @author skywalker
 */
public class SingleProducerStrategy<T> extends AbstractDispatchStrategy<T> {

    private final Object monitor = new Object();
    private final int count;
    private final Logger log = Util.getLogger(this.getClass());

    public SingleProducerStrategy(List<Consumer<T>> consumers) {
        super(consumers);
        this.count = consumers.size();
    }

    @Override
    public Consumer<T> acquire() {
        Consumer<T> result = acquireSPSC();
        if (result == null) {
            if (log != null && log.isDebugEnabled()) {
                log.debug("Currently there are no sp consumer available, so use mp instead.");
            }
            result = acquireMPMC();
        }
        return result;
    }

    @Override
    public void release(Consumer<T> consumer) {
        if (!(consumer instanceof MultiThreadsConsumer)) {
            synchronized (monitor) {
                InternalSPConsumer internal = (InternalSPConsumer) consumer;
                internal.setAvailable(true);
            }
        }
    }

    private Consumer<T> acquireSPSC() {
        Consumer<T> result = null;
        synchronized (monitor) {
            for (int i = 0; i < count - 1; i++) {
                InternalSPConsumer<T> consumer = (InternalSPConsumer<T>) consumers.get(i);
                if (consumer.isAvailable()) {
                    result = consumer;
                    consumer.setAvailable(false);
                    break;
                }
            }
        }
        return result;
    }

    private MultiThreadsConsumer<T> acquireMPMC() {
        return (MultiThreadsConsumer<T>) consumers.get(count - 1);
    }

}
