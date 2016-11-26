package sampletime;

import consumer.SubmitableConsumer;
import org.openjdk.jmh.annotations.*;
import sampletime.consumer.SimpleLockConsumer;

import java.util.concurrent.ExecutionException;

/**
 * {@link SimpleLockConsumer}测试.
 *
 * @author skywalker
 */
@BenchmarkMode(Mode.SampleTime)
@State(Scope.Benchmark)
public class LockedConsumerBenchmark {

    private SubmitableConsumer<String> consumer;
    private final String seed = "skywalker";

    @Setup
    public void init() {
        consumer = new SimpleLockConsumer(1024);
        consumer.start();
    }

    /**
     * 测试{@link sampletime.consumer.SimpleLockConsumer}.
     */
    @Benchmark
    @Warmup(iterations = 10)
    @Measurement(iterations = 1, batchSize = 10000)
    @Threads(2)
    public void locked() {
        consumer.submitSync(seed);
    }

    @TearDown
    public void finish() throws ExecutionException, InterruptedException {
        long consumed = (Long) consumer.terminate().get();
        System.out.println("成功处理: " + consumed);
    }

}
