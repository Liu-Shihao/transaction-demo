package com.banking.transaction.service;

import com.banking.transaction.exception.ResourceNotFoundException;
import com.banking.transaction.exception.TransactionException;
import com.banking.transaction.model.PaginatedResponse;
import com.banking.transaction.model.Transaction;
import com.banking.transaction.model.TransactionRequest;
import com.banking.transaction.repository.TransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Slf4j
public class VirtualThreadTransactionServiceImpl implements VirtualThreadTransactionService {
    
    private final TransactionRepository transactionRepository;
    private final CacheService cacheService;
    private final ExecutorService virtualThreadExecutor;
    
    public VirtualThreadTransactionServiceImpl(TransactionRepository transactionRepository, CacheService cacheService) {
        this.transactionRepository = transactionRepository;
        this.cacheService = cacheService;
        this.virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
        log.info("VirtualThreadTransactionServiceImpl initialized");
    }
    
    @Override
    public CompletableFuture<Transaction> createTransactionAsync(TransactionRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            Transaction transaction = new Transaction();
            transaction.setAccountNumber(request.getAccountNumber());
            transaction.setTransactionType(request.getTransactionType());
            transaction.setAmount(request.getAmount());
            transaction.setDescription(request.getDescription());
            return transactionRepository.save(transaction);
        }, virtualThreadExecutor);
    }
    
    @Override
    public CompletableFuture<List<Transaction>> createTransactionsBatchAsync(List<TransactionRequest> requests) {
        List<CompletableFuture<Transaction>> futures = requests.stream()
                .map(this::createTransactionAsync)
                .toList();
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .toList());
    }
    
    @Override
    public CompletableFuture<Transaction> getTransactionByIdAsync(UUID id) {
        return CompletableFuture.supplyAsync(() -> {
            return cacheService.getWithNullProtection(
                "transactions", 
                id.toString(), 
                () -> transactionRepository.findById(id).orElse(null)
            );
        }, virtualThreadExecutor);
    }
    
    @Override
    public CompletableFuture<PaginatedResponse<Transaction>> getAllTransactionsAsync(int page, int size) {
        return CompletableFuture.supplyAsync(() -> {
            List<Transaction> transactions = transactionRepository.findAll(page, size);
            long total = transactionRepository.count();
            return PaginatedResponse.of(transactions, page, size, total);
        }, virtualThreadExecutor);
    }
    
    @Override
    public CompletableFuture<PaginatedResponse<Transaction>> getTransactionsByAccountAsync(String accountNumber, int page, int size) {
        return CompletableFuture.supplyAsync(() -> {
            List<Transaction> transactions = transactionRepository.findByAccountNumber(accountNumber, page, size);
            long total = transactionRepository.countByAccountNumber(accountNumber);
            return PaginatedResponse.of(transactions, page, size, total);
        }, virtualThreadExecutor);
    }
    
    @Override
    public CompletableFuture<PaginatedResponse<Transaction>> getTransactionsByTypeAsync(String transactionType, int page, int size) {
        return CompletableFuture.supplyAsync(() -> {
            List<Transaction> transactions = transactionRepository.findByTransactionType(transactionType, page, size);
            long total = transactionRepository.countByTransactionType(transactionType);
            return PaginatedResponse.of(transactions, page, size, total);
        }, virtualThreadExecutor);
    }
    
    @Override
    public CompletableFuture<Transaction> updateTransactionAsync(UUID id, TransactionRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            // Check if transaction exists first
            Transaction existingTransaction = getTransactionByIdAsync(id).join();
            if (existingTransaction == null) {
                throw new ResourceNotFoundException("Transaction not found with id: " + id);
            }
            
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
            
            return transactionRepository.update(existingTransaction);
        }, virtualThreadExecutor);
    }
    
    @Override
    public CompletableFuture<Boolean> deleteTransactionAsync(UUID id) {
        return CompletableFuture.supplyAsync(() -> {
            return transactionRepository.deleteById(id);
        }, virtualThreadExecutor);
    }
    
    @Override
    public CompletableFuture<TransactionStatistics> getTransactionStatisticsAsync() {
        return CompletableFuture.supplyAsync(() -> {
            List<Transaction> allTransactions = transactionRepository.getAllTransactions();
            int totalTransactions = allTransactions.size();
            double totalAmount = allTransactions.stream()
                    .mapToDouble(t -> t.getAmount().doubleValue())
                    .sum();
            double averageAmount = totalTransactions > 0 ? totalAmount / totalTransactions : 0.0;
            return new TransactionStatistics(totalTransactions, totalAmount, averageAmount);
        }, virtualThreadExecutor);
    }
    
    @Override
    public CompletableFuture<List<Transaction>> searchTransactionsAsync(String accountNumber, String type, int page, int size) {
        return CompletableFuture.supplyAsync(() -> {
            List<Transaction> transactions = transactionRepository.findByAccountNumber(accountNumber, page, size);
            return transactions.stream().limit(size).toList();
        }, virtualThreadExecutor);
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
} 