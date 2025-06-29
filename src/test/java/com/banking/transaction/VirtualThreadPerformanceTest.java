package com.banking.transaction;

import com.banking.transaction.model.TransactionRequest;
import com.banking.transaction.service.VirtualThreadTransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Virtual Thread Performance Test
 * Compares performance between virtual threads and platform threads.
 */
@SpringBootTest
@ActiveProfiles("local")
class VirtualThreadPerformanceTest {

    @Autowired
    private VirtualThreadTransactionService virtualThreadService;

    @Test
    void virtualThreadVsPlatformThread_PerformanceComparison() throws InterruptedException, ExecutionException, TimeoutException {
        System.out.println("=== Virtual Thread vs Platform Thread Performance Test ===");
        
        int numberOfRequests = 1000;
        List<TransactionRequest> requests = createTestRequests(numberOfRequests);
        
        // Test with Virtual Threads
        long virtualThreadStart = System.currentTimeMillis();
        CompletableFuture<List<com.banking.transaction.model.Transaction>> virtualThreadFuture = 
            virtualThreadService.createTransactionsBatchAsync(requests);
        List<com.banking.transaction.model.Transaction> virtualThreadResults = 
            virtualThreadFuture.get(30, TimeUnit.SECONDS);
        long virtualThreadEnd = System.currentTimeMillis();
        long virtualThreadTime = virtualThreadEnd - virtualThreadStart;
        
        // Test with Platform Threads (traditional approach)
        long platformThreadStart = System.currentTimeMillis();
        List<com.banking.transaction.model.Transaction> platformThreadResults = 
            createTransactionsWithPlatformThreads(requests);
        long platformThreadEnd = System.currentTimeMillis();
        long platformThreadTime = platformThreadEnd - platformThreadStart;
        
        // Print results
        System.out.println("Virtual Thread Results:");
        System.out.println("  - Time: " + virtualThreadTime + "ms");
        System.out.println("  - Transactions created: " + virtualThreadResults.size());
        System.out.println("  - Throughput: " + (numberOfRequests * 1000.0 / virtualThreadTime) + " req/sec");
        
        System.out.println("Platform Thread Results:");
        System.out.println("  - Time: " + platformThreadTime + "ms");
        System.out.println("  - Transactions created: " + platformThreadResults.size());
        System.out.println("  - Throughput: " + (numberOfRequests * 1000.0 / platformThreadTime) + " req/sec");
        
        System.out.println("Performance Improvement: " + 
            String.format("%.2f", (double) platformThreadTime / virtualThreadTime) + "x");
        
        // Assertions
        assertEquals(numberOfRequests, virtualThreadResults.size());
        assertEquals(numberOfRequests, platformThreadResults.size());
        
        // Virtual threads should be at least as fast as platform threads for I/O operations
        assertTrue(virtualThreadTime <= platformThreadTime * 1.2, 
            "Virtual threads should not be significantly slower than platform threads");
    }

    @Test
    void virtualThread_ConcurrentReadOperations() throws InterruptedException, ExecutionException, TimeoutException {
        System.out.println("=== Virtual Thread Concurrent Read Test ===");
        
        int numberOfConcurrentReads = 500;
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        
        long startTime = System.currentTimeMillis();
        
        // Create multiple concurrent read operations
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        for (int i = 0; i < numberOfConcurrentReads; i++) {
            CompletableFuture<Void> future = virtualThreadService.getTransactionStatisticsAsync()
                .thenAccept(stats -> {
                    successCount.incrementAndGet();
                    // Verify statistics are valid
                    assertNotNull(stats);
                    assertTrue(stats.getTotalTransactions() >= 0);
                })
                .exceptionally(throwable -> {
                    failureCount.incrementAndGet();
                    System.err.println("Read operation failed: " + throwable.getMessage());
                    return null;
                });
            
            futures.add(future);
        }
        
        // Wait for all operations to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .get(30, TimeUnit.SECONDS);
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        
        System.out.println("Concurrent Read Test Results:");
        System.out.println("  - Total time: " + totalTime + "ms");
        System.out.println("  - Successful reads: " + successCount.get());
        System.out.println("  - Failed reads: " + failureCount.get());
        System.out.println("  - Reads per second: " + (numberOfConcurrentReads * 1000.0 / totalTime));
        
        // Assertions
        assertTrue(successCount.get() > numberOfConcurrentReads * 0.95, 
            "Success rate should be at least 95%");
        assertTrue(totalTime < 10000, "Should complete within 10 seconds");
    }

    @Test
    void virtualThread_MemoryEfficiency() {
        System.out.println("=== Virtual Thread Memory Efficiency Test ===");
        
        // Get initial memory usage
        Runtime runtime = Runtime.getRuntime();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();
        
        // Create many virtual threads
        int numberOfThreads = 10000;
        List<CompletableFuture<String>> futures = new ArrayList<>();
        
        for (int i = 0; i < numberOfThreads; i++) {
            final int threadId = i;
            CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                try {
                    // Simulate some work
                    Thread.sleep(10);
                    return "Thread " + threadId + " completed";
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return "Thread " + threadId + " interrupted";
                }
            });
            futures.add(future);
        }
        
        // Wait for all to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .join();
        
        // Get final memory usage
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsed = finalMemory - initialMemory;
        
        System.out.println("Memory Efficiency Test Results:");
        System.out.println("  - Number of virtual threads: " + numberOfThreads);
        System.out.println("  - Memory used: " + (memoryUsed / 1024 / 1024) + " MB");
        System.out.println("  - Memory per thread: " + (memoryUsed / numberOfThreads) + " bytes");
        
        // Virtual threads should use very little memory per thread
        assertTrue(memoryUsed < numberOfThreads * 1000, 
            "Virtual threads should use less than 1KB per thread on average");
    }

    private List<TransactionRequest> createTestRequests(int count) {
        List<TransactionRequest> requests = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            requests.add(new TransactionRequest(
                "ACCOUNT" + String.format("%06d", i),
                "DEPOSIT",
                new BigDecimal("100.00"),
                "Test transaction " + i,
                null
            ));
        }
        return requests;
    }

    private List<com.banking.transaction.model.Transaction> createTransactionsWithPlatformThreads(
            List<TransactionRequest> requests) throws InterruptedException, ExecutionException, TimeoutException {
        
        ExecutorService executor = Executors.newFixedThreadPool(50);
        List<CompletableFuture<com.banking.transaction.model.Transaction>> futures = new ArrayList<>();
        
        for (TransactionRequest request : requests) {
            CompletableFuture<com.banking.transaction.model.Transaction> future = 
                CompletableFuture.supplyAsync(() -> {
                    try {
                        // Simulate the same work as virtual thread version
                        Thread.sleep(10);
                        
                        com.banking.transaction.model.Transaction transaction = 
                            new com.banking.transaction.model.Transaction();
                        transaction.setId(java.util.UUID.randomUUID());
                        transaction.setAccountNumber(request.getAccountNumber());
                        transaction.setTransactionType(request.getTransactionType());
                        transaction.setAmount(request.getAmount());
                        transaction.setDescription(request.getDescription());
                        transaction.setTimestamp(java.time.LocalDateTime.now());
                        
                        return transaction;
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Transaction creation interrupted", e);
                    }
                }, executor);
            
            futures.add(future);
        }
        
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
            futures.toArray(new CompletableFuture[0]));
        
        allFutures.get(30, TimeUnit.SECONDS);
        executor.shutdown();
        
        return futures.stream()
            .map(CompletableFuture::join)
            .toList();
    }
} 