package com.lsh.transaction.repository;

import com.lsh.transaction.model.Transaction;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for transaction data access.
 * This interface defines methods for managing transaction data in memory.
 */
public interface TransactionRepository {
    
    /**
     * Save a new transaction.
     * @param transaction the transaction to save
     * @return the saved transaction
     */
    Transaction save(Transaction transaction);
    
    /**
     * Find a transaction by its ID.
     * @param id the transaction ID
     * @return Optional containing the transaction if found
     */
    Optional<Transaction> findById(UUID id);
    
    /**
     * Find all transactions with pagination.
     * @param page the page number (0-based)
     * @param size the page size
     * @return list of transactions for the specified page
     */
    List<Transaction> findAll(int page, int size);
    
    /**
     * Find transactions by account number with pagination.
     * @param accountNumber the account number to search for
     * @param page the page number (0-based)
     * @param size the page size
     * @return list of transactions for the specified account and page
     */
    List<Transaction> findByAccountNumber(String accountNumber, int page, int size);
    
    /**
     * Find transactions by transaction type with pagination.
     * @param transactionType the transaction type to search for
     * @param page the page number (0-based)
     * @param size the page size
     * @return list of transactions for the specified type and page
     */
    List<Transaction> findByTransactionType(String transactionType, int page, int size);
    
    /**
     * Update an existing transaction.
     * @param transaction the transaction to update
     * @return the updated transaction
     */
    Transaction update(Transaction transaction);
    
    /**
     * Delete a transaction by its ID.
     * @param id the transaction ID to delete
     * @return true if the transaction was deleted, false otherwise
     */
    boolean deleteById(UUID id);
    
    /**
     * Get the total count of all transactions.
     * @return total number of transactions
     */
    long count();
    
    /**
     * Get the total count of transactions for a specific account.
     * @param accountNumber the account number
     * @return total number of transactions for the account
     */
    long countByAccountNumber(String accountNumber);
    
    /**
     * Get the total count of transactions for a specific type.
     * @param transactionType the transaction type
     * @return total number of transactions for the type
     */
    long countByTransactionType(String transactionType);
    
    /**
     * Check if a transaction exists by its ID.
     * @param id the transaction ID
     * @return true if the transaction exists, false otherwise
     */
    boolean existsById(UUID id);
    
    /**
     * Get all transactions without pagination (useful for statistics).
     * @return list of all transactions
     */
    List<Transaction> getAllTransactions();
} 