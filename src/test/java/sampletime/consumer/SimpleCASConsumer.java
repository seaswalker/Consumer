package sampletime.consumer;

import consumer.cas.AbstractCASConsumer;

/**
 * 简单的{@link AbstractCASConsumer}实现.
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
