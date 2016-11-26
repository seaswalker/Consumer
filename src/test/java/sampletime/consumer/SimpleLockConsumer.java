package sampletime.consumer;

import consumer.AbstractLockedConsumer;

/**
 * 简单的{@link consumer.AbstractLockedConsumer}.
 *
 * @author skywalker
 */
public class SimpleLockConsumer extends AbstractLockedConsumer<String> {

    public SimpleLockConsumer(int queueSize) {
        super(queueSize);
    }

    @Override
    public void consume(String task) {
        //do nothing here
    }

}
