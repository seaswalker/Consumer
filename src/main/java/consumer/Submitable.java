package consumer;

/**
 * 可以提交.
 *
 * @author skywalker
 */
public interface Submitable<T> {

    /**
     * 任务提交.
     *
     * @param task 任务/消息
     * @return true, 如果提交成功
     */
    boolean submit(T task);

    /**
     * 任务提交，如果提交失败那会将会一直等待.
     *
     * @param task 任务/消息
     * @throws InterruptedException 如果在提交时被中断
     */
    void submitSync(T task) throws InterruptedException;

}
