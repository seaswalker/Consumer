package manager;

import lifecycle.LifeCycle;

/**
 * {@link Manager}骨架实现，非线程安全.
 *
 * @author skywalker
 */
public abstract class AbstractManager<T> implements Manager<T> {

    protected T[] slavers;
    protected int slaveCount;
    protected int index = 0;

    @Override
    public T next() {
        T result = slavers[index];
        ++index;
        if (index >= slaveCount) {
            index = 0;
        }
        return result;
    }

    @Override
    public boolean start() {
        int count = count();
        slavers = (T[]) new Object[count];
        for (int i = 0; i < count; i++) {
            T slaver = newSlaver(i);
            if (slaver instanceof LifeCycle) {
                ((LifeCycle) slaver).start();
            }
        }
        return true;
    }

}
