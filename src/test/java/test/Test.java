package test;

import org.openjdk.jmh.annotations.*;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 测试:
 * <p>volatile对性能的影响，结论: 没有此关键字的性能约为有的12倍.</p>
 * <p>对比测试volatile与cas的性能，结论: 基本一致.</p>
 * <p>以上结论在每次都执行写操作的基础上得出，在只读环境下，cas性能与无volatile, cas环境下写性能相当，
 * volatile强于cas.</p>
 *
 * @author skywalker
 */
@State(Scope.Thread)
public class Test {

    private volatile int vi = 0;

    private final int max = Integer.MAX_VALUE;
    private final AtomicInteger i = new AtomicInteger();

    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    public void noVolatile() {
        final int max = Integer.MAX_VALUE;
        int i = 0;
        while (i++ < max) {
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    public void cas() {
        final int max = Integer.MAX_VALUE;
        int i = 0;
        while (i++ < max) {
            if (i == this.i.get()) {
                System.out.println("");
            }
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    public void useVolatile() {
        final int max = Integer.MAX_VALUE;
        int i = 0;
        while (i++ < max) {
            if (i == vi) {
                System.out.println("");
            }
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    public void useVolatileAndMethod() {
        final int max = Integer.MAX_VALUE;
        while (shouldConsume(max)) {

        }
    }

    private boolean shouldConsume(int max) {
        return (vi++ < max);
    }

}
