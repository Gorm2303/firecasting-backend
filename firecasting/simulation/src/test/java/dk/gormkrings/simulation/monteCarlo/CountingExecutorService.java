package dk.gormkrings.simulation.monteCarlo;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CountingExecutorService extends ThreadPoolExecutor {
    private final AtomicInteger submitCount = new AtomicInteger(0);

    public CountingExecutorService(int nThreads) {
        super(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        submitCount.incrementAndGet();
        return super.submit(task);
    }

    public int getSubmitCount() {
        return submitCount.get();
    }
}