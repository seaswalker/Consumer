package manager;

import lifecycle.LifeCycle;
import lifecycle.StateCheckDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

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

    /**
     * 对结果进行累加.
     * <br>
     * 如果覆盖了getAccumulator方法，那么必须覆盖此方法.
     *
     * @param accumulator 累加器
     * @param value       加数
     * @return 新的累加器
     */
    protected <V> V accumulate(V accumulator, V value) {
        throw new UnsupportedOperationException();
    }

    /**
     * 获得结果累加器，如果返回null,表示对结果不感兴趣，这会导致不进行结果收集.
     * 默认返回null.
     */
    protected <V> V getAccumulator() {
        return null;
    }

    @Override
    public State getState() {
        return state;
    }
}
