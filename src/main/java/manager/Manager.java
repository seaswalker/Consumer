package manager;

import lifecycle.LifeCycle;

import java.util.concurrent.Future;

public interface Manager<T> extends LifeCycle {
	
	T newSlaver(int id);
	
	T next();
	
	/**
	 * 被管理者的数量.
	 */
	int count();

    @Override
    Future<Void> terminateNow();

    @Override
    Future<Void> terminate();

}
