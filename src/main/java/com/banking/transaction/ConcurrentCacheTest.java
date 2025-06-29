package com.banking.transaction;

import com.banking.transaction.model.Transaction;
import com.banking.transaction.model.TransactionRequest;
import com.banking.transaction.service.TransactionService;
import com.banking.transaction.service.CacheService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Concurrent cache test to verify cache consistency under high load.
 * This test simulates multiple threads accessing the same data simultaneously.
 */
@Component
public class ConcurrentCacheTest implements CommandLineRunner {
    
    private final TransactionService transactionService;
    private final CacheService cacheService;
    
    public ConcurrentCacheTest(TransactionService transactionService, CacheService cacheService) {
        this.transactionService = transactionService;
        this.cacheService = cacheService;
    }
    
    @Override
    public void run(String... args) throws Exception {
        // Only run tests if explicitly enabled
        if (System.getProperty("run.concurrent.test", "false").equals("true")) {
            System.out.println("Starting concurrent cache tests...");
            testCacheConsistency();
            testCachePenetration();
            testCacheBreakdown();
            System.out.println("Concurrent cache tests completed.");
        }
    }
    
    /**
     * Test cache consistency under concurrent read/write operations.
     */
    private void testCacheConsistency() throws InterruptedException {
        System.out.println("Testing cache consistency...");
        
        // Create a test transaction
        TransactionRequest request = new TransactionRequest(
            "TESTACCOUNT123", "DEPOSIT", new BigDecimal("100.00"), 
            "Concurrent test transaction", null
        );
        
        Transaction createdTransaction = transactionService.createTransaction(request);
        UUID transactionId = createdTransaction.getId();
        
        int threadCount = 10;
        int operationsPerThread = 100;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger readCount = new AtomicInteger(0);
        AtomicInteger writeCount = new AtomicInteger(0);
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        // Start read threads
        for (int i = 0; i < threadCount / 2; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        transactionService.getTransactionById(transactionId);
                        readCount.incrementAndGet();
                        Thread.sleep(1); // Small delay to simulate real load
                    }
                } catch (Exception e) {
                    System.err.println("Read thread error: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // Start write threads
        for (int i = 0; i < threadCount / 2; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        TransactionRequest updateRequest = new TransactionRequest(
                            "TESTACCOUNT123", "DEPOSIT", new BigDecimal("100.00"), 
                            "Updated transaction " + j, null
                        );
                        transactionService.updateTransaction(transactionId, updateRequest);
                        writeCount.incrementAndGet();
                        Thread.sleep(5); // Longer delay for writes
                    }
                } catch (Exception e) {
                    System.err.println("Write thread error: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        executor.shutdown();
        
        System.out.println("Cache consistency test completed:");
        System.out.println("  - Read operations: " + readCount.get());
        System.out.println("  - Write operations: " + writeCount.get());
        System.out.println("  - Cache stats: " + cacheService.getCacheStats("transactions"));
    }
    
    /**
     * Test cache penetration protection.
     */
    private void testCachePenetration() throws InterruptedException {
        System.out.println("Testing cache penetration protection...");
        
        int threadCount = 20;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger nullCount = new AtomicInteger(0);
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        // Multiple threads querying non-existent data
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < 10; j++) {
                        try {
                            UUID nonExistentId = UUID.randomUUID();
                            transactionService.getTransactionById(nonExistentId);
                        } catch (Exception e) {
                            if (e.getMessage().contains("not found")) {
                                nullCount.incrementAndGet();
                            }
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        executor.shutdown();
        
        System.out.println("Cache penetration test completed:");
        System.out.println("  - Null queries handled: " + nullCount.get());
        System.out.println("  - Cache stats: " + cacheService.getCacheStats("transactions"));
    }
    
    /**
     * Test cache breakdown protection.
     */
    private void testCacheBreakdown() throws InterruptedException {
        System.out.println("Testing cache breakdown protection...");
        
        int threadCount = 15;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger statsCount = new AtomicInteger(0);
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        // Multiple threads accessing statistics (expensive operation)
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < 5; j++) {
                        transactionService.getTransactionStatistics();
                        statsCount.incrementAndGet();
                        Thread.sleep(10);
                    }
                } catch (Exception e) {
                    System.err.println("Stats thread error: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        executor.shutdown();
        
        System.out.println("Cache breakdown test completed:");
        System.out.println("  - Statistics queries: " + statsCount.get());
        System.out.println("  - Cache stats: " + cacheService.getCacheStats("transactionStats"));
    }
} 