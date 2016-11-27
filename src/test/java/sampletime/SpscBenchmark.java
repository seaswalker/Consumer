package sampletime;

import org.jctools.queues.MpscArrayQueue;
import org.jctools.queues.SpscArrayQueue;
import org.openjdk.jmh.annotations.*;
import sampletime.consumer.SimpleCASConsumer;
import sampletime.consumer.SimpleLockConsumer;

/**
 * 测试{@link org.jctools.queues.SpscArrayQueue}.
 *
 * @author skywalker
 */
@BenchmarkMode(Mode.SingleShotTime)
@State(Scope.Benchmark)
public class SpscBenchmark {

    private MpscArrayQueue<String> queue;
    private Consumer consumer;

    @Setup
    public void init() {
        consumer = new Consumer();
        queue = new MpscArrayQueue<>(1024);
        new Thread(consumer).start();
    }

    /**
     * 测试{@link SimpleLockConsumer}.
     */
    @Benchmark
    @Warmup(iterations = 3)
    @Measurement(iterations = 1, batchSize = 100000)
    @Threads(4)
    public void locked() {
        while (!queue.offer("seed"));
    }


    private class Consumer implements Runnable {

        @Override
        public void run() {
            /*int index = 0;
            while (index < 100000) {
                if (queue.poll() != null) {
                    ++index;
                }
            }*/
            while (true) {
                queue.poll();
            }
        }
    }

}
