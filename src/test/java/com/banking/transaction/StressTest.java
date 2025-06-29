package com.banking.transaction;

import com.banking.transaction.model.TransactionRequest;
import com.banking.transaction.service.TransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("local")
class StressTest {
    
    @Autowired
    private TransactionService transactionService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    void concurrentTransactionCreation_ShouldHandleHighLoad() throws InterruptedException {
        // Arrange
        int numberOfThreads = 50;
        int transactionsPerThread = 20;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        
        long startTime = System.currentTimeMillis();
        
        // Act
        for (int i = 0; i < numberOfThreads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < transactionsPerThread; j++) {
                        try {
                            TransactionRequest request = new TransactionRequest(
                                    "ACCOUNT" + String.format("%03d", threadId) + "THREAD" + String.format("%03d", j),
                                    "DEPOSIT",
                                    new BigDecimal("100.00"),
                                    "Stress test transaction " + j,
                                    null
                            );
                            
                            transactionService.createTransaction(request);
                            successCount.incrementAndGet();
                            
                            // Small delay to simulate real-world scenario
                            Thread.sleep(10);
                        } catch (Exception e) {
                            failureCount.incrementAndGet();
                            System.err.println("Error in thread " + threadId + ": " + e.getMessage());
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // Wait for all threads to complete
        latch.await(60, TimeUnit.SECONDS);
        executor.shutdown();
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        
        // Assert
        int totalExpected = numberOfThreads * transactionsPerThread;
        int totalActual = successCount.get() + failureCount.get();
        
        System.out.println("Stress Test Results:");
        System.out.println("Total time: " + totalTime + "ms");
        System.out.println("Successful transactions: " + successCount.get());
        System.out.println("Failed transactions: " + failureCount.get());
        System.out.println("Total transactions: " + totalActual);
        System.out.println("Transactions per second: " + (totalActual * 1000.0 / totalTime));
        
        // Verify that most transactions succeeded
        assertTrue(successCount.get() > totalExpected * 0.95, 
                "Success rate should be at least 95%. Actual: " + 
                (successCount.get() * 100.0 / totalExpected) + "%");
        
        // Verify reasonable performance (at least 100 transactions per second)
        double tps = totalActual * 1000.0 / totalTime;
        assertTrue(tps > 100, "Should handle at least 100 transactions per second. Actual: " + tps);
    }
    
    @Test
    void concurrentReadOperations_ShouldHandleHighLoad() throws InterruptedException {
        // Arrange
        int numberOfThreads = 100;
        int readsPerThread = 50;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        
        // Create some test data first
        createTestData(100);
        
        long startTime = System.currentTimeMillis();
        
        // Act
        for (int i = 0; i < numberOfThreads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < readsPerThread; j++) {
                        try {
                            // Perform different read operations
                            switch (j % 4) {
                                case 0:
                                    transactionService.getAllTransactions(0, 10);
                                    break;
                                case 1:
                                    transactionService.getTransactionsByAccount("ACCOUNT000000000", 0, 10);
                                    break;
                                case 2:
                                    transactionService.getTransactionsByType("DEPOSIT", 0, 10);
                                    break;
                                case 3:
                                    transactionService.getTransactionStatistics();
                                    break;
                            }
                            successCount.incrementAndGet();
                        } catch (Exception e) {
                            failureCount.incrementAndGet();
                            System.err.println("Error in read thread " + threadId + ": " + e.getMessage());
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // Wait for all threads to complete
        latch.await(60, TimeUnit.SECONDS);
        executor.shutdown();
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        
        // Assert
        int totalExpected = numberOfThreads * readsPerThread;
        int totalActual = successCount.get() + failureCount.get();
        
        System.out.println("Read Stress Test Results:");
        System.out.println("Total time: " + totalTime + "ms");
        System.out.println("Successful reads: " + successCount.get());
        System.out.println("Failed reads: " + failureCount.get());
        System.out.println("Total reads: " + totalActual);
        System.out.println("Reads per second: " + (totalActual * 1000.0 / totalTime));
        
        // Verify that most reads succeeded
        assertTrue(successCount.get() > totalExpected * 0.99, 
                "Read success rate should be at least 99%. Actual: " + 
                (successCount.get() * 100.0 / totalExpected) + "%");
        
        // Verify reasonable performance (at least 500 reads per second)
        double rps = totalActual * 1000.0 / totalTime;
        assertTrue(rps > 500, "Should handle at least 500 reads per second. Actual: " + rps);
    }
    
    @Test
    void mixedOperations_ShouldHandleHighLoad() throws InterruptedException {
        // Arrange
        int numberOfThreads = 30;
        int operationsPerThread = 30;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        
        // Create some test data first
        createTestData(50);
        
        long startTime = System.currentTimeMillis();
        
        // Act
        for (int i = 0; i < numberOfThreads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        try {
                            // Perform different operations
                            switch (j % 5) {
                                case 0:
                                    // Create transaction
                                    TransactionRequest request = new TransactionRequest(
                                            "ACCOUNT" + String.format("%03d", threadId) + "MIXED" + String.format("%03d", j),
                                            "DEPOSIT",
                                            new BigDecimal("100.00"),
                                            "Mixed test transaction " + j,
                                            null
                                    );
                                    transactionService.createTransaction(request);
                                    break;
                                case 1:
                                    // Read all transactions
                                    transactionService.getAllTransactions(0, 10);
                                    break;
                                case 2:
                                    // Read by account
                                    transactionService.getTransactionsByAccount("ACCOUNT000000000", 0, 10);
                                    break;
                                case 3:
                                    // Read by type
                                    transactionService.getTransactionsByType("DEPOSIT", 0, 10);
                                    break;
                                case 4:
                                    // Get statistics
                                    transactionService.getTransactionStatistics();
                                    break;
                            }
                            successCount.incrementAndGet();
                            
                            // Small delay
                            Thread.sleep(5);
                        } catch (Exception e) {
                            failureCount.incrementAndGet();
                            System.err.println("Error in mixed thread " + threadId + ": " + e.getMessage());
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // Wait for all threads to complete
        latch.await(60, TimeUnit.SECONDS);
        executor.shutdown();
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        
        // Assert
        int totalExpected = numberOfThreads * operationsPerThread;
        int totalActual = successCount.get() + failureCount.get();
        
        System.out.println("Mixed Operations Stress Test Results:");
        System.out.println("Total time: " + totalTime + "ms");
        System.out.println("Successful operations: " + successCount.get());
        System.out.println("Failed operations: " + failureCount.get());
        System.out.println("Total operations: " + totalActual);
        System.out.println("Operations per second: " + (totalActual * 1000.0 / totalTime));
        
        // Verify that most operations succeeded
        assertTrue(successCount.get() > totalExpected * 0.95, 
                "Success rate should be at least 95%. Actual: " + 
                (successCount.get() * 100.0 / totalExpected) + "%");
        
        // Verify reasonable performance (at least 200 operations per second)
        double ops = totalActual * 1000.0 / totalTime;
        assertTrue(ops > 200, "Should handle at least 200 operations per second. Actual: " + ops);
    }
    
    private void createTestData(int count) {
        for (int i = 0; i < count; i++) {
            try {
                TransactionRequest request = new TransactionRequest(
                        "ACCOUNT" + String.format("%03d", i),
                        i % 3 == 0 ? "DEPOSIT" : (i % 3 == 1 ? "WITHDRAWAL" : "TRANSFER"),
                        new BigDecimal("100.00"),
                        "Test transaction " + i,
                        i % 3 == 2 ? "RECIPIENT" + String.format("%03d", i) + "000000" : null
                );
                transactionService.createTransaction(request);
            } catch (Exception e) {
                // Ignore errors in test data creation
            }
        }
    }
} 