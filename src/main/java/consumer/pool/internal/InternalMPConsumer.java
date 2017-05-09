package consumer.pool.internal;

import consumer.cas.AbstractMPCASConsumer;
import consumer.pool.ConsumeAction;
import consumer.pool.ConsumeActionFactory;
import consumer.pool.ThreadNameGenerator;

/**
 * {@link AbstractMPCASConsumer}实现，将其consume方法委托给{@link ConsumeAction#consume(Object)}.
 *
 * @author skywalker
 */
public class InternalMPConsumer<T> extends AbstractMPCASConsumer<T> {

    private final ConsumeAction<T> action;
    private final ThreadNameGenerator threadNameGenerator;

    public InternalMPConsumer(int queueSize, int threads, ConsumeActionFactory<T> factory, ThreadNameGenerator threadNameGenerator) {
        super(queueSize, threads);
        this.action = factory.newAction();
        this.threadNameGenerator = threadNameGenerator;
    }

    @Override
    public void consume(T task) {
        action.consume(task);
    }

    @Override
    protected String getThreadName(Thread t) {
        return (threadNameGenerator == null ? super.getThreadName(t) : threadNameGenerator.generate(t));
    }

}
