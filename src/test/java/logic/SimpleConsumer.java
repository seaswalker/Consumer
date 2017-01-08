package logic;

import consumer.cas.AbstractCASConsumer;
import consumer.lock.AbstractLockedConsumer;

/**
 * 简单的{@link consumer.Consumer}实现，打印出Task.
 *
 * @author skywalker
 */
public class SimpleConsumer extends AbstractLockedConsumer<String> {

    public SimpleConsumer(int queueSize, int id) {
        super(queueSize, id);
    }

    @Override
    public void consume(String task) {
        System.out.println("Consumer " + id + ": ");
    }

}
