package com.lsh.transaction.service;

import com.lsh.transaction.exception.ResourceNotFoundException;
import com.lsh.transaction.model.PaginatedResponse;
import com.lsh.transaction.model.Transaction;
import com.lsh.transaction.model.TransactionRequest;

import java.util.UUID;

/**
 * Service interface for transaction business logic.
 * This interface defines the core business operations for transaction management.
 */
public interface TransactionService {
    
    /**
     * Create a new transaction.
     * @param request the transaction creation request
     * @return the created transaction
     */
    Transaction createTransaction(TransactionRequest request);
    
    /**
     * Get a transaction by its ID.
     * @param id the transaction ID
     * @return the transaction if found
     * @throws ResourceNotFoundException if transaction not found
     */
    Transaction getTransactionById(UUID id);
    
    /**
     * Get all transactions with pagination.
     * @param page the page number (0-based)
     * @param size the page size
     * @return paginated response containing transactions
     */
    PaginatedResponse<Transaction> getAllTransactions(int page, int size);
    
    /**
     * Get transactions by account number with pagination.
     * @param accountNumber the account number
     * @param page the page number (0-based)
     * @param size the page size
     * @return paginated response containing transactions for the account
     */
    PaginatedResponse<Transaction> getTransactionsByAccount(String accountNumber, int page, int size);
    
    /**
     * Get transactions by transaction type with pagination.
     * @param transactionType the transaction type
     * @param page the page number (0-based)
     * @param size the page size
     * @return paginated response containing transactions for the type
     */
    PaginatedResponse<Transaction> getTransactionsByType(String transactionType, int page, int size);
    
    /**
     * Update an existing transaction.
     * @param id the transaction ID
     * @param request the transaction update request
     * @return the updated transaction
     * @throws ResourceNotFoundException if transaction not found
     */
    Transaction updateTransaction(UUID id, TransactionRequest request);
    
    /**
     * Delete a transaction by its ID.
     * @param id the transaction ID
     * @return true if the transaction was deleted
     * @throws ResourceNotFoundException if transaction not found
     */
    boolean deleteTransaction(UUID id);
    
    /**
     * Get transaction statistics.
     * @return transaction statistics
     */
    TransactionStatistics getTransactionStatistics();
} 