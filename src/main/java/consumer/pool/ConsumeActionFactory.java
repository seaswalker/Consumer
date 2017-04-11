package consumer.pool;

/**
 * {@link ConsumeAction}工厂.
 *
 * @author skywalker
 */
public interface ConsumeActionFactory<T> {

    /**
     * 生成一个新的{@link ConsumeAction}.
     */
    ConsumeAction<T> newAction();

}
