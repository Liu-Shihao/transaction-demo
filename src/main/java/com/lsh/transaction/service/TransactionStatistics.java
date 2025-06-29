package com.banking.transaction.service;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Statistics class for transaction data.
 * This class provides aggregated information about transactions.
 */
public class TransactionStatistics {
    
    private long totalTransactions;
    private BigDecimal totalAmount;
    private Map<String, Long> transactionsByType;
    private Map<String, BigDecimal> amountByType;
    private long pendingTransactions;
    private long completedTransactions;
    private long failedTransactions;
    private long cancelledTransactions;
    
    // Default constructor
    public TransactionStatistics() {}
    
    // Constructor with all fields
    public TransactionStatistics(long totalTransactions, BigDecimal totalAmount, 
                                Map<String, Long> transactionsByType, Map<String, BigDecimal> amountByType,
                                long pendingTransactions, long completedTransactions, 
                                long failedTransactions, long cancelledTransactions) {
        this.totalTransactions = totalTransactions;
        this.totalAmount = totalAmount;
        this.transactionsByType = transactionsByType;
        this.amountByType = amountByType;
        this.pendingTransactions = pendingTransactions;
        this.completedTransactions = completedTransactions;
        this.failedTransactions = failedTransactions;
        this.cancelledTransactions = cancelledTransactions;
    }
    
    // Getters and Setters
    public long getTotalTransactions() {
        return totalTransactions;
    }
    
    public void setTotalTransactions(long totalTransactions) {
        this.totalTransactions = totalTransactions;
    }
    
    public BigDecimal getTotalAmount() {
        return totalAmount;
    }
    
    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }
    
    public Map<String, Long> getTransactionsByType() {
        return transactionsByType;
    }
    
    public void setTransactionsByType(Map<String, Long> transactionsByType) {
        this.transactionsByType = transactionsByType;
    }
    
    public Map<String, BigDecimal> getAmountByType() {
        return amountByType;
    }
    
    public void setAmountByType(Map<String, BigDecimal> amountByType) {
        this.amountByType = amountByType;
    }
    
    public long getPendingTransactions() {
        return pendingTransactions;
    }
    
    public void setPendingTransactions(long pendingTransactions) {
        this.pendingTransactions = pendingTransactions;
    }
    
    public long getCompletedTransactions() {
        return completedTransactions;
    }
    
    public void setCompletedTransactions(long completedTransactions) {
        this.completedTransactions = completedTransactions;
    }
    
    public long getFailedTransactions() {
        return failedTransactions;
    }
    
    public void setFailedTransactions(long failedTransactions) {
        this.failedTransactions = failedTransactions;
    }
    
    public long getCancelledTransactions() {
        return cancelledTransactions;
    }
    
    public void setCancelledTransactions(long cancelledTransactions) {
        this.cancelledTransactions = cancelledTransactions;
    }
    
    @Override
    public String toString() {
        return "TransactionStatistics{" +
                "totalTransactions=" + totalTransactions +
                ", totalAmount=" + totalAmount +
                ", transactionsByType=" + transactionsByType +
                ", amountByType=" + amountByType +
                ", pendingTransactions=" + pendingTransactions +
                ", completedTransactions=" + completedTransactions +
                ", failedTransactions=" + failedTransactions +
                ", cancelledTransactions=" + cancelledTransactions +
                '}';
    }
} 