package consumer;

/**
 * 可以提交.
 * @author zhao.xudong
 *
 */
public interface Submiable<T> {

	/**
	 * 任务提交.
	 * @param task
	 * @return true, 如果提交成功
	 */
	boolean submit(T task);
	
	/**
	 * 任务提交，如果提交失败那会将会一直等待.
	 * @param task
	 */
	void submitSync(T task);
	
}
