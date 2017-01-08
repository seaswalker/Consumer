package consumer;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import lifecycle.StateCheckDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import queue.SQueue;

/**
 * {@link SubmitableConsumer}骨架实现，提供基本的生命周期以及{@link RuntimeException}处理.
 *
 * @author skywalker
 */
public abstract class AbstractQueuedConsumer<T> implements SubmitableConsumer<T>, Runnable {

    protected SQueue<T> jobQueue;
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    protected Thread thread;
    protected UncaughtExceptionHandler handler;
    protected final int queueSize;
    private volatile State state = State.INIT;
    private volatile boolean consumeLeft = false;
    private long consumed = 0L;
    private CompletableFuture<Long> future;
    private final StateCheckDelegate delegate;
    protected final int id;

    public AbstractQueuedConsumer(int queueSize, int id) {
        this.queueSize = queueSize;
        this.id = id;
        this.delegate = StateCheckDelegate.getInstance();
    }

    /**
     * 由子类确定队列{@link SQueue}类型.
     *
     * @return {@linkplain SQueue}
     */
    protected abstract SQueue<T> newQueue();

    @Override
    public final boolean start() {
        delegate.checkStart(this);
        this.jobQueue = newQueue();
        String name = getThreadName();
        if (doStart()) {
            this.state = State.RUNNING;
            Thread t = new Thread(this, name);
            t.start();
            this.thread = t;
            logger.info("{} start successfully.", name);
            return true;
        }
        return false;
    }

    /**
     * 允许子类执行自己的启动逻辑. 默认直接返回true.
     * 此方法将在线程启动之后被调用.
     */
    protected boolean doStart() {
        return true;
    }

    /**
     * 得到线程名称，默认使用类名.
     */
    protected String getThreadName() {
        return (this.getClass().getSimpleName() + "-" + id);
    }

    /**
     * 处理{@link RuntimeException}, 子类可以定义自己的处理策略。
     *
     * @param e {@linkplain RuntimeException}
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

    @Override
    public final Future<Long> terminate() {
        delegate.checkTerminated(this);
        CompletableFuture<Long> future = new CompletableFuture<>();
        this.future = future;
        this.consumeLeft = true;
        this.state = State.TERMINATED;
        doTerminate();
        future.complete(consumed);
        return future;
    }

    /**
     * 模板方法，允许子类执行自己的terminate逻辑，默认空实现.
     */
    protected void doTerminate() {
    }

    @Override
    public final Future<Long> terminateNow() {
        delegate.checkTerminated(this);
        CompletableFuture<Long> future = new CompletableFuture<>();
        this.future = future;
        this.state = State.TERMINATED;
        doTerminateNow();
        future.complete(consumed);
        return future;
    }

    /**
     * 模板方法，允许子类执行自己的terminateNow逻辑，默认空实现.
     */
    protected void doTerminateNow() {
    }

    @Override
    public final void run() {
        T task;
        while (shouldConsume()) {
            task = getTask();
            if (task != null) {
                doConsume(task);
            }
        }
        if (consumeLeft) {
            while ((task = getLeftTask()) != null)
                doConsume(task);
        }
        this.future.complete(consumed);
    }

    /**
     * consume()辅助方法，捕获{@link RuntimeException}.
     */
    private void doConsume(T task) {
        try {
            consume(task);
            ++consumed;
        } catch (RuntimeException e) {
            handleUncheckedException(e);
        }
    }

    /**
     * 从工作队列中得到任务，由子类实现，此方法将会被run()调用.
     *
     * @return <T>
     */
    protected abstract T getTask();

    /**
     * 得到队列中剩余的任务，当terminate()方法被调用时执行，默认直接委托给getTask()方法.
     *
     * @return <T>
     */
    protected T getLeftTask() {
        return getTask();
    }

    /**
     * 检查当前是否可以继续消费.
     */
    private boolean shouldConsume() {
        return (this.state == State.RUNNING);
    }

    @Override
    public long getConsumedCount() {
        return consumed;
    }

    @Override
    public State getState() {
        return state;
    }

}
