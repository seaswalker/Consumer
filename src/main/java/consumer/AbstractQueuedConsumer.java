package consumer;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Objects;
import java.util.logging.Logger;

import queue.SQueue;

public abstract class AbstractQueuedConsumer<T> implements SubmitableConsumer<T>, Runnable {
	
	protected SQueue<T> jobQueue;
	protected final Logger logger = LoggerFactory.getLogger(this.getClass());
	protected Thread thread;
	protected UncaughtExceptionHandler handler;
	
	/**
	 * 由子类确定队列{@link SQueue}类型.
	 * @return {@linkplain SQueue}
	 */
	protected abstract SQueue<T> newQueue();
	
	@Override
	public boolean start() {
		this.jobQueue = newQueue();
		String name = getThreadName();
		Thread t = new Thread(this, name);
		t.start();
		this.thread = t;
		logger.info("{} start successfully.", name);
		return false;
	}
	
	protected abstract String getThreadName();
	
	/**
	 * 处理{@link RuntimeException}, 子类可以定义自己的处理策略。
	 * @param e
	 */
	protected void handleUncheckedException(RuntimeException e) {
		if (handler != null) {
			handler.uncaughtException(thread, e);
		} else {
			logger.error("RuntimeException occurred when consume() was invoked.", e);
		}
	}
	
	@Override
	public void setUncaughtExceptionHandler(UncaughtExceptionHandler handler) {
		Objects.requireNonNull(handler);
		this.handler = handler;
	}

}
