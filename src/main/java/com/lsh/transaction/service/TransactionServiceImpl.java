package com.lsh.transaction.service;

import com.lsh.transaction.exception.ResourceNotFoundException;
import com.lsh.transaction.exception.TransactionException;
import com.lsh.transaction.model.PaginatedResponse;
import com.lsh.transaction.model.Transaction;
import com.lsh.transaction.model.TransactionRequest;
import com.lsh.transaction.repository.TransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class TransactionServiceImpl implements TransactionService {
    
    private final TransactionRepository transactionRepository;
    private final CacheService cacheService;
    
    public TransactionServiceImpl(TransactionRepository transactionRepository, CacheService cacheService) {
        this.transactionRepository = transactionRepository;
        this.cacheService = cacheService;
        log.info("TransactionServiceImpl initialized with repository and cache service");
    }
    
    @Override
    public Transaction createTransaction(TransactionRequest request) {
        long startTime = System.currentTimeMillis();
        log.info("Creating transaction - Account: {}, Amount: {}, Type: {}", 
                request.getAccountNumber(), request.getAmount(), request.getTransactionType());
        
        try {
            // Validate business rules
            validateTransactionRequest(request);
            
            // Create new transaction
            Transaction transaction = new Transaction();
            transaction.setAccountNumber(request.getAccountNumber());
            transaction.setTransactionType(request.getTransactionType());
            transaction.setAmount(request.getAmount());
            transaction.setDescription(request.getDescription());
            transaction.setRecipientAccount(request.getRecipientAccount());
            
            // Save transaction first
            Transaction savedTransaction = transactionRepository.save(transaction);
            
            // Clear related caches after creation
            clearRelatedCaches(savedTransaction.getId(), request.getAccountNumber());
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("Transaction created successfully - ID: {}, Account: {}, Duration: {}ms", 
                    savedTransaction.getId(), savedTransaction.getAccountNumber(), duration);
            
            return savedTransaction;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Failed to create transaction - Account: {}, Duration: {}ms, Error: {}", 
                    request.getAccountNumber(), duration, e.getMessage(), e);
            throw e;
        }
    }
    
    @Override
    public Transaction getTransactionById(UUID id) {
        long startTime = System.currentTimeMillis();
        log.info("Retrieving transaction by ID: {}", id);
        
        try {
            // Use enhanced cache service with null protection
            Transaction transaction = cacheService.getWithNullProtection(
                "transactions", 
                id.toString(), 
                () -> transactionRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id))
            );
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("Transaction retrieved successfully - ID: {}, Account: {}, Duration: {}ms", 
                    id, transaction.getAccountNumber(), duration);
            
            return transaction;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Failed to retrieve transaction - ID: {}, Duration: {}ms, Error: {}", 
                    id, duration, e.getMessage(), e);
            throw e;
        }
    }
    
    @Override
    public PaginatedResponse<Transaction> getAllTransactions(int page, int size) {
        long startTime = System.currentTimeMillis();
        log.info("Retrieving all transactions - Page: {}, Size: {}", page, size);
        
        try {
            validatePagination(page, size);
            
            String cacheKey = "all_" + page + "_" + size;
            PaginatedResponse<Transaction> response = cacheService.getWithNullProtection(
                "transactions",
                cacheKey,
                () -> {
                    List<Transaction> transactions = transactionRepository.findAll(page, size);
                    long totalElements = transactionRepository.count();
                    return PaginatedResponse.of(transactions, page, size, totalElements);
                }
            );
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("All transactions retrieved successfully - Page: {}, Size: {}, Total: {}, Duration: {}ms", 
                    page, size, response.getTotalElements(), duration);
            
            return response;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Failed to retrieve all transactions - Page: {}, Size: {}, Duration: {}ms, Error: {}", 
                    page, size, duration, e.getMessage(), e);
            throw e;
        }
    }
    
    @Override
    public PaginatedResponse<Transaction> getTransactionsByAccount(String accountNumber, int page, int size) {
        long startTime = System.currentTimeMillis();
        log.info("Retrieving transactions by account - Account: {}, Page: {}, Size: {}", accountNumber, page, size);
        
        try {
            validatePagination(page, size);
            validateAccountNumber(accountNumber);
            
            String cacheKey = "account_" + accountNumber + "_" + page + "_" + size;
            PaginatedResponse<Transaction> response = cacheService.getWithNullProtection(
                "transactions",
                cacheKey,
                () -> {
                    List<Transaction> transactions = transactionRepository.findByAccountNumber(accountNumber, page, size);
                    long totalElements = transactionRepository.countByAccountNumber(accountNumber);
                    return PaginatedResponse.of(transactions, page, size, totalElements);
                }
            );
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("Account transactions retrieved successfully - Account: {}, Page: {}, Size: {}, Total: {}, Duration: {}ms", 
                    accountNumber, page, size, response.getTotalElements(), duration);
            
            return response;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Failed to retrieve account transactions - Account: {}, Page: {}, Size: {}, Duration: {}ms, Error: {}", 
                    accountNumber, page, size, duration, e.getMessage(), e);
            throw e;
        }
    }
    
    @Override
    public PaginatedResponse<Transaction> getTransactionsByType(String transactionType, int page, int size) {
        long startTime = System.currentTimeMillis();
        log.info("Retrieving transactions by type - Type: {}, Page: {}, Size: {}", transactionType, page, size);
        
        try {
            validatePagination(page, size);
            validateTransactionType(transactionType);
            
            String cacheKey = "type_" + transactionType + "_" + page + "_" + size;
            PaginatedResponse<Transaction> response = cacheService.getWithNullProtection(
                "transactions",
                cacheKey,
                () -> {
                    List<Transaction> transactions = transactionRepository.findByTransactionType(transactionType, page, size);
                    long totalElements = transactionRepository.countByTransactionType(transactionType);
                    return PaginatedResponse.of(transactions, page, size, totalElements);
                }
            );
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("Type transactions retrieved successfully - Type: {}, Page: {}, Size: {}, Total: {}, Duration: {}ms", 
                    transactionType, page, size, response.getTotalElements(), duration);
            
            return response;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Failed to retrieve type transactions - Type: {}, Page: {}, Size: {}, Duration: {}ms, Error: {}", 
                    transactionType, page, size, duration, e.getMessage(), e);
            throw e;
        }
    }
    
    @Override
    public Transaction updateTransaction(UUID id, TransactionRequest request) {
        long startTime = System.currentTimeMillis();
        log.info("Updating transaction - ID: {}, Account: {}, Amount: {}", 
                id, request.getAccountNumber(), request.getAmount());
        
        try {
            // Check if transaction exists first
            Transaction existingTransaction = getTransactionById(id);
            
            // Validate business rules
            validateTransactionRequest(request);
            
            // Update transaction fields
            existingTransaction.setAccountNumber(request.getAccountNumber());
            existingTransaction.setTransactionType(request.getTransactionType());
            existingTransaction.setAmount(request.getAmount());
            existingTransaction.setDescription(request.getDescription());
            existingTransaction.setRecipientAccount(request.getRecipientAccount());
            
            // Update timestamp
            existingTransaction.setTimestamp(java.time.LocalDateTime.now());
            
            // Save updated transaction first
            Transaction updatedTransaction = transactionRepository.update(existingTransaction);
            
            // Clear related caches and update specific cache
            clearRelatedCaches(id, request.getAccountNumber());
            cacheService.atomicUpdate("transactions", id.toString(), updatedTransaction);
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("Transaction updated successfully - ID: {}, Account: {}, Duration: {}ms", 
                    id, updatedTransaction.getAccountNumber(), duration);
            
            return updatedTransaction;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Failed to update transaction - ID: {}, Duration: {}ms, Error: {}", 
                    id, duration, e.getMessage(), e);
            throw e;
        }
    }
    
    @Override
    public boolean deleteTransaction(UUID id) {
        long startTime = System.currentTimeMillis();
        log.info("Deleting transaction - ID: {}", id);
        
        try {
            // Check if transaction exists first
            Transaction existingTransaction = getTransactionById(id);
            
            // Delete transaction first
            boolean deleted = transactionRepository.deleteById(id);
            
            if (deleted) {
                // Clear related caches after deletion
                clearRelatedCaches(id, existingTransaction.getAccountNumber());
            }
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("Transaction deletion completed - ID: {}, Success: {}, Duration: {}ms", 
                    id, deleted, duration);
            
            return deleted;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Failed to delete transaction - ID: {}, Duration: {}ms, Error: {}", 
                    id, duration, e.getMessage(), e);
            throw e;
        }
    }
    
    @Override
    public TransactionStatistics getTransactionStatistics() {
        long startTime = System.currentTimeMillis();
        log.info("Retrieving transaction statistics");
        
        try {
            TransactionStatistics statistics = cacheService.getWithNullProtection(
                "transactionStats",
                "all",
                () -> {
                    List<Transaction> allTransactions = transactionRepository.getAllTransactions();
                    return calculateStatistics(allTransactions);
                }
            );
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("Transaction statistics retrieved successfully - Total: {}, TotalAmount: {}, Duration: {}ms", 
                    statistics.getTotalTransactions(), statistics.getTotalAmount(), duration);
            
            return statistics;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Failed to retrieve transaction statistics - Duration: {}ms, Error: {}", 
                    duration, e.getMessage(), e);
            throw e;
        }
    }
    
    // Private helper methods with logging
    
    private void validateTransactionRequest(TransactionRequest request) {
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            log.error("Invalid transaction amount: {}", request.getAmount());
            throw new TransactionException("Transaction amount must be greater than zero", "INVALID_AMOUNT");
        }
        
        if (request.getAccountNumber() == null || request.getAccountNumber().trim().isEmpty()) {
            log.error("Invalid account number: {}", request.getAccountNumber());
            throw new TransactionException("Account number is required", "INVALID_ACCOUNT");
        }
        
        if (request.getTransactionType() == null) {
            log.error("Invalid transaction type: {}", request.getTransactionType());
            throw new TransactionException("Transaction type is required", "INVALID_TYPE");
        }
        
        log.info("Transaction request validation passed - Account: {}, Amount: {}, Type: {}", 
                request.getAccountNumber(), request.getAmount(), request.getTransactionType());
    }
    
    private void validatePagination(int page, int size) {
        if (page < 0) {
            log.error("Invalid page number: {}", page);
            throw new IllegalArgumentException("Page number must be non-negative");
        }
        
        if (size <= 0 || size > 1000) {
            log.error("Invalid page size: {}", size);
            throw new IllegalArgumentException("Page size must be between 1 and 1000");
        }
    }
    
    private void validateAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            log.error("Invalid account number: {}", accountNumber);
            throw new IllegalArgumentException("Account number cannot be null or empty");
        }
    }
    
    private void validateTransactionType(String transactionType) {
        if (transactionType == null || transactionType.trim().isEmpty()) {
            log.error("Invalid transaction type: {}", transactionType);
            throw new IllegalArgumentException("Transaction type cannot be null or empty");
        }
    }
    
    private void clearRelatedCaches(UUID transactionId, String accountNumber) {
        try {
            // Clear specific transaction cache
            cacheService.conditionalEvict("transactions", transactionId.toString(), true);
            
            // Clear account-related caches
            cacheService.conditionalEvict("transactions", "account_" + accountNumber, true);
            
            // Clear statistics cache
            cacheService.conditionalEvict("transactionStats", "all", true);
            
            log.info("Related caches cleared - Transaction ID: {}, Account: {}", transactionId, accountNumber);
        } catch (Exception e) {
            log.error("Failed to clear related caches - Transaction ID: {}, Account: {}, Error: {}", 
                    transactionId, accountNumber, e.getMessage(), e);
        }
    }
    
    private TransactionStatistics calculateStatistics(List<Transaction> transactions) {
        long totalTransactions = transactions.size();
        BigDecimal totalAmount = transactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        double averageAmount = totalTransactions > 0 ? 
                totalAmount.doubleValue() / totalTransactions : 0.0;
        
        log.info("Statistics calculated - Total: {}, TotalAmount: {}, Average: {}", 
                totalTransactions, totalAmount, averageAmount);
        
        // Create empty maps for the additional parameters
        Map<String, Long> transactionsByType = new java.util.HashMap<>();
        Map<String, BigDecimal> amountByType = new java.util.HashMap<>();
        
        return new TransactionStatistics(totalTransactions, totalAmount, 
                transactionsByType, amountByType, 0L, totalTransactions, 0L, 0L);
    }
} 