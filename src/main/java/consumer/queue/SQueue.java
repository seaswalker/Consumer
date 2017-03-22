package consumer.queue;

/**
 * 队列接口.
 * 
 * @author zhao.xudong
 *
 */
public interface SQueue<T> {

	/**
	 * 向队列中添加元素，如果添加成功，返回true.
	 */
	boolean offer(T element);

	/**
	 * 从队头取出元素，如果当前没有可用的元素，那么返回null.
	 */
	T poll();

    /**
     * 得到队列的大小.
     */
	int size();

    /**
     * 当前队列是否为空.
     */
	boolean isEmpty();
	
}
