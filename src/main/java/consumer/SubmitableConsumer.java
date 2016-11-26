package consumer;

/**
 * 支持任务提交的{@link Consumer}.
 *
 * @author skywalker
 */
public interface SubmitableConsumer<T> extends Consumer<T>, Submiable<T> {

}
