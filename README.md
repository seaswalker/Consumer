# Consumer

封装了常用的生产者-消费者模型，自定义消费者只需覆盖consume方法(关心逻辑实现)，支持有锁(有界)队列和无锁(有界队列)，无锁(CAS)队列支持阻塞、循环以及自旋策略。

## 示例

### 消费者

```java
public class SimpleConsumer extends AbstractCASConsumer<String> {
    public SimpleConsumer(int queueSize, int id) {
        super(queueSize, id);
    }
    @Override
    public void consume(String task) {
        System.out.println("Consumer " + id + ": ");
    }
}
```

## 逻辑

```java
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
```

