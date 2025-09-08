// application/src/main/java/dk/gormkrings/config/ExecConfig.java
package dk.gormkrings.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;

@Configuration
public class ExecConfig {

    // Single-worker queue that serializes simulations
    @Bean(name = "simExecutor", destroyMethod = "shutdown")
    public ExecutorService simExecutor(@Value("${simulation.queue.capacity:100}") int cap) {
        var ex = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(cap),
                r -> { var t = new Thread(r, "sim-queue"); t.setDaemon(true); return t; },
                new ThreadPoolExecutor.AbortPolicy());
        ex.prestartAllCoreThreads();
        return Executors.unconfigurableExecutorService(ex); // hides concrete type
    }

    // CPU-bound pool used inside MonteCarloSimulation
    @Bean(name = "simWorkerPool", destroyMethod = "shutdown")
    public ExecutorService simWorkerPool(@Value("${simulation.parallelism:#{T(java.lang.Runtime).getRuntime().availableProcessors()}}") int p) {
        return Executors.newFixedThreadPool(p, r -> { var t = new Thread(r, "sim-worker"); t.setDaemon(true); return t; });
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
}
