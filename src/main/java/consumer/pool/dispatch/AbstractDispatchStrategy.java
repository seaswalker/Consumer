package consumer.pool.dispatch;

import consumer.Consumer;
import consumer.pool.dispatch.DispatchStrategy;

import java.util.List;
import java.util.concurrent.Future;

/**
 * 屏蔽掉{@link consumer.lifecycle.LifeCycle}的相关方法.
 *
 * @author skywalker
 */
public abstract class AbstractDispatchStrategy<T> implements DispatchStrategy<T> {

    protected final List<Consumer<T>> consumers;

    protected AbstractDispatchStrategy(List<Consumer<T>> consumers) {
        this.consumers = consumers;
    }

    @Override
    public final boolean start() {
        throw new UnsupportedOperationException();
    }

    @Override
    public final Future<Void> terminate() {
        throw new UnsupportedOperationException();
    }

    @Override
    public final Future<Void> terminateNow() {
        throw new UnsupportedOperationException();
    }

    @Override
    public final State getState() {
        throw new UnsupportedOperationException();
    }

}
