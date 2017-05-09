package test;

import consumer.Consumer;
import consumer.cas.strategy.BlockStrategy;
import consumer.manager.AbstractLockedManager;
import consumer.manager.Manager;
import consumer.pool.ConsumeAction;
import org.junit.Assert;
import org.junit.Test;
import consumer.pool.DefaultConsumerPool;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 测试逻辑的正确性.
 *
 * @author skywalker
 */
public class LogicTest {

    /**
     * 测试CAS消费者.
     */
    @Test
    public void cas() throws ExecutionException, InterruptedException {
        Manager<SimpleConsumer> manager = new AbstractLockedManager<SimpleConsumer>() {
            @Override
            public SimpleConsumer newSlaver(int id) {
                return new SimpleConsumer(10);
            }

            @Override
            public int count() {
                return 2;
            }

        };
        Assert.assertTrue(manager.start());
        class Producer implements Runnable {

            final int id;
            int index = 0;

            private Producer(int id) {
                this.id = id;
            }

            @Override
            public void run() {
                for (int i = 0; i < 10; i++) {
                    manager.next().submit("Producer " + id + ":" + index++);
                }
            }
        }
        ExecutorService service = Executors.newFixedThreadPool(2);
        service.execute(new Producer(0));
        service.execute(new Producer(1));
        service.shutdown();
        Future<Void> future = manager.terminate();
        future.get();
        System.out.println("Total consumed: " + future.get());
    }

    /**
     * 测试消费者池.
     */
    @Test
    public void pool() throws ExecutionException, InterruptedException {
        AtomicLong counter = new AtomicLong();
        ConsumeAction<String> action = message -> {
            System.out.println(message);
            counter.incrementAndGet();
        };
        DefaultConsumerPool<String> pool = new DefaultConsumerPool<>(false, 3, 2, 64, () -> action);
        pool.setRetryStrategy(new BlockStrategy<>());
        Assert.assertTrue(pool.start());
        class Producer implements Runnable {
            final int id;
            final Consumer<String> consumer;
            int index = 0;

            Producer(int id, Consumer<String> consumer) {
                this.id = id;
                this.consumer = consumer;
            }

            @Override
            public void run() {
                try {
                    for (int i = 0; i < 10; i++) {
                        consumer.submitSync("Producer " + id + ":" + index++);
                    }
                } catch (InterruptedException e) {}
            }
        }
        ExecutorService service = Executors.newFixedThreadPool(2);
        service.execute(new Producer(0, pool.acquire()));
        service.execute(new Producer(1, pool.acquire()));
        service.execute(new Producer(2, pool.acquire()));
        service.execute(new Producer(3, pool.acquire()));
        Thread.sleep(2000);
        service.shutdown();
        Future<Void> future = pool.terminate();
        future.get();
        System.out.println("Total consumed: " + counter);
    }

}
