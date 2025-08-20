// application/src/main/java/dk/gormkrings/config/ExecConfig.java
package dk.gormkrings.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class ExecConfig {

    // Single-worker queue that serializes simulations
    @Bean(name = "simExecutor", destroyMethod = "shutdown")
    public ThreadPoolExecutor simExecutor(
            @Value("${simulation.queue.capacity:100}") int queueCapacity
    ) {
        BlockingQueue<Runnable> q = new ArrayBlockingQueue<>(queueCapacity);
        ThreadFactory tf = new ThreadFactory() {
            private final AtomicInteger n = new AtomicInteger(1);
            @Override public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "sim-queue-" + n.getAndIncrement());
                t.setDaemon(true);
                return t;
            }
        };
        ThreadPoolExecutor exec = new ThreadPoolExecutor(
                1, 1, 0L, TimeUnit.MILLISECONDS, q, tf, new ThreadPoolExecutor.AbortPolicy()
        );
        exec.prestartAllCoreThreads();
        return exec;
    }

    // Scheduler for SSE/queue heartbeats
    @Bean(name = "sseScheduler", destroyMethod = "shutdown")
    public ScheduledExecutorService sseScheduler() {
        return Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "sse-heartbeat");
            t.setDaemon(true);
            return t;
        });
    }

    // CPU-bound pool used inside MonteCarloSimulation
    @Bean(name = "simWorkerPool", destroyMethod = "shutdown")
    public ExecutorService simWorkerPool(
            @Value("${simulation.parallelism:#{T(java.lang.Runtime).getRuntime().availableProcessors()}}")
            int parallelism
    ) {
        return new ThreadPoolExecutor(
                parallelism, parallelism,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),
                r -> { Thread t = new Thread(r, "sim-worker-" + r.hashCode()); t.setDaemon(true); return t; },
                new ThreadPoolExecutor.AbortPolicy()
        );
    }
}
