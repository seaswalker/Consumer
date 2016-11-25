package manager;

import lifecycle.LifeCycle;

public interface Manager<T> extends LifeCycle {
	
	T newSlaver(int id);
	
	T next();
	
	/**
	 * 被管理者的数量.
	 */
	int count();

}
