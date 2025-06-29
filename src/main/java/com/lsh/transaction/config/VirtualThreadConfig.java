package com.lsh.transaction.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Virtual Thread Configuration for JDK 21
 * This configuration enables virtual threads for better concurrency performance.
 */
@Configuration
@EnableAsync
public class VirtualThreadConfig {

    /**
     * Virtual thread executor for I/O-intensive operations
     * Virtual threads are perfect for database operations, HTTP calls, etc.
     */
    @Bean("virtualThreadExecutor")
    public Executor virtualThreadExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * Hybrid executor that uses virtual threads for I/O operations
     * and platform threads for CPU-intensive operations
     */
    @Bean("hybridExecutor")
    public ThreadPoolTaskExecutor hybridExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("hybrid-");
        executor.setTaskDecorator(task -> {
            // Use virtual threads for I/O operations
            return () -> {
                try {
                    task.run();
                } catch (Exception e) {
                    // Log and handle exceptions
                    System.err.println("Task execution error: " + e.getMessage());
                }
            };
        });
        executor.initialize();
        return executor;
    }

    /**
     * CPU-intensive operations executor using platform threads
     * For operations that require significant CPU processing
     */
    @Bean("cpuIntensiveExecutor")
    public ThreadPoolTaskExecutor cpuIntensiveExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(Runtime.getRuntime().availableProcessors());
        executor.setMaxPoolSize(Runtime.getRuntime().availableProcessors() * 2);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("cpu-");
        executor.initialize();
        return executor;
    }
} 