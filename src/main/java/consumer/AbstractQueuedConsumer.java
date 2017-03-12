package consumer;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Objects;
import java.util.concurrent.*;

import lifecycle.StateCheckDelegate;
import queue.SQueue;

/**
 * {@link Consumer}骨架实现，提供基本的生命周期以及{@link RuntimeException}处理.
 *
 * @author skywalker
 */
public abstract class AbstractQueuedConsumer<T> implements Consumer<T>, Runnable {

    protected SQueue<T> jobQueue;
    protected ExecutorService executor;
    protected UncaughtExceptionHandler handler;

    protected final int queueSize;

    private volatile State state = State.INIT;
    private volatile boolean consumeLeft = false;

    private CompletableFuture<Void> future;

    private final StateCheckDelegate delegate;
    /**
     * 默认的线程工厂，将线程名设置为{@link #getThreadName(Thread)}.
     */
    private final ThreadFactory defaultThreadFactory = r -> {
        Thread t = new Thread(r);
        t.setName(getThreadName(t));
        return t;
    };

    public AbstractQueuedConsumer(int queueSize) {
        this.queueSize = queueSize;
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
        this.state = State.RUNNING;
        this.executor = startExecutor(defaultThreadFactory);
        return true;
    }

    /**
     * 启动消费线程，子类可覆盖此方法以实现自己的启动逻辑，比如对于实现了{@link MultiThreadsConsumer}的
     * 消费者来说，可覆盖此方法以启动多个消费线程.
     * <p>注意: 子类在覆盖此方法时，务必保证返回的{@link ExecutorService}使用参数给定的线程工厂.</p>
     * <p>默认使用{@link Executors#newSingleThreadExecutor()}方法来启动一个消费线程.</p>
     *
     * @param threadFactory 使用的线程工厂
     * @return {@linkplain ExecutorService}
     */
    protected ExecutorService startExecutor(ThreadFactory threadFactory) {
        ExecutorService executor = Executors.newSingleThreadExecutor(threadFactory);
        executor.execute(this);
        return executor;
    }

    /**
     * 得到线程名称，默认使用类名-线程ID的格式.
     *
     * @param t {@linkplain Thread}, 线程
     */
    protected String getThreadName(Thread t) {
        return (this.getClass().getSimpleName() + "-" + t.getId());
    }

    /**
     * 处理{@link RuntimeException}, 子类可以定义自己的处理策略。
     *
     * @param e {@linkplain RuntimeException}
     */
    protected void handleUncheckedException(RuntimeException e) {
        if (handler != null) {
            handler.uncaughtException(Thread.currentThread(), e);
        }
    }

    @Override
    public void setUncaughtExceptionHandler(UncaughtExceptionHandler handler) {
        Objects.requireNonNull(handler);
        this.handler = handler;
    }

    @Override
    public final Future<Void> terminate() {
        delegate.checkTerminated(this);
        CompletableFuture<Void> future = new CompletableFuture<>();
        this.future = future;
        this.consumeLeft = true;
        this.state = State.TERMINATED;
        this.executor.shutdown();
        doTerminate();
        return future;
    }

    /**
     * 模板方法，允许子类执行自己的terminate逻辑，默认空实现.
     */
    protected void doTerminate() {
    }

    @Override
    public final Future<Void> terminateNow() {
        delegate.checkTerminated(this);
        CompletableFuture<Void> future = new CompletableFuture<>();
        this.future = future;
        this.state = State.TERMINATED;
        this.executor.shutdownNow();
        doTerminateNow();
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
        try {
            while (shouldConsume()) {
                task = getTask();
                if (task != null) {
                    doConsume(task);
                }
            }
            if (consumeLeft) {
                while ((task = getLeftTask()) != null) {
                    doConsume(task);
                }
            }
        } catch (InterruptedException e) {
            //exit...
        }
        if (future != null && !future.isDone()) {
            future.complete(null);
        }
    }

    /**
     * consume()辅助方法，捕获{@link RuntimeException}.
     */
    private void doConsume(T task) {
        try {
            consume(task);
        } catch (RuntimeException e) {
            handleUncheckedException(e);
        }
    }

    /**
     * 从工作队列中得到任务，由子类实现，此方法将会被run()调用.
     *
     * @return <T>
     * @throws InterruptedException 如果获取任务时被中断
     */
    protected abstract T getTask() throws InterruptedException;

    /**
     * 得到队列中剩余的任务，当terminate()方法被调用时执行.
     *
     * @return <T>
     */
    private T getLeftTask() {
        return jobQueue.poll();
    }

    /**
     * 检查当前是否可以继续消费.
     */
    private boolean shouldConsume() {
        return (this.state == State.RUNNING);
    }

    @Override
    public State getState() {
        return state;
    }

}
