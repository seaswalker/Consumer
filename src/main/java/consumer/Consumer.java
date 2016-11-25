package consumer;

import java.lang.Thread.UncaughtExceptionHandler;

import lifecycle.LifeCycle;

public interface Consumer<T> extends LifeCycle {

	/**
	 * 任务消费.
	 * @param task
	 */
	void consume(T task);
	
	/**
	 * 设置{@link RuntimeException}处理器.
	 * @param handler
	 */
	void setUncaughtExceptionHandler(UncaughtExceptionHandler handler);
	
}
