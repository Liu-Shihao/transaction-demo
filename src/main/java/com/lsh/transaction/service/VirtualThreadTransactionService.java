package com.lsh.transaction.service;

import com.lsh.transaction.model.Transaction;
import com.lsh.transaction.model.TransactionRequest;
import com.lsh.transaction.model.PaginatedResponse;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface VirtualThreadTransactionService {

    /**
     * Create transaction using virtual threads for better I/O performance
     */
    CompletableFuture<Transaction> createTransactionAsync(TransactionRequest request);

    /**
     * Batch create transactions using virtual threads
     */
    CompletableFuture<List<Transaction>> createTransactionsBatchAsync(List<TransactionRequest> requests);

    /**
     * Get transaction with virtual thread for cache/database operations
     */
    CompletableFuture<Transaction> getTransactionByIdAsync(UUID id);

    /**
     * Get transaction statistics using CPU-intensive executor for calculations
     */
    CompletableFuture<TransactionStatistics> getTransactionStatisticsAsync();

    /**
     * Search transactions with virtual threads for parallel processing
     */
    CompletableFuture<List<Transaction>> searchTransactionsAsync(
            String accountNumber, String type, int page, int size);

    /**
     * Get all transactions with pagination using virtual threads
     */
    CompletableFuture<PaginatedResponse<Transaction>> getAllTransactionsAsync(int page, int size);

    /**
     * Get transactions by account number with pagination using virtual threads
     */
    CompletableFuture<PaginatedResponse<Transaction>> getTransactionsByAccountAsync(String accountNumber, int page, int size);

    /**
     * Get transactions by type with pagination using virtual threads
     */
    CompletableFuture<PaginatedResponse<Transaction>> getTransactionsByTypeAsync(String transactionType, int page, int size);

    /**
     * Update transaction using virtual threads
     */
    CompletableFuture<Transaction> updateTransactionAsync(UUID id, TransactionRequest request);

    /**
     * Delete transaction using virtual threads
     */
    CompletableFuture<Boolean> deleteTransactionAsync(UUID id);

    /**
     * Simple statistics class
     */
    class TransactionStatistics {
        private final int totalTransactions;
        private final double totalAmount;
        private final double averageAmount;

        public TransactionStatistics(int totalTransactions, double totalAmount, double averageAmount) {
            this.totalTransactions = totalTransactions;
            this.totalAmount = totalAmount;
            this.averageAmount = averageAmount;
        }

        // Getters
        public int getTotalTransactions() { return totalTransactions; }
        public double getTotalAmount() { return totalAmount; }
        public double getAverageAmount() { return averageAmount; }
    }
} 