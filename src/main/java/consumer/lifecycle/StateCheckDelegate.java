package consumer.lifecycle;

/**
 * 检查{@link consumer.lifecycle.LifeCycle.State}，其它类的检查可委托给此类实现.
 *
 * @author skywalker
 */
public class StateCheckDelegate {

    private static final StateCheckDelegate instance = new StateCheckDelegate();

    private StateCheckDelegate() {
    }

    public static StateCheckDelegate getInstance() {
        return instance;
    }

    /**
     * 检查当前是否可以启动.
     *
     * @param lifeCycle {@link LifeCycle}
     * @throws IllegalStateException 如果不能
     */
    public void checkStart(LifeCycle lifeCycle) {
        LifeCycle.State state = lifeCycle.getState();
        if (state != LifeCycle.State.INIT) {
            String className = lifeCycle.getClass().getSimpleName();
            throw new IllegalStateException(className + " start failed, because it's state is " + state.name());
        }
    }

    /**
     * 检查组件目前是否可以被终结.
     *
     * @param lifeCycle {@link LifeCycle}
     * @throws IllegalStateException 如果不能
     */
    public void checkTerminated(LifeCycle lifeCycle) {
        String className = lifeCycle.getClass().getSimpleName();
        if (lifeCycle.getState() == LifeCycle.State.TERMINATED) {
            throw new IllegalStateException(className + " has been terminated.");
        }
    }

    /**
     * 检查组件当前是否正在运行.
     *
     * @param lifeCycle {@link LifeCycle}
     * @throws IllegalStateException 如果没有正在运行
     */
    public void checkRunning(LifeCycle lifeCycle) {
        String className = lifeCycle.getClass().getSimpleName();
        if (lifeCycle.getState() != LifeCycle.State.RUNNING) {
            throw new IllegalStateException(className + " should be running.");
        }
    }

}
