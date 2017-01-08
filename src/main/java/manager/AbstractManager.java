package manager;

import lifecycle.LifeCycle;
import lifecycle.StateCheckDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Function;

/**
 * {@link Manager}骨架实现，非线程安全.
 *
 * @author skywalker
 */
public abstract class AbstractManager<T extends LifeCycle> implements Manager<T> {

    protected List<T> slavers;
    protected int slaveCount;
    protected final StateCheckDelegate delegate;
    private State state = State.INIT;
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected AbstractManager() {
        this.delegate = StateCheckDelegate.getInstance();
    }


    @Override
    public boolean start() {
        int count = count();
        slavers = new ArrayList<T>(count);
        int succeed = 0;
        for (int i = 0; i < count; i++) {
            T slaver = newSlaver(i);
            if (slaver.start()) {
                ++succeed;
                slavers.add(slaver);
            }
        }
        slaveCount = succeed;
        boolean result = (succeed > 0);
        if (result) {
            state = State.RUNNING;
        }
        return result;
    }

    @Override
    public Future<Long> terminate() {
        return terminateHelper(LifeCycle::terminate);
    }

    @Override
    public Future<Long> terminateNow() {
        return terminateHelper(LifeCycle::terminateNow);
    }

    /**
     * termintae()和termintaeNow()辅助方法.
     *
     * @param function {@link Function}
     * @return {@link Future} 如果对结果不感兴趣，那么返回null
     */
    private Future<Long> terminateHelper(Function<T, Future<Long>> function) {
        delegate.checkTerminated(this);
        final Future<Long>[] futures = new Future[slaveCount];
        for (int i = 0; i < slaveCount; i++) {
            futures[i] = function.apply(slavers.get(i));
        }
        final CompletableFuture<Long> future = new CompletableFuture<>();
        new Thread(() -> {
            long consumed = 0L;
            try {
                for (int i = 0; i < slaveCount; i++) {
                    consumed += futures[i].get();
                }
                future.complete(consumed);
            } catch (Exception e) {
                logger.error("Result collect failed.", e);
                future.complete(-1L);
            }
        }).start();
        return future;
    }

    @Override
    public State getState() {
        return state;
    }
}
