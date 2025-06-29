package com.banking.transaction.repository;

import com.banking.transaction.model.Transaction;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory implementation of TransactionRepository.
 * This class provides thread-safe transaction storage using ConcurrentHashMap.
 */
@Repository
public class InMemoryTransactionRepository implements TransactionRepository {
    
    private final Map<UUID, Transaction> transactions = new ConcurrentHashMap<>();
    
    @Override
    public Transaction save(Transaction transaction) {
        if (transaction.getId() == null) {
            transaction.setId(UUID.randomUUID());
        }
        transactions.put(transaction.getId(), transaction);
        return transaction;
    }
    
    @Override
    public Optional<Transaction> findById(UUID id) {
        return Optional.ofNullable(transactions.get(id));
    }
    
    @Override
    public List<Transaction> findAll(int page, int size) {
        return transactions.values().stream()
                .sorted(Comparator.comparing(Transaction::getTimestamp).reversed())
                .skip((long) page * size)
                .limit(size)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Transaction> findByAccountNumber(String accountNumber, int page, int size) {
        return transactions.values().stream()
                .filter(t -> accountNumber.equals(t.getAccountNumber()))
                .sorted(Comparator.comparing(Transaction::getTimestamp).reversed())
                .skip((long) page * size)
                .limit(size)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Transaction> findByTransactionType(String transactionType, int page, int size) {
        return transactions.values().stream()
                .filter(t -> transactionType.equals(t.getTransactionType()))
                .sorted(Comparator.comparing(Transaction::getTimestamp).reversed())
                .skip((long) page * size)
                .limit(size)
                .collect(Collectors.toList());
    }
    
    @Override
    public Transaction update(Transaction transaction) {
        if (!transactions.containsKey(transaction.getId())) {
            throw new IllegalArgumentException("Transaction with ID " + transaction.getId() + " does not exist");
        }
        transactions.put(transaction.getId(), transaction);
        return transaction;
    }
    
    @Override
    public boolean deleteById(UUID id) {
        return transactions.remove(id) != null;
    }
    
    @Override
    public long count() {
        return transactions.size();
    }
    
    @Override
    public long countByAccountNumber(String accountNumber) {
        return transactions.values().stream()
                .filter(t -> accountNumber.equals(t.getAccountNumber()))
                .count();
    }
    
    @Override
    public long countByTransactionType(String transactionType) {
        return transactions.values().stream()
                .filter(t -> transactionType.equals(t.getTransactionType()))
                .count();
    }
    
    @Override
    public boolean existsById(UUID id) {
        return transactions.containsKey(id);
    }
    
    /**
     * Clear all transactions (useful for testing).
     */
    public void clear() {
        transactions.clear();
    }
    
    /**
     * Get all transactions without pagination (useful for testing).
     */
    public List<Transaction> getAllTransactions() {
        return new ArrayList<>(transactions.values());
    }
} 