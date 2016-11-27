package sampletime;

import consumer.SubmitableConsumer;
import org.jctools.queues.MpscArrayQueue;
import org.openjdk.jmh.annotations.Threads;
import sampletime.consumer.SimpleCASConsumer;
import sampletime.consumer.SimpleLockConsumer;

import java.util.concurrent.ExecutionException;

/**
 * 测试.
 *
 * @author skywalker
 */
public class Bootstrap {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        SubmitableConsumer<String> consumer = new SimpleCASConsumer(512);
        consumer.start();
        long begin = System.currentTimeMillis();
        Thread t1 = new Thread(new Inserter(consumer));
        Thread t2 = new Thread(new Inserter(consumer));
        Thread t3 = new Thread(new Inserter(consumer));
        Thread t4 = new Thread(new Inserter(consumer));
        Thread t5 = new Thread(new Inserter(consumer));
        Thread t6 = new Thread(new Inserter(consumer));
        Thread t7 = new Thread(new Inserter(consumer));
        t1.start();
        t2.start();
        t3.start();
        t4.start();
        t5.start();
        t6.start();
        t7.start();
        t1.join();
        t2.join();
        t3.join();
        t4.join();
        t5.join();
        t6.join();
        t7.join();
        long consumed = (Long) consumer.terminate().get();
        long end = System.currentTimeMillis();
        System.out.println("消费" + consumed + "个" + "，耗时: " + (end - begin) + "毫秒.");
    }

    private static class Inserter implements Runnable {

        private final SubmitableConsumer<String> consumer;

        public Inserter(SubmitableConsumer<String> consumer) {
            this.consumer = consumer;
        }

        @Override
        public void run() {
            for (int i = 0; i < 50000; i++) {
                consumer.submitSync("seed");
            }
        }
    }

}
