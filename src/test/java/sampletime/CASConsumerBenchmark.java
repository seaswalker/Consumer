package sampletime;

import consumer.SubmitableConsumer;
import org.openjdk.jmh.annotations.*;
import sampletime.consumer.SimpleCASConsumer;
import sampletime.consumer.SimpleLockConsumer;

import java.util.concurrent.ExecutionException;

/**
 * {@link SimpleLockConsumer}测试.
 *
 * @author skywalker
 */
@BenchmarkMode(Mode.SingleShotTime)
@State(Scope.Benchmark)
public class CASConsumerBenchmark {

    private SubmitableConsumer<String> consumer;
    private final String seed = "skywalker";

    @Setup
    public void init() {
        consumer = new SimpleCASConsumer(1024);
        consumer.start();
    }

    /**
     * 测试{@link SimpleLockConsumer}.
     */
    @Benchmark
    @Warmup(iterations = 3)
    @Measurement(iterations = 1, batchSize = 100000)
    @Threads(1)
    public void locked() {
        consumer.submitSync(seed);
    }

    @TearDown
    public void finish() throws ExecutionException, InterruptedException {
        long consumed = (Long) consumer.terminate().get();
        System.out.println("成功处理: " + consumed);
    }

}
