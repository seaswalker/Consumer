package logic;

import manager.AbstractLockedManager;
import manager.Manager;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
                return new SimpleConsumer(10, id);
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
        Future<Long> future = manager.terminate();
        System.out.println("Total consumed: " + future.get());
    }

}
