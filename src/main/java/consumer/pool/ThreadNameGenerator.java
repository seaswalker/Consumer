package consumer.pool;

/**
 * 线程名字生成器，应配合{@link DefaultConsumerPool}使用.
 *
 * @author skywalker
 */
public interface ThreadNameGenerator {

    String generate(Thread thread);

}
