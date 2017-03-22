package test;

import consumer.Consumer;
import consumer.cas.strategy.BlockStrategy;
import consumer.manager.AbstractLockedManager;
import consumer.manager.Manager;
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
        class Producter implements Runnable {

            final int id;
            int index = 0;

            Producter(int id) {
                this.id = id;
            }

            @Override
            public void run() {
                for (int i = 0; i < 10; i++) {
                    manager.next().submit("Producter " + id + ":" + index++);
                }
            }
        }
        ExecutorService service = Executors.newFixedThreadPool(2);
        service.execute(new Producter(0));
        service.execute(new Producter(1));
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
        DefaultConsumerPool<String> pool = new DefaultConsumerPool<>(2, 2, 10, 20, message -> {
            System.out.println(message);
            counter.incrementAndGet();
        });
        pool.setRetryStrategy(new BlockStrategy<>());
        Assert.assertTrue(pool.start());
        class Producter implements Runnable {
            final int id;
            final Consumer<String> consumer;
            int index = 0;

            Producter(int id, Consumer<String> consumer) {
                this.id = id;
                this.consumer = consumer;
            }

            @Override
            public void run() {
                for (int i = 0; i < 10; i++) {
                    if (!consumer.submit("Producter " + id + ":" + index++)) {
                        System.out.println("丢了");
                    }
                }
            }
        }
        ExecutorService service = Executors.newFixedThreadPool(2);
        service.execute(new Producter(0, pool.acquire()));
        service.execute(new Producter(1, pool.acquire()));
        service.execute(new Producter(2, pool.acquire()));
        service.execute(new Producter(3, pool.acquire()));
        Thread.sleep(2000);
        service.shutdown();
        Future<Void> future = pool.terminate();
        future.get();
        System.out.println("Total consumed: " + counter);
    }

}
