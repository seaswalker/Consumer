package lifecycle;

import java.util.concurrent.Future;

/**
 * 生命周期.
 *
 * @author skywalker
 */
public interface LifeCycle {

    /**
     * 启动.
     *
     * @return true，如果启动成功
     */
    boolean start();

    /**
     * 关闭，此方法只是通知组件关闭，调用此方法后，组件不再接受新的任务.
     *
     * @return {@link Future}
     */
    <T> Future<T> terminate();

    /**
     * 立即关闭.
     *
     * @return {@link Future}
     */
    <T> Future<T> terminateNow();

    /**
     * 得到组件当前的状态.
     *
     * @return {@link State}
     */
    State getState();

    /**
     * 组件的运行状态
     */
    enum State {

        INIT,
        RUNNING,
        TERMINATED

    }

}
