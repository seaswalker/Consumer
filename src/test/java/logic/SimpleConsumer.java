package logic;

import consumer.cas.AbstractCASConsumer;

/**
 * 简单的{@link consumer.Consumer}实现，打印出Task.
 *
 * @author skywalker
 */
public class SimpleConsumer extends AbstractCASConsumer<String> {

    public SimpleConsumer(int queueSize, int id) {
        super(queueSize, id);
    }

    @Override
    public void consume(String task) {
        System.out.println("Consumer " + id + ": " + task);
    }

}
