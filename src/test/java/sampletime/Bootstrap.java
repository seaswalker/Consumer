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
        SubmitableConsumer<String> consumer = new SimpleLockConsumer(1024);
        consumer.start();
        long begin = System.currentTimeMillis();
        Thread t1 = new Thread(new Inserter(consumer));
        //Thread t2 = new Thread(new Inserter(consumer));
        t1.start();
        //t2.start();
        t1.join();
        //t2.join();
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
            for (int i = 0; i < 10000000; i++) {
                consumer.submitSync("seed");
            }
        }
    }

    private static class Con extends Thread {

        private int i = 0;
        private final MpscArrayQueue<String> queue;

        public Con(MpscArrayQueue<String> queue) {
            this.queue = queue;
        }

        @Override
        public void run() {
            String s = null;
            for (; i < 2000000; ) {
                s = queue.poll();
                if (s != null) {
                    ++i;
                }
            }
        }

        public int get() {
            return i;
        }

    }

}
