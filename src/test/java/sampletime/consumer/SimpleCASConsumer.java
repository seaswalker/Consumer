package sampletime.consumer;

import consumer.AbstractCASConsumer;

/**
 * 简单的{@link consumer.AbstractCASConsumer}实现.
 *
 * @author skywalker
 */
public class SimpleCASConsumer extends AbstractCASConsumer<String> {

    public SimpleCASConsumer(int queueSize) {
        super(queueSize);
    }

    @Override
    public void consume(String task) {
        // do nothing here
    }
}
