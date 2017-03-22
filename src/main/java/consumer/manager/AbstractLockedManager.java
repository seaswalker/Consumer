package consumer.manager;

import consumer.lifecycle.LifeCycle;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * {@link Manager}骨架实现，线程安全的next方法.
 *
 * @author skywalker
 */
public abstract class AbstractLockedManager<T extends LifeCycle> extends AbstractManager<T> {
	
	private final Lock lock = new ReentrantLock();
    private int index = 0;
	
	@Override
	public T next() {
		T result = null;
		lock.lock();
		try {
			result = slavers.get(index);
			++index;
			if (index >= slaveCount) {
				index = 0;
			}
		} finally {
			lock.unlock();
		}
		return result;
	}

}
