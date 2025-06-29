package com.banking.transaction.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Entity representing a banking transaction.
 * This class contains all the necessary information for a financial transaction.
 */
public class Transaction {
    
    @NotNull(message = "Transaction ID is required")
    private UUID id;
    
    @NotBlank(message = "Account number is required")
    @Pattern(regexp = "^[A-Z0-9]{10,20}$", message = "Account number must be 10-20 alphanumeric characters")
    private String accountNumber;
    
    @NotBlank(message = "Transaction type is required")
    @Pattern(regexp = "^(DEPOSIT|WITHDRAWAL|TRANSFER)$", message = "Transaction type must be DEPOSIT, WITHDRAWAL, or TRANSFER")
    private String transactionType;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @DecimalMax(value = "999999999.99", message = "Amount cannot exceed 999,999,999.99")
    private BigDecimal amount;
    
    @NotBlank(message = "Description is required")
    @Size(min = 1, max = 255, message = "Description must be between 1 and 255 characters")
    private String description;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    
    @Pattern(regexp = "^[A-Z0-9]{10,20}$", message = "Recipient account must be 10-20 alphanumeric characters")
    private String recipientAccount;
    
    @Pattern(regexp = "^(PENDING|COMPLETED|FAILED|CANCELLED)$", message = "Status must be PENDING, COMPLETED, FAILED, or CANCELLED")
    private String status;
    
    // Default constructor
    public Transaction() {
        this.id = UUID.randomUUID();
        this.timestamp = LocalDateTime.now();
        this.status = "PENDING";
    }
    
    // Constructor with required fields
    public Transaction(String accountNumber, String transactionType, BigDecimal amount, String description) {
        this();
        this.accountNumber = accountNumber;
        this.transactionType = transactionType;
        this.amount = amount;
        this.description = description;
    }
    
    // Getters and Setters
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public String getAccountNumber() {
        return accountNumber;
    }
    
    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }
    
    public String getTransactionType() {
        return transactionType;
    }
    
    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getRecipientAccount() {
        return recipientAccount;
    }
    
    public void setRecipientAccount(String recipientAccount) {
        this.recipientAccount = recipientAccount;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + id +
                ", accountNumber='" + accountNumber + '\'' +
                ", transactionType='" + transactionType + '\'' +
                ", amount=" + amount +
                ", description='" + description + '\'' +
                ", timestamp=" + timestamp +
                ", recipientAccount='" + recipientAccount + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
} 