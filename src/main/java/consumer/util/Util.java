package consumer.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 工具类.
 *
 * @author skywalker
 */
public class Util {

    private Util() {
    }

    /**
     * 根据给定的类获取{@link Logger}.
     *
     * @param clazz {@linkplain Class}
     * @return 如果当前classpath中未引入slf4j及相关jar包，那么返回null.
     */
    public static Logger getLogger(Class<?> clazz) {
        Logger logger = null;
        try {
            Class.forName("org.slf4j.impl.StaticLoggerBinder");
            logger = LoggerFactory.getLogger(clazz);
        } catch (Throwable e) {
            //ignore
        }
        return logger;
    }

}
